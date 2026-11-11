# Thiết kế kiểm thử EP, BVA và Decision Table cho 2 API POST

**Dự án:** Hospital Management System (HMS)

**Phạm vi kiểm thử:**

1. `POST /api/auth/register`
2. `POST /api/admin/departments`

**Phạm vi loại trừ:** Không kiểm thử các API GET.

Tài liệu được xây dựng theo cấu trúc và quy ước của tài liệu mẫu `kiem-thu-3-api-post.md`. Do chưa có source code/DTO/Swagger của hai API này, các nội dung chưa được xác minh được ghi rõ là "cần đối chiếu", không tự đặt giới hạn hay status code.

**Quy ước:** EP dùng V (hợp lệ), X (không hợp lệ). BVA dùng tag B1, B2, .... Decision Table dùng C (Condition), A (Action), R (Rule). Y = có/đúng, N = không/sai, - = không ảnh hưởng/không xét.

---

## 1. POST /api/auth/register

### 1.1. Thông tin API và dữ liệu đầu vào

- **Method:** POST
- **URL:** `/api/auth/register`
- **Content-Type:** `application/json`
- **Authorization:** Chưa có căn cứ xác nhận endpoint có yêu cầu Bearer Token hay không. Cần kiểm tra Security config/Controller.

**Request Body tham khảo (đã dùng trong bộ test trước):**

```json
{
  "username": "testuser01",
  "password": "Test123456",
  "email": "test01@gmail.com",
  "firstName": "Nguyen",
  "lastName": "Van A",
  "phoneNumber": "0901234567",
  "role": "PATIENT"
}
```

**Các trường cần đối chiếu với RegisterRequest/DTO thực tế:**

| Trường | Vai trò kiểm thử | Căn cứ hiện có |
|---|---|---|
| username | Tên đăng nhập | Có trong bộ test trước |
| password | Mật khẩu | Có trong bộ test trước |
| email | Email | Có trong bộ test trước |
| firstName | Tên | Có trong bộ test trước |
| lastName | Họ | Có trong bộ test trước |
| phoneNumber | Số điện thoại | Có trong bộ test trước |
| role | Vai trò tài khoản | Có trong bộ test trước; cần xác minh role nào được phép đăng ký |

**Luồng xử lý cần xác minh từ source:**

1. Controller nhận body và có dùng `@Valid` hay không.
2. Service kiểm tra username/email đã tồn tại hay chưa.
3. Service mã hóa password và lưu User.
4. Controller trả status/response nào khi thành công (200 hay 201) và cấu trúc response.

**Kết quả thành công:** Bộ test trước dự kiến HTTP 200, cần chạy lại để xác nhận.
**Kết quả thất bại:** Bộ test trước dự kiến HTTP 400 cho dữ liệu không hợp lệ; cần xác nhận theo exception handler.

### 1.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| username | Username hợp lệ, chưa được sử dụng | V1 | (a) Thiếu trường<br>(b) null<br>(c) Chuỗi rỗng<br>(d) Username đã tồn tại<br>(e) Sai kiểu dữ liệu | X1–X5 |
| password | Mật khẩu đáp ứng quy tắc backend | V2 | (a) Thiếu trường<br>(b) null<br>(c) Chuỗi rỗng<br>(d) Sai kiểu dữ liệu<br>(e) Không đạt chính sách mật khẩu (nếu có) | X6–X10 |
| email | Email đúng định dạng, chưa được sử dụng | V3 | (a) Thiếu trường<br>(b) null<br>(c) Chuỗi rỗng<br>(d) Sai định dạng<br>(e) Email đã tồn tại<br>(f) Sai kiểu dữ liệu | X11–X16 |
| firstName | Giá trị hợp lệ theo DTO | V4 | Thiếu/null/rỗng/sai kiểu (nếu bắt buộc) | X17 |
| lastName | Giá trị hợp lệ theo DTO | V5 | Thiếu/null/rỗng/sai kiểu (nếu bắt buộc) | X18 |
| phoneNumber | Số điện thoại đúng quy tắc backend | V6 | Sai định dạng/thiếu/null/rỗng (nếu bắt buộc hoặc có kiểm tra định dạng) | X19 |
| role | Role được phép đăng ký | V7 | (a) Role không được phép<br>(b) Role không tồn tại<br>(c) Thiếu/null/rỗng (nếu bắt buộc)<br>(d) Sai kiểu dữ liệu | X20–X23 |

