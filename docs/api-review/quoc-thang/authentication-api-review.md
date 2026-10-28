# Bảng điều kiện test và thiết kế test case BVA

## 1. POST `/api/auth/register`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `username` | Chuỗi không rỗng, chưa tồn tại trong hệ thống | V1 | Thiếu, `null`, chuỗi rỗng hoặc chỉ có khoảng trắng; username đã tồn tại | X1, X2 | Giá trị tối thiểu được xác nhận bởi code là chuỗi có ít nhất một ký tự không phải khoảng trắng; không có min/max length khai báo | B1 |
| `password` | Chuỗi không rỗng | V2 | Thiếu, `null`, chuỗi rỗng hoặc chỉ có khoảng trắng | X3 | Chuỗi không rỗng / chuỗi rỗng / chỉ khoảng trắng | B2, B3, B4 |
| `email` | Email đúng định dạng, chưa được sử dụng | V3 | Thiếu, `null`, rỗng; sai định dạng; email đã tồn tại | X4, X5, X6 | Địa chỉ email đúng định dạng ngắn nhất phù hợp với validator cần được kiểm chứng; code không khai báo giới hạn độ dài | B5 |
| `firstName`, `lastName` | Chuỗi không rỗng | V4 | Thiếu, `null`, rỗng hoặc chỉ có khoảng trắng | X7, X8 | Chuỗi có nội dung / rỗng / chỉ khoảng trắng | B6, B7 |
| `phoneNumber` | Đúng 10 chữ số (`^[0-9]{10}$`) | V5 | 9 chữ số, 11 chữ số, có chữ cái/ký tự đặc biệt, thiếu/null/rỗng | X9, X10, X11, X12 | 9 chữ số; đúng 10 chữ số; 11 chữ số | B8, B9, B10 |
| `role` | `PATIENT`, `DOCTOR`, `ADMIN` (service chuyển thành chữ hoa trước khi parse) | V6 | Thiếu/null/rỗng; giá trị không thuộc enum, ví dụ `ABC` | X13, X14 | Một role hợp lệ; chuỗi rỗng; role không hợp lệ | B11, B12, B13 |
| `address`, `dateOfBirth`, `bloodGroup`, `emergencyContact` | Có thể bỏ qua theo DTO; nếu gửi ngày sinh thì dùng định dạng ISO `yyyy-MM-dd` | V7 | `dateOfBirth` sai định dạng có thể phát sinh lỗi khi `LocalDate.parse` (cần xác minh) | X15 | Không gửi trường tùy chọn / gửi ngày hợp lệ; không có giới hạn độ dài được khai báo | B14 |

> **Căn cứ và giới hạn:** `RegistrationRequest` khai báo `@NotBlank` cho `username`, `password`, `email`, `firstName`, `lastName`, `phoneNumber`, `role`; `email` có thêm `@Email`; `phoneNumber` có `@Pattern(regexp = "^[0-9]{10}$")`. Không thấy giới hạn min/max độ dài cho username, password, email, firstName hoặc lastName. ID và các giới hạn độ dài khác không được tự suy diễn thành quy tắc nghiệp vụ. `AuthController` dùng `@Valid` cho request đăng ký. Việc xử lý `role` không thuộc enum và ngày sinh sai định dạng cần được xác minh bằng request thực tế.

### Thiết kế test cases

Dữ liệu nền hợp lệ dùng cho các test đăng ký (thay username/email bằng giá trị mới cho mỗi lần chạy):

