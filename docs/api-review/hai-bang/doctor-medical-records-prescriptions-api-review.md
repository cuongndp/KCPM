# Thiết kế bộ test case BVA — API hồ sơ bệnh án và kê đơn của bác sĩ

## 1. Mục tiêu

Thiết kế test case theo phương pháp **Boundary Value Analysis (BVA)** cho hai API của bác sĩ; xác định phân vùng hợp lệ/không hợp lệ, kiểm tra các giá trị biên của dữ liệu đầu vào và ghi rõ kết quả mong đợi để thực thi trên Postman.

## 2. Phạm vi API

1. `GET /api/doctor/medical-records` — Query parameter: `doctorId`
2. `POST /api/doctor/prescriptions` — Query parameter: `doctorId`; body: `PrescriptionRequest`

> **Lưu ý về căn cứ:** Các điều kiện bắt buộc bên dưới được đối chiếu với `DoctorController.java`, `PrescriptionRequest.java` và `PrescriptionService.java` trong mã nguồn được cung cấp. Mã nguồn không khai báo min/max cho ID, không khai báo giới hạn độ dài cho các chuỗi thuốc và không khai báo min/max cho `duration`. Vì vậy, các giá trị `1`, `2` được dùng làm dữ liệu fixture nếu chúng tồn tại trong môi trường test; không được xem là giới hạn nghiệp vụ đã xác nhận.

## 3. API GET `/api/doctor/medical-records`

### 3.1. Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `doctorId` | ID bác sĩ tồn tại trong dữ liệu test, ví dụ `1` hoặc `2` | V1 | Thiếu `doctorId` | X1 | ID bác sĩ tồn tại nhỏ nhất trong fixture, ví dụ `1` | B1 |
|  |  |  | Không phải số nguyên (`abc`) | X2 | ID bác sĩ tồn tại kế tiếp trong fixture, ví dụ `2` | B2 |
|  |  |  | Vượt miền `Long`: `9223372036854775808` | X3 | Giá trị số nguyên lớn nhất của `Long`: `9223372036854775807` | B3 |
|  |  |  | ID không tồn tại, ví dụ `999999` | X4 | `0` và `-1` để kiểm tra ID không dương | B4 |
|  |  |  |  |  | Giá trị số nguyên nhỏ nhất của `Long`: `-9223372036854775808` | B5 |

> Controller nhận `doctorId` kiểu `Long` và gọi `findByDoctorId(doctorId)` trực tiếp; không thấy kiểm tra min/max hay kiểm tra bác sĩ tồn tại trong endpoint GET. Vì vậy, với ID số nhưng không có bản ghi, kết quả dự kiến theo truy vấn là `200 OK` cùng danh sách rỗng `[]`. Thiếu/sai định dạng/vượt miền `Long` thường bị Spring từ chối khi bind tham số; hãy ghi lại status/body thực tế khi chạy.

### 3.2. Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| GET-BVA-01 | `GET /api/doctor/medical-records?doctorId=1` với bác sĩ `1` tồn tại | `200 OK`; trả danh sách hồ sơ bệnh án của bác sĩ `1` (danh sách có thể rỗng nếu fixture chưa có hồ sơ) | V1, B1 |
| GET-BVA-02 | `GET /api/doctor/medical-records?doctorId=2` với bác sĩ `2` tồn tại | `200 OK`; chỉ trả hồ sơ thuộc bác sĩ `2` | B2 |
| GET-BVA-03 | `GET /api/doctor/medical-records?doctorId=999999` | Dự kiến `200 OK`, body `[]` nếu không có hồ sơ gắn với ID này; xác nhận bằng thực thi | X4 |
| GET-BVA-04 | `GET /api/doctor/medical-records` (không gửi `doctorId`) | Request bị từ chối do thiếu query parameter; ghi status/body thực tế (thường là `4xx`) | X1 |
| GET-BVA-05 | `GET /api/doctor/medical-records?doctorId=abc` | Request bị từ chối do không bind được sang `Long`; ghi status/body thực tế (thường là `400 Bad Request`) | X2 |
| GET-BVA-06 | `GET /api/doctor/medical-records?doctorId=9223372036854775808` | Request bị từ chối do vượt miền `Long`; ghi status/body thực tế | X3 |
| GET-BVA-07 | Gửi lần lượt `doctorId=0` và `doctorId=-1` | Không có validation dương ở controller; dự kiến `200 OK` với `[]` nếu không có hồ sơ tương ứng. Ghi nhận kết quả thực tế | B4 |
| GET-BVA-08 | `GET /api/doctor/medical-records?doctorId=9223372036854775807` | Giá trị nằm trong miền `Long`; dự kiến `200 OK` và `[]` nếu không có hồ sơ tương ứng | B3 |
| GET-BVA-09 | `GET /api/doctor/medical-records?doctorId=-9223372036854775808` | Giá trị nằm trong miền `Long`; dự kiến `200 OK` và `[]` nếu không có hồ sơ tương ứng | B5 |

## 4. API POST `/api/doctor/prescriptions`

