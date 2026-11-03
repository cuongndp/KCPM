# Phân hoạch lớp tương đương và phân tích giá trị biên

## 1. Phạm vi và quy ước

Tài liệu áp dụng kỹ thuật **Phân hoạch lớp tương đương (EP)** và **Phân tích giá trị biên (BVA)** cho năm API trong collection Postman Quốc Thắng:

1. `POST /api/auth/login`
2. `POST /api/auth/register`
3. `GET /api/admin/departments`
4. `POST /api/admin/departments`
5. `DELETE /api/admin/departments/{departmentId}`

Nội dung chỉ gồm các bảng phân hoạch và giá trị biên; không thiết kế test case hoặc viết mã kiểm thử.

- Phân hoạch dựa trên `AuthController`, `AdminController`, `RegistrationRequest`, `AuthService`, `AdminService` và entity `Department` trong mã nguồn dự án.
- API Register được cấu hình cho phép truy cập công khai. Với ba API `/api/admin/**`, giả định request có JWT hợp lệ và tài khoản có vai trò `ADMIN`; kiểm thử xác thực và phân quyền không thuộc phạm vi bảng đầu vào nghiệp vụ bên dưới.
- `departmentId` là ID kiểu `Long`. Mã nguồn không đặt miền min/max nghiệp vụ cho ID; việc xử lý phụ thuộc vào bản ghi khoa có tồn tại hay không. Vì vậy không gán BVA số học min/max cho ID.
- Tag `V` biểu thị lớp hợp lệ/được API chấp nhận; tag `X` biểu thị lớp không hợp lệ hoặc bị nghiệp vụ từ chối. Tag được đánh số riêng trong từng API.
- Kết quả HTTP được mô tả theo cách xử lý hiện có trong mã nguồn, không thay thế bằng kết quả API lý tưởng.

## 2. POST `/api/auth/login`

### Bảng phân hoạch lớp tương đương

| **Đầu vào/điều kiện** | **Lớp tương đương hợp lệ/được chấp nhận** | **Tag** | **Lớp tương đương không hợp lệ/bị từ chối** | **Tag** |
|---|---|---|---|---|
| `username` (body) | Username của tài khoản đã tồn tại; kết hợp với mật khẩu đúng của tài khoản. | V1 | Username không tồn tại: `CustomUserDetailsService` không tìm thấy tài khoản; xác thực thất bại và thông thường được trả HTTP 401. | X1 |
| `password` (body) | Mật khẩu đúng tương ứng với username. API xác thực bằng `AuthenticationManager` và PasswordEncoder. | V2 | Mật khẩu sai: phát sinh `BadCredentialsException`; `GlobalExceptionHandler` trả HTTP 401 với thông báo `Invalid username or password`. | X2 |
| `username`, `password` (body) | Cả hai trường được truyền và thông tin đăng nhập chính xác; API trả HTTP 200 cùng JWT, token type, user ID, username và role. | V3 | Thiếu hoặc truyền `null`/chuỗi rỗng cho username hoặc password: `LoginRequest` không khai báo `@NotBlank` hay annotation validation khác. Vì vậy không được kết luận đây là lỗi validation HTTP 400; kết quả phụ thuộc vào quá trình xác thực và có thể đi vào handler tổng quát HTTP 500. | X3 |
| Request body | JSON hợp lệ, ánh xạ được thành `LoginRequest`. | V4 | Body không phải JSON hợp lệ hoặc không ánh xạ được thành `LoginRequest`: lỗi đọc/chuyển đổi request body. | X4 |
| Tài khoản | Tài khoản hợp lệ và xác thực thành công; JWT được tạo theo username và role của người dùng. | V5 | Tài khoản không hợp lệ hoặc thông tin xác thực không khớp: xác thực bị từ chối. | X5 |