```json
{
  "username": "bva_patient_01",
  "password": "Test@1234",
  "email": "bva_patient_01@example.com",
  "firstName": "Test",
  "lastName": "Patient",
  "phoneNumber": "0123456789",
  "role": "PATIENT",
  "address": "Ho Chi Minh City",
  "dateOfBirth": "2000-01-01",
  "bloodGroup": "O+",
  "emergencyContact": "0987654321"
}
```

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| REG-BVA-01 | Gửi body hợp lệ ở trên với username/email chưa tồn tại | `200 OK`; body `User registered successfully`; user được tạo. Với `PATIENT`, service thực hiện tạo thêm hồ sơ Patient | V1, V2, V3, V4, V5, V6, V7, B1, B6, B9, B11 |
| REG-BVA-02 | Lần lượt bỏ `username`, đặt `username: null`, `username: ""`, `username: "   "` | Dự kiến validation từ chối (`4xx`, thường là `400 Bad Request`); xác nhận response thực tế | X1, B1 |
| REG-BVA-03 | Lần lượt bỏ `password`, đặt `password: null`, `password: ""`, `password: "   "` | Dự kiến validation từ chối (`4xx`); không tạo tài khoản | X3, B2, B3, B4 |
| REG-BVA-04 | Lần lượt bỏ `email`, đặt `email: null`, `email: ""`, `email: "not-an-email"` | Dự kiến validation từ chối (`4xx`); không tạo tài khoản | X4, X5, B5 |
| REG-BVA-05 | Lần lượt đặt `firstName` thành `""`/`"   "`, rồi làm tương tự với `lastName` | Dự kiến validation từ chối (`4xx`) cho trường không có nội dung | X7, X8, B6, B7 |
| REG-BVA-06 | Body hợp lệ nhưng `phoneNumber: "123456789"` (9 chữ số) | Dự kiến validation từ chối (`4xx`) | X9, B8 |
| REG-BVA-07 | Body hợp lệ với `phoneNumber: "0123456789"` (đúng 10 chữ số) | Dự kiến vượt qua validation số điện thoại; dùng username/email mới để tránh trùng dữ liệu | V5, B9 |
| REG-BVA-08 | Body hợp lệ nhưng `phoneNumber: "01234567890"` (11 chữ số) | Dự kiến validation từ chối (`4xx`) | X10, B10 |
| REG-BVA-09 | Lần lượt đặt `phoneNumber: "012345678a"` và `phoneNumber: "01234-6789"` | Dự kiến validation từ chối do có ký tự không phải chữ số | X11, X12 |
| REG-BVA-10 | Lần lượt bỏ `role`, đặt `role: null` hoặc `role: ""` | Dự kiến validation từ chối (`4xx`) | X13, B12 |
| REG-BVA-11 | Lần lượt đăng ký với `role: "PATIENT"`, `"DOCTOR"`, `"ADMIN"`; mỗi lần dùng username/email mới | Dự kiến `200 OK` cho từng role hợp lệ; xác minh role được lưu đúng. Role `PATIENT` cần tạo thêm hồ sơ Patient | V6, B11 |
| REG-BVA-12 | Gửi `role: "ABC"` với các trường còn lại hợp lệ | Request phải bị từ chối; ghi nhận status thực tế và kiểm tra có phát sinh `500` thay vì lỗi đầu vào `4xx` hay không | X14, B13 |
| REG-BVA-13 | Gửi body hợp lệ nhưng dùng username đã tồn tại | Từ chối đăng ký trùng username; không tạo tài khoản mới | X2 |
| REG-BVA-14 | Gửi body hợp lệ nhưng dùng email đã tồn tại | Từ chối đăng ký trùng email; không tạo tài khoản mới | X6 |
| REG-BVA-15 | Bỏ các trường tùy chọn `address`, `dateOfBirth`, `bloodGroup`, `emergencyContact` | Theo DTO, các trường này không có validation bắt buộc; dự kiến đăng ký được nếu các trường bắt buộc hợp lệ | V7, B14 |
| REG-BVA-16 | Với role `PATIENT`, lần lượt gửi `dateOfBirth: "2000-01-01"` và `dateOfBirth: "01-01-2000"` | Ngày ISO hợp lệ được parse; ngày sai định dạng cần bị xử lý an toàn. Ghi nhận status thực tế, không mặc định rằng code đã validate định dạng trước | X15, B14 |

## 2. POST `/api/auth/login`

### Bảng điều kiện test

| Conditions | Valid Partitions | Tag | Invalid Partitions | Tag | Valid Boundaries | Tag |
|---|---|---|---|---|---|---|
| `username` | Username của tài khoản tồn tại | V1 | Tài khoản không tồn tại; thiếu/null/rỗng/chuỗi chỉ có khoảng trắng | X1, X2 | Username tồn tại nhỏ nhất theo fixture test (ví dụ `1`); không có giới hạn độ dài khai báo | B1 |
| `password` | Mật khẩu đúng của tài khoản | V2 | Sai mật khẩu; thiếu/null/rỗng/chuỗi chỉ có khoảng trắng | X3, X4 | Mật khẩu đúng; chuỗi rỗng; chỉ khoảng trắng | B2, B3, B4 |
| Request body | JSON có `username` và `password` | V3 | Body rỗng `{}`, thiếu trường hoặc JSON không hợp lệ | X5, X6 | Hai trường đều có giá trị / một trường thiếu / body `{}` | B5, B6 |
| Authentication response | Tài khoản hợp lệ có thể xác thực | V4 | Thông tin xác thực không hợp lệ | X7 | Response thành công có `token`, `type`, `userId`, `username`, `role` | B7 |

> **Lưu ý quan trọng:** `LoginRequest` chỉ khai báo hai trường `String username` và `String password`; không có annotation `@NotBlank`, `@Size` hoặc giới hạn min/max. `AuthController` dùng `@Valid`, nhưng DTO không khai báo constraint để `@Valid` thực thi. Vì vậy các trường hợp thiếu/rỗng phải được chạy trên môi trường thực tế; không khẳng định trước rằng chúng chắc chắn trả `400`. Service xác thực thông qua `AuthenticationManager`; đăng nhập thành công trả `200 OK` cùng `LoginResponse`. Username/password hợp lệ là dữ liệu fixture, không phải giá trị biên độ dài do code quy định.

