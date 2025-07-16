package com.example.budgetbuddy;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView userName, userEmail;
    private TextView weeklyExpensesText, weeklyBudgetSetText, weeklyRemainingBudgetText;
    private TextView monthlyExpensesText, monthlyBudgetSetText, monthlyRemainingBudgetText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    private double weeklyBudget = 0.0;
    private double monthlyBudget = 0.0;
    private double weeklyExpenses = 0.0;
    private double monthlyExpenses = 0.0;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userName = view.findViewById(R.id.userName);
        userEmail = view.findViewById(R.id.userEmail);
        monthlyExpensesText = view.findViewById(R.id.monthlyExpensesText);
        monthlyBudgetSetText = view.findViewById(R.id.monthlyBudgetSetText);
        monthlyRemainingBudgetText = view.findViewById(R.id.monthlyRemainingBudgetText);
        weeklyExpensesText = view.findViewById(R.id.weeklyExpensesText);
        weeklyBudgetSetText = view.findViewById(R.id.weeklyBudgetSetText);
        weeklyRemainingBudgetText = view.findViewById(R.id.weeklyRemainingBudgetText);

        Button logoutButton = view.findViewById(R.id.logoutButton);
        Button editProfileButton = view.findViewById(R.id.editProfileButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            userEmail.setText(user.getEmail());
            fetchUserName();
            fetchBudgets();  // also fetches and loads expenses
        }

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        editProfileButton.setOnClickListener(v -> showEditProfileDialog(uid));
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        Button generatePdfButton = view.findViewById(R.id.generatePdfButton);
        generatePdfButton.setOnClickListener(v -> generateProfileSummaryPDF());


        return view;
    }

    private void fetchUserName() {
        DocumentReference userDoc = db.collection("users").document(uid);
        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                if (name != null && !name.isEmpty()) {
                    userName.setText(name);
                }
            }
        });
    }

    private void fetchBudgets() {
        DocumentReference userDoc = db.collection("budgets").document(uid);
        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("weekly")) {
                    weeklyBudget = documentSnapshot.getDouble("weekly");
                }
                if (documentSnapshot.contains("monthly")) {
                    monthlyBudget = documentSnapshot.getDouble("monthly");
                }

                weeklyBudgetSetText.setText("Weekly Budget: Tk" + weeklyBudget);
                monthlyBudgetSetText.setText("Monthly Budget: Tk" + monthlyBudget);

                fetchExpenses();


            }
        });
    }

    private void fetchExpenses() {
        CollectionReference expensesRef = db.collection("expenses").document(uid).collection("userExpenses");

        expensesRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            weeklyExpenses = 0.0;
            monthlyExpenses = 0.0;

            Calendar calendar = Calendar.getInstance();
            int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = calendar.get(Calendar.YEAR);

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String dateStr = doc.getString("date");
                Double amount = doc.getDouble("amount");

                if (dateStr != null && amount != null) {
                    try {
                        Date date = dateFormat.parse(dateStr);
                        if (date != null) {
                            Calendar expenseCal = Calendar.getInstance();
                            expenseCal.setTime(date);
                            int expenseWeek = expenseCal.get(Calendar.WEEK_OF_YEAR);
                            int expenseMonth = expenseCal.get(Calendar.MONTH);
                            int expenseYear = expenseCal.get(Calendar.YEAR);

                            if (expenseYear == currentYear && expenseWeek == currentWeek) {
                                weeklyExpenses += amount;
                            }
                            if (expenseYear == currentYear && expenseMonth == currentMonth) {
                                monthlyExpenses += amount;
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }

            weeklyExpensesText.setText("Weekly Expenses: Tk" + weeklyExpenses);
            monthlyExpensesText.setText("Monthly Expenses: Tk" + monthlyExpenses);

            double remainingWeekly = weeklyBudget - weeklyExpenses;
            double remainingMonthly = monthlyBudget - monthlyExpenses;

            weeklyRemainingBudgetText.setText("Remaining Weekly Budget: Tk" + remainingWeekly);
            monthlyRemainingBudgetText.setText("Remaining Monthly Budget: Tk" + remainingMonthly);
            if (weeklyExpenses > weeklyBudget) {
                NotificationHelper.showNotification(
                        getContext(),
                        "Weekly Budget Exceeded",
                        "You’ve spent Tk" + weeklyExpenses + " out of Tk" + weeklyBudget + ".",
                        1001
                );
            }

            if (monthlyExpenses > monthlyBudget) {
                NotificationHelper.showNotification(
                        getContext(),
                        "Monthly Budget Exceeded",
                        "You’ve spent Tk" + monthlyExpenses + " out of Tk" + monthlyBudget + ".",
                        1002
                );
            }
        });

    }

    private void showEditProfileDialog(String uid) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText nameEt = dialogView.findViewById(R.id.editName);
        EditText phoneEt = dialogView.findViewById(R.id.editPhone);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Profile")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEt.getText().toString().trim();
                    String phone = phoneEt.getText().toString().trim();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", name);
                    updates.put("phone", phone);

                    db.collection("users").document(uid).update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                                userName.setText(name);
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        EditText oldPassEt = dialogView.findViewById(R.id.oldPassword);
        EditText newPassEt = dialogView.findViewById(R.id.newPassword);
        EditText confirmPassEt = dialogView.findViewById(R.id.confirmPassword);

        new AlertDialog.Builder(getContext())
                .setTitle("Change Password")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String oldPass = oldPassEt.getText().toString();
                    String newPass = newPassEt.getText().toString();
                    String confirmPass = confirmPassEt.getText().toString();

                    FirebaseUser user = mAuth.getCurrentUser();

                    if (user == null || user.getEmail() == null) return;

                    if (!newPass.equals(confirmPass)) {
                        Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused -> user.updatePassword(newPass)
                                    .addOnSuccessListener(unused2 -> Toast.makeText(getContext(), "Password updated", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void generateProfileSummaryPDF() {
        String fileName = "Budget_Summary_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(requireContext().getExternalFilesDir(null), fileName);

        PdfDocument pdfDoc = new PdfDocument();
        Paint paint = new Paint();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
        PdfDocument.Page page = pdfDoc.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        paint.setColor(Color.BLACK);
        paint.setTextSize(20f);
        int x = 40, y = 60;

        canvas.drawText("Budget Summary", x, y, paint);
        y += 40;

        paint.setTextSize(14f);
        canvas.drawText("Name: " + userName.getText().toString(), x, y, paint);
        y += 25;
        canvas.drawText("Email: " + userEmail.getText().toString(), x, y, paint);
        y += 40;

        canvas.drawText("Weekly Budget: Tk" + weeklyBudget, x, y, paint);
        y += 25;
        canvas.drawText("Weekly Expenses: Tk" + weeklyExpenses, x, y, paint);
        y += 25;
        canvas.drawText("Remaining Weekly Budget: Tk" + (weeklyBudget - weeklyExpenses), x, y, paint);
        y += 40;

        canvas.drawText("Monthly Budget: Tk" + monthlyBudget, x, y, paint);
        y += 25;
        canvas.drawText("Monthly Expenses: Tk" + monthlyExpenses, x, y, paint);
        y += 25;
        canvas.drawText("Remaining Monthly Budget: Tk" + (monthlyBudget - monthlyExpenses), x, y, paint);

        pdfDoc.finishPage(page);

        try {
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDoc.writeTo(fos);
            Toast.makeText(getContext(), "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            pdfDoc.close();
        }
    }

}
