# Báo Cáo Thiết Kế Kiểm Thử Hộp Đen (Black-box Testing)
**Dự án:** Quản lý bệnh viện (HMS)  
**Phân hệ:** Quản trị viên (Admin - Doctor Management)  
**Thành viên phụ trách:** Nguyễn Đức Cường  
**Mã nhiệm vụ:** KAN-44, KAN-57, KAN-75, KAN-87  

---

## 1. Phạm vi API kiểm thử
1. `GET /api/admin/doctors` (Lấy danh sách bác sĩ)
2. `GET /api/admin/users/doctor-candidates` (Lấy danh sách ứng viên bác sĩ)
3. `POST /api/admin/doctors?departmentId={departmentId}` (Thêm hồ sơ bác sĩ mới)
4. `PATCH /api/admin/doctors/{doctorId}/availability` (Bật/tắt trạng thái nhận khám)
5. `DELETE /api/admin/doctors/{doctorId}` (Xóa hồ sơ bác sĩ)

---

## 2. Phân hoạch lớp tương đương (Equivalence Partitioning - EP) & BVA

### 2.1. API `GET /api/admin/doctors` & `GET /api/admin/users/doctor-candidates`
* **Authorization:**
  * EP1 (Hợp lệ): Token Role `ADMIN`.
  * EP2 (Không hợp lệ): Thiếu Header Authorization.
  * EP3 (Không hợp lệ): Token Role `PATIENT` / `DOCTOR`.
* **Dữ liệu trong Database (BVA):**
  * Min = 0: Không có bản ghi nào $\rightarrow$ Trả về `[]` (HTTP 200 OK).
  * Min = 1 / N > 1: Có dữ liệu $\rightarrow$ Trả về mảng JSON chứa đầy đủ thông tin (HTTP 200 OK).

### 2.2. API `POST /api/admin/doctors?departmentId={departmentId}`
* **departmentId (Query Param):**
  * EP4 (Hợp lệ): Khoa tồn tại trong CSDL.
  * EP5 (Không hợp lệ): Khoa không tồn tại (`99999`) $\rightarrow$ 404 Not Found.
* **userId (Body):**
  * EP6 (Hợp lệ): User tồn tại, Role = DOCTOR, chưa có hồ sơ bác sĩ.
  * EP7 (Không hợp lệ): User không tồn tại $\rightarrow$ 404 Not Found.
  * EP8 (Không hợp lệ): User có Role khác DOCTOR $\rightarrow$ 400 Bad Request ("Tài khoản không có vai trò bác sĩ").
  * EP9 (Không hợp lệ): User đã có hồ sơ bác sĩ $\rightarrow$ 409 Conflict ("Tài khoản này đã có hồ sơ bác sĩ").
* **specialization (Body):**
  * EP10 (Hợp lệ): Chuỗi không rỗng (1 - 255 ký tự).
  * EP11 (Không hợp lệ): Null hoặc chuỗi rỗng $\rightarrow$ 400 Bad Request.

### 2.3. API `PATCH /api/admin/doctors/{doctorId}/availability`
* `doctorId`:
  * EP12 (Hợp lệ): Bác sĩ tồn tại trong CSDL.
  * EP13 (Không hợp lệ): Bác sĩ không tồn tại $\rightarrow$ 404 Not Found.
* `available` (Body):
  * EP14: `true` / `false` $\rightarrow$ Cập nhật thành công (HTTP 200 OK).

### 2.4. API `DELETE /api/admin/doctors/{doctorId}`
* `doctorId`:
  * EP15 (Hợp lệ): Bác sĩ tồn tại và chưa có ràng buộc cuộc hẹn/bệnh án/đơn thuốc $\rightarrow$ Xóa thành công (HTTP 200 OK).
  * EP16 (Không hợp lệ): Bác sĩ không tồn tại $\rightarrow$ 404 Not Found.
  * EP17 (Không hợp lệ): Bác sĩ đã có lịch hẹn hoặc bệnh án $\rightarrow$ 409 Conflict ("Không thể xóa bác sĩ đã có cuộc hẹn...").

---

## 3. Bảng quyết định (Decision Table)

| Điều kiện / Quy tắc | R1 | R2 | R3 | R4 | R5 | R6 |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| C1: Token Role ADMIN? | **F** | **T** | **T** | **T** | **T** | **T** |
| C2: Khoa tồn tại? | - | **F** | **T** | **T** | **T** | **T** |
| C3: User tồn tại? | - | - | **F** | **T** | **T** | **T** |
| C4: Role DOCTOR? | - | - | - | **F** | **T** | **T** |
| C5: Chưa có profile? | - | - | - | - | **F** | **T** |
| **Hành động** | | | | | | |
| Tạo bác sĩ thành công (200/201) | | | | | | **X** |
| Lỗi 401/403 (Auth/RBAC) | **X** | | | | | |
| Lỗi 404 Not Found (Dept/User) | | **X** | **X** | | | |
| Lỗi 400 Bad Request (Role) | | | | **X** | | |
| Lỗi 409 Conflict (Profile Exist) | | | | | **X** | |

---

## 4. Minh chứng thực thi kiểm thử
* Bộ test cases chi tiết: `postman-doctors-test-case.csv`
* File kết quả chạy tự động trên Postman Collection Runner: `DucCuong.postman_test_run.json`
* Ảnh chụp minh chứng các trường hợp Pass/Fail được lưu trong thư mục `evidence/`.
