# Giai thich file va kien truc du an — Expense App

---

## Kien truc tong quan

```
GK/
├── app/src/main/
│   ├── AndroidManifest.xml          <- Khai bao app (Activity, quyen, v.v.)
│   ├── java/com/example/gk/
│   │   ├── MainActivity.java        <- Activity chinh, quan ly navigation
│   │   ├── DashboardFragment.java   <- Man hinh chinh (logic phuc tap)
│   │   ├── ThongKeFragment.java     <- Man hinh thong ke (placeholder)
│   │   ├── DatabaseHelper.java      <- Quan ly SQLite (CRUD)
│   │   ├── Expense.java             <- Model du lieu (1 ban ghi)
│   │   └── ExpenseAdapter.java      <- Adapter cho RecyclerView
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml    <- Layout nen (toolbar + frame + bottomnav)
│       │   ├── fragment_dashboard.xml <- Giao dien chinh
│       │   ├── fragment_thong_ke.xml  <- Giao dien thong ke
│       │   └── item_expense.xml     <- 1 hang trong danh sach
│       ├── menu/nav_menu.xml        <- Menu bottom navigation
│       ├── drawable/                <- Icon SVG cho bottom nav
│       └── values/                  <- colors, strings, themes
├── ltdd.txt                         <- Tai lieu y tuong + thiet ke
└── kientruc.md                      <- Tai lieu ky thuat chi tiet
```

**Luong du lieu:**
```
User nhap -> DashboardFragment -> DatabaseHelper -> SQLite DB
SQLite DB -> DatabaseHelper -> DashboardFragment -> ExpenseAdapter -> RecyclerView -> User xem
```

---

## 1. MainActivity.java

**Vai tro:** Activity chinh — "khung" chua toan bo app. Khoi dong Fragment dau tien va xu ly viec chuyen tab.

**Code giai thich:**

```java
// Extend AppCompatActivity: ke thua cac tinh nang Activity hien dai
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Gan layout activity_main.xml lam giao dien cho Activity nay
        setContentView(R.layout.activity_main);

        // Thiet lap thanh Toolbar tren cung lam ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Tim BottomNavigationView (thanh tab phia duoi)
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Mo Dashboard ngay khi khoi dong app
        loadFragment(new DashboardFragment());

        // Lang nghe su kien khi nguoi dung bam vao tab
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navDashboard) {
                loadFragment(new DashboardFragment());
                return true;
            } else if (itemId == R.id.navThongKe) {
                loadFragment(new ThongKeFragment());
                return true;
            }
            return false;
        });
    }

    // Ham tien ich: thay the Fragment trong khung frameContainer
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()         // Bat dau 1 transaction
                .replace(R.id.frameContainer, fragment) // Thay the noi dung
                .commit();                  // Thuc thi
    }
}
```

**Diem chinh:**
- Chi co 1 Activity — cac man hinh dung Fragment de tranh tao nhieu Activity
- `loadFragment()` thay the noi dung trong `frameContainer` moi lan doi tab

---

## 2. Expense.java

**Vai tro:** Model (Plain Old Java Object) — dai dien cho 1 ban ghi trong bang `expenses`.

**Code giai thich:**

```java
public class Expense {
    // 5 truong tuong ung 5 cot trong bang SQLite
    private int id;          // Khoa chinh, tu dong tang
    private double amount;   // So tien (VND)
    private String note;     // Mo ta / noi dung
    private String category; // Loai: "variable" | "fixed" | "salary" | "income"
    private String date;     // Ngay, format khac nhau tuy category

    // Constructor rong: dung khi tao object roi set tung truong
    public Expense() {}

    // Constructor day du: dung khi co du du lieu san
    public Expense(int id, double amount, String note, String category, String date) { ... }

    // Getter/Setter cho moi truong (chuan Java Bean)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    // ... tuong tu cho cac truong con lai
}
```

**Diem chinh:**
- Khong co logic nghiep vu — chi la "tui chua du lieu"
- Duoc dung khap noi: DatabaseHelper tra ve `List<Expense>`, Adapter nhan `List<Expense>`

---

## 3. DatabaseHelper.java

**Vai tro:** Lop quan ly SQLite. Ke thua `SQLiteOpenHelper` — Android tu goi `onCreate` lan dau va `onUpgrade` khi tang version.

**Code giai thich:**

