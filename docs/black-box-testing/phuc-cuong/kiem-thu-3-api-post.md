# Thiết kế kiểm thử EP, BVA và Decision Table cho 3 API POST

**Dự án:** Hospital Management System  
**Phạm vi:**  
1. `POST /api/auth/login`
2. `POST /api/doctor/medical-records`
3. `POST /api/doctor/prescriptions`

> Tài liệu được xây dựng dựa trên source code trong ZIP: `AuthController`, `LoginRequest`, `AuthService`, `DoctorController`, `MedicalRecordRequest`, `MedicalRecordService`, `PrescriptionRequest`, `PrescriptionService` và các model liên quan.
>
> **Quy ước:** EP dùng các lớp `V` (hợp lệ) và `X` (không hợp lệ). BVA dùng các tag `B1, B2, ...` theo thứ tự biến và vị trí `min, min+, nominal, max-, max`. Decision Table dùng `C` cho Condition, `A` cho Action, `R` cho Rule. `Y` = có/đúng, `N` = không/sai, `-` = không ảnh hưởng/không xét.

---

# 1. POST /api/auth/login

## 1.1. Thông tin API và dữ liệu đầu vào

- **Method:** `POST`
- **URL:** `/api/auth/login`
- **Authorization:** Không yêu cầu Bearer Token để đăng nhập.
- **Content-Type:** `application/json`
- **Request Body:**

```json
{
  "username": "2",
  "password": "2"
}
```

`LoginRequest` có hai thuộc tính kiểu `String`: `username`, `password`. Trong source hiện tại, hai thuộc tính này **không có annotation giới hạn độ dài hoặc `@NotBlank`**. `AuthService.login()` xác thực username/password qua `AuthenticationManager`, sau đó trả token và thông tin người dùng.

**Kết quả thành công:** HTTP `200 OK`, response có `token`, `type`, `userId`, `username`, `role`.  
**Kết quả không xác thực được:** thông thường là `401 Unauthorized` theo cấu hình Spring Security; cần chạy Postman để xác nhận response thực tế của môi trường.

## 1.2. Bảng phân vùng tương đương (EP)

Theo đúng mẫu bài tập, mỗi biến có lớp hợp lệ, các lớp không hợp lệ và tag riêng. Với LoginRequest, code chỉ khai báo `String username`, `String password`, không có `@NotBlank`, `@Size`, min/max. Vì vậy các lớp dưới đây phân biệt dữ liệu đăng nhập theo nghiệp vụ; không tự đặt giới hạn độ dài.

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| Username | Username của tài khoản tồn tại | V1 | (a) Username không tồn tại<br>(b) Username rỗng `""`<br>(c) Username bị thiếu/null | X1, X2, X3 |
| Password | Password đúng với username | V2 | (a) Password sai<br>(b) Password rỗng `""`<br>(c) Password bị thiếu/null | X4, X5, X6 |

**Lưu ý:** Username tồn tại và password đúng phải đi cùng nhau mới tạo thành một request đăng nhập hợp lệ. Các lớp rỗng/thiếu/null được đưa vào để kiểm thử đầu vào; source không khai báo validation riêng cho các trường này nên cần ghi nhận status thực tế từ Postman.

## 1.3. Bảng phân tích giá trị biên (BVA)

Bài mẫu yêu cầu 7 vị trí `min-`, `min`, `min+`, `nominal`, `max-`, `max`, `max+`. Tuy nhiên, `LoginRequest` không quy định độ dài tối thiểu/tối đa cho username và password. Do đó **không có cơ sở xác định min/max thực tế**. Không nên tự điền các số ký tự giả định.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| Username | Chưa quy định | Chưa quy định | Chưa quy định | Username tài khoản test | Chưa quy định | Chưa quy định | Chưa quy định | B1–B7: chưa xác định được |
| Password | Chưa quy định | Chưa quy định | Chưa quy định | Password tài khoản test | Chưa quy định | Chưa quy định | Chưa quy định | B8–B14: chưa xác định được |

**Kiểm tra bổ sung:** Có thể test chuỗi rỗng, thiếu trường và null, nhưng đây là các lớp EP/kiểm tra validation đầu vào, không được gọi là `min-` nếu hệ thống chưa định nghĩa min length.

