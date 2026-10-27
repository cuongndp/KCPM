# Bảng điều kiện test và thiết kế test case BVA

## 1. GET `/api/patient/appointments`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `patientId` | ID bệnh nhân tồn tại (ví dụ `1`, `2`) | V1 | Thiếu `patientId` | X1 | ID nhỏ nhất có trong dữ liệu test: `1` | B1 |
|  |  |  | Không phải số (`abc`) hoặc vượt miền `Long` (`9223372036854775808`) | X2 | ID kế tiếp trong dữ liệu test: `2` | B2 |
|  |  |  | ID không tồn tại (ví dụ `999999`) | X3 |  |  |

> API không khai báo min/max cho ID. `1` và `2` là biên theo dữ liệu fixture, không phải giới hạn được xác nhận trong nghiệp vụ. ID `0`/âm hiện không có validation riêng; nếu không có bản ghi tương ứng, API trả danh sách rỗng.

### Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `GET /api/patient/appointments?patientId=1` (bệnh nhân tồn tại) | `200 OK`; trả danh sách lịch hẹn của bệnh nhân `1` (có thể là `[]`) | V1, B1 |
| 2 | `GET /api/patient/appointments?patientId=2` (bệnh nhân tồn tại kế tiếp) | `200 OK`; chỉ trả lịch hẹn của bệnh nhân `2` | B2 |
| 3 | `GET /api/patient/appointments?patientId=999999` | `200 OK`; body `[]` nếu ID không tồn tại | X3 |
| 4 | Gửi lần lượt: `GET /api/patient/appointments` (thiếu `patientId`); `GET /api/patient/appointments?patientId=abc` (sai định dạng) | Request bị từ chối. Theo bằng chứng rà soát trước đây, hai trường hợp hiện trả `500`; ghi nhận là kết quả thực tế cần sửa/xác minh (lỗi đầu vào nên là `4xx`). | X1, X2 |

## 2. POST `/api/patient/appointments`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `patientId` (query) | ID bệnh nhân tồn tại | V1 | Thiếu; sai định dạng; ID không tồn tại | X1, X2, X3 | ID tồn tại nhỏ nhất trong fixture: `1` | B1 |
| `doctorId` (body) | ID bác sĩ tồn tại và nhận lịch | V2 | Thiếu/null; ID không tồn tại | X4, X5 | ID tồn tại nhỏ nhất trong fixture: `1` | B2 |
| `departmentId` (body) | Khoa tồn tại và đang hoạt động | V3 | Thiếu/null; ID không tồn tại hoặc khoa ngừng hoạt động | X6, X7 | ID tồn tại nhỏ nhất trong fixture: `1` | B3 |
| `appointmentDateTime` | Thời điểm lớn hơn hiện tại | V4 | Thiếu/null; nhỏ hơn hoặc bằng hiện tại | X8, X9 | `now + 1 giây` | B4 |
|  |  |  |  |  | `now` | B5 |
|  |  |  |  |  | `now - 1 giây` | B6 |
| `reason`, `notes` | Có thể bỏ trống hoặc gửi chuỗi | V5 | Không có giới hạn độ dài được khai báo | — | Bỏ trống | B7 |

> Các giá trị ID `1` là giá trị biên theo dữ liệu test đã chuẩn bị; mã nguồn không khai báo giới hạn min/max ID. Thời gian hẹn được so với giờ hiện tại `Asia/Ho_Chi_Minh`.

### Thiết kế test cases

Body hợp lệ dùng làm dữ liệu nền:

```json
{
  "doctorId": 1,
  "departmentId": 1,
  "appointmentDateTime": "<thời điểm kiểm thử + 1 giây>",
  "reason": "Khám định kỳ",
  "notes": "Ghi chú kiểm thử"
}
```

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| 1 | `POST /api/patient/appointments?patientId=1`; body hợp lệ ở trên, slot chưa được đặt | `200 OK`; lịch hẹn được tạo cho bệnh nhân `1`, bác sĩ `1`, trạng thái `SCHEDULED` | V1, V2, V3, V4, B1, B2, B3, B4 |
| 2 | Cùng request hợp lệ; bỏ `reason` và `notes` | `200 OK`; lịch hẹn được tạo, hai trường tùy chọn để trống | V5, B7 |
| 3 | `patientId=1`; bỏ đồng thời `doctorId`, `departmentId`, `appointmentDateTime` khỏi body | `400 Bad Request`; validation báo cả ba trường bắt buộc | X4, X6, X8 |
| 4 | `patientId=1`; body hợp lệ nhưng đặt `doctorId=999999` | `404 Not Found`; không tìm thấy bác sĩ | X5 |
| 5 | `patientId=1`; body hợp lệ nhưng đặt `departmentId=999999` | `404 Not Found`; không tìm thấy khoa | X7 |
| 6 | Cùng request hợp lệ; lần lượt đặt `appointmentDateTime` bằng `now` và `now - 1 giây` | Cả hai lần trả `400 Bad Request`; thời gian hiện tại và quá khứ không được chấp nhận | X9, B5, B6 |
| 7 | Gửi lần lượt: thiếu `patientId`; `patientId=abc`; `patientId=999999` | Thiếu/sai định dạng bị từ chối; ID bệnh nhân không tồn tại trả `404 Not Found` | X1, X2, X3 |

## 3. Tiền điều kiện chạy test

- Dùng môi trường test, token hợp lệ có role `PATIENT`, không dùng dữ liệu bệnh nhân thật.
- Chuẩn bị bệnh nhân `1`, `2`, bác sĩ `1`, khoa `1`; bác sĩ đang nhận lịch và thuộc khoa đang hoạt động `1`.
- Chọn slot lịch hẹn tương lai chưa bị đặt.
- Với các giá trị `now`, `now - 1 giây`, cố định đồng hồ hoặc đồng bộ thời điểm chạy để kết quả ổn định.
- Mỗi dòng test case có thể gồm nhiều request được ghi rõ là “gửi lần lượt”; đây là cách gom test theo cùng một mục đích, không gửi nhiều giá trị trong một request.

## 4. Căn cứ mã nguồn

- `backend/src/main/java/com/hospital/management/controller/PatientController.java`
- `backend/src/main/java/com/hospital/management/dto/AppointmentRequest.java`
- `backend/src/main/java/com/hospital/management/service/AppointmentService.java`
- `backend/src/main/java/com/hospital/management/util/BusinessTime.java`
