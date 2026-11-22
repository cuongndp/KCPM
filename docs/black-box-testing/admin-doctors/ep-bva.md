# Tài Liệu Thiết Kế Kiểm Thử Hộp Đen (Black-box Testing)
## Phân Hoạch Lớp Tương Đương (EP) & Phân Tích Giá Trị Biên (BVA)
**Mã nhiệm vụ Jira:** KAN-57  
**Thành viên thực hiện:** Nguyễn Đức Cường  
**Module:** Quản lý Bác sĩ (Admin Doctor APIs)  
**Phạm vi kiểm thử:**
1. `GET /api/admin/doctors`
2. `GET /api/admin/users/doctor-candidates`

---

## 1. Phân hoạch lớp tương đương (Equivalence Partitioning - EP)

### 1.1. Bảng EP cho Xác thực & Phân quyền (Áp dụng chung cho cả 2 API)
| Tham số kiểm tra | Lớp tương đương hợp lệ (Valid EP) | Lớp tương đương không hợp lệ (Invalid EP) | Dữ liệu kiểm thử đại diện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| **Authorization Token** | **EP_AUTH_V1:** Token hợp lệ của tài khoản Role `ADMIN` | **EP_AUTH_INV1:** Không gửi kèm token (Missing Header)<br>**EP_AUTH_INV2:** Token sai định dạng / hết hạn<br>**EP_AUTH_INV3:** Token của Role `PATIENT`<br>**EP_AUTH_INV4:** Token của Role `DOCTOR` | Bearer `{{ADMIN_TOKEN}}`<br>None<br>`Bearer invalid_token_xyz`<br>`{{PATIENT_TOKEN}}`<br>`{{DOCTOR_TOKEN}}` | **200 OK**<br>**401 Unauthorized**<br>**401 Unauthorized**<br>**403 Forbidden**<br>**403 Forbidden** |

### 1.2. Bảng EP cho Dữ liệu trả về (Response Data State)
| API | Lớp tương đương hợp lệ (Valid EP) | Dữ liệu đại diện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| `GET /api/admin/doctors` | **EP_DOC_V1:** Bảng `doctors` có từ 1 bản ghi trở lên<br>**EP_DOC_V2:** Bảng `doctors` hiện tại chưa có dữ liệu | DB có ít nhất 1 doctor<br>DB trống | **200 OK**, mảng JSON không rỗng, chứa các trường `id`, `user`, `department`, `specialization`...<br>**200 OK**, mảng JSON rỗng `[]` |
| `GET /api/admin/users/doctor-candidates` | **EP_CAND_V1:** Có user role `DOCTOR` chưa có profile<br>**EP_CAND_V2:** Tất cả user role `DOCTOR` đã có profile | Có ứng viên thỏa mãn<br>Không có ứng viên nào | **200 OK**, mảng JSON chứa `id`, `username`, `email`...<br>**200 OK**, mảng JSON rỗng `[]` |

---

## 2. Phân tích giá trị biên (Boundary Value Analysis - BVA)

| Tham số / Điều kiện | Giá trị biên kiểm thử (BVA Tag) | Giá trị đại diện | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| **Số lượng Doctor trong hệ thống** | **BVA_DOC_0:** Không có bác sĩ nào (Min = 0)<br>**BVA_DOC_1:** Có đúng 1 bác sĩ (Min = 1)<br>**BVA_DOC_MANY:** Có nhiều bác sĩ ($N > 1$) | CSDL 0 bác sĩ<br>CSDL 1 bác sĩ<br>CSDL nhiều bác sĩ | **200 OK**, body: `[]`<br>**200 OK**, body: `[doctor_1]`<br>**200 OK**, body: `[doctor_1, doctor_2, ...]` |
| **Số lượng Candidate (ứng viên)** | **BVA_CAND_0:** 0 ứng viên (Min = 0)<br>**BVA_CAND_1:** 1 ứng viên (Min = 1)<br>**BVA_CAND_MANY:** Nhiều ứng viên ($N > 1$) | CSDL 0 ứng viên<br>CSDL 1 ứng viên<br>CSDL nhiều ứng viên | **200 OK**, body: `[]`<br>**200 OK**, body: `[user_cand_1]`<br>**200 OK**, body: `[cand_1, cand_2, ...]` |
| **Header Authorization Length** | **BVA_HDR_EMPTY:** Header trống độ dài 0<br>**BVA_HDR_VALID:** Chuỗi Bearer token hợp lệ | `Authorization: ""` hoặc không có<br>`Authorization: Bearer <valid_jwt>` | **401 Unauthorized**<br>**200 OK** |

---

## 3. Tổng kết độ bao phủ
* Toàn bộ các phân vùng tương đương và giá trị biên của 2 API GET đã được tổng hợp thành file `design_testcase.csv` dùng để nạp vào Postman Runner kiểm thử tự động.