**Căn cứ code cần đối chiếu:**

- Annotation trong RegisterRequest (`@NotBlank`, `@Email`, `@Size`, `@Pattern`...).
- Kiểm tra trùng username/email trong service/repository.
- Danh sách role được phép đăng ký trong service hoặc enum.

**Lưu ý:** Không mặc định tất cả trường đều bắt buộc nếu chưa có annotation hoặc kiểm tra tương ứng.

### 1.3. Bảng phân tích giá trị biên (BVA)

Chưa có source/DTO xác nhận giới hạn min/max của các trường chuỗi. Vì vậy không điền số ký tự giả định.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| username | Chưa xác định | Chưa xác định | Chưa xác định | Username hợp lệ | Chưa xác định | Chưa xác định | Chưa xác định | B1–B7 |
| password | Chưa xác định | Chưa xác định | Chưa xác định | Mật khẩu hợp lệ | Chưa xác định | Chưa xác định | Chưa xác định | B8–B14 |
| email | Chưa xác định | Chưa xác định | Chưa xác định | Email hợp lệ | Chưa xác định | Chưa xác định | Chưa xác định | B15–B21 |
| firstName | Chưa xác định | Chưa xác định | Chưa xác định | Tên hợp lệ | Chưa xác định | Chưa xác định | Chưa xác định | B22–B28 |
| lastName | Chưa xác định | Chưa xác định | Chưa xác định | Họ hợp lệ | Chưa xác định | Chưa xác định | Chưa xác định | B29–B35 |
| phoneNumber | Chưa xác định | Chưa xác định | Chưa xác định | SĐT hợp lệ | Chưa xác định | Chưa xác định | Chưa xác định | B36–B42 |

**Cách hoàn thiện BVA:** Mở DTO và tìm `@Size(min, max)`, `@Min`, `@Max`, `@Length` hoặc kiểm tra tương đương trong service. Khi có min/max, lập 7 giá trị min-, min, min+, nominal, max-, max, max+. Nếu không có giới hạn, ghi "Không quy định trong source", không tự đặt số.

### 1.4. Bảng quyết định (Decision Table)

**Conditions / Actions**

- C1: Username hợp lệ và chưa tồn tại?
- C2: Password đáp ứng quy tắc backend?
- C3: Email hợp lệ và chưa tồn tại?
- C4: firstName/lastName/phoneNumber hợp lệ theo backend?
- C5: Role được phép đăng ký?
- A1: Tạo tài khoản.
- A2: Từ chối do username.
- A3: Từ chối do password.
- A4: Từ chối do email.
- A5: Từ chối do thông tin cá nhân.
- A6: Từ chối do role.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---|---|---|---|---|---|
| C1. Username hợp lệ/chưa tồn tại | Y | N | Y | Y | Y | Y |
| C2. Password hợp lệ | Y | - | N | Y | Y | Y |
| C3. Email hợp lệ/chưa tồn tại | Y | - | - | N | Y | Y |
| C4. Thông tin cá nhân hợp lệ | Y | - | - | - | N | Y |
| C5. Role được phép | Y | - | - | - | - | N |
| A1. Tạo tài khoản | X | - | - | - | - | - |
| A2. Từ chối do username | - | X | - | - | - | - |
| A3. Từ chối do password | - | - | X | - | - | - |
| A4. Từ chối do email | - | - | - | X | - | - |
| A5. Từ chối do thông tin cá nhân | - | - | - | - | X | - |
| A6. Từ chối do role | - | - | - | - | - | X |

**Diễn giải Rules:**

