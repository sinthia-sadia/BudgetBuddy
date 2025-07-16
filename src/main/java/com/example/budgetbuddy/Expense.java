package com.example.budgetbuddy;

public class Expense {
    private String id;           // ✅ Firestore document ID
    private String title;
    private double amount;
    private String category;
    private String date;
    private String note;

    public Expense() {
        // Default constructor required for Firebase
    }

    public Expense(String title, double amount, String category, String date, String note) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Existing getters
    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }

    // Existing setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