### 4.1. Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `doctorId` (query) | ID bác sĩ tồn tại | V1 | Thiếu hoặc sai định dạng | X1, X2 | ID bác sĩ tồn tại nhỏ nhất trong fixture, ví dụ `1` | B1 |
|  |  |  | ID bác sĩ không tồn tại | X3 | `0`, `-1`, và các giá trị biên miền `Long` để xác minh hành vi | B2 |
| `patientId` (body) | ID bệnh nhân tồn tại | V2 | `null`/thiếu; bệnh nhân không tồn tại | X4, X5 | ID bệnh nhân tồn tại nhỏ nhất trong fixture, ví dụ `1` | B3 |
| `appointmentId` (body) | ID lịch hẹn tồn tại | V3 | `null`/thiếu; lịch hẹn không tồn tại | X6, X7 | ID lịch hẹn tồn tại nhỏ nhất trong fixture, ví dụ `1` | B4 |
| `medication` | Chuỗi không rỗng và không chỉ gồm khoảng trắng | V4 | `null`, thiếu, `""` hoặc chỉ khoảng trắng | X8 | Chuỗi 1 ký tự, ví dụ `"A"` | B5 |
| `dosage` | Chuỗi không rỗng và không chỉ gồm khoảng trắng | V5 | `null`, thiếu, `""` hoặc chỉ khoảng trắng | X9 | Chuỗi 1 ký tự, ví dụ `"1"` | B6 |
| `frequency` | Chuỗi không rỗng và không chỉ gồm khoảng trắng | V6 | `null`, thiếu, `""` hoặc chỉ khoảng trắng | X10 | Chuỗi 1 ký tự, ví dụ `"1"` | B7 |
| `duration` | Có thể bỏ qua hoặc gửi số nguyên vì DTO không khai báo validation cho trường này | V7 | Chưa có miền giá trị không hợp lệ được khai báo trong DTO | — | `null`, `0`, `1` (kiểm tra hành vi, không khẳng định 0 là hợp lệ về nghiệp vụ) | B8 |
| `instructions` | Có thể bỏ qua hoặc gửi chuỗi | V8 | Chưa có giới hạn độ dài/điều kiện nội dung được khai báo trong DTO | — | Bỏ trống và chuỗi 1 ký tự | B9 |

> `PrescriptionRequest` dùng `@NotNull` cho `patientId`, `appointmentId`; dùng `@NotBlank` cho `medication`, `dosage`, `frequency`. Không có annotation validation cho `duration` và `instructions`. Service kiểm tra bác sĩ, bệnh nhân và lịch hẹn có tồn tại; nếu không tìm thấy, sẽ phát sinh `ResourceNotFoundException`. Mã nguồn không khai báo giới hạn min/max cho ID, độ dài chuỗi hoặc `duration`, nên các test đó là kiểm tra hành vi chứ không phải khẳng định giới hạn nghiệp vụ.

### 4.2. Body hợp lệ dùng làm dữ liệu nền

Thay các ID ví dụ bằng ID thực sự tồn tại trong môi trường test. Đảm bảo lịch hẹn `1` phù hợp với dữ liệu fixture.

```json
{
  "patientId": 1,
  "appointmentId": 1,
  "medication": "Paracetamol",
  "dosage": "500 mg",
  "frequency": "2 lần/ngày",
  "duration": 5,
  "instructions": "Uống sau ăn"
}
```