- R1: Tất cả dữ liệu hợp lệ, username/email chưa tồn tại, role được phép → tạo tài khoản.
- R2: Username thiếu, không hợp lệ hoặc đã tồn tại → từ chối.
- R3: Password không đáp ứng quy tắc backend → từ chối.
- R4: Email sai định dạng, thiếu hoặc đã tồn tại → từ chối.
- R5: Thông tin cá nhân không đạt validation (nếu backend có quy định) → từ chối.
- R6: Role không được phép đăng ký → từ chối.

**Lưu ý khi chạy:** Khi nhiều điều kiện cùng sai, response có thể chỉ trả lỗi đầu tiên hoặc danh sách lỗi. Không tự đặt trước status code cho từng lỗi nếu chưa kiểm tra exception handler.

---

## 2. POST /api/admin/departments

### 2.1. Thông tin API và dữ liệu đầu vào

- **Method:** POST
- **URL:** `/api/admin/departments`
- **Authorization:** Cần xác nhận từ Controller/SecurityConfig. API thuộc nhóm `/api/admin` nên dự kiến cần Bearer Token role ADMIN (cần kiểm tra `@PreAuthorize` hoặc cấu hình security).
- **Content-Type:** `application/json`

**Request Body tham khảo:**

```json
{
  "name": "Khoa Da Nghiem Thu",
  "description": "Phong ban kiem thu",
  "active": true
}
```

**Dữ liệu quan sát ở lần tạo trước:** response có `id: 10`, `name`, `description`, `active: true`, `createdAt`, `updatedAt`. Đây chỉ là dữ liệu quan sát, không thay thế đặc tả validation.

**Luồng xử lý cần xác minh từ source:**

1. Controller nhận body, có `@Valid` hay không, có `@PreAuthorize` hay không.
2. Service có kiểm tra trùng tên phòng ban hay không.
3. Service lưu Department bằng repository.
4. Status và cấu trúc response khi thành công.

### 2.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| name | Tên phòng ban hợp lệ | V1 | (a) Thiếu trường<br>(b) null<br>(c) Chuỗi rỗng<br>(d) Chỉ chứa khoảng trắng<br>(e) Sai kiểu dữ liệu<br>(f) Trùng tên (nếu hệ thống cấm trùng) | X1–X6 |
| description | Chuỗi mô tả hợp lệ hoặc không gửi (nếu backend cho phép tùy chọn) | V2 | Sai kiểu dữ liệu; null/rỗng (chỉ không hợp lệ nếu backend quy định bắt buộc) | X7 |
| active | Boolean `true` | V3 | Chuỗi `"true"`/`"false"`, số, object hoặc kiểu khác Boolean; null (nếu backend không chấp nhận) | X8–X9 |
| active | Boolean `false` | V4 | (xem X8–X9) | - |
| Authorization | Bearer Token hợp lệ, role ADMIN | V5 | (a) Không gửi token<br>(b) Token sai/hết hạn<br>(c) Token hợp lệ nhưng role không đủ quyền | X10–X12 |

**Căn cứ code cần đối chiếu:**

- DepartmentRequest/DTO: `@NotBlank`, `@Size`, `@NotNull` trên name/description/active.
- Service/DB: ràng buộc unique của name.
- Controller/SecurityConfig: quyền cần có.

**Lưu ý:** Không mặc định `description` hoặc `active` là bắt buộc. Chỉ kết luận tên trùng là không hợp lệ khi source/database có ràng buộc tương ứng.

### 2.3. Bảng phân tích giá trị biên (BVA)

Chưa có DTO/source xác nhận giới hạn độ dài của `name` và `description`. `active` là Boolean nên không có miền số học min/max.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| name | Chưa xác định | Chưa xác định | Chưa xác định | "Khoa Cong nghe thong tin" | Chưa xác định | Chưa xác định | Chưa xác định | B1–B7 |
| description | Chưa xác định | Chưa xác định | Chưa xác định | "Phong ban kiem thu" | Chưa xác định | Chưa xác định | Chưa xác định | B8–B14 |
| active | Không áp dụng | false | Không áp dụng | true | Không áp dụng | true | Không áp dụng | B15–B16 |

