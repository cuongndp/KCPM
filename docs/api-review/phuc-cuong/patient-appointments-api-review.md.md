# Bảng điều kiện test và thiết kế test case BVA

## 1. GET `/api/patient/appointments`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Boundary Values (BVA) | Tag |
|---|---|---|---|---|---|---|
| `patientId` (query) | ID bệnh nhân có lịch hẹn | V1 | Thiếu `patientId` | X1 | API không quy định min/max ID nghiệp vụ; không áp dụng BVA | — |
|  | ID dạng số không có lịch hẹn (bao gồm ID không tồn tại) | V2 | Không phải số (`abc`) hoặc vượt miền `Long` | X2 |  | — |

> BVA không áp dụng cho `patientId` vì API không định nghĩa biên nghiệp vụ. Với ID số không có lịch hẹn, GET trả về danh sách rỗng; endpoint không kiểm tra bệnh nhân có tồn tại hay không.

### Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `GET /api/patient/appointments?patientId=1` (bệnh nhân `1` có lịch hẹn) | `200 OK`; trả về danh sách lịch hẹn của bệnh nhân `1`. | V1 |
| 2 | `GET /api/patient/appointments?patientId=999999` (ID dạng số không có lịch hẹn) | `200 OK`; trả về danh sách rỗng `[]`. | V2 |
| 3 | `GET /api/patient/appointments` | `400 Bad Request`; từ chối request vì thiếu `patientId`. | X1 |
| 4 | `GET /api/patient/appointments?patientId=abc` | `400 Bad Request`; từ chối request vì `patientId` không phải số. | X2 |

## 2. POST `/api/patient/appointments`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Boundary Values (BVA) | Tag |
|---|---|---|---|---|---|---|
| `patientId` (query) | ID bệnh nhân tồn tại (`1`) | V1 | Thiếu | X1 | Không khai báo min/max ID nghiệp vụ; không áp dụng BVA | — |
|  |  |  | Không phải số (`abc`) | X2 |  | — |
|  |  |  | ID không tồn tại (`999999`) | X3 |  | — |
| `doctorId` (body) | Bác sĩ tồn tại và đang nhận lịch (`1`) | V2 | Thiếu/null | X4 | Không khai báo min/max ID nghiệp vụ; không áp dụng BVA | — |
|  |  |  | Bác sĩ không tồn tại (`999999`) | X5 |  | — |
|  |  |  | Bác sĩ không nhận lịch | X6 |  | — |
|  |  |  | Bác sĩ không thuộc `departmentId` đã chọn | X7 |  | — |
| `departmentId` (body) | Khoa tồn tại, đang hoạt động và là khoa của bác sĩ (`1`) | V3 | Thiếu/null | X8 | Không khai báo min/max ID nghiệp vụ; không áp dụng BVA | — |
|  |  |  | Khoa không tồn tại (`999999`) | X9 |  | — |
|  |  |  | Khoa ngừng hoạt động | X10 |  | — |
| `appointmentDateTime` (body) | Thời điểm hẹn sau giờ hiện tại | V4 | Thiếu/null | X11 | `now - 1 giây`; `now`; `now + 1 giây` | B1, B2, B3 |
|  |  |  | Sai định dạng ngày giờ | X12 |  | — |
|  |  |  | Thời điểm trước hoặc bằng giờ hiện tại | X13 |  | — |
| Slot bác sĩ | Slot chưa được đặt | V5 | Slot đã được đặt và lịch cũ chưa bị hủy/đánh dấu `NO_SHOW` | X14 | Không áp dụng BVA; kiểm tra trùng slot | — |
| `reason` (body) | Có thể gửi chuỗi hoặc bỏ trống | V6 | Không có ràng buộc dữ liệu/độ dài được khai báo | — | Không khai báo giới hạn; không áp dụng BVA | — |
| `notes` (body) | Có thể gửi chuỗi hoặc bỏ trống | V7 | Không có ràng buộc dữ liệu/độ dài được khai báo | — | Không khai báo giới hạn; không áp dụng BVA | — |

### Thiết kế test cases

#### Tiền điều kiện

