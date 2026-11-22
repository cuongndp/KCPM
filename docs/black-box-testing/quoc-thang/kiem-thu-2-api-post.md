# Thiết kế kiểm thử EP, BVA và Decision Table cho 2 API POST

**Dự án:** Hospital Management System (HMS)\
**Phạm vi kiểm thử:** 1. `POST /api/auth/register` 2.
`POST /api/admin/departments`

**Không thuộc phạm vi:** Các API GET.

> **Lưu ý về căn cứ:** Tài liệu này được chuyển đổi từ file mẫu bạn cung
> cấp và các thông tin đã có trong quá trình làm Postman trước đây. Các
> trường đăng ký từng được dùng trong bộ dữ liệu thử nghiệm gồm
> `username`, `password`, `email`, `firstName`, `lastName`,
> `phoneNumber`, `role`. API tạo phòng ban từng được gửi dữ liệu có
> `name`, `description`, `active`. Tuy nhiên, chưa có source code/DTO
> hoặc tài liệu Swagger xác nhận đầy đủ annotation, độ dài min/max, quy
> tắc định dạng, trùng lặp và status code. Vì vậy, các nội dung chưa
> được xác minh được ghi rõ là **cần đối chiếu**, không tự đặt giới hạn.
>
> **Quy ước:** EP dùng `V` (hợp lệ) và `X` (không hợp lệ). BVA chỉ xác
> định được giá trị số cụ thể khi có giới hạn min/max từ backend hoặc
> đặc tả. Decision Table dùng `C` (Condition), `A` (Action), `R` (Rule);
> `Y` = có/đúng, `N` = không/sai, `-` = không xét.

------------------------------------------------------------------------

# 1. POST /api/auth/register

## 1.1. Thông tin API và dữ liệu đầu vào

-   **Method:** `POST`
-   **URL:** `/api/auth/register`
-   **Content-Type:** `application/json`
-   **Authorization:** Chưa có căn cứ xác nhận endpoint đăng ký có yêu
    cầu Bearer Token hay không. Kiểm tra cấu hình Security/Controller.
-   **Request Body tham khảo đã dùng trong bộ test trước đây:**

