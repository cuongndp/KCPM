# Bảng điều kiện test và thiết kế test case BVA

## 1. GET `/api/doctor/profile`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `userId` | ID người dùng tồn tại và có hồ sơ bác sĩ (ví dụ `2`) | V1 | Thiếu `userId` | X1 | ID tồn tại dùng trong dữ liệu test: `2` | B1 |
|  |  |  | Không phải số (`abc`) hoặc vượt miền `Long` (`9223372036854775808`) | X2 |  |  |
|  |  |  | ID không tồn tại (ví dụ `999999`) | X3 |  |  |
| Token | Token hợp lệ, tài khoản có quyền truy cập | V2 | Thiếu token hoặc token không hợp lệ | X4 | — | — |

> `2` là ID được dùng trong ảnh Postman và trả `200 OK`. Đây là giá trị theo dữ liệu test, không phải giới hạn ID được quy định trong nghiệp vụ.

### Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `GET /api/doctor/profile?userId=2` với token hợp lệ | `200 OK`; trả về JSON hồ sơ bác sĩ, `user.id = 2`, `role = "DOCTOR"` | V1, V2, B1 |
| 2 | `GET /api/doctor/profile` (thiếu `userId`) | Request bị từ chối; kỳ vọng `400 Bad Request` cho tham số bắt buộc không hợp lệ | X1 |
| 3 | `GET /api/doctor/profile?userId=abc` | Request bị từ chối; kỳ vọng `400 Bad Request` cho sai định dạng | X2 |
| 4 | `GET /api/doctor/profile?userId=999999` | Không tìm thấy hồ sơ; xác nhận status theo hợp đồng API, dự kiến `404 Not Found` | X3 |
| 5 | `GET /api/doctor/profile?userId=2` không gửi token | Request bị từ chối do thiếu xác thực | X4 |
| 6 | `GET /api/doctor/profile?userId=2` với token không hợp lệ | Request bị từ chối do token không hợp lệ | X4 |

## 2. GET `/api/doctor/appointments`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `doctorId` | ID bác sĩ tồn tại (ví dụ `2`) | V1 | Thiếu `doctorId` | X1 | ID bác sĩ dùng trong dữ liệu test: `2` | B1 |
|  |  |  | Không phải số (`abc`) hoặc vượt miền `Long` (`9223372036854775808`) | X2 |  |  |
|  |  |  | ID không tồn tại (ví dụ `999999`) | X3 |  |  |
| Token | Token hợp lệ, tài khoản có quyền truy cập | V2 | Thiếu token hoặc token không hợp lệ | X4 | — | — |

> ID `2` là giá trị mẫu; hãy xác nhận bác sĩ tồn tại trong dữ liệu test trước khi chạy.

### Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `GET /api/doctor/appointments?doctorId=2` với token hợp lệ | `200 OK`; trả danh sách lịch hẹn của bác sĩ `2` (có thể là `[]` nếu chưa có lịch) | V1, V2, B1 |
| 2 | `GET /api/doctor/appointments` (thiếu `doctorId`) | Request bị từ chối; kỳ vọng `400 Bad Request` cho tham số bắt buộc không hợp lệ | X1 |
| 3 | `GET /api/doctor/appointments?doctorId=abc` | Request bị từ chối; kỳ vọng `400 Bad Request` cho sai định dạng | X2 |
| 4 | `GET /api/doctor/appointments?doctorId=999999` | Không có lịch hẹn phù hợp; xác nhận kết quả theo hợp đồng API, có thể là `200 OK` với `[]` | X3 |
| 5 | `GET /api/doctor/appointments?doctorId=2` không gửi token | Request bị từ chối do thiếu xác thực | X4 |
| 6 | `GET /api/doctor/appointments?doctorId=2` với token không hợp lệ | Request bị từ chối do token không hợp lệ | X4 |

## 3. Tiền điều kiện chạy test

- Dùng môi trường test và token hợp lệ có role `DOCTOR`; không đưa token thật vào tài liệu hoặc ảnh công khai.
- Xác nhận `{{baseUrl}}` trỏ đến `http://localhost:8080`.
- Chuẩn bị người dùng `2` có hồ sơ bác sĩ và xác nhận ID bác sĩ dùng cho API lịch hẹn.
- Lưu status code và response thực tế của từng request. Trường hợp chưa chạy chỉ là kết quả kỳ vọng, không phải kết quả thực tế.
- Ảnh Postman của `PROFILE-01` xác nhận `200 OK` trong `197 ms`; response có `user.id = 2`, `role = "DOCTOR"` và `active = true`.
- Nếu gặp `403 Forbidden`, ghi nhận là lần chạy riêng và kiểm tra token, role, quyền truy cập cùng log backend.

## 4. Căn cứ

- Ảnh Postman của `GET /api/doctor/profile?userId=2`.
- Kết quả kiểm thử thực tế được lưu trong thư mục `evidence/`.
## Căn cứ mã nguồn
- `backend/src/main/java/com/hospital/management/controller/PatientController.java`
- `backend/src/main/java/com/hospital/management/dto/AppointmentRequest.java`
- `backend/src/main/java/com/hospital/management/service/AppointmentService.java`
- `backend/src/main/java/com/hospital/management/util/BusinessTime.java`
