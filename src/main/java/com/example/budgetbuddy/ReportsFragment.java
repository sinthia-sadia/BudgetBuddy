package com.example.budgetbuddy;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsFragment extends Fragment {

    private BarChart barChart;
    private PieChart pieChart;
    private RadioGroup viewToggleGroup;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String currentFilter = "weekly"; // default
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final String[] monthNames = new java.text.DateFormatSymbols().getShortMonths();

    public ReportsFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);
        viewToggleGroup = view.findViewById(R.id.viewToggleGroup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        viewToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.weeklyToggle) {
                currentFilter = "weekly";
            } else if (checkedId == R.id.monthlyToggle) {
                currentFilter = "monthly";
            }
            fetchAndDisplayData();
        });

        fetchAndDisplayData();
        return view;
    }

    private void fetchAndDisplayData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        // For PieChart (category totals this week or month)
        String[] range = getDateRange(currentFilter);
        String startDate = range[0];
        String endDate = range[1];

        // Fetch ALL expenses for time-based BarChart
        db.collection("expenses").document(uid).collection("userExpenses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(getContext(), "No expenses found", Toast.LENGTH_SHORT).show();
                        barChart.clear();
                        pieChart.clear();
                        return;
                    }

                    Map<String, Float> pieCategoryTotals = new HashMap<>();
                    Map<String, Float> timeBasedTotals = new TreeMap<>();
                    Calendar cal = Calendar.getInstance();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String dateStr = doc.getString("date");
                        String category = doc.getString("category");
                        Double amountDouble = doc.getDouble("amount");

                        if (dateStr == null || amountDouble == null) continue;

                        float amount = amountDouble.floatValue();

                        // For PieChart: only add if in current range
                        if (dateStr.compareTo(startDate) >= 0 && dateStr.compareTo(endDate) <= 0) {
                            if (category != null) {
                                pieCategoryTotals.put(category, pieCategoryTotals.getOrDefault(category, 0f) + amount);
                            }
                        }

                        // For BarChart: group all by week or month
                        try {
                            Date date = sdf.parse(dateStr);
                            cal.setTime(date);

                            String key;
                            if ("weekly".equals(currentFilter)) {
                                int week = cal.get(Calendar.WEEK_OF_YEAR);
                                int year = cal.get(Calendar.YEAR);
                                key = String.format(Locale.getDefault(), "%04d-Week%02d", year, week);
                            } else {
                                int month = cal.get(Calendar.MONTH);
                                int year = cal.get(Calendar.YEAR);
                                key = String.format(Locale.getDefault(), "%04d-%02d", year, month);
                            }

                            timeBasedTotals.put(key, timeBasedTotals.getOrDefault(key, 0f) + amount);

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                    setupBarChart(timeBasedTotals);
                    setupPieChart(pieCategoryTotals);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private String[] getDateRange(String type) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        String endDate = sdf.format(calendar.getTime());
        String startDate;

        if ("weekly".equals(type)) {
            // Set to Monday
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = (dayOfWeek + 5) % 7;
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        } else {
            // Set to 1st of month
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        }

        startDate = sdf.format(calendar.getTime());
        return new String[]{startDate, endDate};
    }

    private void setupBarChart(Map<String, Float> dataMap) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Float> entry : dataMap.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            String rawKey = entry.getKey();
            String label;

            if ("weekly".equals(currentFilter)) {
                label = rawKey; // keep "2025-W27" for weekly
            } else {
                // Format like "Jul 2025" for monthly
                String[] parts = rawKey.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1; // monthNames is 0-based
                label = monthNames[month] + " " + year;
            }

            labels.add(label);

            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Total Expenses");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f);
        barChart.setData(barData);

        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setBackgroundColor(Color.parseColor("#121212"));
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
            }
        });
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-20);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularityEnabled(true);

        YAxis leftAxis = barChart.getAxisLeft();
        YAxis rightAxis = barChart.getAxisRight();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(false);
        rightAxis.setTextColor(Color.WHITE);
        rightAxis.setDrawGridLines(false);

        Description desc = new Description();
        desc.setText("");
        barChart.setDescription(desc);

        Legend legend = barChart.getLegend();
        legend.setTextColor(Color.WHITE);

        barChart.setExtraBottomOffset(24f);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void setupPieChart(Map<String, Float> categoryTotals) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Category Breakdown");
        dataSet.setColors(
                Color.parseColor("#66BB6A"),
                Color.parseColor("#29B6F6"),
                Color.parseColor("#FF7043"),
                Color.parseColor("#AB47BC"),
                Color.parseColor("#FFCA28")
        );
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        pieChart.setUsePercentValues(false);
        pieChart.setDrawHoleEnabled(false);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setCenterTextColor(Color.WHITE);
        pieChart.setBackgroundColor(Color.parseColor("#121212"));

        Description desc = new Description();
        desc.setText("");
        pieChart.setDescription(desc);

        Legend legend = pieChart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}
