package com.example.budgetbuddy;

import android.app.DatePickerDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddExpenseFragment extends Fragment {

    private EditText titleEditText, amountEditText, noteEditText;
    private Spinner categorySpinner;
    private TextView dateTextView;
    private Button addExpenseButton;
    private RecyclerView recentExpenseRecyclerView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private RecentExpenseAdapter recentExpenseAdapter;
    private List<Expense> expenseList;

    private String selectedDate = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_expense, container, false);

        titleEditText = view.findViewById(R.id.titleEditText);
        amountEditText = view.findViewById(R.id.amountEditText);
        noteEditText = view.findViewById(R.id.noteEditText);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        dateTextView = view.findViewById(R.id.dateTextView);
        addExpenseButton = view.findViewById(R.id.addExpenseButton);
        recentExpenseRecyclerView = view.findViewById(R.id.recentExpenseRecyclerView);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Category Spinner setup (optional if not done in XML)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.expense_categories,  // <- define in res/values/strings.xml
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        expenseList = new ArrayList<>();
        recentExpenseAdapter = new RecentExpenseAdapter(requireContext(), expenseList);
        recentExpenseRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recentExpenseRecyclerView.setAdapter(recentExpenseAdapter);

        loadRecentExpenses();

        dateTextView.setOnClickListener(v -> showDatePicker());

        addExpenseButton.setOnClickListener(v -> addExpense());

        return view;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = sdf.format(calendar.getTime());
                    dateTextView.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void addExpense() {
        String title = titleEditText.getText().toString().trim();
        String amount = amountEditText.getText().toString().trim();
        String category = categorySpinner.getSelectedItem() != null
                ? categorySpinner.getSelectedItem().toString()
                : "Other";
        String note = noteEditText.getText().toString().trim();
        String date = !TextUtils.isEmpty(selectedDate)
                ? selectedDate
                : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amount)) {
            Toast.makeText(getContext(), "Please enter title and amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amountValue;
        try {
            amountValue = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid amount entered", Toast.LENGTH_SHORT).show();
            return;
        }

        Expense expense = new Expense(title, amountValue, category, date, note);
        db.collection("expenses")
                .document(mAuth.getUid())
                .collection("userExpenses")
                .add(expense)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Expense added", Toast.LENGTH_SHORT).show();
                    titleEditText.setText("");
                    amountEditText.setText("");
                    noteEditText.setText("");
                    selectedDate = "";
                    dateTextView.setText("Select Date");
                    loadRecentExpenses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error adding expense", Toast.LENGTH_SHORT).show());
    }

    private void loadRecentExpenses() {
        db.collection("expenses")
                .document(mAuth.getUid())
                .collection("userExpenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    expenseList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Expense expense = doc.toObject(Expense.class);
                        expense.setId(doc.getId());
                        expenseList.add(expense);
                    }
                    recentExpenseAdapter.notifyDataSetChanged();
                });
    }

}