``` json
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

**Các trường cần đối chiếu với `RegisterRequest`/DTO thực tế:**

  -----------------------------------------------------------------------
  Trường                  Vai trò kiểm thử        Căn cứ hiện có
  ----------------------- ----------------------- -----------------------
  `username`              Tên đăng nhập           Đã xuất hiện trong bộ
                                                  dữ liệu test trước

  `password`              Mật khẩu                Đã xuất hiện trong bộ
                                                  dữ liệu test trước

  `email`                 Email                   Đã xuất hiện trong bộ
                                                  dữ liệu test trước

  `firstName`             Tên                     Đã xuất hiện trong bộ
                                                  dữ liệu test trước

  `lastName`              Họ                      Đã xuất hiện trong bộ
                                                  dữ liệu test trước

  `phoneNumber`           Số điện thoại           Đã xuất hiện trong bộ
                                                  dữ liệu test trước

  `role`                  Vai trò tài khoản       Đã xuất hiện trong bộ
                                                  dữ liệu test trước; cần
                                                  xác minh role được phép
                                                  đăng ký
  -----------------------------------------------------------------------

**Kết quả thành công:** Bộ test trước đây dự kiến HTTP `200`, nhưng cần
chạy lại để xác nhận API thực tế trả `200` hay `201` và cấu trúc
response.\
**Kết quả thất bại:** Bộ test trước đây dự kiến HTTP `400` cho một số dữ
liệu không hợp lệ; phải xác nhận theo response thực tế và exception
handler.

## 1.2. Bảng phân vùng tương đương (EP)

  -----------------------------------------------------------------------------------
  Biến đầu vào    Lớp hợp lệ     Tag            Lớp không hợp lệ / cần Tag
                                                kiểm tra               
  --------------- -------------- -------------- ---------------------- --------------
  `username`      Username hợp   V1             \(a\) Thiếu trường (b) X1--X5
                  lệ, chưa được                 null (c) chuỗi rỗng    
                  sử dụng                       (d) username đã tồn    
                                                tại (e) sai kiểu dữ    
                                                liệu                   

  `password`      Mật khẩu đáp   V2             \(a\) Thiếu trường (b) X6--X10
                  ứng quy tắc                   null (c) chuỗi rỗng    
                  backend                       (d) sai kiểu dữ liệu   
                                                (e) không đạt chính    
                                                sách mật khẩu nếu      
                                                backend có quy định    

  `email`         Email đúng     V3             \(a\) Thiếu trường (b) X11--X16
                  định dạng và                  null (c) chuỗi rỗng    
                  chưa được sử                  (d) sai định dạng (e)  
                  dụng                          email đã tồn tại (f)   
                                                sai kiểu dữ liệu       

  `firstName`     Giá trị hợp lệ V4             Thiếu/null/rỗng/sai    X17
                  theo                          kiểu nếu trường này    
                  DTO/backend                   bắt buộc theo backend  

  `lastName`      Giá trị hợp lệ V5             Thiếu/null/rỗng/sai    X18
                  theo                          kiểu nếu trường này    
                  DTO/backend                   bắt buộc theo backend  

  `phoneNumber`   Số điện thoại  V6             Sai định               X19
                  đáp ứng quy                   dạng/thiếu/null/rỗng   
                  tắc backend                   nếu backend quy định   
                                                bắt buộc hoặc có kiểm  
                                                tra định dạng          

  `role`          Role được phép V7             \(a\) Role không được  X20--X23
                  đăng ký theo                  phép (b) role không    
                  backend                       tồn tại (c)            
                                                thiếu/null/rỗng nếu    
                                                bắt buộc (d) sai kiểu  
                                                dữ liệu                
  -----------------------------------------------------------------------------------

**Lưu ý:** Các lớp trên là danh sách tình huống cần kiểm thử. Trước khi
chốt Expected Result, phải kiểm tra `RegisterRequest`, `AuthController`,
service và cấu hình phân quyền. Không mặc định tất cả trường đều bắt
buộc nếu chưa có annotation hoặc kiểm tra tương ứng.

## 1.3. Bảng phân tích giá trị biên (BVA)

Hiện chưa có source code/DTO xác nhận các giới hạn min/max của
`username`, `password`, `email`, `firstName`, `lastName`, `phoneNumber`.
Vì vậy không điền số ký tự giả định.

  --------------------------------------------------------------------------------------
  Biến đầu vào    min-    min     min+    nominal    max-    max     max+    Tag
  --------------- ------- ------- ------- ---------- ------- ------- ------- -----------
  `username`      Chưa    Chưa    Chưa    Username   Chưa    Chưa    Chưa    B1--B7: chờ
                  xác     xác     xác     hợp lệ     xác     xác     xác     xác nhận
                  định    định    định               định    định    định    giới hạn

  `password`      Chưa    Chưa    Chưa    Mật khẩu   Chưa    Chưa    Chưa    B8--B14:
                  xác     xác     xác     hợp lệ     xác     xác     xác     chờ xác
                  định    định    định               định    định    định    nhận giới
                                                                             hạn

  `email`         Chưa    Chưa    Chưa    Email hợp  Chưa    Chưa    Chưa    B15--B21:
                  xác     xác     xác     lệ         xác     xác     xác     chờ xác
                  định    định    định               định    định    định    nhận giới
                                                                             hạn

  `firstName`     Chưa    Chưa    Chưa    Tên hợp lệ Chưa    Chưa    Chưa    B22--B28:
                  xác     xác     xác                xác     xác     xác     chờ xác
                  định    định    định               định    định    định    nhận giới
                                                                             hạn

  `lastName`      Chưa    Chưa    Chưa    Họ hợp lệ  Chưa    Chưa    Chưa    B29--B35:
                  xác     xác     xác                xác     xác     xác     chờ xác
                  định    định    định               định    định    định    nhận giới
                                                                             hạn

  `phoneNumber`   Chưa    Chưa    Chưa    Số điện    Chưa    Chưa    Chưa    B36--B42:
                  xác     xác     xác     thoại hợp  xác     xác     xác     chờ xác
                  định    định    định    lệ         định    định    định    nhận giới
                                                                             hạn
  --------------------------------------------------------------------------------------

**Cách hoàn thiện BVA:** Mở DTO/Request class và tìm các annotation như
`@Size(min=..., max=...)`, `@Min`, `@Max`, `@Length`, hoặc kiểm tra
tương đương trong service. Khi có min/max, lập các giá trị `min-`,
`min`, `min+`, `nominal`, `max-`, `max`, `max+`. Nếu không có giới hạn,
ghi "Không quy định trong source", không tự đặt số.

## 1.4. Bảng quyết định (Decision Table)

### Conditions / Actions

-   C1: Username hợp lệ và chưa tồn tại?
-   C2: Password đáp ứng quy tắc backend?
-   C3: Email hợp lệ và chưa tồn tại?
-   C4: Các trường tên/số điện thoại hợp lệ theo quy tắc backend?
-   C5: Role được phép đăng ký?
-   A1: Tạo tài khoản.
-   A2: Từ chối do username.
-   A3: Từ chối do password.
-   A4: Từ chối do email.
-   A5: Từ chối do thông tin cá nhân.
-   A6: Từ chối do role.

  Condition / Action                   R1   R2   R3   R4   R5   R6
  ---------------------------------- ---- ---- ---- ---- ---- ----
  C1. Username hợp lệ/chưa tồn tại      Y    N    Y    Y    Y    Y
  C2. Password hợp lệ                   Y   \-    N    Y    Y    Y
  C3. Email hợp lệ/chưa tồn tại         Y   \-   \-    N    Y    Y
  C4. Thông tin cá nhân hợp lệ          Y   \-   \-   \-    N    Y
  C5. Role được phép                    Y   \-   \-   \-   \-    N
  A1. Tạo tài khoản                     X   \-   \-   \-   \-   \-
  A2. Từ chối do username              \-    X   \-   \-   \-   \-
  A3. Từ chối do password              \-   \-    X   \-   \-   \-
  A4. Từ chối do email                 \-   \-   \-    X   \-   \-
  A5. Từ chối do thông tin cá nhân     \-   \-   \-   \-    X   \-
  A6. Từ chối do role                  \-   \-   \-   \-   \-    X

**Diễn giải Rules:** - R1: Tất cả dữ liệu hợp lệ, username/email chưa
tồn tại, role được phép → tạo tài khoản. - R2: Username thiếu, không hợp
lệ hoặc đã tồn tại → từ chối theo quy tắc backend. - R3: Password không
đáp ứng quy tắc backend → từ chối. - R4: Email sai định dạng, thiếu hoặc
đã tồn tại → từ chối. - R5: Thông tin cá nhân không đáp ứng validation
nếu backend có quy định → từ chối. - R6: Role không được phép đăng ký →
từ chối.

**Lưu ý:** Bảng là mô hình logic ban đầu. Khi có source code, cần xác
nhận điều kiện nào thực sự được backend kiểm tra và thứ tự validation.
Nếu nhiều điều kiện cùng sai, response có thể chỉ trả về lỗi đầu tiên
hoặc danh sách lỗi.

------------------------------------------------------------------------

# 2. POST /api/admin/departments

## 2.1. Thông tin API và dữ liệu đầu vào

-   **Method:** `POST`
-   **URL:** `/api/admin/departments`
-   **Content-Type:** `application/json`
-   **Authorization:** Cần xác nhận từ
    `DepartmentController`/SecurityConfig. API thuộc nhóm `/api/admin`;
    khi kiểm thử nên chuẩn bị token ADMIN và kiểm tra cả trường hợp
    thiếu token/không đủ quyền.
-   **Request Body tham khảo từ lần tạo phòng ban trước đây:**

``` json
{
  "name": "Khoa Da Nghiem Thu",
  "description": "Phong ban kiem thu",
  "active": true
}
```

**Các trường đã quan sát trong lần tạo trước:**

  Trường          Nội dung
  --------------- ----------------------
  `name`          Tên phòng ban
  `description`   Mô tả phòng ban
  `active`        Trạng thái hoạt động

Lần tạo trước đó đã nhận response có `id: 10`, `name`, `description`,
`active: true`, `createdAt`, `updatedAt`. Đây là dữ liệu quan sát ở lần
chạy trước, không thay thế cho đặc tả validation.

**Kết quả cần xác minh:** HTTP status khi tạo thành công; cấu trúc
response; quy tắc bắt buộc của `name`; `description` có bắt buộc không;
`active` có giá trị mặc định không; tên phòng ban có được phép trùng
không; quyền cần có là ADMIN hay quy tắc khác.

## 2.2. Bảng phân vùng tương đương (EP)

  ----------------------------------------------------------------------------------
  Biến đầu vào    Lớp hợp lệ     Tag            Lớp không hợp lệ /    Tag
                                                cần kiểm tra          
  --------------- -------------- -------------- --------------------- --------------
  `name`          Tên phòng ban  V1             \(a\) Thiếu trường    X1--X6
                  hợp lệ theo                   (b) null (c) chuỗi    
                  backend                       rỗng (d) chỉ chứa     
                                                khoảng trắng (e) sai  
                                                kiểu dữ liệu (f)      
                                                trùng tên nếu hệ      
                                                thống cấm trùng       

  `description`   Chuỗi mô tả    V2             Sai kiểu dữ liệu;     X7
                  hợp lệ hoặc                   null/rỗng chỉ được    
                  không gửi nếu                 xem là không hợp lệ   
                  backend cho                   nếu backend quy định  
                  phép tùy chọn                 bắt buộc              

  `active`        Boolean `true` V3             Chuỗi                 X8--X9
                  hoặc `false`,                 `"true"`/`"false"`,   
                  hoặc không gửi                số, object hoặc kiểu  
                  nếu backend có                dữ liệu khác Boolean; 
                  giá trị mặc                   null nếu backend      
                  định                          không chấp nhận       

  Authorization   Token hợp lệ,  V4             Thiếu token; token    X10--X12
                  đúng quyền                    sai/hết hạn; token    
                  được endpoint                 hợp lệ nhưng không đủ 
                  yêu cầu                       quyền                 
  ----------------------------------------------------------------------------------

**Lưu ý:** Không mặc định `description` là bắt buộc hoặc `active` bắt
buộc phải gửi. Cần đối chiếu DTO và service. Tương tự, chỉ kết luận tên
trùng không hợp lệ khi source code/database có ràng buộc tương ứng.

## 2.3. Bảng phân tích giá trị biên (BVA)

Chưa có DTO/source xác nhận giới hạn độ dài của `name` và `description`;
cũng chưa có giới hạn số học cho các trường. `active` là Boolean nên
không áp dụng BVA độ dài.

  ------------------------------------------------------------------------------------------------------------
  Biến đầu vào    min-    min       min+    nominal                        max-    max     max+    Tag
  --------------- ------- --------- ------- ------------------------------ ------- ------- ------- -----------
  `name`          Chưa    Chưa xác  Chưa    `"Khoa Cong nghe thong tin"`   Chưa    Chưa    Chưa    B1--B7: chờ
                  xác     định      xác                                    xác     xác     xác     xác nhận
                  định              định                                   định    định    định    min/max

  `description`   Chưa    Chưa xác  Chưa    `"Phong ban kiem thu"`         Chưa    Chưa    Chưa    B8--B14:
                  xác     định      xác                                    xác     xác     xác     chờ xác
                  định              định                                   định    định    định    nhận
                                                                                                   min/max

  `active`        Không   `false`   Không   `true`                         Không   Không   Không   B15--B16:
                  áp dụng           áp dụng                                áp dụng áp dụng áp dụng kiểm tra
                                                                                                   hai giá trị
                                                                                                   Boolean
  ------------------------------------------------------------------------------------------------------------

**Cách hoàn thiện BVA:** Kiểm tra DTO để tìm `@NotBlank`, `@Size`,
`@Length` hoặc các kiểm tra độ dài trong service. Nếu `name` có min/max,
lập đủ 7 vị trí biên. Với `active`, kiểm thử hai giá trị Boolean `true`
và `false`; kiểm tra thêm trường hợp thiếu/null nếu DTO cho phép hoặc
không cho phép.

## 2.4. Bảng quyết định (Decision Table)

### Conditions / Actions

-   C1: Người gửi có token hợp lệ và đủ quyền quản trị?
-   C2: `name` hợp lệ?
-   C3: Tên phòng ban đã tồn tại (nếu hệ thống có kiểm tra trùng)?
-   C4: `description` hợp lệ theo quy tắc backend?
-   C5: `active` có kiểu Boolean hợp lệ hoặc được backend gán mặc định?
-   A1: Tạo phòng ban.
-   A2: Từ chối xác thực/phân quyền.
-   A3: Từ chối do tên phòng ban.
-   A4: Từ chối do trùng tên.
-   A5: Từ chối do description.
-   A6: Từ chối do active.

  Condition / Action                    R1   R2   R3   R4   R5   R6
  ----------------------------------- ---- ---- ---- ---- ---- ----
  C1. Có quyền quản trị                  Y    N    Y    Y    Y    Y
  C2. `name` hợp lệ                      Y   \-    N    Y    Y    Y
  C3. Tên chưa tồn tại                   Y   \-   \-    N    Y    Y
  C4. `description` hợp lệ               Y   \-   \-   \-    N    Y
  C5. `active` hợp lệ/được mặc định      Y   \-   \-   \-   \-    N
  A1. Tạo phòng ban                      X   \-   \-   \-   \-   \-
  A2. Từ chối quyền                     \-    X   \-   \-   \-   \-
  A3. Từ chối do name                   \-   \-    X   \-   \-   \-
  A4. Từ chối do trùng tên              \-   \-   \-    X   \-   \-
  A5. Từ chối do description            \-   \-   \-   \-    X   \-
  A6. Từ chối do active                 \-   \-   \-   \-   \-    X

**Diễn giải Rules:** - R1: Người dùng đủ quyền, dữ liệu hợp lệ và không
vi phạm quy tắc trùng tên → tạo phòng ban. - R2: Thiếu token hoặc không
đủ quyền → từ chối. - R3: `name` thiếu/rỗng/sai kiểu hoặc không đạt
validation → từ chối. - R4: Tên đã tồn tại → từ chối nếu backend có quy
định không cho trùng. - R5: `description` không hợp lệ → từ chối nếu
backend có validation tương ứng. - R6: `active` sai kiểu hoặc không hợp
lệ theo DTO → từ chối.

**Lưu ý:** R4 chỉ áp dụng khi xác minh được quy tắc chống trùng. Nếu hệ
thống cho phép trùng tên, bỏ điều kiện C3 và Rule R4. Nếu `description`
hoặc `active` là tùy chọn, phải phản ánh đúng hành vi mặc định trong
bảng.

------------------------------------------------------------------------

# 3. Tổng kết phạm vi EP, BVA và Decision Table

  -------------------------------------------------------------------------------------
  API                             EP -- Phân vùng   BVA -- Giá trị    Decision Table
                                  tương đương       biên              
  ------------------------------- ----------------- ----------------- -----------------
  `POST /api/auth/register`       username,         Chưa có giới hạn  Dữ liệu đăng ký
                                  password, email,  min/max được xác  hợp lệ,
                                  firstName,        minh; cần đọc     username/email
                                  lastName,         DTO/source trước  trùng, dữ liệu
                                  phoneNumber, role khi chốt số biên  thiếu/sai và role

  `POST /api/admin/departments`   name,             Chưa có giới hạn  Quyền quản trị,
                                  description,      độ dài được xác   name, trùng tên
                                  active, quyền     minh; Boolean     (nếu có),
                                  truy cập          kiểm tra          description và
                                                    true/false        active
  -------------------------------------------------------------------------------------

------------------------------------------------------------------------

# 4. Danh sách Test Case cần đưa vào CSV

## 4.1. Register

Các tình huống tối thiểu cần có:

1.  Đăng ký với toàn bộ dữ liệu hợp lệ.
2.  Username bị thiếu.
3.  Username rỗng.
4.  Username đã tồn tại.
5.  Password bị thiếu.
6.  Password rỗng.
7.  Email sai định dạng.
8.  Email bị thiếu.
9.  Email đã tồn tại.
10. Role không hợp lệ/không được phép.
11. Sai kiểu dữ liệu của một trường.
12. Các trường tùy chọn bị bỏ qua (nếu backend cho phép).

## 4.2. Departments

Các tình huống tối thiểu cần có:

1.  Tạo phòng ban với dữ liệu hợp lệ.
2.  Thiếu `name`.
3.  `name` rỗng.
4.  `name` chỉ chứa khoảng trắng.
5.  `name` trùng (nếu backend cấm trùng).
6.  Thiếu `description` (nếu tùy chọn thì phải chấp nhận).
7.  `description` sai kiểu dữ liệu.
8.  `active = true`.
9.  `active = false`.
10. `active` sai kiểu dữ liệu.
11. Không có token.
12. Token không đủ quyền.

> Các tình huống này là danh sách thiết kế. Expected Status/Expected
> Result phải được xác nhận bằng source code hoặc chạy Postman, không tự
> mặc định mọi lỗi đều trả HTTP 400.

------------------------------------------------------------------------

# 5. Quy ước CSV và Postman Collection Runner

Nên tách dữ liệu theo API để tránh chạy nhầm request hoặc dùng sai biến.

## 5.1. File `register-testcase.csv`

Các cột đề xuất:

``` csv
testCaseId,username,password,email,firstName,lastName,phoneNumber,role,expectedStatus
REG-001,testuser01,Test123456,test01@gmail.com,Nguyen,Van A,0901234567,PATIENT,200
REG-002,,Test123456,test02@gmail.com,Nguyen,Van B,0901234568,PATIENT,400
```

Các status `200` và `400` ở đây chỉ là giá trị mẫu theo giả định đã dùng
trước đây. Hãy thay bằng status đã xác nhận. Dữ liệu hợp lệ phải duy
nhất mỗi lần chạy nếu API không cho phép username/email trùng.

## 5.2. File `departments-testcase.csv`

Các cột đề xuất:

``` csv
testCaseId,name,description,active,expectedStatus
DEP-001,Khoa CNTT,Phong ban CNTT,true,201
DEP-002,,Phong ban thu nghiem,true,400
```

Status `201` và `400` chỉ là ví dụ; cần xác nhận response thực tế. Với
trường tùy chọn, có thể cần tách bộ CSV hoặc xử lý giá trị rỗng đúng
cách trong Pre-request Script.

## 5.3. Cấu hình request Register

-   Method: `POST`
-   URL: `{{baseUrl}}/api/auth/register`
-   Body → raw → JSON:

``` json
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

