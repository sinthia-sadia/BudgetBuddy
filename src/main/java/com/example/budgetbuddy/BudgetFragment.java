package com.example.budgetbuddy;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BudgetFragment extends Fragment {

    private EditText weeklyBudgetEditText, monthlyBudgetEditText;
    private TextView budgetSummaryTextView, budgetStatusTextView;
    private RadioGroup timeToggleGroup;
    private Button saveButton, generatePdfButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private double weekly = 0.0;
    private double monthly = 0.0;
    private boolean isMonthly = false;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        TextView userNameTextView = view.findViewById(R.id.userNameTextView);
        checkAndPromptForName(userNameTextView);

        weeklyBudgetEditText = view.findViewById(R.id.weeklyBudgetEditText);
        monthlyBudgetEditText = view.findViewById(R.id.monthlyBudgetEditText);
        budgetSummaryTextView = view.findViewById(R.id.budgetSummaryTextView);
        budgetStatusTextView = view.findViewById(R.id.budgetStatusTextView);
        timeToggleGroup = view.findViewById(R.id.timeToggleGroup);
        saveButton = view.findViewById(R.id.saveButton);
        generatePdfButton = view.findViewById(R.id.generatePdfButton);



        loadBudgets();

        saveButton.setOnClickListener(v -> saveBudgets());

        generatePdfButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                generatePdfReport();
            }
        });

        timeToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isMonthly = (checkedId == R.id.monthlyToggle);
            updateBudgetSummary();
        });

        return view;
    }

    private void loadBudgets() {
        String userId = mAuth.getUid();
        db.collection("budgets").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                if (snapshot.contains("weekly")) {
                    weekly = snapshot.getDouble("weekly");
                    weeklyBudgetEditText.setText(String.valueOf(weekly));
                }
                if (snapshot.contains("monthly")) {
                    monthly = snapshot.getDouble("monthly");
                    monthlyBudgetEditText.setText(String.valueOf(monthly));
                }
                updateBudgetSummary();
            }
        });
    }

    private void saveBudgets() {
        String weeklyStr = weeklyBudgetEditText.getText().toString().trim();
        String monthlyStr = monthlyBudgetEditText.getText().toString().trim();

        weekly = weeklyStr.isEmpty() ? 0.0 : Double.parseDouble(weeklyStr);
        monthly = monthlyStr.isEmpty() ? 0.0 : Double.parseDouble(monthlyStr);

        String userId = mAuth.getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("weekly", weekly);
        data.put("monthly", monthly);

        db.collection("budgets").document(userId).set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Budgets saved", Toast.LENGTH_SHORT).show();
                    updateBudgetSummary();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save budgets", Toast.LENGTH_SHORT).show());
    }

    private void updateBudgetSummary() {
        String type = isMonthly ? "Monthly" : "Weekly";
        double budget = isMonthly ? monthly : weekly;
        budgetSummaryTextView.setText(String.format("%s Budget: Tk %.2f", type, budget));
        updateBudgetStatus();
    }

    private void updateBudgetStatus() {
        String userId = mAuth.getUid();
        db.collection("expenses")
                .document(userId)
                .collection("userExpenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double total = 0.0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Expense expense = doc.toObject(Expense.class);
                        String dateStr = expense.getDate();
                        if (dateStr == null) continue;

                        if ((isMonthly && DateUtils.isCurrentMonth(dateStr)) ||
                                (!isMonthly && DateUtils.isCurrentWeek(dateStr))) {
                            total += expense.getAmount();
                        }
                    }

                    double budget = isMonthly ? monthly : weekly;
                    String status;
                    int color;

                    if (total > budget) {
                        status = "Over Budget!";
                        color = Color.RED;
                    } else if (budget > 0 && total >= 0.9 * budget) {
                        status = "Warning: Near Limit";
                        color = Color.parseColor("#FFA500"); // Orange
                    } else {
                        status = "Within limit";
                        color = Color.GREEN;
                    }

                    budgetStatusTextView.setText(String.format("Spent: Tk %.2f | Status: %s", total, status));
                    budgetStatusTextView.setTextColor(color);
                })
                .addOnFailureListener(e -> budgetStatusTextView.setText("Error loading budget status"));
    }


    private void generatePdfReport() {
        String userId = mAuth.getUid();
        db.collection("expenses")
                .document(userId)
                .collection("userExpenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Expense> expenses = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Expense expense = doc.toObject(Expense.class);
                        String dateStr = expense.getDate();
                        if (dateStr == null) continue;

                        if ((isMonthly && DateUtils.isCurrentMonth(dateStr)) ||
                                (!isMonthly && DateUtils.isCurrentWeek(dateStr))) {
                            expenses.add(expense);
                        }
                    }

                    if (expenses.isEmpty()) {
                        Toast.makeText(getContext(), isMonthly ? "No expenses found for this month" : "No expenses found for this week", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createPdf(expenses);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading expenses", Toast.LENGTH_SHORT).show());
    }

    private void createPdf(List<Expense> expenses) {
        android.graphics.pdf.PdfDocument pdfDocument = new android.graphics.pdf.PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(20);
        titlePaint.setFakeBoldText(true);

        int x = 10, y = 25;
        int pageWidth = 300, pageHeight = 600;

        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        android.graphics.pdf.PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        canvas.drawText(isMonthly ? "Monthly Expense Report" : "Weekly Expense Report", x, y, titlePaint);
        y += 30;

        for (Expense exp : expenses) {
            if (y > pageHeight - 40) {
                pdfDocument.finishPage(page);
                pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 25;
            }

            canvas.drawText("Title: " + exp.getTitle(), x, y, paint); y += 15;
            canvas.drawText("Amount: Tk " + exp.getAmount(), x, y, paint); y += 15;
            canvas.drawText("Category: " + exp.getCategory(), x, y, paint); y += 15;
            canvas.drawText("Date: " + exp.getDate(), x, y, paint); y += 25;
        }

        pdfDocument.finishPage(page);

        File baseDir = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "BudgetBuddy");
        if (!baseDir.exists()) baseDir.mkdirs();

        String fileName = (isMonthly ? "Monthly" : "Weekly") + "_Expense_Report_" + System.currentTimeMillis() + ".pdf";
        File file = new File(baseDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            Toast.makeText(getContext(), "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }

    private void checkAndPromptForName(TextView userNameTextView) {
        String userId = mAuth.getUid();
        if (userId == null) return;

        db.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.contains("name")) {
                String name = snapshot.getString("name");
                userNameTextView.setText("Hello, " + name + "!");
            } else {
                promptForName(userId, userNameTextView);
            }
        });
    }

    private void promptForName(String userId, TextView userNameTextView) {
        EditText input = new EditText(requireContext());
        input.setHint("Enter your name");

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Welcome!")
                .setMessage("Please enter your name:")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("name", name);
                        db.collection("users").document(userId).set(data, SetOptions.merge());
                        userNameTextView.setText("Hello, " + name + "!");
                    } else {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        promptForName(userId, userNameTextView); // Prompt again
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdfReport();
            } else {
                Toast.makeText(getContext(), "Permission denied to write PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