> `LoginRequest` chỉ khai báo hai thuộc tính `username` và `password`, không có `@NotBlank`, `@NotNull`, `@Size` hoặc các annotation validation khác. Controller dùng `@Valid`, nhưng do DTO không có constraint nên `@Valid` không tự tạo ra quy tắc bắt buộc cho hai trường. `AuthService.login` gọi `AuthenticationManager.authenticate(...)`, sau đó tìm user và tạo JWT. `GlobalExceptionHandler` xử lý `BadCredentialsException` thành HTTP 401; các exception khác không được xử lý riêng sẽ đi vào handler tổng quát HTTP 500. Không suy diễn rằng mọi trường hợp thiếu/rỗng đều trả cùng một mã HTTP nếu chưa thực thi kiểm thử trên đúng môi trường.

### Bảng giá trị biên

| **Đầu vào** | **Giá trị biên** | **Kết luận** |
|---|---|---|
| `username` | Không có min/max length hoặc quy tắc độ dài được khai báo trong `LoginRequest`. | Không áp dụng BVA độ dài; phân hoạch theo tài khoản tồn tại/không tồn tại và thông tin xác thực. |
| `password` | Không có min/max length hoặc quy tắc độ dài được khai báo trong `LoginRequest`. | Không áp dụng BVA độ dài; phân hoạch theo mật khẩu đúng/sai. |
| `username`, `password` | Hai trường không có constraint `@NotBlank`/`@NotNull` trong DTO. | Có thể kiểm tra tình huống thiếu, `null`, rỗng bằng EP; không có ngưỡng min/max để lập BVA số học hoặc độ dài. |

## 3. POST `/api/auth/register`

### Bảng phân hoạch lớp tương đương

| **Đầu vào** | **Lớp tương đương hợp lệ/được chấp nhận** | **Tag** | **Lớp tương đương không hợp lệ/bị từ chối** | **Tag** |
|---|---|---|---|---|
| `username` (body, bắt buộc) | Chuỗi không rỗng và không chỉ chứa khoảng trắng; chưa tồn tại trong hệ thống. | V1 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X1 |
| `username` (body, bắt buộc) | — | — | Username đã tồn tại: service ném `RuntimeException`; theo handler tổng quát hiện tại trả HTTP 500. | X2 |
| `password` (body, bắt buộc) | Chuỗi không rỗng và không chỉ chứa khoảng trắng. Mật khẩu được mã hóa bằng PasswordEncoder trước khi lưu. | V2 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X3 |
| `email` (body, bắt buộc) | Email không rỗng, đúng định dạng `@Email` và chưa tồn tại trong hệ thống. | V3 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X4 |
| `email` (body, bắt buộc) | — | — | Email sai định dạng: vi phạm `@Email`, trả HTTP 400. | X5 |
| `email` (body, bắt buộc) | — | — | Email đã tồn tại: service ném `RuntimeException`; theo handler tổng quát hiện tại trả HTTP 500. | X6 |
| `firstName` (body, bắt buộc) | Chuỗi không rỗng và không chỉ chứa khoảng trắng. | V4 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X7 |
| `lastName` (body, bắt buộc) | Chuỗi không rỗng và không chỉ chứa khoảng trắng. | V5 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X8 |
| `phoneNumber` (body, bắt buộc) | Chuỗi gồm đúng 10 ký tự và tất cả đều là chữ số (`0–9`). | V6 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X9 |
| `phoneNumber` (body, bắt buộc) | — | — | Không đúng 10 chữ số; có chữ cái, ký tự đặc biệt hoặc khoảng trắng: vi phạm `@Pattern`, trả HTTP 400. | X10 |
| `role` (body, bắt buộc) | Chuỗi không rỗng, sau khi chuyển thành chữ hoa tương ứng một trong các giá trị enum `PATIENT`, `DOCTOR`, `ADMIN`. | V7 | Thiếu, `null`, chuỗi rỗng hoặc chỉ chứa khoảng trắng: vi phạm `@NotBlank`, trả HTTP 400. | X11 |
| `role` (body, bắt buộc) | — | — | Chuỗi không tương ứng với enum `PATIENT`, `DOCTOR`, `ADMIN`: `User.Role.valueOf(...)` phát sinh exception; handler tổng quát trả HTTP 500. | X12 |
| `address` (body, tùy chọn) | Có thể bỏ qua hoặc truyền chuỗi; nếu role là `PATIENT`, giá trị được gán vào hồ sơ bệnh nhân. | V8 | Không có ràng buộc validation nghiệp vụ được khai báo trong `RegistrationRequest`. | — |
| `dateOfBirth` (body, tùy chọn) | Có thể bỏ qua (`null`) hoặc truyền ngày hợp lệ theo định dạng mà `LocalDate.parse` chấp nhận, thông thường `yyyy-MM-dd`. | V9 | Giá trị khác `null` nhưng không phân tích được thành `LocalDate` (ví dụ sai định dạng hoặc ngày không tồn tại): phát sinh exception; handler tổng quát trả HTTP 500. | X13 |
| `bloodGroup`, `emergencyContact` (body, tùy chọn) | Có thể bỏ qua hoặc truyền chuỗi; khi role là `PATIENT`, các giá trị được gán vào hồ sơ bệnh nhân. | V10 | Không có annotation validation hoặc giới hạn độ dài được khai báo trong `RegistrationRequest`. | — |