## 1.4. Bảng quyết định (Decision Table)

### Conditions / Actions

- C1: Username tồn tại?
- C2: Password có khớp với username?
- C3: Tài khoản được AuthenticationManager chấp nhận/được phép đăng nhập?
- A1: Trả HTTP 200 và token.
- A2: Từ chối đăng nhập.

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| C1. Username tồn tại | Y | Y | N | Y | Y |
| C2. Password chính xác | Y | N | - | Y | Y |
| C3. Tài khoản được phép xác thực | Y | - | - | N | Y |
| A1. Trả 200 + token | X | - | - | - | X |
| A2. Từ chối đăng nhập | - | X | X | X | - |

**Diễn giải Rules:**
- R1: Tài khoản tồn tại, password đúng, tài khoản được phép xác thực → đăng nhập thành công.
- R2: Tài khoản tồn tại nhưng password sai → từ chối.
- R3: Username không tồn tại → từ chối.
- R4: Tài khoản không được phép xác thực (ví dụ bị vô hiệu hóa nếu cấu hình xác thực có kiểm tra trạng thái) → từ chối.
- R5: Trường hợp hợp lệ tổng quát, dùng để xác nhận luồng đăng nhập thành công. R1 và R5 có thể gộp nếu không cần tách riêng điều kiện trạng thái.

**Lưu ý triển khai:** `AuthService` gọi `AuthenticationManager`; việc kiểm tra trạng thái active phụ thuộc cấu hình `UserDetailsService`/Spring Security. Cần đối chiếu cấu hình xác thực khi chạy R4. Source `LoginRequest` hiện không có validation annotation nên các trường thiếu/null có thể cho status khác nhau tùy exception handler.

---

# 2. POST /api/doctor/medical-records

## 2.1. Thông tin API và dữ liệu đầu vào

- **Method:** `POST`
- **URL:** `/api/doctor/medical-records?doctorId={doctorId}`
- **Authorization:** Bearer Token có role `DOCTOR` (`@PreAuthorize("hasRole('DOCTOR')")`).
- **Content-Type:** `application/json`
- **Query Parameter:** `doctorId` kiểu `Long`.
- **Request Body:**

```json
{
  "patientId": 3,
  "diagnosis": "Cảm cúm",
  "symptoms": "Sốt, ho",
  "consultationNotes": "Nghỉ ngơi và theo dõi",
  "labResults": "Chưa có",
  "recommendations": "Uống đủ nước"
}
```

`MedicalRecordRequest` quy định:
- `patientId`: `@NotNull` — bắt buộc, không được null.
- `diagnosis`: `@NotBlank` — bắt buộc, không được null/rỗng/toàn khoảng trắng.
- `symptoms`, `consultationNotes`, `labResults`, `recommendations`: tùy chọn, không có giới hạn độ dài trong DTO.
- Service tìm Doctor theo `doctorId`, tìm Patient theo `patientId`; không tìm thấy thì ném `ResourceNotFoundException`.

## 2.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| doctorId (Query) | ID bác sĩ tồn tại | V1 | (a) Thiếu/null doctorId<br>(b) doctorId không phải số Long<br>(c) doctorId không tồn tại | X1, X2, X3 |
| patientId (Body) | ID bệnh nhân tồn tại | V2 | (a) Thiếu/null patientId<br>(b) patientId không tồn tại | X4, X5 |
| diagnosis (Body) | Chuỗi có ít nhất một ký tự không phải khoảng trắng | V3 | (a) Chuỗi rỗng<br>(b) Chỉ có khoảng trắng<br>(c) Thiếu/null | X6, X7, X8 |
| symptoms (Body) | Chuỗi có nội dung hoặc không gửi trường | V4 | Không có lớp không hợp lệ được quy định trong DTO | — |
| consultationNotes (Body) | Chuỗi có nội dung hoặc không gửi trường | V5 | Không có lớp không hợp lệ được quy định trong DTO | — |
| labResults (Body) | Chuỗi có nội dung hoặc không gửi trường | V6 | Không có lớp không hợp lệ được quy định trong DTO | — |
| recommendations (Body) | Chuỗi có nội dung hoặc không gửi trường | V7 | Không có lớp không hợp lệ được quy định trong DTO | — |
| Authorization | Bearer Token hợp lệ, role DOCTOR | V8 | (a) Thiếu/sai/hết hạn token<br>(b) Token hợp lệ nhưng role không phải DOCTOR | X9, X10 |

