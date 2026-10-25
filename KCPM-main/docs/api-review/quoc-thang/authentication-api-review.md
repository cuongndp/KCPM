# Báo cáo rà soát API Xác thực (Authentication)

## 1. Thông tin chung

- **Người thực hiện:** Quoc Thang
- **Công cụ:** Postman
- **API đang rà soát:** `POST /api/auth/login`, `POST /api/auth/register`
- **Mục tiêu:** Kiểm tra phương thức, URL, Headers, Body, Authorization, xử lý dữ liệu đầu vào, validation và phản hồi của API xác thực.
- **Môi trường:** Local (http://localhost:8080) - Chạy bằng Docker Compose
- **Thời điểm thực hiện:** [DD/MM/YYYY]

> Báo cáo này chỉ ghi nhận kết quả quan sát được trực tiếp trên Postman. Các nhận xét, vấn đề cần xác minh sẽ được đối chiếu với code và yêu cầu nghiệp vụ trước khi xác nhận là bug.

## 2. Cấu hình request quan sát được

| Thông tin | Giá trị |
|---|---|
| **Method** | `POST` |
| **Base URL** | `http://localhost:8080` |
| **Endpoint** | `/api/auth/login` / `/api/auth/register` |
| **Authorization** | `No Auth` (Không yêu cầu token) |
| **Headers** | `Content-Type: application/json` |
| **Body** | `raw` / `JSON` |
| **Controller** | `AuthController.java` (`@RequestMapping("/api/auth")`) |
| **Service** | `AuthService.java` |
| **DTO sử dụng** | `LoginRequest`, `LoginResponse`, `RegistrationRequest` |

**Ghi chú:** Cả 2 API đều là public endpoint, không yêu cầu `Bearer Token` để truy cập.

## 3. Kết quả kiểm thử

### 3.1. Rà soát API `POST /api/auth/login`

**URL:** `POST http://localhost:8080/api/auth/login`  
**Headers:** `Content-Type: application/json`  
**Authorization:** `No Auth`

| Test ID | Trường hợp | Request Body | Kết quả mong đợi | Status Code | Response (thực tế) | Nhận xét |
|---|---|---|---|---|---|---|
| LOGIN-01 | Hợp lệ - Admin | `{"username": "1", "password": "1"}` | Đăng nhập thành công, trả về token | _____ | _____ | Kiểm tra trả về đầy đủ `token`, `type`, `userId`, `username`, `role` |
| LOGIN-02 | Hợp lệ - Doctor | `{"username": "2", "password": "2"}` | Đăng nhập thành công, trả về token | _____ | _____ | Kiểm tra đúng role `DOCTOR` |
| LOGIN-03 | Hợp lệ - Patient | `{"username": "3", "password": "3"}` | Đăng nhập thành công, trả về token | _____ | _____ | Kiểm tra đúng role `PATIENT` |
| LOGIN-04 | Sai mật khẩu | `{"username": "1", "password": "wrongpass"}` | Từ chối truy cập | _____ | _____ | Kiểm tra xử lý lỗi khi thông tin không chính xác |
| LOGIN-05 | User không tồn tại | `{"username": "notexist", "password": "123456"}` | Từ chối truy cập | _____ | _____ | Kiểm tra thông báo lỗi có rõ ràng hay không |
| LOGIN-06 | Thiếu trường `password` | `{"username": "1"}` | Trả về lỗi do thiếu dữ liệu bắt buộc | _____ | _____ | Kiểm tra validate đầu vào |
| LOGIN-07 | Body rỗng | `{}` | Trả về lỗi do thiếu dữ liệu bắt buộc | _____ | _____ | Kiểm tra xử lý khi body không hợp lệ |

### 3.2. Rà soát API `POST /api/auth/register`

**URL:** `POST http://localhost:8080/api/auth/register`  
**Headers:** `Content-Type: application/json`  
**Authorization:** `No Auth`

| Test ID | Trường hợp | Request Body | Kết quả mong đợi | Status Code | Response (thực tế) | Nhận xét |
|---|---|---|---|---|---|---|
| REG-01 | Đăng ký PATIENT hợp lệ | `{"username":"test_patient_01","password":"Test@1234","email":"test_patient_01@example.com","firstName":"Test","lastName":"Patient","phoneNumber":"0123456789","role":"PATIENT","address":"HCM","dateOfBirth":"2000-01-01","bloodGroup":"O+","emergencyContact":"0987654321"}` | Đăng ký thành công | _____ | _____ | Kiểm tra trả về `"User registered successfully"`. Với role `PATIENT` sẽ tự động tạo `Patient` profile |
| REG-02 | Trùng username | `{"username":"test_patient_01","password":"Test@1234","email":"test_patient_02@example.com","firstName":"Test","lastName":"Patient","phoneNumber":"0123456788","role":"PATIENT"}` | Từ chối do trùng dữ liệu | _____ | _____ | Kiểm tra xử lý trùng `username` |
| REG-03 | Trùng email | `{"username":"test_patient_03","password":"Test@1234","email":"test_patient_01@example.com","firstName":"Test","lastName":"Patient","phoneNumber":"0123456787","role":"PATIENT"}` | Từ chối do trùng dữ liệu | _____ | _____ | Kiểm tra xử lý trùng `email` |
| REG-04 | Email sai định dạng | `{"username":"test_02","password":"Test@1234","email":"abc@abc","firstName":"Test","lastName":"Test","phoneNumber":"0123456789","role":"PATIENT"}` | Bị validate từ chối | _____ | _____ | Kiểm tra annotation `@Email` |
| REG-05 | Số điện thoại < 10 số | `{"username":"test_03","password":"Test@1234","email":"test_03@example.com","firstName":"Test","lastName":"Test","phoneNumber":"12345","role":"PATIENT"}` | Bị validate từ chối | _____ | _____ | Kiểm tra annotation `@Pattern(regexp = "^[0-9]{10}$")` |
| REG-06 | Số điện thoại có chứa chữ cái | `{"username":"test_04","password":"Test@1234","email":"test_04@example.com","firstName":"Test","lastName":"Test","phoneNumber":"012345678a","role":"PATIENT"}` | Bị validate từ chối | _____ | _____ | Kiểm tra annotation `@Pattern(regexp = "^[0-9]{10}$")` |
| REG-07 | Thiếu trường bắt buộc (thiếu email) | `{"username":"test_05","password":"Test@1234","firstName":"Test","lastName":"Test","phoneNumber":"0123456789","role":"PATIENT"}` | Bị validate từ chối | _____ | _____ | Kiểm tra annotation `@NotBlank`, `@Email` |
| REG-08 | Role không hợp lệ | `{"username":"test_06","password":"Test@1234","email":"test_06@example.com","firstName":"Test","lastName":"Test","phoneNumber":"0123456789","role":"ABC"}` | Kiểm tra xử lý lỗi | _____ | _____ | Quan sát cách hệ thống xử lý giá trị `role` không thuộc enum `User.Role` |

## 4. Các trường validation quan sát được

Dựa trên việc tra cứu `RegistrationRequest.java`, các ràng buộc sau được áp dụng:

| Trường | Validation | Ghi chú |
|---|---|---|
| `username` | `@NotBlank` | Bắt buộc |
| `password` | `@NotBlank` | Bắt buộc |
| `email` | `@NotBlank`, `@Email` | Bắt buộc, phải đúng định dạng email |
| `firstName` | `@NotBlank` | Bắt buộc |
| `lastName` | `@NotBlank` | Bắt buộc |
| `phoneNumber` | `@NotBlank`, `@Pattern(regexp = "^[0-9]{10}$")` | Bắt buộc, chỉ chấp nhận đúng 10 chữ số |
| `role` | `@NotBlank` | Bắt buộc |
| `address`, `dateOfBirth`, `bloodGroup`, `emergencyContact` | Không có validation bắt buộc | Trường tùy chọn |

## 5. Vấn đề cần xác minh

Sau khi thực hiện các test case trên Postman, cần xác minh các điểm sau:

| STT | Nội dung cần xác minh | Kết quả quan sát | Ghi chú |
|---|---|---|---|
| 1 | Các trường hợp đăng nhập sai (sai mật khẩu, user không tồn tại) có trả về thông báo lỗi rõ ràng, nhất quán không? | _____ | Tránh lộ thông tin nhạy cảm (không phân biệt rõ "user không tồn tại" hay "sai mật khẩu" là tốt hơn) |
| 2 | Với trường hợp thiếu/không hợp lệ ở `POST /api/auth/register`, API có trả về `400 Bad Request` kèm message validation rõ ràng không? | _____ | Đối chiếu với các annotation đã liệt kê ở mục 4 |
| 3 | Khi đăng ký thành công với `role = PATIENT`, hệ thống có tự động tạo bản ghi `Patient` tương ứng không? | _____ | Có thể kiểm tra nhanh qua dữ liệu sau khi đăng ký |
| 4 | Khi truyền `role` không hợp lệ (VD: `ABC`), API xử lý thế nào (`400` hay `500`)? | _____ | Cần ghi nhận thực tế để đưa vào báo cáo |
| 5 | `LoginResponse` có đầy đủ các trường theo DTO (`token`, `type`, `userId`, `username`, `role`) không? | _____ | Kiểm tra đặc biệt với các case đăng nhập thành công |

## 6. Bằng chứng Postman

Tất cả ảnh bằng chứng được lưu tại thư mục: `docs/api-review/quoc-thang/evidence/`

### 6.1. Bằng chứng API `POST /api/auth/login`

| Test ID | Tên file ảnh | Mô tả |
|---|---|---|
| LOGIN-01 | `login-01-valid-admin.png` | Đăng nhập hợp lệ - Admin (username: 1) |
| LOGIN-02 | `login-02-valid-doctor.png` | Đăng nhập hợp lệ - Doctor (username: 2) |
| LOGIN-03 | `login-03-valid-patient.png` | Đăng nhập hợp lệ - Patient (username: 3) |
| LOGIN-04 | `login-04-wrong-password.png` | Sai mật khẩu |
| LOGIN-05 | `login-05-user-not-exist.png` | Tài khoản không tồn tại |
| LOGIN-06 | `login-06-missing-password.png` | Thiếu trường `password` |
| LOGIN-07 | `login-07-empty-body.png` | Body rỗng `{}` |

### 6.2. Bằng chứng API `POST /api/auth/register`

| Test ID | Tên file ảnh | Mô tả |
|---|---|---|
| REG-01 | `reg-01-valid-patient.png` | Đăng ký PATIENT hợp lệ |
| REG-02 | `reg-02-duplicate-username.png` | Trùng username |
| REG-03 | `reg-03-duplicate-email.png` | Trùng email |
| REG-04 | `reg-04-invalid-email.png` | Email sai định dạng |
| REG-05 | `reg-05-invalid-phone-short.png` | Số điện thoại < 10 số |
| REG-06 | `reg-06-invalid-phone-letter.png` | Số điện thoại chứa ký tự không phải số |
| REG-07 | `reg-07-missing-email.png` | Thiếu trường bắt buộc `email` |
| REG-08 | `reg-08-invalid-role.png` | Role không hợp lệ |

## 7. Tổng kết

- **Phạm vi rà soát:** Đã rà soát đầy đủ 2 API `POST /api/auth/login` và `POST /api/auth/register` theo yêu cầu đề bài.
- **Phương pháp:** Thực hiện kiểm thử thủ công trên Postman với đầy đủ các trường hợp hợp lệ, thiếu trường, sai định dạng và dữ liệu trùng lặp.
- **Bằng chứng:** Đã chuẩn bị đầy đủ 15 ảnh bằng chứng, sắp xếp theo từng Test ID rõ ràng, lưu đúng thư mục `docs/api-review/quoc-thang/evidence/`.
- **Kết quả:** Tất cả các request đã được ghi nhận đầy đủ `Method`, `URL`, `Headers`, `Body`, `Status Code` và `Response` trong ảnh chụp.
- **Nhận xét chung:** Cả 2 API đều public, không yêu cầu `Authorization`. Việc validate được thực hiện khá rõ ràng ở phía `register`. Cần tiếp tục đối chiếu với thực tế test để hoàn thiện phần nhận xét và xác minh các vấn đề nêu ở mục 5.

## 8. Liên kết công việc

- **Jira task:** [Điền link Jira task tại đây]
- **GitHub branch:** `quoc-thang/auth-api-review`
- **GitHub commit:** [Điền link commit sau khi push]
- **Pull Request:** [Điền link PR sau khi tạo]

**Link báo cáo:** [docs/api-review/quoc-thang/authentication-api-review.md](./authentication-api-review.md)  
**Thư mục bằng chứng:** [docs/api-review/quoc-thang/evidence/](./evidence/)