> `RegistrationRequest` khai báo `@NotBlank` cho `username`, `password`, `email`, `firstName`, `lastName`, `phoneNumber`, `role`; `email` có thêm `@Email`, `phoneNumber` có thêm `@Pattern(regexp = "^[0-9]{10}$")`. Mã nguồn không khai báo giới hạn độ dài cho các trường chuỗi. Kiểm tra trùng username/email và chuyển role được thực hiện trong service. Các exception trùng username/email và role không hợp lệ hiện đi vào handler tổng quát nên trả HTTP 500, không phải HTTP 400.

### Bảng giá trị biên

| **Đầu vào** | **`min-` / sát dưới ngưỡng** | **`min` / tại ngưỡng** | **`min+` / sát trên ngưỡng** | **Giá trị đại diện hợp lệ** | **`max-`, `max`, `max+`** | **Tag / kết luận** |
|---|---|---|---|---|---|---|
| `phoneNumber` | 9 chữ số — không hợp lệ (`X10`) | 10 chữ số — hợp lệ (`B1`) | 11 chữ số — không hợp lệ (`X10`) | `0123456789` (`B2`) | Độ dài tối đa được chấp nhận cũng là 10; 9/11 chữ số lần lượt nằm ngoài ngưỡng | B1 (độ dài hợp lệ), B2 (giá trị đại diện); X10 (ngoài độ dài cho phép) |
| `username`, `password`, `firstName`, `lastName` | Không xác định | Không xác định | Không xác định | Chuỗi không blank | Không xác định | Không áp dụng BVA độ dài: mã nguồn không khai báo min/max length. Chỉ có ràng buộc không blank. |
| `email` | Không xác định | Không xác định | Không xác định | Email đúng định dạng và chưa tồn tại | Không xác định | Không áp dụng BVA độ dài: mã nguồn không khai báo min/max length; kiểm tra theo lớp định dạng email. |
| `role` | Không xác định | Không xác định | Không xác định | `PATIENT`, `DOCTOR` hoặc `ADMIN` | Không xác định | Không áp dụng BVA số học/độ dài; đây là tập giá trị enum được chấp nhận. |
| `dateOfBirth` | Không có ngưỡng ngày nhỏ nhất được khai báo | Không xác định | Không xác định | Ngày hợp lệ theo `LocalDate.parse` | Không có ngưỡng ngày lớn nhất được khai báo | Không áp dụng BVA min/max nghiệp vụ vì mã nguồn không đặt giới hạn tuổi/ngày sinh. |
| `address`, `bloodGroup`, `emergencyContact` | Không xác định | Không xác định | Không xác định | Có thể bỏ trống hoặc truyền chuỗi | Không xác định | Không áp dụng BVA: không có ràng buộc độ dài/miền giá trị được khai báo. |

## 4. GET `/api/admin/departments`