```java
public class DatabaseHelper extends SQLiteOpenHelper {

    // Hang so
    private static final String DB_NAME = "expense_db";  // Ten file DB
    private static final int DB_VERSION = 2;              // Version hien tai
    private static final String TABLE_EXPENSES = "expenses";
    // Cac hang so ten cot
    private static final String COL_ID = "id";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_NOTE = "note";
    private static final String COL_CATEGORY = "category";
    private static final String COL_DATE = "date";

    // Tao bang lan dau cai app
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE expenses ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "amount REAL, note TEXT, category TEXT, date TEXT)";
        db.execSQL(createTable);
    }

    // Chay khi tang DB_VERSION — migrate du lieu cu thay vi xoa
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion >= 2) {
            // Cap nhat fixed cu (date="") thanh thang hien tai
            String currentMonth = ...; // yyyy-MM
            db.update("expenses", values, "category=? AND date=?",
                    new String[]{"fixed", ""});
        }
    }

    // --- CRUD ---

    // Them 1 ban ghi moi, tra ve id vua tao
    public long addExpense(double amount, String note, String category, String date) { ... }

    // Xoa theo id
    public void deleteExpense(int id) { ... }

    // Sua so tien cua 1 ban ghi theo id (dung cho cap nhat luong)
    public void updateExpenseAmount(int id, double newAmount) { ... }

    // Migration: update fixed co date="" thanh currentMonth
    public void migrateOldFixedExpenses(String currentMonth) { ... }

    // --- QUERY ---

    // Lay tat ca fixed (moi thang) — dung cho tinh tong balance
    public List<Expense> getFixedExpenses() { ... }

    // Lay fixed cua dung thang yyyy-MM — dung cho hien thi tab
    public List<Expense> getFixedExpensesForMonth(String month) { ... }

    // Lay variable cua 1 ngay cu the yyyy-MM-dd
    public List<Expense> getVariableExpenses(String date) { ... }

    // Lay income (thu nhap khac, du phong)
    public List<Expense> getIncomes() { ... }

    // Lay salary cua 1 thang — tra ve null neu chua co
    public Expense getSalaryForMonth(String month) { ... }

    // --- TINH TONG ---

    // Tong chi tieu 1 ngay (variable ngay do + tat ca fixed)
    public double getTotalForDate(String date) { ... }

    // Chi tong variable cua 1 ngay (dung hien footer)
    public double getTotalVariableForDate(String date) { ... }

    // Tong tat ca income
    public double getTotalIncome() { ... }

    // So du toan thoi gian = SUM(salary) - SUM(variable + fixed)
    public double getTotalBalanceAllTime() { ... }

    // Ham noi bo: doc 1 row tu Cursor thanh object Expense
    private Expense cursorToExpense(Cursor cursor) { ... }
}
```

**Diem chinh:**
- Moi lan lay/ghi du lieu: mo DB -> thao tac -> dong DB ngay
- `cursorToExpense()` la ham tien ich dung chung cho cac query

---

## 4. ExpenseAdapter.java

**Vai tro:** Adapter cua RecyclerView — nhan `List<Expense>` va "ban" tung item len giao dien.

**Code giai thich:**

```java
// Ke thua RecyclerView.Adapter voi ViewHolder la ExpenseViewHolder
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    // Interface callback: Fragment goi ham nay khi user bam vao item
    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    private List<Expense> expenseList;
    private OnItemClickListener listener;

    // Constructor nhan danh sach va callback
    public ExpenseAdapter(List<Expense> expenseList, OnItemClickListener listener) { ... }

    // Tao 1 View moi khi RecyclerView can them item (inflate item_expense.xml)
    @Override
    public ExpenseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    // Gan du lieu vao View tai vi tri 'position'
    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.textNote.setText(expense.getNote());
        holder.textAmount.setText(String.format("%,.0f d", expense.getAmount()));

        // Hien thi dong thoi gian khac nhau tuy category
        switch (expense.getCategory()) {
            case "fixed":
                holder.textTime.setText("Hang thang"); // Co dinh
                break;
            default:
                holder.textTime.setText(expense.getDate()); // Ngay cu the
        }

        // Khi bam vao item -> goi callback de Fragment xu ly (hien dialog xoa)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(expense);
        });
    }

    // ViewHolder: "giu" tham chieu toi cac TextView de tranh tim lai nhieu lan
    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView textNote, textAmount, textTime;

        ExpenseViewHolder(View itemView) {
            super(itemView);
            textNote = itemView.findViewById(R.id.textNote);
            textAmount = itemView.findViewById(R.id.textAmount);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}
```

