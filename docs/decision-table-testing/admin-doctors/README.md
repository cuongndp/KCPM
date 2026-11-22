# Thiết Kế Kiểm Thử EP, BVA & Bảng Quyết Định (Decision Table)
**Mã nhiệm vụ Jira:** KAN-75  
**Thành viên thực hiện:** Nguyễn Đức Cường  
**Module:** Admin Doctor APIs  
**Phạm vi API:**
1. `POST /api/admin/doctors?departmentId={departmentId}` — Tạo mới hồ sơ bác sĩ
2. `DELETE /api/admin/doctors/{doctorId}` — Xóa hồ sơ bác sĩ

---

## 1. Phân hoạch lớp tương đương (EP) & Phân tích giá trị biên (BVA)

### 1.1. Tham số của API `POST /api/admin/doctors`
* `departmentId` (Query Param, Long):
  * **Hợp lệ:** ID khoa tồn tại trong CSDL (`departmentId > 0`).
  * **Không hợp lệ:** Khoa không tồn tại (`departmentId = 99999`), số âm (`departmentId = -1`), bằng 0, không đúng định dạng số (`departmentId = "abc"`).
* `userId` (Body, Long, `@NotNull`):
  * **Hợp lệ:** User tồn tại, có role `DOCTOR`, chưa có hồ sơ bác sĩ.
  * **Không hợp lệ:** `userId = null` (lỗi 400), user không tồn tại (404), user có role khác `DOCTOR` (`PATIENT`, `ADMIN` $\rightarrow$ 400), user đã có hồ sơ bác sĩ (409 Conflict).
* `specialization` (Body, String, `@NotBlank`):
  * **Hợp lệ:** Chuỗi không rỗng (1 đến 255 ký tự).
  * **Biên không hợp lệ:** `null`, rỗng `""`, chuỗi chỉ có khoảng trắng `"   "`.
* `experience` (Body, Integer):
  * **Hợp lệ:** Số nguyên dương $\ge 0$ (ví dụ: 0, 1, 5, 40).
  * **Biên:** Min = 0 (vừa tốt nghiệp), số âm (-1).

### 1.2. Tham số của API `DELETE /api/admin/doctors/{doctorId}`
* `doctorId` (Path Variable, Long):
  * **Hợp lệ:** Bác sĩ tồn tại và chưa có ràng buộc dữ liệu (chưa có cuộc hẹn, bệnh án hay đơn thuốc).
  * **Không hợp lệ:** Bác sĩ không tồn tại (`doctorId = 99999` $\rightarrow$ 404), bác sĩ đã có dữ liệu liên quan (lịch hẹn/bệnh án $\rightarrow$ 409 Conflict).

---

## 2. Bảng quyết định (Decision Table)

### 2.1. Decision Table cho API Tạo Bác Sĩ (`POST /api/admin/doctors`)

| Điều kiện (Conditions) | R1 | R2 | R3 | R4 | R5 | R6 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **C1:** Token Role ADMIN hợp lệ? | **F** | **T** | **T** | **T** | **T** | **T** |
| **C2:** Khoa (`departmentId`) tồn tại? | - | **F** | **T** | **T** | **T** | **T** |
| **C3:** Tài khoản (`userId`) tồn tại? | - | - | **F** | **T** | **T** | **T** |
| **C4:** Tài khoản có Role `DOCTOR`? | - | - | - | **F** | **T** | **T** |
| **C5:** Tài khoản CHƯA có hồ sơ bác sĩ? | - | - | - | - | **F** | **T** |
| **C6:** `specialization` không rỗng? | - | - | - | - | - | **T** |
| **Hành động (Actions)** | | | | | | |
| **A1:** Tạo hồ sơ thành công (HTTP 200/201) | | | | | | **X** |
| **A2:** Trả về HTTP 401/403 (Auth/RBAC Error) | **X** | | | | | |
| **A3:** Trả về HTTP 404 Not Found (Khoa/User) | | **X** | **X** | | | |
| **A4:** Trả về HTTP 400 Bad Request (Role/Validation) | | | | **X** | | |
| **A5:** Trả về HTTP 409 Conflict (Profile Exist) | | | | | **X** | |

### 2.2. Decision Table cho API Xóa Bác Sĩ (`DELETE /api/admin/doctors/{doctorId}`)

| Điều kiện (Conditions) | R1 | R2 | R3 | R4 |
| :--- | :---: | :---: | :---: | :---: |
| **C1:** Token Role ADMIN hợp lệ? | **F** | **T** | **T** | **T** |
| **C2:** `doctorId` tồn tại trong hệ thống? | - | **F** | **T** | **T** |
| **C3:** Bác sĩ ĐÃ CÓ lịch hẹn / bệnh án / đơn thuốc? | - | - | **T** | **F** |
| **Hành động (Actions)** | | | | |
| **A1:** Xóa bác sĩ thành công (HTTP 200 OK) | | | | **X** |
| **A2:** Trả về HTTP 401/403 (Auth Error) | **X** | | | |
| **A3:** Trả về HTTP 404 Not Found | | **X** | | |
| **A4:** Trả về HTTP 409 Conflict ("Không thể xóa...") | | | **X** | |

---

## 3. Danh sách Test Cases theo Decision Table
Tất cả các Rule trong 2 bảng quyết định trên đã được chuyển đổi đầy đủ thành các test case trong file `decision_testcases.csv` và thực thi trên Postman Collection Runner.