**Bộ giá trị kiểm thử cho active:**

| Tag | Giá trị | Kỳ vọng |
|---|---|---|
| B15 | `false` | Hợp lệ |
| B16 | `true` | Hợp lệ |

Các trường hợp thiếu/null/sai kiểu của `active` thuộc EP (X8–X9), kết quả phụ thuộc DTO.

**Cách hoàn thiện BVA:** Kiểm tra DTO tìm `@NotBlank`, `@Size`, `@Length`. Nếu `name` có min/max, lập đủ 7 vị trí biên.

### 2.4. Bảng quyết định (Decision Table)

**Conditions / Actions**

- C1: Token hợp lệ và có quyền quản trị?
- C2: name hợp lệ?
- C3: Tên phòng ban chưa tồn tại (nếu hệ thống có kiểm tra trùng)?
- C4: description hợp lệ theo backend?
- C5: active là Boolean hợp lệ hoặc được backend gán mặc định?
- A1: Tạo phòng ban.
- A2: Từ chối xác thực/phân quyền.
- A3: Từ chối do name.
- A4: Từ chối do trùng tên.
- A5: Từ chối do description.
- A6: Từ chối do active.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---|---|---|---|---|---|
| C1. Có quyền quản trị | Y | N | Y | Y | Y | Y |
| C2. name hợp lệ | Y | - | N | Y | Y | Y |
| C3. Tên chưa tồn tại | Y | - | - | N | Y | Y |
| C4. description hợp lệ | Y | - | - | - | N | Y |
| C5. active hợp lệ/được mặc định | Y | - | - | - | - | N |
| A1. Tạo phòng ban | X | - | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | - | - | - |
| A3. Từ chối do name | - | - | X | - | - | - |
| A4. Từ chối do trùng tên | - | - | - | X | - | - |
| A5. Từ chối do description | - | - | - | - | X | - |
| A6. Từ chối do active | - | - | - | - | - | X |

**Diễn giải Rules:**

- R1: Đủ quyền, dữ liệu hợp lệ, không vi phạm quy tắc trùng tên → tạo phòng ban.
- R2: Thiếu token hoặc không đủ quyền → từ chối.
- R3: name thiếu/rỗng/sai kiểu hoặc không đạt validation → từ chối.
- R4: Tên đã tồn tại → từ chối (chỉ áp dụng nếu backend cấm trùng).
- R5: description không hợp lệ → từ chối (nếu backend có validation).
- R6: active sai kiểu hoặc không hợp lệ theo DTO → từ chối.

**Lưu ý:** Nếu hệ thống cho phép trùng tên, bỏ C3 và R4. Nếu `description`/`active` là tùy chọn, phản ánh đúng hành vi mặc định trong bảng.

---

## 3. Tổng kết phạm vi EP, BVA và Decision Table

| API | EP – Phân vùng tương đương | BVA – Giá trị biên | Decision Table |
|---|---|---|---|
| POST /api/auth/register | username, password, email, firstName, lastName, phoneNumber, role | Chưa có min/max được xác minh; cần đọc DTO trước khi chốt số biên | Dữ liệu hợp lệ, username/email trùng, dữ liệu thiếu/sai, role |
| POST /api/admin/departments | name, description, active, quyền truy cập | Chưa có giới hạn độ dài; active chỉ có true/false | Quyền quản trị, name, trùng tên (nếu có), description, active |

---

## 4. Danh sách test case cần đưa vào CSV

Các test case dưới đây là danh sách thiết kế ban đầu. Khi chạy Postman, cập nhật Actual Status, Actual Result và Pass/Fail theo kết quả thực tế.

### 4.1. POST Register

