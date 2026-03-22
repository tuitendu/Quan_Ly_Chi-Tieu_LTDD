# Kiến trúc & Tài liệu kỹ thuật — Expense App

## 1. Cấu trúc màn hình

```
MainActivity
├── DashboardFragment   (mặc định khi mở app)
└── ThongKeFragment     (placeholder)
```

Bottom Navigation: `navDashboard` | `navThongKe`

---

## 2. Database (SQLite)

> **Cách lưu trữ này gọi là: SQLite — Cơ sở dữ liệu quan hệ (Relational Database)**
> - Dữ liệu được tổ chức thành **bảng (table)** gồm **hàng (row)** và **cột (column)**
> - Mỗi hàng = 1 bản ghi (record). Mỗi cột = 1 thuộc tính của bản ghi
> - Truy vấn bằng **SQL** (Structured Query Language)
> - Android tích hợp sẵn SQLite, không cần cài thêm thư viện
> - File DB lưu tại: `data/data/com.example.gk/databases/expense_db`

### Tên DB: `expense_db` | Version: `2`

### Bảng: `expenses`

| Cột | Kiểu | Mô tả |
|---|---|---|
| `id` | INTEGER PK AUTOINCREMENT | Khóa chính |
| `amount` | REAL | Số tiền (VNĐ) |
| `note` | TEXT | Nội dung / mô tả |
| `category` | TEXT | Phân loại — xem bảng dưới |
| `date` | TEXT | Ngày theo định dạng (xem bảng dưới) |

### Giá trị `category` và `date`

| `category` | Ý nghĩa | `date` format |
|---|---|---|
| `"variable"` | Chi phí phát sinh theo ngày | `"yyyy-MM-dd"` (vd: `2026-03-22`) |
| `"fixed"` | Chi phí cố định tháng này | `"yyyy-MM"` (vd: `2026-03`) |
| `"salary"` | Lương tháng | `"yyyy-MM"` (vd: `2026-03`) |
| `"income"` | Thu nhập khác (dự phòng) | `"yyyy-MM-dd"` |

### Dữ liệu mẫu trong bảng `expenses`

| id | amount | note | category | date |
|---|---|---|---|---|
| 1 | 5000000 | Lương tháng 2026-03 | salary | 2026-03 |
| 2 | 1500000 | Tiền trọ | fixed | 2026-03 |
| 3 | 800000 | Điện nước | fixed | 2026-03 |
| 4 | 50000 | Cà phê sáng | variable | 2026-03-22 |
| 5 | 120000 | Ăn trưa | variable | 2026-03-22 |
| 6 | 5000000 | Lương tháng 2026-04 | salary | 2026-04 |
| 7 | 1500000 | Tiền trọ | fixed | 2026-04 |
| 8 | 200000 | Đi di chơi | variable | 2026-04-01 |

> **Số dư** = (5M + 5M) − (1.5M + 0.8M + 0.05M + 0.12M + 1.5M + 0.2M) = **5,830,000 đ**

### Các hàm trong `DatabaseHelper.java`

| Hàm | Mô tả |
|---|---|
| `addExpense(amount, note, category, date)` | Thêm bản ghi mới |
| `deleteExpense(id)` | Xóa theo `id` |
| `updateExpenseAmount(id, newAmount)` | Sửa số tiền theo `id` |
| `getFixedExpenses()` | Lấy tất cả `category="fixed"` (mọi tháng — dùng cho tính balance) |
| `getFixedExpensesForMonth(month)` | Lấy `category="fixed"` của đúng tháng `month` (yyyy-MM) — dùng cho UI tab |
| `getVariableExpenses(date)` | Lấy `category="variable"` của ngày `date` |
| `getIncomes()` | Lấy tất cả `category="income"` |
| `getSalaryForMonth(month)` | Lấy lương của tháng `month` (yyyy-MM), trả về `Expense` hoặc `null` |
| `getTotalForDate(date)` | Tổng variable theo `date` + tổng tất cả fixed |
| `getTotalVariableForDate(date)` | Chỉ tổng variable theo `date` |
| `getTotalIncome()` | Tổng tất cả income |
| `getTotalBalanceAllTime()` | **Số dư tổng** = Σ(salary) − Σ(variable + fixed) |