**Căn cứ code:** `patientId` có `@NotNull`; `diagnosis` có `@NotBlank`. Bốn trường mô tả còn lại không có annotation bắt buộc hoặc giới hạn. Controller yêu cầu role `DOCTOR`.

## 2.3. Bảng phân tích giá trị biên (BVA)

Theo mẫu, mỗi biến có 7 vị trí `min-`, `min`, `min+`, `nominal`, `max-`, `max`, `max+`. Chỉ điền được biên khi source có quy định miền giá trị. Trong DTO hiện tại, ID chỉ có `@NotNull`, không có `@Positive`, `@Min`, `@Max`; diagnosis chỉ có `@NotBlank`, không có max length.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| doctorId | Không quy định | Không quy định | Không quy định | ID bác sĩ tồn tại | Không quy định | Không quy định | Không quy định | B1–B7: chưa xác định |
| patientId | null/thiếu (EP X4) | Không có min số học | Không xác định | ID bệnh nhân tồn tại | Không quy định | Không quy định | Không quy định | B8–B14: chỉ kiểm tra NotNull/tồn tại |
| diagnosis | `""`/toàn khoảng trắng (X6/X7) | 1 ký tự không trắng | 2 ký tự không trắng | `"Cảm cúm"` | Không quy định | Không quy định | Không quy định | B15–B21: chỉ có biên dưới NotBlank |

Các trường `symptoms`, `consultationNotes`, `labResults`, `recommendations` không có ràng buộc min/max trong DTO nên không lập được BVA độ dài có căn cứ. Có thể kiểm tra trường bị bỏ qua hoặc có nội dung như kiểm thử EP.

**Quan trọng:** Với `diagnosis`, `@NotBlank` cho phép xác định ngưỡng tối thiểu là phải có ít nhất một ký tự không phải khoảng trắng. Không có giới hạn tối đa nên không được tự tạo `max-`, `max`, `max+`. Với ID, `@NotNull` không đồng nghĩa với `min = 1`; giá trị 0/âm chỉ được kiểm tra theo kết quả tìm ID trong database, không phải lỗi min/max đã khai báo.

## 2.4. Bảng quyết định (Decision Table)

### Conditions / Actions

- C1: Bearer Token hợp lệ và role là DOCTOR?
- C2: doctorId có tồn tại?
- C3: patientId có giá trị và khác null?
- C4: Patient có tồn tại?
- C5: diagnosis không rỗng/không chỉ có khoảng trắng?
- A1: Tạo MedicalRecord và trả HTTP 200.
- A2: Từ chối do xác thực/phân quyền.
- A3: Từ chối do dữ liệu đầu vào.
- A4: Từ chối do không tìm thấy Doctor/Patient.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1. Token hợp lệ, role DOCTOR | Y | N | Y | Y | Y | Y |
| C2. doctorId tồn tại | Y | - | N | Y | Y | Y |
| C3. patientId khác null | Y | - | Y | N | Y | Y |
| C4. Patient tồn tại | Y | - | - | - | N | Y |
| C5. diagnosis hợp lệ | Y | - | Y | Y | Y | N |
| A1. Tạo hồ sơ, trả 200 | X | - | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | - | - | - |
| A3. Từ chối validation dữ liệu | - | - | - | X | - | X |
| A4. Báo không tìm thấy tài nguyên | - | - | X | - | X | - |

**Diễn giải Rules:**
- R1: Tất cả điều kiện hợp lệ → tạo hồ sơ bệnh án.
- R2: Không có token hợp lệ hoặc role không phải DOCTOR → từ chối.
- R3: doctorId không tồn tại → không tạo hồ sơ.
- R4: patientId thiếu/null → validation thất bại.
- R5: patientId không tồn tại → không tạo hồ sơ.
- R6: diagnosis rỗng/null/toàn khoảng trắng → validation thất bại.