| TC ID | Nhóm | Dữ liệu / Điều kiện | Kết quả mong đợi |
|---|---|---|---|
| REG_01 | EP hợp lệ | Toàn bộ dữ liệu hợp lệ, username/email chưa tồn tại | Tạo tài khoản thành công |
| REG_02 | EP không hợp lệ | Thiếu username | Bị từ chối do validation |
| REG_03 | EP không hợp lệ | username rỗng | Bị từ chối do validation |
| REG_04 | EP không hợp lệ | username đã tồn tại | Bị từ chối do trùng |
| REG_05 | EP không hợp lệ | Thiếu password | Bị từ chối do validation |
| REG_06 | EP không hợp lệ | password rỗng | Bị từ chối do validation |
| REG_07 | EP không hợp lệ | email sai định dạng | Bị từ chối do validation |
| REG_08 | EP không hợp lệ | Thiếu email | Bị từ chối do validation |
| REG_09 | EP không hợp lệ | email đã tồn tại | Bị từ chối do trùng |
| REG_10 | EP không hợp lệ | role không tồn tại/không được phép | Bị từ chối do role |
| REG_11 | EP sai kiểu | Một trường gửi sai kiểu dữ liệu (ví dụ username là số/object) | Bị từ chối do lỗi parse/binding |
| REG_12 | EP tùy chọn | Bỏ qua trường tùy chọn (nếu backend cho phép) | Tạo thành công nếu trường là tùy chọn |

### 4.2. POST Departments

| TC ID | Nhóm | Dữ liệu / Điều kiện | Kết quả mong đợi |
|---|---|---|---|
| DEP_01 | EP hợp lệ | Token ADMIN, name/description/active hợp lệ | Tạo phòng ban thành công, response có id, name, description, active |
| DEP_02 | EP không hợp lệ | Thiếu name | Bị từ chối do validation |
| DEP_03 | EP không hợp lệ | name rỗng | Bị từ chối do validation |
| DEP_04 | EP không hợp lệ | name chỉ chứa khoảng trắng | Bị từ chối do validation |
| DEP_05 | EP không hợp lệ | name trùng (nếu backend cấm trùng) | Bị từ chối do trùng tên |
| DEP_06 | EP tùy chọn | Thiếu description | Tạo thành công nếu description là tùy chọn |
| DEP_07 | EP sai kiểu | description sai kiểu dữ liệu | Bị từ chối do lỗi parse/binding |
| DEP_08 | BVA/Boolean | active = true | Tạo thành công, response active = true |
| DEP_09 | BVA/Boolean | active = false | Tạo thành công, response active = false |
| DEP_10 | EP sai kiểu | active = "abc" hoặc số | Bị từ chối do lỗi parse/binding |
| DEP_11 | Authorization | Không gửi token | Bị từ chối truy cập |
| DEP_12 | Authorization | Token hợp lệ nhưng role không đủ quyền | Bị từ chối phân quyền |

---

## 5. Hướng dẫn CSV và Postman Collection Runner

### 5.1. Cấu trúc cột CSV báo cáo đề xuất

| Cột | Ý nghĩa |
|---|---|
| TC_ID | Mã test case |
| API | Tên API |
| Method | POST |
| Endpoint | URL hoặc URL có biến |
| Test_Group | EP/BVA/DecisionTable/Authorization |
| Input_Data | Dữ liệu đầu vào |
| Expected_Result | Kết quả mong đợi |
| Expected_Status | HTTP status mong đợi nếu đã xác định |
| Actual_Status | HTTP status thực tế |
| Actual_Result | Nội dung response thực tế |
| Test_Result | PASS/FAIL |
| Notes | Ghi chú hoặc lỗi phát hiện |

Không tự điền status code nếu chưa xác nhận từ exception handler. Sau lần chạy đầu, căn cứ response thực tế để hoàn thiện `Expected_Status`.

### 5.2. File dữ liệu cho Collection Runner

Nên tách file dữ liệu theo API để tránh chạy nhầm request.

**register-data.csv**

```csv
TC_ID,username,password,email,firstName,lastName,phoneNumber,role,Expected_Status
REG_01,testuser01,Test123456,test01@gmail.com,Nguyen,Van A,0901234567,PATIENT,
REG_02,,Test123456,test02@gmail.com,Nguyen,Van B,0901234568,PATIENT,
```