- Dùng môi trường test với token hợp lệ có role `PATIENT`.
- Bệnh nhân `1`, bác sĩ `1`, khoa `1` tồn tại; bác sĩ `1` đang nhận lịch và thuộc khoa hoạt động `1`.
- Bác sĩ `2` tồn tại, đang nhận lịch và thuộc khoa `2`; bác sĩ `3` tồn tại, thuộc khoa `1` nhưng không nhận lịch; khoa `2` tồn tại nhưng ngừng hoạt động.
- Mock `BusinessTime.now()` là `2026-10-28T12:00:00`.
- Mỗi dòng là một request độc lập. Dùng slot chưa được đặt cho các test cần tạo lịch; reset fixture giữa các test.

Body hợp lệ làm nền: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}`. Ở các test BVA thời gian, thay `appointmentDateTime` bằng giá trị ghi trong input.

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2026-10-28T12:00:01","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}`; slot chưa được đặt | `200 OK`; tạo lịch cho bệnh nhân `1`, bác sĩ `1`, khoa `1`, trạng thái `SCHEDULED`. | V1, V2, V3, V4, V5, V6, V7, B3 |
| 2 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2026-10-28T11:59:59","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}` | `400 Bad Request`; không tạo lịch vì thời điểm hẹn sớm hơn `now` một giây. | X13, B1 |
| 3 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2026-10-28T12:00:00","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}` | `400 Bad Request`; không tạo lịch vì thời điểm hẹn bằng `now`. | X13, B2 |
| 4 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":null,"departmentId":null,"appointmentDateTime":null}` | `400 Bad Request`; validation từ chối các trường body bắt buộc bị null. | X4, X8, X11 |
| 5 | `POST /api/patient/appointments`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}` | `400 Bad Request`; từ chối request vì thiếu query parameter `patientId`. | X1 |
| 6 | `POST /api/patient/appointments?patientId=abc`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}` | `400 Bad Request`; từ chối request vì `patientId` không phải số. | X2 |
| 7 | `POST /api/patient/appointments?patientId=999999`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}` | `404 Not Found`; không tìm thấy bệnh nhân `999999`. | X3 |
| 8 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":999999,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00"}` | `404 Not Found`; không tìm thấy bác sĩ `999999`. | X5 |
| 9 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":3,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00"}`; bác sĩ `3` tồn tại nhưng không nhận lịch | `409 Conflict`; không tạo lịch vì bác sĩ không nhận lịch hẹn. | X6 |
| 10 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":2,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00"}`; bác sĩ `2` thuộc khoa `2` | `400 Bad Request`; không tạo lịch vì bác sĩ không thuộc khoa `1` đã chọn. | X7 |
| 11 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":1,"departmentId":999999,"appointmentDateTime":"2030-01-01T10:00:00"}` | `404 Not Found`; không tìm thấy khoa `999999`. | X9 |
| 12 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":2,"departmentId":2,"appointmentDateTime":"2030-01-01T10:00:00"}`; khoa `2` tồn tại nhưng ngừng hoạt động | `409 Conflict`; không tạo lịch vì khoa `2` ngừng nhận lịch hẹn. | X10 |
| 13 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"28-10-2026 12:00"}` | `400 Bad Request`; từ chối ngày giờ sai định dạng. | X12 |
| 14 | `POST /api/patient/appointments?patientId=1`<br>Body: `{"doctorId":1,"departmentId":1,"appointmentDateTime":"2030-01-01T10:00:00","reason":"Khám định kỳ","notes":"Ghi chú kiểm thử"}`<br>Đã có lịch chưa hủy của bác sĩ `1` tại `2030-01-01T10:00:00`. | `409 Conflict`; không tạo lịch vì slot đã được đặt. | X14 |

## Căn cứ mã nguồn

- `backend/src/main/java/com/hospital/management/controller/PatientController.java`
- `backend/src/main/java/com/hospital/management/dto/AppointmentRequest.java`
- `backend/src/main/java/com/hospital/management/service/AppointmentService.java`
- `backend/src/main/java/com/hospital/management/util/BusinessTime.java`
- `backend/src/main/java/com/hospital/management/exception/GlobalExceptionHandler.java`