**Lưu ý:** Nếu `doctorId` thiếu hoặc không chuyển được sang Long, Spring có thể trả lỗi binding/request parameter trước khi vào service. Nếu `patientId` hoặc `diagnosis` vi phạm validation, response status phụ thuộc exception handler. Xác nhận mã HTTP thực tế bằng Postman.

---

# 3. POST /api/doctor/prescriptions

## 3.1. Thông tin API và dữ liệu đầu vào

- **Method:** `POST`
- **URL:** `/api/doctor/prescriptions?doctorId={doctorId}`
- **Authorization:** Bearer Token có role `DOCTOR` (`@PreAuthorize("hasRole('DOCTOR')")`).
- **Content-Type:** `application/json`
- **Query Parameter:** `doctorId` kiểu `Long`.
- **Request Body:**

```json
{
  "patientId": 3,
  "appointmentId": 10,
  "medication": "Paracetamol",
  "dosage": "500mg",
  "frequency": "2 lần/ngày",
  "duration": 5,
  "instructions": "Uống sau ăn"
}
```

`PrescriptionRequest` quy định:
- `patientId`: `@NotNull` — bắt buộc.
- `appointmentId`: `@NotNull` — bắt buộc theo validation hiện tại.
- `medication`: `@NotBlank` — bắt buộc, không rỗng/toàn khoảng trắng.
- `dosage`: `@NotBlank` — bắt buộc, không rỗng/toàn khoảng trắng.
- `frequency`: `@NotBlank` — bắt buộc, không rỗng/toàn khoảng trắng.
- `duration`: `Integer`, không có `@Min`/`@Max`; không có ràng buộc số ngày trong DTO.
- `instructions`: tùy chọn, không có giới hạn độ dài trong DTO.

Service tìm Doctor, Patient và Appointment theo ID; nếu không tìm thấy thì ném `ResourceNotFoundException`. Sau đó tạo Prescription và đặt `notificationSent = false`.

**Điểm cần chú ý:** Service có nhánh xử lý `appointmentId == null`, nhưng DTO lại khai báo `@NotNull` cho `appointmentId`. Do controller dùng `@Valid`, request thiếu/null appointmentId phải bị validation từ chối trước khi vào service trong luồng thông thường. Bảng kiểm thử bên dưới tuân theo validation đang khai báo ở DTO.

## 3.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| doctorId (Query) | ID bác sĩ tồn tại | V1 | (a) Thiếu/null doctorId<br>(b) doctorId không phải số Long<br>(c) doctorId không tồn tại | X1, X2, X3 |
| patientId (Body) | ID bệnh nhân tồn tại | V2 | (a) Thiếu/null patientId<br>(b) patientId không tồn tại | X4, X5 |
| appointmentId (Body) | ID lịch hẹn tồn tại | V3 | (a) Thiếu/null appointmentId<br>(b) appointmentId không tồn tại | X6, X7 |
| medication (Body) | Chuỗi có ít nhất một ký tự không phải khoảng trắng | V4 | (a) Rỗng<br>(b) Chỉ có khoảng trắng<br>(c) Thiếu/null | X8, X9, X10 |
| dosage (Body) | Chuỗi có ít nhất một ký tự không phải khoảng trắng | V5 | (a) Rỗng<br>(b) Chỉ có khoảng trắng<br>(c) Thiếu/null | X11, X12, X13 |
| frequency (Body) | Chuỗi có ít nhất một ký tự không phải khoảng trắng | V6 | (a) Rỗng<br>(b) Chỉ có khoảng trắng<br>(c) Thiếu/null | X14, X15, X16 |
| duration (Body) | Số nguyên được gửi hoặc không gửi trường | V7 | Không có min/max hoặc bắt buộc trong DTO; sai kiểu dữ liệu có thể bị lỗi binding | X17 |
| instructions (Body) | Chuỗi có nội dung hoặc không gửi trường | V8 | Không có lớp không hợp lệ được quy định trong DTO | — |
| Authorization | Bearer Token hợp lệ, role DOCTOR | V9 | (a) Thiếu/sai/hết hạn token<br>(b) Token hợp lệ nhưng role không phải DOCTOR | X18, X19 |