### Bảng phân hoạch lớp tương đương

| **Đầu vào/điều kiện** | **Lớp tương đương hợp lệ/được chấp nhận** | **Tag** | **Lớp tương đương không hợp lệ/bị từ chối** | **Tag** |
|---|---|---|---|---|
| Request (không có query parameter hoặc body) | Gửi GET endpoint với quyền `ADMIN`; API trả về danh sách tất cả khoa từ `departmentRepository.findAll()`. | V1 | Tài khoản không có quyền `ADMIN` hoặc chưa xác thực: bị Spring Security từ chối; đây là kiểm thử xác thực/phân quyền, không phải lớp đầu vào nghiệp vụ. | X1 |
| Dữ liệu khoa trong hệ thống | Có một hoặc nhiều khoa: API trả về danh sách khoa tương ứng. | V2 | Không có khoa nào: API vẫn xử lý hợp lệ và trả danh sách rỗng; không xem đây là đầu vào không hợp lệ. | — |

### Bảng giá trị biên

| **Đầu vào** | **Giá trị biên** | **Kết luận** |
|---|---|---|
| Query parameter / request body | API không khai báo query parameter hoặc request body. | Không có dữ liệu đầu vào dạng miền giá trị để áp dụng BVA. |
| Số lượng khoa trả về | Không có min/max nghiệp vụ được khai báo cho số lượng khoa. | Danh sách rỗng và danh sách có dữ liệu là các tình huống thuộc EP; không áp dụng BVA số học. |

## 5. POST `/api/admin/departments`

### Bảng phân hoạch lớp tương đương

| **Đầu vào/điều kiện** | **Lớp tương đương hợp lệ/được chấp nhận** | **Tag** | **Lớp tương đương không hợp lệ/bị từ chối** | **Tag** |
|---|---|---|---|---|
| Request body | JSON ánh xạ được thành đối tượng `Department`; gửi request với quyền `ADMIN`. | V1 | Body không phải JSON hợp lệ hoặc không ánh xạ được thành `Department`: request bị từ chối trong quá trình đọc body. | X1 |
| `name` (body) | Tên khoa là chuỗi khác `null`, không trùng tên khoa đã có; trường được lưu với ràng buộc `nullable = false` và `unique = true` ở database. | V2 | Tên khoa `null` hoặc không được cung cấp: database có ràng buộc `nullable = false`; có thể phát sinh lỗi khi lưu và handler tổng quát trả HTTP 500. Không có `@NotNull`/`@NotBlank` trong entity để bảo đảm HTTP 400. | X2 |
| `name` (body) | — | — | Tên khoa đã tồn tại: service kiểm tra `existsByName` rồi ném `RuntimeException`; handler tổng quát trả HTTP 500. | X3 |
| `description` (body, tùy chọn) | Có thể bỏ qua (`null`) hoặc truyền chuỗi. | V3 | Không có ràng buộc validation hoặc giới hạn độ dài được khai báo trong entity. | — |
| `active` (body, tùy chọn) | Có thể bỏ qua; entity khởi tạo giá trị mặc định `true`. Có thể truyền giá trị Boolean hợp lệ. | V4 | Truyền `null` tường minh có thể ghi đè giá trị mặc định; database khai báo `nullable = false`, nên có thể phát sinh lỗi khi lưu và trả HTTP 500. Giá trị không chuyển đổi được sang Boolean bị lỗi khi đọc body. | X4 |

> `AdminController.createDepartment` chỉ nhận `@RequestBody Department`, không có `@Valid`. Vì vậy không có Bean Validation tự động cho các trường của entity. `AdminService.createDepartment` chỉ kiểm tra tên khoa trùng bằng `existsByName`; nếu trùng sẽ ném `RuntimeException`. Entity `Department` khai báo `name` là `unique = true, nullable = false`, `active` là `nullable = false` và khởi tạo `active = true`; `description` không có ràng buộc tương ứng. Lỗi trùng tên hiện được handler tổng quát xử lý thành HTTP 500.

### Bảng giá trị biên

