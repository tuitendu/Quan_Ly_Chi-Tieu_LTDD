package com.example.gk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "expense_db";
    private static final int DB_VERSION = 2;
    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_ID = "id";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_NOTE = "note";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_EXPENSES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_AMOUNT + " REAL, "
                + COL_NOTE + " TEXT, "
                + COL_CATEGORY + " TEXT, "
                + COL_DATE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion >= 2) {
            String currentMonth = new java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                    .format(new java.util.Date());
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(COL_DATE, currentMonth);
            db.update(TABLE_EXPENSES, values,
                    COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                    new String[]{"fixed", ""});
        }
    }

    public long addExpense(double amount, String note, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMOUNT, amount);
        values.put(COL_NOTE, note);
        values.put(COL_CATEGORY, category);
        values.put(COL_DATE, date);
        long result = db.insert(TABLE_EXPENSES, null, values);
        db.close();
        return result;
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void migrateOldFixedExpenses(String currentMonth) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DATE, currentMonth);
        db.update(TABLE_EXPENSES, values,
                COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                new String[]{"fixed", ""});
        db.close();
    }

    public void updateExpenseAmount(int id, double newAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMOUNT, newAmount);
        db.update(TABLE_EXPENSES, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Expense getSalaryForMonth(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                new String[]{"salary", month},
                null, null, null, "1");
        Expense result = null;
        if (cursor.moveToFirst()) {
            result = cursorToExpense(cursor);
        }
        cursor.close();
        db.close();
        return result;
    }

    public double getTotalBalanceAllTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        double totalSalary = 0;
        double totalExpenses = 0;

        Cursor c1 = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                        + " WHERE " + COL_CATEGORY + "=?",
                new String[]{"salary"});
        if (c1.moveToFirst() && !c1.isNull(0)) totalSalary = c1.getDouble(0);
        c1.close();

        Cursor c2 = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                        + " WHERE " + COL_CATEGORY + " IN ('variable', 'fixed')",
                null);
        if (c2.moveToFirst() && !c2.isNull(0)) totalExpenses = c2.getDouble(0);
        c2.close();

        db.close();
        return totalSalary - totalExpenses;
    }

    public List<Expense> getFixedExpenses() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_CATEGORY + "=?", new String[]{"fixed"},
                null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Expense> getFixedExpensesForMonth(String month) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                new String[]{"fixed", month},
                null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Expense> getVariableExpenses(String date) {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                new String[]{"variable", date},
                null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public double getTotalForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;

        Cursor c1 = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                        + " WHERE " + COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                new String[]{"variable", date});
        if (c1.moveToFirst() && !c1.isNull(0)) {
            total += c1.getDouble(0);
        }
        c1.close();

        Cursor c2 = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                        + " WHERE " + COL_CATEGORY + "=?",
                new String[]{"fixed"});
        if (c2.moveToFirst() && !c2.isNull(0)) {
            total += c2.getDouble(0);
        }
        c2.close();
        db.close();
        return total;
    }

    public double getTotalVariableForDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                        + " WHERE " + COL_CATEGORY + "=? AND " + COL_DATE + "=?",
                new String[]{"variable", date});
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public List<Expense> getIncomes() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null,
                COL_CATEGORY + "=?", new String[]{"income"},
                null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToExpense(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public double getTotalIncome() {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0;
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES
                        + " WHERE " + COL_CATEGORY + "=?",
                new String[]{"income"});
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    private Expense cursorToExpense(Cursor cursor) {
        Expense e = new Expense();
        e.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)));
        e.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        e.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)));
        e.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        e.setDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)));
        return e;
    }
}
