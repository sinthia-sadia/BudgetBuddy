package com.example.budgetbuddy;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private Spinner filterSpinner;
    private RecyclerView expensesRecyclerView;
    private TextView totalTextView, alertTextView;
    private FirebaseFirestore db;
    private String userId;
    private RecentExpenseAdapter adapter;
    private List<Expense> expenseList = new ArrayList<>();
    private String currentFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        filterSpinner = view.findViewById(R.id.filterSpinner);
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView);
        totalTextView = view.findViewById(R.id.totalTextView);
        alertTextView = view.findViewById(R.id.alertTextView);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new RecentExpenseAdapter(getContext(), expenseList);
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expensesRecyclerView.setAdapter(adapter);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.filter_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = parent.getItemAtPosition(position).toString();
                // Trigger re-filtering
                loadExpenses(currentFilter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        setupRealtimeListener();
        return view;
    }
    private void setupRealtimeListener() {
        db.collection("expenses")
                .document(userId)
                .collection("userExpenses")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Toast.makeText(getContext(), "Failed to load expenses", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    expenseList.clear();
                    double total = 0.0;

                    Calendar todayCal = Calendar.getInstance();
                    int currentWeek = todayCal.get(Calendar.WEEK_OF_YEAR);
                    int currentMonth = todayCal.get(Calendar.MONTH);
                    int currentDay = todayCal.get(Calendar.DAY_OF_YEAR);
                    int currentYear = todayCal.get(Calendar.YEAR);

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String dateStr = doc.getString("date");
                        if (dateStr == null) continue;

                        try {
                            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                            if (date == null) continue;

                            Calendar expenseCal = Calendar.getInstance();
                            expenseCal.setTime(date);

                            boolean include = false;
                            switch (currentFilter) {
                                case "Today":
                                    include = expenseCal.get(Calendar.DAY_OF_YEAR) == currentDay &&
                                            expenseCal.get(Calendar.YEAR) == currentYear;
                                    break;
                                case "This Week":
                                    include = expenseCal.get(Calendar.WEEK_OF_YEAR) == currentWeek &&
                                            expenseCal.get(Calendar.YEAR) == currentYear;
                                    break;
                                case "This Month":
                                    include = expenseCal.get(Calendar.MONTH) == currentMonth &&
                                            expenseCal.get(Calendar.YEAR) == currentYear;
                                    break;
                                case "All":
                                    include = true;
                                    break;
                            }

                            if (include) {
                                Expense expense = doc.toObject(Expense.class);
                                expense.setId(doc.getId());
                                expenseList.add(expense);
                                total += expense.getAmount();
                            }

                        } catch (ParseException ignored) {
                        }
                    }

                    adapter.notifyDataSetChanged();
                    totalTextView.setText("Total: ৳" + total);
                    checkBudgetLimit(total);
                });
    }


    private void checkBudgetLimit(double totalThisMonth) {
        db.collection("budgets").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    Double monthlyLimit = doc.getDouble("monthly");
                    if (monthlyLimit == null || monthlyLimit == 0) {
                        alertTextView.setText("No monthly budget set.");
                        alertTextView.setTextColor(Color.GRAY);
                        return;
                    }

                    String alertMsg;
                    int alertColor;

                    if (totalThisMonth >= monthlyLimit) {
                        alertMsg = "Alert: Monthly budget exceeded!";
                        alertColor = Color.RED;
                    } else if (totalThisMonth >= 0.9 * monthlyLimit) {
                        alertMsg = "Warning: Approaching monthly budget";
                        alertColor = Color.YELLOW;
                    } else {
                        alertMsg = "Spending within monthly budget";
                        alertColor = Color.GREEN;
                    }

                    alertTextView.setText(alertMsg);
                    alertTextView.setTextColor(alertColor);
                });
    }

    private void loadExpenses(String filter) {
        // Just trigger re-filtering using latest snapshot from real-time listener
        setupRealtimeListener();
    }
}