---

## 3. SharedPreferences

| Key | Giá trị | Dùng ở đâu |
|---|---|---|
| File: `expense_prefs` | | |
| ~~`salary`~~ | ~~Lương (cũ)~~ | ~~Không còn dùng — đã chuyển sang DB~~ |

> Lương hiện tại lưu trong bảng `expenses` với `category="salary"`.

---

## 4. XML IDs — `fragment_dashboard.xml`

| ID | Widget | Mô tả |
|---|---|---|
| `textBalance` | `TextView` | Hiển thị số dư hiện tại (tất cả tháng) |
| `editSalary` | `EditText` | Nhập lương tháng này |
| `buttonUpdateSalary` | `Button` | "Cập nhật" nếu chưa có lương tháng này, "Sửa" nếu đã có |
| `editAmount` | `EditText` | Nhập số tiền khi thêm chi tiêu |
| `editNote` | `EditText` | Nhập nội dung khi thêm chi tiêu |
| `spinnerCategory` | `Spinner` | Chọn loại: "Phát sinh" → `variable` / "Cố định" → `fixed` |
| `buttonAdd` | `Button` | Xác nhận thêm chi tiêu |
| `tabChiTieu` | `TextView` | Tab "CHI TIÊU HÔM NAY" |
| `tabThuNhap` | `TextView` | Tab "CHI PHÍ CỐ ĐỊNH" — hiện fixed của **tháng hiện tại** |
| `recyclerExpenses` | `RecyclerView` | Danh sách chi tiêu / chi phí cố định |
| `textTotalToday` | `TextView` | Footer: Tổng chi tiêu hôm nay |
| `textRemaining` | `TextView` | Footer: Số dư ròng hiện tại |

## 5. XML IDs — `item_expense.xml` (mỗi item trong RecyclerView)

| ID | Widget | Nội dung hiển thị |
|---|---|---|
| `textNote` | `TextView` | Nội dung / mô tả khoản chi |
| `textAmount` | `TextView` | Số tiền (format: `1,000,000 đ`) |
| `textTime` | `TextView` | Ngày (`yyyy-MM-dd`) nếu variable · `"Hằng tháng"` nếu fixed |

> **Click vào item** → AlertDialog "Bạn muốn xóa khoản X không?" → Xóa → reload danh sách + cập nhật số dư.

## 6. XML IDs — `activity_main.xml`

| ID | Widget | Mô tả |
|---|---|---|
| `toolbar` | `Toolbar` | Thanh tiêu đề trên cùng |
| `fragmentContainer` | `FrameLayout` | Vùng chứa fragment hiện tại |
| `bottomNav` | `BottomNavigationView` | Điều hướng dưới màn hình |

---

## 7. Logic cốt lõi

### Số dư hiện tại
```
Số dư = Σ tất cả lương (mọi tháng) − Σ tất cả chi tiêu (variable + fixed)
```

### Nút "Cập nhật" / "Sửa"
```
Mở fragment → getSalaryForMonth("yyyy-MM")
  ├── Null   → setText("Cập nhật"), field trống
  └── Có     → setText("Sửa"), field điền sẵn số tiền cũ

Bấm nút:
  ├── currentSalaryId == -1 → addExpense(..., "salary", "yyyy-MM") → nút = "Sửa"
  └── currentSalaryId != -1 → updateExpenseAmount(id, newAmount)
```

### Tab hoạt động
```
tabChiTieu  → getVariableExpenses(today)             → danh sách phát sinh hôm nay
tabThuNhap  → getFixedExpensesForMonth(currentMonth) → danh sách cố định của THÁNG NÀY
```