| **Đầu vào** | **`min-` / sát dưới ngưỡng** | **`min` / tại ngưỡng** | **`min+` / sát trên ngưỡng** | **Giá trị đại diện hợp lệ** | **`max-`, `max`, `max+`** | **Tag / kết luận** |
|---|---|---|---|---|---|---|
| `name` | Không xác định | Không xác định | Không xác định | Tên khoa khác `null`, không trùng tên đã có | Không xác định | Không áp dụng BVA độ dài: entity không khai báo min/max length hoặc `@Size`; chỉ có ràng buộc database không null và duy nhất. |
| `description` | Không xác định | Không xác định | Không xác định | Có thể bỏ trống hoặc truyền chuỗi | Không xác định | Không áp dụng BVA: mã nguồn không khai báo giới hạn độ dài. |
| `active` | Không áp dụng | Không áp dụng | Không áp dụng | `true` hoặc `false`; nếu bỏ qua thì giá trị khởi tạo mặc định là `true` | Không áp dụng | Không áp dụng BVA số học: đây là kiểu Boolean, không phải miền số liên tục. `null` không được database chấp nhận. |

## 6. DELETE `/api/admin/departments/{departmentId}`

### Bảng phân hoạch lớp tương đương

| **Đầu vào** | **Lớp tương đương hợp lệ/được chấp nhận** | **Tag** | **Lớp tương đương không hợp lệ/bị từ chối** | **Tag** |
|---|---|---|---|---|
| `departmentId` (path, bắt buộc) | ID dạng `Long` của khoa đang tồn tại: service tìm thấy khoa và thực hiện xóa; controller trả HTTP 200 với thông báo `Department deleted successfully`. | V1 | Giá trị path không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối trong quá trình binding. | X1 |
| `departmentId` (path, bắt buộc) | — | — | ID dạng `Long` không có khoa tương ứng: `getDepartmentById` ném `ResourceNotFoundException`; handler trả HTTP 404. | X2 |
| Quyền truy cập | Request có JWT hợp lệ và vai trò `ADMIN`. | V2 | Chưa xác thực hoặc không có vai trò `ADMIN`: Spring Security từ chối request; không thuộc phân hoạch giá trị ID nghiệp vụ. | X3 |

### Bảng giá trị biên

| **Đầu vào** | **Giá trị biên** | **Kết luận** |
|---|---|---|
| `departmentId` | Không có ngưỡng min/max nghiệp vụ; đây là ID dùng để tra cứu bản ghi khoa, không phải miền số liên tục. | Không áp dụng BVA số học; ID tồn tại/không tồn tại được phân hoạch bằng EP. |

## 7. Tổng kết phạm vi EP/BVA

- **EP có thể áp dụng rõ ràng:** thông tin đăng nhập đúng/sai và tài khoản tồn tại/không tồn tại trong Login; các trường bắt buộc/định dạng trong Register; role hợp lệ; username/email trùng; trạng thái tồn tại của khoa; body tạo khoa; ID khoa tồn tại hoặc không tồn tại.
- **BVA có ngưỡng rõ ràng:** `phoneNumber` phải có đúng 10 chữ số. Login không khai báo giới hạn độ dài cho username/password nên không có ngưỡng BVA tương ứng. Giá trị 9 và 11 chữ số nằm ngoài ngưỡng; 10 chữ số là giá trị hợp lệ.
- **Không tự đặt ngưỡng:** mã nguồn không khai báo min/max length cho username, password, firstName, lastName, email, name, description; không khai báo giới hạn tuổi cho dateOfBirth; không có miền min/max nghiệp vụ cho `departmentId`.
- **Lưu ý hành vi hiện tại:** một số trường hợp lỗi nghiệp vụ như trùng username/email, role không hợp lệ, trùng tên khoa hoặc dữ liệu database không thỏa ràng buộc có thể trả HTTP 500 do handler tổng quát. Đây là hành vi quan sát từ mã nguồn hiện tại, không phải khuyến nghị thiết kế API.