**Căn cứ code:** `patientId`, `appointmentId` có `@NotNull`; `medication`, `dosage`, `frequency` có `@NotBlank`. `duration` và `instructions` không có annotation bắt buộc hoặc min/max. Controller yêu cầu role `DOCTOR`.

## 3.3. Bảng phân tích giá trị biên (BVA)

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| doctorId | Không quy định | Không quy định | Không quy định | ID bác sĩ tồn tại | Không quy định | Không quy định | Không quy định | B1–B7: chưa xác định |
| patientId | null/thiếu (EP X4) | Không có min số học | Không xác định | ID bệnh nhân tồn tại | Không quy định | Không quy định | Không quy định | B8–B14: chỉ kiểm tra NotNull/tồn tại |
| appointmentId | null/thiếu (EP X6) | Không có min số học | Không xác định | ID lịch hẹn tồn tại | Không quy định | Không quy định | Không quy định | B15–B21: chỉ kiểm tra NotNull/tồn tại |
| medication | `""`/toàn khoảng trắng (X8/X9) | 1 ký tự không trắng | 2 ký tự không trắng | `"Paracetamol"` | Không quy định | Không quy định | Không quy định | B22–B28: chỉ có biên dưới NotBlank |
| dosage | `""`/toàn khoảng trắng (X11/X12) | 1 ký tự không trắng | 2 ký tự không trắng | `"500mg"` | Không quy định | Không quy định | Không quy định | B29–B35: chỉ có biên dưới NotBlank |
| frequency | `""`/toàn khoảng trắng (X14/X15) | 1 ký tự không trắng | 2 ký tự không trắng | `"2 lần/ngày"` | Không quy định | Không quy định | Không quy định | B36–B42: chỉ có biên dưới NotBlank |
| duration | Không quy định | Không quy định | Không quy định | `5` (giá trị ví dụ) | Không quy định | Không quy định | Không quy định | B43–B49: chưa xác định |
| instructions | Không quy định | Không quy định | Không quy định | `"Uống sau ăn"` | Không quy định | Không quy định | Không quy định | B50–B56: chưa xác định |

**Giải thích:** DTO không khai báo miền số ngày hợp lệ cho `duration`, không khai báo min/max độ dài cho các chuỗi và không khai báo min/max số học cho ID. Vì vậy chỉ xác định được biên dưới của các trường `@NotBlank` (phải có ít nhất một ký tự không trắng). Những vị trí không có đặc tả được ghi `Không quy định`, không tự điền con số giả định.

## 3.4. Bảng quyết định (Decision Table)

### Conditions / Actions

- C1: Token hợp lệ và role là DOCTOR?
- C2: doctorId tồn tại?
- C3: patientId khác null?
- C4: Patient tồn tại?
- C5: appointmentId khác null?
- C6: Appointment tồn tại?
- C7: medication, dosage và frequency đều không blank?
- A1: Tạo Prescription và trả HTTP 200.
- A2: Từ chối xác thực/phân quyền.
- A3: Từ chối validation dữ liệu.
- A4: Báo không tìm thấy Doctor/Patient/Appointment.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 | R7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| C1. Token DOCTOR hợp lệ | Y | N | Y | Y | Y | Y | Y |
| C2. doctorId tồn tại | Y | - | N | Y | Y | Y | Y |
| C3. patientId khác null | Y | - | Y | N | Y | Y | Y |
| C4. Patient tồn tại | Y | - | - | - | N | Y | Y |
| C5. appointmentId khác null | Y | - | Y | Y | Y | N | Y |
| C6. Appointment tồn tại | Y | - | - | - | - | - | N |
| C7. medication/dosage/frequency hợp lệ | Y | - | Y | Y | Y | Y | Y |
| A1. Tạo đơn thuốc, trả 200 | X | - | - | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | - | - | - | - |
| A3. Từ chối validation dữ liệu | - | - | - | X | - | X | - |
| A4. Báo không tìm thấy tài nguyên | - | - | X | - | X | - | X |