## 5.4. Cấu hình request Departments

-   Method: `POST`
-   URL: `{{baseUrl}}/api/admin/departments`
-   Authorization: dùng Bearer Token ADMIN nếu endpoint yêu cầu.
-   Body → raw → JSON:

``` json
{
  "name": "{{name}}",
  "description": "{{description}}",
  "active": {{active}}
}
```

**Chú ý:** `active` là Boolean nên không đặt trong dấu nháy kép nếu CSV
chứa `true`/`false`. Nếu trường này được phép bỏ qua, cần cấu hình
request riêng hoặc script tạo body phù hợp.

## 5.5. Test Script tham khảo

``` javascript
const expectedStatus = Number(pm.iterationData.get("expectedStatus"));

pm.test("HTTP status matches expected status", function () {
    pm.expect(pm.response.code).to.eql(expectedStatus);
});

pm.test("Response time under 2000ms", function () {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

console.log("Test Case:", pm.iterationData.get("testCaseId"));
console.log("Actual Status:", pm.response.code);
console.log("Response:", pm.response.text());
```

Nếu request dùng CSV, tên biến trong `pm.iterationData.get()` phải khớp
chính xác tên cột. Không đánh dấu test case PASS chỉ vì request trả về
status bất kỳ; phải so sánh với expected status đã xác minh.