### 4.3. Thiết kế test cases

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| POST-BVA-01 | `POST /api/doctor/prescriptions?doctorId=1`; body hợp lệ ở trên; bác sĩ, bệnh nhân và lịch hẹn tồn tại | `200 OK`; tạo đơn thuốc, response có dữ liệu đơn thuốc mới | V1, V2, V3, V4, V5, V6, B1, B3, B4 |
| POST-BVA-02 | Cùng request hợp lệ nhưng bỏ `duration` và `instructions` | Dự kiến `200 OK`; DTO không đánh dấu hai trường này là bắt buộc | V7, V8, B9 |
| POST-BVA-03 | Gửi lần lượt `medication=""`, `medication="   "` | Validation từ chối vì `@NotBlank`; dự kiến `400 Bad Request` | X8 |
| POST-BVA-04 | Gửi lần lượt `dosage=""`, `dosage="   "` | Validation từ chối vì `@NotBlank`; dự kiến `400 Bad Request` | X9 |
| POST-BVA-05 | Gửi lần lượt `frequency=""`, `frequency="   "` | Validation từ chối vì `@NotBlank`; dự kiến `400 Bad Request` | X10 |
| POST-BVA-06 | Bỏ `patientId` khỏi body hoặc đặt `"patientId": null` | Validation từ chối vì `@NotNull`; dự kiến `400 Bad Request` | X4 |
| POST-BVA-07 | Bỏ `appointmentId` khỏi body hoặc đặt `"appointmentId": null` | Validation từ chối vì `@NotNull`; dự kiến `400 Bad Request` | X6 |
| POST-BVA-08 | Body hợp lệ nhưng `patientId=999999` không tồn tại | Service không tìm thấy bệnh nhân; dự kiến `404 Not Found` theo xử lý `ResourceNotFoundException` | X5 |
| POST-BVA-09 | Body hợp lệ nhưng `appointmentId=999999` không tồn tại | Service không tìm thấy lịch hẹn; dự kiến `404 Not Found` | X7 |
| POST-BVA-10 | `doctorId=999999` với body hợp lệ | Service không tìm thấy bác sĩ; dự kiến `404 Not Found` | X3 |
| POST-BVA-11 | Bỏ query parameter `doctorId` | Request bị từ chối do thiếu tham số bắt buộc; ghi status/body thực tế | X1 |
| POST-BVA-12 | Gửi `doctorId=abc` | Request bị từ chối do không bind được sang `Long`; dự kiến `400 Bad Request` | X2 |
| POST-BVA-13 | Body hợp lệ, lần lượt đặt `duration` là `null`, `0`, `1` | DTO không có validation min/max cho `duration`; ghi nhận request có được tạo hay không. Không tự kết luận `0` là hợp lệ theo nghiệp vụ | B8 |
| POST-BVA-14 | Body hợp lệ nhưng `doctorId=0` hoặc `doctorId=-1` | ID không có validation dương ở controller; service sẽ tìm bác sĩ theo ID và dự kiến trả `404 Not Found` nếu không tồn tại | B2 |
| POST-BVA-15 | Body JSON sai cú pháp, ví dụ `{ "patientId": 1,` | Request bị từ chối khi parse JSON; dự kiến `400 Bad Request` | X11 |
| POST-BVA-16 | `doctorId=9223372036854775808` | Query vượt miền `Long`; request bị từ chối khi bind tham số | X12 |
| POST-BVA-17 | Body hợp lệ nhưng `patientId=0` hoặc `patientId=-1` | Không có validation dương cho ID trong DTO; dự kiến `404 Not Found` nếu không có bệnh nhân tương ứng | B3 |
| POST-BVA-18 | Body hợp lệ nhưng `appointmentId=0` hoặc `appointmentId=-1` | Không có validation dương cho ID trong DTO; dự kiến `404 Not Found` nếu không có lịch hẹn tương ứng | B4 |
| POST-BVA-19 | Body hợp lệ; lần lượt dùng `medication="A"`, `dosage="1"`, `frequency="1"` | Các chuỗi 1 ký tự không rỗng; validation `@NotBlank` cho phép qua bước kiểm tra này. Kết quả cuối phụ thuộc dữ liệu và quy tắc nghiệp vụ khác | B5, B6, B7 |

## 5. Tiền điều kiện chạy test

- Dùng môi trường test và token hợp lệ có role `DOCTOR`; controller có `@PreAuthorize("hasRole('DOCTOR')")`.
- Chuẩn bị bác sĩ, bệnh nhân và lịch hẹn tồn tại; dùng ID đúng theo dữ liệu trong database thay vì mặc định rằng ID `1` luôn tồn tại.
- Với POST, dùng body hợp lệ làm dữ liệu nền và chỉ thay đổi trường đang kiểm tra để kết quả dễ phân tích.
- Với các test tạo đơn thuốc thành công, kiểm tra database hoặc GET tương ứng để xác nhận bản ghi đã được tạo; tránh dùng lại dữ liệu nếu điều đó gây trùng lặp ngoài ý muốn.
- Ghi lại **status code, response body, kết quả thực tế và ảnh Postman** cho mỗi trường hợp.
- Các dòng ghi “gửi lần lượt” là nhiều request riêng biệt, mỗi request chỉ thay đổi giá trị được nêu.

## 6. Căn cứ mã nguồn

- `backend/src/main/java/com/hospital/management/controller/DoctorController.java`
- `backend/src/main/java/com/hospital/management/dto/PrescriptionRequest.java`
- `backend/src/main/java/com/hospital/management/service/MedicalRecordService.java`
- `backend/src/main/java/com/hospital/management/service/PrescriptionService.java`
- `backend/src/main/java/com/hospital/management/repository/MedicalRecordRepository.java`
- `backend/src/main/java/com/hospital/management/repository/PrescriptionRepository.java`

## 7. Trạng thái và bàn giao

- **Trạng thái Jira:** `In Review` — chờ trưởng nhóm kiểm tra.
- **GitHub path đề xuất:** `docs/api-review/hai-bang/doctor-medical-records-prescriptions-bva-test-cases.md`
- **GitHub branch:** cập nhật theo branch/task Jira của nhóm.
- **Commit / Pull Request:** bổ sung link sau khi commit, push và tạo PR (nếu nhóm sử dụng PR).

## 8. Tiêu chí nghiệm thu

- Có bảng điều kiện test và phân vùng hợp lệ/không hợp lệ cho cả hai API.
- Có test case hợp lệ, không hợp lệ và các giá trị biên quan trọng.
- Mỗi test case ghi rõ input, expected outcome và tag điều kiện được bao phủ.
- Phân biệt rõ kết quả dự kiến dựa trên mã nguồn với kết quả thực tế cần xác minh khi chạy Postman.
- File được lưu đúng thư mục và push lên GitHub để leader review.