**Diễn giải Rules:**
- R1: Token DOCTOR hợp lệ, các ID tồn tại và các trường bắt buộc hợp lệ → tạo đơn thuốc.
- R2: Token thiếu/sai hoặc role không phải DOCTOR → từ chối.
- R3: doctorId không tồn tại → không tạo đơn.
- R4: patientId thiếu/null → validation thất bại.
- R5: patientId không tồn tại → không tạo đơn.
- R6: appointmentId thiếu/null → validation thất bại theo `@NotNull`.
- R7: appointmentId không tồn tại → không tạo đơn.

**Bổ sung cần kiểm thử riêng:** medication, dosage hoặc frequency rỗng/null/toàn khoảng trắng đều phải bị từ chối theo `@NotBlank`. Có thể tách thành ba Rules riêng để kiểm thử từng trường, hoặc giữ một Rule đại diện nếu mục tiêu là kiểm tra bảng quyết định ở mức điều kiện tổng hợp. Khi lập test case chi tiết, nên tách từng trường để xác định chính xác lỗi.

---

# 4. Tổng kết phạm vi EP, BVA và Decision Table

| API | EP – Phân vùng tương đương | BVA – Giá trị biên | Decision Table |
|---|---|---|---|
| POST /api/auth/login | Username, password: tồn tại, không tồn tại, sai, rỗng, thiếu, null | Chưa có min/max độ dài trong source; kiểm tra rỗng/thiếu/null là EP và kiểm tra bổ sung | Username tồn tại, password đúng, tài khoản được phép xác thực |
| POST /api/doctor/medical-records | doctorId, patientId, diagnosis, trường tùy chọn, quyền DOCTOR | NotBlank của diagnosis; ID kiểm tra tồn tại, không tự đặt max | Quyền, Doctor, Patient, patientId, diagnosis |
| POST /api/doctor/prescriptions | doctorId, patientId, appointmentId, medication, dosage, frequency, duration, instructions, quyền DOCTOR | NotBlank của medication/dosage/frequency; ID kiểm tra tồn tại; duration chưa có min/max | Quyền, Doctor, Patient, Appointment và các trường bắt buộc |

## 5. Những điểm cần xác nhận khi chạy Postman

1. **Login:** thử username/password hợp lệ, sai, rỗng, thiếu và null; ghi lại status thực tế. Source không có `@NotBlank` ở `LoginRequest`.
2. **Medical Records:** request cần `doctorId` ở Query Params, `patientId` và `diagnosis` trong Body. Các trường còn lại là tùy chọn theo DTO.
3. **Prescriptions:** request cần `doctorId` ở Query Params; `patientId`, `appointmentId`, `medication`, `dosage`, `frequency` trong Body. `duration` và `instructions` là tùy chọn.
4. Hai API Doctor yêu cầu Bearer Token có role `DOCTOR`.
5. Với ID không tồn tại, cần ghi status/message thực tế do exception handler quyết định.
6. Không đánh dấu test case Pass chỉ dựa trên bảng thiết kế; phải chạy request và ghi Actual Status/Actual Result.

## 6. Ví dụ Request Body để chạy thử

### 6.1. Login

```json
{
  "username": "2",
  "password": "2"
}
```

### 6.2. Create Medical Record

```http
POST /api/doctor/medical-records?doctorId=2
```

```json
{
  "patientId": 3,
  "diagnosis": "Cảm cúm",
  "symptoms": "Sốt, ho",
  "consultationNotes": "Bệnh nhân cần nghỉ ngơi",
  "labResults": "Chưa có",
  "recommendations": "Uống đủ nước"
}
```

### 6.3. Create Prescription

```http
POST /api/doctor/prescriptions?doctorId=2
```

```json
{
  "patientId": 3,
  "appointmentId": 10,
  "medication": "Paracetamol",
  "dosage": "500mg",
  "frequency": "2 lần/ngày",
  "duration": 5,
  "instructions": "Uống sau ăn"
}
```

> Các ID `2`, `3`, `10` chỉ là ví dụ minh họa. Khi chạy cần thay bằng doctorId, patientId và appointmentId thực sự tồn tại trong database test.