**departments-data.csv**

```csv
TC_ID,name,description,active,Expected_Status
DEP_01,Khoa CNTT,Phong ban CNTT,true,
DEP_02,,Phong ban thu nghiem,true,
```

Cột `Expected_Status` để trống cho đến khi xác nhận status thực tế. Dữ liệu hợp lệ phải duy nhất mỗi lần chạy nếu API không cho phép trùng username/email/name.

### 5.3. Biến Postman đề xuất

- `baseUrl`: địa chỉ backend đang chạy.
- `adminToken`: Bearer Token của tài khoản ADMIN.
- `nonAdminToken`: Token của tài khoản không có role ADMIN để kiểm tra phân quyền.

### 5.4. Request mẫu

**POST Register**

```
POST {{baseUrl}}/api/auth/register
Content-Type: application/json
```

```json
{
  "username": "{{username}}",
  "password": "{{password}}",
  "email": "{{email}}",
  "firstName": "{{firstName}}",
  "lastName": "{{lastName}}",
  "phoneNumber": "{{phoneNumber}}",
  "role": "{{role}}"
}
```

Chỉ giữ các trường đúng với DTO thực tế.

**POST Departments**

```
POST {{baseUrl}}/api/admin/departments
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

```json
{
  "name": "{{name}}",
  "description": "{{description}}",
  "active": {{active}}
}
```

`active` là Boolean nên không đặt trong dấu nháy kép khi CSV chứa true/false. Nếu trường được phép bỏ qua, cần request riêng hoặc script tạo body phù hợp.

### 5.5. Script kiểm tra cơ bản trong Postman

```javascript
const expectedStatus = Number(pm.iterationData.get("Expected_Status"));

pm.test("HTTP status matches expected status", function () {
    pm.expect(pm.response.code).to.eql(expectedStatus);
});

pm.test("Response time under 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

console.log("Test Case:", pm.iterationData.get("TC_ID"));
console.log("Actual Status:", pm.response.code);
console.log("Response:", pm.response.text());
```

Với Departments khi tạo thành công, có thể bổ sung:

```javascript
pm.test("Department created with expected data", function () {
    const res = pm.response.json();
    pm.expect(res.name).to.eql(pm.iterationData.get("name"));
});
```

Tên biến trong `pm.iterationData.get()` phải khớp chính xác tên cột CSV.

### 5.6. Xuất kết quả JSON

Trong Collection Runner:

1. Chọn Collection chứa hai request.
2. Chọn file CSV làm Data file.
3. Chạy collection.
4. Kiểm tra tổng số iteration, passed và failed (đối chiếu với số dòng CSV).
5. Export kết quả run dưới dạng JSON.
6. Lưu JSON vào thư mục evidence của project.

Tên file gợi ý: `register-results.json`, `departments-results.json`.

---


Ảnh minh chứng nên thể hiện:

- Request URL, method, Params/Body và Authorization (che token).
- Status code và response body.
- Kết quả Collection Runner.
- Kết quả export JSON.

Không đưa token thật, mật khẩu hoặc thông tin nhạy cảm vào ảnh/CSV/JSON commit lên GitHub.

---

## 6. Tiêu chí nghiệm thu

| STT | Tiêu chí | Minh chứng |
|---|---|---|
| 1 | Có bảng EP cho POST Register | Mục 1.2 |
| 2 | Có bảng BVA cho POST Register | Mục 1.3 |
| 3 | Có bảng EP cho POST Departments | Mục 2.2 |
| 4 | Có bảng BVA cho POST Departments | Mục 2.3 |
| 5 | Có Decision Table cho hai API | Mục 1.4 và 2.4 |
| 6 | CSV khớp với các test case đã thiết kế | File test-cases.csv |
| 7 | Postman Collection chạy được với CSV | Collection và kết quả Runner |
| 8 | Có file JSON kết quả thực thi | File JSON export từ Runner |
| 9 | Có ảnh minh chứng và tài liệu trên GitHub | Thư mục evidence/ và commit/push |

---

