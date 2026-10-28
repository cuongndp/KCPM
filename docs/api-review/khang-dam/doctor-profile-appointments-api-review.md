# Bảng điều kiện test và thiết kế test case BVA

## 1. GET `/api/doctor/profile`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundary / Representative Value | Tag |
|---|---|---|---|---|---|---|
| `userId` | ID người dùng tồn tại và có hồ sơ bác sĩ (ví dụ `2`) | V1 | Thiếu `userId` | X1 | Giá trị đại diện trong evidence: `2`; mã nguồn không định nghĩa biên nghiệp vụ cho ID | B1 |
|  |  |  | Không phải số (`abc`) hoặc vượt miền `Long` (`9223372036854775808`) | X2 |  |  |
|  |  |  | Không có hồ sơ bác sĩ gắn với `userId` (ví dụ `999999`) | X3 |  |  |
| Token | JWT hợp lệ của tài khoản có role `DOCTOR` | V2 | Thiếu token | X4 | — | — |
|  |  |  | JWT không hợp lệ hoặc hết hạn | X5 |  |  |

> `2` là ID được dùng trong ảnh Postman và trả `200 OK`; đây là giá trị đại diện theo dữ liệu test, không phải biên ID được quy định trong nghiệp vụ. Controller chỉ kiểm tra role `DOCTOR`; với endpoint profile, service tìm hồ sơ theo `userId` được truyền vào.

### Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `GET /api/doctor/profile?userId=2` với token hợp lệ | `200 OK`; trả về JSON hồ sơ bác sĩ, `user.id = 2`, `role = "DOCTOR"` | V1, V2, B1 |
| 2 | `GET /api/doctor/profile` (thiếu `userId`) | Theo mã nguồn hiện tại, lỗi tham số bắt buộc đi vào handler tổng quát và trả `500 Internal Server Error`; ghi nhận status thực tế khi chạy | X1 |
| 3 | `GET /api/doctor/profile?userId=abc` | Theo mã nguồn hiện tại, lỗi chuyển kiểu `Long` đi vào handler tổng quát và trả `500 Internal Server Error`; ghi nhận status thực tế khi chạy | X2 |
| 4 | `GET /api/doctor/profile?userId=999999` | Không có hồ sơ bác sĩ gắn với user ID này: `DoctorService` ném `403 Forbidden` (không phải `404`) | X3 |
| 5 | `GET /api/doctor/profile?userId=2` không gửi token | Request bị từ chối do thiếu xác thực | X4 |
| 6 | `GET /api/doctor/profile?userId=2` với JWT không hợp lệ hoặc hết hạn | Ghi nhận kết quả thực tế; bộ lọc JWT phân tích token trước khi vào controller và hiện không bắt lỗi phân tích token | X5 |

## 2. GET `/api/doctor/appointments`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundary / Representative Value | Tag |
|---|---|---|---|---|---|---|
| `doctorId` | ID bác sĩ tồn tại (ví dụ `2`) | V1 | Thiếu `doctorId` | X1 | Giá trị đại diện trong dữ liệu test: `2`; mã nguồn không định nghĩa biên nghiệp vụ cho ID | B1 |
|  |  |  | Không phải số (`abc`) hoặc vượt miền `Long` (`9223372036854775808`) | X2 |  |  |
|  |  |  | ID không có lịch hẹn phù hợp (ví dụ `999999`) | X3 |  |  |
| Token | JWT hợp lệ của tài khoản có role `DOCTOR` | V2 | Thiếu token | X4 | — | — |
|  |  |  | JWT không hợp lệ hoặc hết hạn | X5 |  |  |

> ID `2` là giá trị mẫu; hãy xác nhận bác sĩ tồn tại trong dữ liệu test trước khi chạy.

### Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `GET /api/doctor/appointments?doctorId=2` với token hợp lệ | `200 OK`; trả danh sách lịch hẹn của bác sĩ `2` (có thể là `[]` nếu chưa có lịch) | V1, V2, B1 |
| 2 | `GET /api/doctor/appointments` (thiếu `doctorId`) | Theo mã nguồn hiện tại, lỗi tham số bắt buộc đi vào handler tổng quát và trả `500 Internal Server Error`; ghi nhận status thực tế khi chạy | X1 |
| 3 | `GET /api/doctor/appointments?doctorId=abc` | Theo mã nguồn hiện tại, lỗi chuyển kiểu `Long` đi vào handler tổng quát và trả `500 Internal Server Error`; ghi nhận status thực tế khi chạy | X2 |
| 4 | `GET /api/doctor/appointments?doctorId=999999` | Service truy vấn trực tiếp theo `doctorId`, không kiểm tra bác sĩ tồn tại; trả `200 OK` với `[]` nếu không có lịch | X3 |
| 5 | `GET /api/doctor/appointments?doctorId=2` không gửi token | Request bị từ chối do thiếu xác thực | X4 |
| 6 | `GET /api/doctor/appointments?doctorId=2` với JWT không hợp lệ hoặc hết hạn | Ghi nhận kết quả thực tế; bộ lọc JWT phân tích token trước khi vào controller và hiện không bắt lỗi phân tích token | X5 |

> Endpoint này xác thực role `DOCTOR`, nhưng mã nguồn không đối chiếu `doctorId` với bác sĩ đang đăng nhập. Vì vậy, test theo ID khác cần ghi nhận đúng hành vi hiện tại; không xem role check là kiểm tra quyền sở hữu dữ liệu.

## 3. Tiền điều kiện chạy test

- Dùng môi trường test và JWT hợp lệ của tài khoản có role `DOCTOR`; không đưa token thật vào tài liệu hoặc ảnh công khai.
- Xác nhận `{{baseUrl}}` trỏ đến `http://localhost:8080`.
- Chuẩn bị người dùng `2` có hồ sơ bác sĩ và xác nhận ID bác sĩ dùng cho API lịch hẹn.
- Lưu status code và response thực tế của từng request. Trường hợp chưa chạy chỉ là kết quả kỳ vọng, không phải kết quả thực tế.
- Ảnh `evidence/profile-01-forbidden.png` hiển thị request profile trả `200 OK` trong `197 ms`, response có `user.id = 2`, `role = "DOCTOR"` và `active = true`; tên file có chữ `forbidden` nhưng nội dung ảnh không phải phản hồi `403`.
- Với thiếu/thừa định dạng tham số, mã nguồn hiện có handler tổng quát cho `Exception`, nên expected status trong bảng là `500`; cần đối chiếu lần chạy thực tế trước khi kết luận.
- Nếu gặp lỗi xác thực hoặc phân quyền, lưu status và response thực tế; kiểm tra JWT, role và log backend. Bộ lọc JWT hiện không bắt ngoại lệ khi phân tích token sai/hết hạn.

## 4. Căn cứ

- Ảnh Postman và các lần chạy được lưu trong `evidence/`; đối chiếu đúng tên file và nội dung ảnh trước khi trích dẫn status.

### Căn cứ mã nguồn

- `backend/src/main/java/com/hospital/management/controller/DoctorController.java`
- `backend/src/main/java/com/hospital/management/service/DoctorService.java`
- `backend/src/main/java/com/hospital/management/service/AppointmentService.java` và `backend/src/main/java/com/hospital/management/repository/AppointmentRepository.java`
- `backend/src/main/java/com/hospital/management/config/SecurityConfig.java`: `/api/doctor/**` yêu cầu role `DOCTOR`.
- `backend/src/main/java/com/hospital/management/security/JwtAuthenticationFilter.java` và `backend/src/main/java/com/hospital/management/util/JwtUtil.java`
- `backend/src/main/java/com/hospital/management/exception/GlobalExceptionHandler.java`