## 5.6. Chạy và xuất kết quả

1.  Mở Collection Runner.
2.  Chọn folder/request Register và file `register-testcase.csv`.
3.  Chạy và lưu kết quả JSON.
4.  Chọn folder/request Departments và file `departments-testcase.csv`.
5.  Chạy và lưu kết quả JSON.
6.  Đối chiếu số Iterations với số dòng dữ liệu CSV.
7.  Chụp ảnh tổng quan Passed/Failed và một số response tiêu biểu.

Tên file gợi ý:

-   `register-results.json`
-   `departments-results.json`

------------------------------------------------------------------------

# 6. Những thông tin phải xác minh trước khi chốt bài

1.  `RegisterRequest`: trường nào bắt buộc, annotation nào đang dùng,
    giới hạn min/max và role nào được phép đăng ký.
2.  `DepartmentRequest`/DTO: `name`, `description`, `active` có bắt buộc
    không; giới hạn độ dài; có cấm trùng tên không.
3.  `DepartmentController`: endpoint tạo phòng ban yêu cầu quyền ADMIN
    hay quyền nào khác.
4.  HTTP status thực tế khi thành công và khi validation thất bại.
5.  Response body thực tế cho từng trường hợp lỗi.

**Kết luận:** File này đã được chuyển phạm vi từ 3 API mẫu sang đúng 2
API trong yêu cầu của bạn: Register và Departments. Những giới hạn chưa
có bằng chứng từ source code được để ở trạng thái chờ xác minh để tránh
đưa test case sai vào báo cáo.