**Diem chinh:**
- Adapter khong biet gi ve xoa — no chi goi callback, de Fragment quyet dinh
- `ViewHolder` giup tai su dung View, tranh lag khi scroll

---

## 5. DashboardFragment.java

**Vai tro:** Fragment chinh — chua toan bo logic man hinh Dashboard.

**Cac thanh phan ben trong:**

| Bien | Y nghia |
|---|---|
| `currentDate` | Ngay hien tai yyyy-MM-dd dung lam key cho variable |
| `currentMonth` | Thang hien tai yyyy-MM dung lam key cho fixed va salary |
| `currentSalaryId` | ID cua ban ghi luong thang nay (-1 neu chua co) |
| `isShowingFixed` | `false` = hien tab chi tieu hom nay / `true` = hien tab co dinh |
| `buttonUpdateSalary` | Bien field de co the thay doi text ("Cap nhat"/"Sua") o moi noi |

**Cac ham chinh:**

```
onCreateView()        <- Khoi tao UI, gan event, goi checkSalaryThisMonth
checkSalaryThisMonth()<- Kiem tra DB co luong thang nay chua -> doi text nut
loadData()            <- Load danh sach tuong ung tab dang hien
addExpense()          <- Validate + them chi tieu vao DB
updateSalary()        <- INSERT hoac UPDATE luong trong DB
updateSummary()       <- Tinh so du + cap nhat footer
updateTabUI()         <- Doi mau chu tab dang chon / khong chon
formatCurrency()      <- Format so thanh "1,000,000 d"
```

**Flow them chi tieu:**
```
User nhap so tien + noi dung + chon loai -> bam "+ Them"
-> addExpense() kiem tra hop le
-> category = "variable" hoac "fixed"
-> date = currentDate (neu variable) hoac currentMonth (neu fixed)
-> dbHelper.addExpense(amount, note, category, date)
-> loadData() cap nhat danh sach
-> updateSummary() cap nhat so du
```

**Flow cap nhat luong:**
```
Lan dau: currentSalaryId == -1 -> INSERT moi -> nut doi thanh "Sua"
Lan sau: currentSalaryId != -1 -> UPDATE ban ghi cu -> thong bao cap nhat
```

---

## 6. ThongKeFragment.java

**Vai tro:** Placeholder — chi inflate layout, chua co logic.

```java
// Chi inflate layout fragment_thong_ke.xml, khong co gi khac
public View onCreateView(...) {
    return inflater.inflate(R.layout.fragment_thong_ke, container, false);
}
```

---

## 7. Layout XML

### activity_main.xml
```
LinearLayout (vertical)
├── Toolbar (id: toolbar)         <- Thanh tieu de tren cung
├── FrameLayout (id: frameContainer) <- Noi hien thi Fragment
└── BottomNavigationView (id: bottomNav) <- Tab bar duoi cung
```

### fragment_dashboard.xml
```
NestedScrollView
└── LinearLayout (vertical)
    ├── Card: So du hien tai
    │   └── textBalance
    ├── Card: Luong & Thu nhap
    │   ├── editSalary + buttonUpdateSalary
    ├── Card: Them chi tieu nhanh
    │   ├── editAmount + editNote
    │   └── spinnerCategory + buttonAdd
    ├── LinearLayout: Tab bar
    │   ├── tabChiTieu (TextView)
    │   └── tabThuNhap (TextView)
    ├── RecyclerView (id: recyclerExpenses)
    └── LinearLayout: Footer
        ├── textTotalToday
        └── textRemaining
```

### item_expense.xml
```
LinearLayout (1 hang trong danh sach, clickable)
├── LinearLayout (vertical, weight=1)
│   ├── textNote  <- Ten khoan chi
│   └── textTime  <- Ngay hoac "Hang thang"
└── textAmount    <- So tien ben phai
```

### nav_menu.xml
```
menu
├── item id=navDashboard  title="Dashboard"
└── item id=navThongKe    title="Thong ke"
```

---

## 8. res/values/

| File | Noi dung |
|---|---|
| `colors.xml` | Dinh nghia cac mau: colorBackground, colorCard, colorPrimary, colorButtonAccent, colorTextPrimary, colorTextSecondary, colorDivider |
| `themes.xml` | Theme chinh: `Theme.Material3.Dark.NoActionBar` — Dark mode toan app |
| `values-night/themes.xml` | Theme night mode — giong themes.xml (ca hai cung Dark) |
| `strings.xml` | Chuoi van ban dung trong app |
