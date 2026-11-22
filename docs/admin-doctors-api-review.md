# Báo Cáo Thiết Kế Bộ Test Case Kiểm Thử API (BVA)
**Mã nhiệm vụ Jira:** KAN-44  
**Người thực hiện:** Nguyễn Đức Cường  
**Đối tượng kiểm thử:** Module Quản lý Bác sĩ (Admin - Doctor Management)  
**Phạm vi API:**
1. `GET /api/admin/doctors` — Lấy danh sách tất cả bác sĩ trong hệ thống
2. `GET /api/admin/users/doctor-candidates` — Lấy danh sách tài khoản hợp lệ chờ tạo hồ sơ bác sĩ

---

## 1. Mục tiêu
Áp dụng kỹ thuật phân tích giá trị biên (**Boundary Value Analysis - BVA**) và phân hoạch tương đương (**Equivalence Partitioning - EP**) để thiết kế bộ kiểm thử toàn diện cho 2 API đọc dữ liệu của Admin, phát hiện sớm các lỗi về bảo mật xác thực (Authorization), phân quyền (Role-based Access Control), và xử lý dữ liệu biên trong cơ sở dữ liệu.

---

## 2. Phân tích điều kiện & Giá trị biên (BVA Analysis)

### 2.1. API 1: `GET /api/admin/doctors`
* **Xác thực (Authorization Header):**
  * Hợp lệ: Bearer token của user có Role `ADMIN`.
  * Không hợp lệ biên: Token trống (Missing Header), Token sai định dạng (Malformed), Token hết hạn (Expired), Token của Role khác (`PATIENT`, `DOCTOR`).
* **Trạng thái dữ liệu trong CSDL (Database State):**
  * Biên dưới (Min = 0): Bảng `doctors` trống $\rightarrow$ Kỳ vọng HTTP 200 OK, trả về mảng rỗng `[]`.
  * Biên danh sách (Min = 1, Max = N): Có 1 bác sĩ hoặc nhiều bác sĩ $\rightarrow$ Kỳ vọng HTTP 200 OK, trả về mảng danh sách đối tượng Doctor đầy đủ thông tin `id`, `user`, `department`, `specialization`, `available`.

### 2.2. API 2: `GET /api/admin/users/doctor-candidates`
* **Xác thực:** Yêu cầu Role `ADMIN`.
* **Điều kiện lọc nghiệp vụ:**
  * User có `role = 'DOCTOR'` VÀ chưa tồn tại bản ghi trong bảng `doctors`.
  * Biên dữ liệu:
    * Không có ứng viên nào (tất cả user role DOCTOR đã có profile hoặc không có user nào role DOCTOR) $\rightarrow$ Trả về `[]`.
    * Có 1 hoặc nhiều ứng viên hợp lệ $\rightarrow$ Trả về danh sách chứa `id`, `username`, `firstName`, `lastName`, `email`.

---

## 3. Bảng thiết kế Test Cases chi tiết (BVA Test Cases)

| Test Case ID | API Endpoint | Mục tiêu kiểm thử | Dữ liệu đầu vào (Headers / Params) | Phân vùng / Biên (BVA Tag) | Kết quả mong đợi (HTTP Status & Body) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC_DOC_01** | `GET /api/admin/doctors` | Lấy danh sách bác sĩ thành công khi có dữ liệu | Header: `Authorization: Bearer {{ADMIN_TOKEN}}` | Hợp lệ (Normal / BVA Min+) | **200 OK**<br>Mảng JSON danh sách bác sĩ, không rỗng, chứa đầy đủ trường thông tin. |
| **TC_DOC_02** | `GET /api/admin/doctors` | Kiểm tra biên khi hệ thống chưa có bác sĩ nào | Header: `Authorization: Bearer {{ADMIN_TOKEN}}` (DB Empty) | Biên rỗng (BVA Min = 0) | **200 OK**<br>Mảng rỗng `[]`. |
| **TC_DOC_03** | `GET /api/admin/doctors` | Từ chối truy cập khi không truyền token | Header: Không có `Authorization` | Thiếu auth (BVA Missing) | **401 Unauthorized** |
| **TC_DOC_04** | `GET /api/admin/doctors` | Từ chối truy cập khi dùng token sai / hết hạn | Header: `Authorization: Bearer invalid_or_expired_token` | Token sai (BVA Invalid) | **401 Unauthorized** |
| **TC_DOC_05** | `GET /api/admin/doctors` | Từ chối truy cập khi dùng token Role `PATIENT` | Header: `Authorization: Bearer {{PATIENT_TOKEN}}` | Sai quyền (RBAC Boundary) | **403 Forbidden** |
| **TC_DOC_06** | `GET /api/admin/doctors` | Từ chối truy cập khi dùng token Role `DOCTOR` | Header: `Authorization: Bearer {{DOCTOR_TOKEN}}` | Sai quyền (RBAC Boundary) | **403 Forbidden** |
| **TC_DOC_07** | `GET /api/admin/doctors` | Kiểm tra sai phương thức HTTP (Method not allowed) | Method: `POST /api/admin/doctors` (không gửi body/param) | Phương thức sai | **400 Bad Request** hoặc **405 Method Not Allowed** |
| **TC_CAND_01** | `GET /api/admin/users/doctor-candidates` | Lấy danh sách ứng viên bác sĩ thành công | Header: `Authorization: Bearer {{ADMIN_TOKEN}}` | Hợp lệ (Normal) | **200 OK**<br>Mảng các user role DOCTOR chưa có profile. |
| **TC_CAND_02** | `GET /api/admin/users/doctor-candidates` | Kiểm tra khi không có ứng viên nào hợp lệ | Header: `Authorization: Bearer {{ADMIN_TOKEN}}` | Biên rỗng (BVA Min = 0) | **200 OK**<br>Mảng rỗng `[]`. |
| **TC_CAND_03** | `GET /api/admin/users/doctor-candidates` | Không có token xác thực | Header: Không có | Thiếu auth (BVA Missing) | **401 Unauthorized** |
| **TC_CAND_04** | `GET /api/admin/users/doctor-candidates` | Quyền truy cập bị từ chối với Role `PATIENT` | Header: `Authorization: Bearer {{PATIENT_TOKEN}}` | Sai quyền (RBAC Boundary) | **403 Forbidden** |

---

## 4. Kết luận & Minh chứng
* Bộ test case đã bao phủ 100% các biên xác thực, biên phân quyền RBAC và biên dữ liệu rỗng/có dữ liệu của 2 API `GET /api/admin/doctors` và `GET /api/admin/users/doctor-candidates`.
* Toàn bộ test case đã được tích hợp vào bộ sưu tập Postman `postman/DucCuong.postman_collection.json`.