### Thiết kế test cases

Dữ liệu nền đăng nhập hợp lệ (thay bằng tài khoản test có thật trong môi trường):

```json
{
  "username": "demo1",
  "password": "1"
}
```

| Test Case | Input | Expected Outcome | New Tags Covered |
|---|---|---|---|
| LOGIN-BVA-01 | Gửi thông tin đúng của tài khoản Admin test | `200 OK`; response có `token`, `type`, `userId`, `username`, `role`; role phải đúng tài khoản | V1, V2, V3, V4, B1, B2, B7 |
| LOGIN-BVA-02 | Gửi thông tin đúng của tài khoản Doctor test | `200 OK`; response có token và `role: "DOCTOR"` | V1, V2, V4, B7 |
| LOGIN-BVA-03 | Gửi thông tin đúng của tài khoản Patient test | `200 OK`; response có token và `role: "PATIENT"` | V1, V2, V4, B7 |
| LOGIN-BVA-04 | Username tồn tại nhưng password sai | Từ chối xác thực; dự kiến `4xx`/`401` tùy cơ chế xử lý exception hiện tại; không trả token | X3, X7 |
| LOGIN-BVA-05 | Username không tồn tại và password bất kỳ | Từ chối xác thực; không trả token. Ghi nhận status/body thực tế | X1, X7 |
| LOGIN-BVA-06 | Lần lượt bỏ `username`, đặt `username: null`, `username: ""`, `username: "   "` | Request phải không đăng nhập thành công; ghi nhận status/body thực tế vì DTO không khai báo validation | X2, B1 |
| LOGIN-BVA-07 | Lần lượt bỏ `password`, đặt `password: null`, `password: ""`, `password: "   "` | Request phải không đăng nhập thành công; ghi nhận status/body thực tế vì DTO không khai báo validation | X4, B3, B4 |
| LOGIN-BVA-08 | Gửi body rỗng `{}` | Không đăng nhập thành công; ghi nhận status/body thực tế. Không giả định `400` khi chưa chạy test | X5, B5 |
| LOGIN-BVA-09 | Gửi JSON sai cú pháp, ví dụ `{"username":"1", "password":}` | Request bị từ chối do JSON không hợp lệ; dự kiến `4xx` | X6, B6 |
| LOGIN-BVA-10 | Gửi đúng username/password nhưng kiểm tra response JSON | Xác minh đủ `token`, `type`, `userId`, `username`, `role`; `type` dự kiến `Bearer` theo `AuthService` | V4, B7 |

## 3. Tiền điều kiện chạy test

- Chạy trên môi trường test/local, ví dụ `http://localhost:8080`; cả hai endpoint là public và không yêu cầu Bearer Token.
- Header: `Content-Type: application/json`; Method: `POST`.
- Chuẩn bị tài khoản test có role `ADMIN`, `DOCTOR`, `PATIENT`, với thông tin đăng nhập đã xác minh.
- Với đăng ký thành công, dùng username và email mới cho từng lần chạy; dùng dữ liệu test, không dùng thông tin cá nhân thật.
- Với test trùng username/email, tạo trước tài khoản fixture tương ứng hoặc dùng tài khoản test đã biết tồn tại.
- Các kết quả ghi “dự kiến” cần được đối chiếu bằng Postman. Ghi status code, response body và kết quả thực tế trước khi kết luận pass/fail.
- Một dòng test case ghi “lần lượt” nghĩa là gửi nhiều request riêng biệt, mỗi request chỉ thay đổi trường được nêu để cô lập điều kiện kiểm thử.

## 4. Căn cứ mã nguồn

- `backend/src/main/java/com/hospital/management/controller/AuthController.java`
- `backend/src/main/java/com/hospital/management/dto/RegistrationRequest.java`
- `backend/src/main/java/com/hospital/management/dto/LoginRequest.java`
- `backend/src/main/java/com/hospital/management/dto/LoginResponse.java`
- `backend/src/main/java/com/hospital/management/service/AuthService.java`
- `backend/src/main/java/com/hospital/management/model/User.java`
- `backend/src/main/java/com/hospital/management/config/SecurityConfig.java`

## 5. Trạng thái bàn giao

- Trạng thái: `In Review` – chờ trưởng nhóm kiểm tra.
- File này là thiết kế test case BVA; chỉ chuyển `Done` sau khi trưởng nhóm kiểm tra và các kết quả thực thi (nếu được yêu cầu) đã được cập nhật.
- Thư mục báo cáo: `docs/api-review/quoc-thang/`.
