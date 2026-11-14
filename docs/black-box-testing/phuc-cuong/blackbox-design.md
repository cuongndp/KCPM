# Thiết kế kiểm thử Black-box — HMS

Phạm vi gồm ba API POST: đăng nhập, tạo hồ sơ bệnh án và tạo đơn thuốc. Thiết kế dựa trên `docs/black-box-testing/phuc-cuong/kiem-thu-3-api-post.md`, các DTO/controller/service và xử lý ngoại lệ của backend. Đây là kỳ vọng thiết kế; cần chạy trên môi trường test để xác nhận response thực tế.

## 1. Phạm vi, quy ước và dữ liệu kiểm thử

| API | Endpoint | Xác thực | Thành công dự kiến |
|---|---|---|---|
| Login | `POST /api/auth/login` | Công khai | `200`, có `token`, `type`, `userId`, `username`, `role` |
| Medical record | `POST /api/doctor/medical-records?doctorId={id}` | Bearer token role `DOCTOR` | `200`, trả hồ sơ vừa tạo |
| Prescription | `POST /api/doctor/prescriptions?doctorId={id}` | Bearer token role `DOCTOR` | `200`, trả đơn thuốc vừa tạo |

EP = phân vùng tương đương; BVA = giá trị biên; DT = bảng quyết định; RBAC = phân quyền theo vai trò. `V` là lớp hợp lệ, `X` là lớp không hợp lệ; `Y/N/-` trong bảng quyết định lần lượt là có/không/không xét. Mã HTTP trong CSV là một số nguyên để có thể assert tự động. Những kết quả phụ thuộc môi trường hoặc dữ liệu được ghi chú, không coi là Pass trước khi chạy.

| Loại dữ liệu | Seed mẫu khi `app.seed.enabled=true` |
|---|---|
| ADMIN | username `1`, password `1` |
| DOCTOR | username `2`, password `2`; DB mới: `doctorId=1` |
| PATIENT | username `3`, password `3`; DB mới: `patientId=1` |
| Appointment | DB mới: `appointmentId=1` chỉ khi appointment test đã được tạo |

Seed thật trong `SampleDataInitializer`: user `1`/`1` = ADMIN, `2`/`2` = DOCTOR, `3`/`3` = PATIENT (mật khẩu trùng username). Với DB mới khởi tạo, `doctorId=1` là hồ sơ Doctor của user `2`, `patientId=1` là hồ sơ Patient của user `3`.

Nếu DB không phải bản mới, lấy doctorId thật bằng GET /api/doctor/profile?userId=2 (token của user 2) và patientId thật bằng GET /api/patient/profile?userId=3 (token của user 3); appointment tạo trước bằng POST /api/patient/appointments?patientId=<pid> với token PATIENT.

## 2. EP — Bảng phân vùng tương đương

| API | Trường | Lớp hợp lệ | Lớp không hợp lệ / mã |
|---|---|---|---|
| Login | username | Tài khoản tồn tại (V1) | Không tồn tại, rỗng, thiếu, null, sai chữ hoa/thường (X1–X5) |
| Login | password | Khớp username (V2) | Sai, rỗng, thiếu, null, chỉ khoảng trắng (X6–X10) |
| Login | Tài khoản | Đang hoạt động và xác thực được (V3) | Tài khoản inactive/không được phép xác thực (X11; cần seed) |
| Login | username độ dài / nội dung | Username chuỗi hợp lệ, gồm Unicode và độ dài không bị giới hạn trong DTO | 256 ký tự và chuỗi SQL injection được kiểm tra bằng TC-LGN-22..24 |
| Medical record | doctorId query | ID Doctor tồn tại (V1) | Thiếu, sai kiểu Long, không tồn tại (X1–X3) |
| Medical record | patientId | ID Patient tồn tại (V2) | Thiếu/null, không tồn tại (X4–X5) |
| Medical record | diagnosis | Có ít nhất một ký tự không trắng (V3) | Rỗng, blank, thiếu, null (X6–X9) |
| Medical record | ID biên | doctorId/patientId bằng 0 hoặc âm là ID không tồn tại; diagnosis dài được kiểm tra tại TC-MRC-31..32 |
| Medical record | trường mô tả | Có nội dung hoặc không gửi (V4–V7) | DTO không quy định lớp không hợp lệ |
| Medical record | Authorization | Bearer hợp lệ, role DOCTOR (V8) | Thiếu/sai/hết hạn hoặc role khác (X10–X11) |
| Prescription | doctorId query | ID Doctor tồn tại (V1) | Thiếu, sai kiểu Long, không tồn tại (X1–X3) |
| Prescription | patientId / appointmentId | ID tồn tại và khác null (V2–V3) | Thiếu/null hoặc không tồn tại (X4–X7) |
| Prescription | medication / dosage / frequency | Chuỗi có ký tự không trắng (V4–V6) | Rỗng, blank, thiếu, null (X8–X19) |
| Prescription | duration | Số nguyên hoặc không gửi (V7) | Sai kiểu dữ liệu (X20); DTO không có min/max |
| Prescription | duration nghiệp vụ | Đề xuất số ngày trong khoảng 1–365 | 0, âm, vượt 365 và tràn Integer; cần chốt nghiệp vụ |
| Prescription | appointment/patient relation | Lịch hẹn thuộc đúng patientId | Appointment của bệnh nhân khác (X23) |
| Prescription | instructions | Chuỗi hoặc không gửi (V8) | DTO không quy định lớp không hợp lệ |
| Prescription | Authorization | Bearer hợp lệ, role DOCTOR (V9) | Thiếu/sai/hết hạn hoặc role khác (X21–X22) |
| Login | Content-Type và trường thừa | JSON đúng kiểu với Content-Type hợp lệ; extra role không ảnh hưởng role đã lưu | `text/plain` (TC-LGN-25); `role=ADMIN` thừa (TC-LGN-26) |

### 2.1 Số test case tối thiểu để phủ 100% lớp tương đương

Quy tắc: một test case hợp lệ có thể đồng thời đại diện lớp hợp lệ của **mọi** trường; mỗi test case không hợp lệ chỉ được chứa **một** giá trị không hợp lệ, các trường còn lại giữ giá trị hợp lệ.

Công thức: **TC_min = max(số lớp hợp lệ của một trường) + tổng số lớp không hợp lệ của tất cả trường**.

| API | Trường (lớp hợp lệ / không hợp lệ) | max lớp hợp lệ | Tổng lớp không hợp lệ | TC tối thiểu |
|---|---|---:|---:|---:|
| Login | username (1/5), password (1/5), trạng thái tài khoản (1/1), Content-Type (1/1) | 1 | 12 | **13** |
| Tạo hồ sơ bệnh án | doctorId (1/4), patientId (1/4), diagnosis (1/4), token (1/5), quyền sở hữu bản ghi (1/1) | 1 | 18 | **19** |
| Tạo đơn thuốc | doctorId (1/4), patientId (1/4), appointmentId (1/4), medication (1/3), dosage (1/3), frequency (1/3), duration (1/4), token (1/5), quan hệ lịch hẹn (1/2) | 1 | 32 | **33** |

Như vậy chỉ cần **13 / 19 / 33** test case là phủ 100% các lớp tương đương. Ba file CSV hiện có 26 / 32 / 36 case, tức đã vượt mức tối thiểu; phần dư dành cho kiểm thử độ bền, bảo mật và kiểm tra hợp đồng response.

## 3. BVA — Bảng giá trị biên

| API / trường | Biên xác định được từ DTO | Giá trị cần thử | Giới hạn chưa được đặc tả |
|---|---|---|---|
| Login username, password | Không có `@NotBlank`, `@Size`, min/max | Rỗng/blank, thiếu, null là EP/kiểm tra đầu vào, không phải min-length | Không suy đoán min/max độ dài |
| Medical record doctorId, patientId | `patientId @NotNull`; không có min/max số học | Null/thiếu; ID tồn tại và ID không tồn tại | `doctorId`/`patientId` không có `@Min`, `@Positive` hay `@Max` |
| Medical record diagnosis | `@NotBlank`: ranh giới thấp là 0 ký tự không trắng so với 1 ký tự không trắng | `""`, `"   "`, `"A"`, `"AB"`, giá trị thường | Không có giới hạn độ dài tối đa |
| Medical record trường tùy chọn | Không có ràng buộc độ dài/giá trị | Bỏ qua, null, chuỗi có nội dung; chuỗi dài là kiểm tra thăm dò | Không áp đặt min/max |
| Prescription IDs | `patientId`, `appointmentId @NotNull`; không có min/max số học | Thiếu/null và ID có/không tồn tại | Không suy ra min=1 từ `@NotNull` |
| Prescription medication, dosage, frequency | `@NotBlank`: tối thiểu 1 ký tự không trắng | `""`, blank, `"A"`, `"AB"`, giá trị thường | Không có giới hạn tối đa |
| Prescription duration | Integer tùy chọn, không `@Min/@Max` | Không gửi, 0, số âm, số dương, số nguyên lớn, sai kiểu | Quy tắc số ngày hợp lệ và miền Integer nghiệp vụ chưa chốt |
| Prescription instructions | Trường tùy chọn, không giới hạn độ dài | Bỏ qua, null, có nội dung; chuỗi dài thăm dò | Không có min/max |
| Medical record doctorId/patientId | Không có min/max số học | 0, -1; diagnosis 255 và 5000 ký tự | ID không tồn tại; diagnosis chưa quy định max |
| Login username | Không có min/max trong DTO | 256 ký tự, SQL injection, Unicode; Content-Type text/plain và trường role thừa | Không tự đặt giới hạn username |
| Prescription duration/instructions | Kiểu Integer và chuỗi tùy chọn | 3.5, "abc", 2147483648; instructions 1000 ký tự | DTO dùng Integer; business max duration đề xuất 365 |

Các trường hợp BVA không có ràng buộc nguồn được đánh dấu “chưa chốt”; không tạo giả định min/max. CSV bao gồm giá trị đại diện cho biên `@NotBlank`, null/thiếu và giá trị tùy chọn.

### 3.1 Số test case tối thiểu cho BVA

Quy tắc: mỗi miền có biên xác định cần tối thiểu bốn giá trị min, min+1, max-1, max. Với trường chỉ ràng buộc `@NotBlank`, biên là 0 ký tự và 1 ký tự. Trường không có max trong đặc tả thì ghi "chưa chốt", không tự đặt ngưỡng.

| API | Miền và giá trị biên | Số giá trị biên | Đã phủ bởi EP | Case BVA bổ sung |
|---|---|---:|---:|---:|
| Login | đặc tả không quy định miền số hay độ dài | 0 | 0 | **0** |
| Tạo hồ sơ bệnh án | doctorId {0, 1}; patientId {0, 1}; diagnosis {0 ký tự, 1 ký tự} | 6 | 5 | **1** (diagnosis 1 ký tự) |
| Tạo đơn thuốc | duration {0, 1, 365, 2147483648}; doctorId, patientId {0, 1} | 8 | 6 | **2** (duration = 1 và 365) |

Giá trị 0 của ID và 0 ký tự đã nằm trong lớp không hợp lệ của bảng EP; ID = 1 đã dùng ở case hợp lệ. Vì vậy BVA chỉ thêm **0 / 1 / 2** case, không cần lặp lại toàn bộ bảng.

## 4. Bảng quyết định (Decision Table)

### 4.1 Login

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| C1 username tồn tại | Y | Y | N | Y | Y |
| C2 password đúng | Y | N | - | Y | Y |
| C3 tài khoản được xác thực/active | Y | - | - | N | Y |
| A1 HTTP 200 và cấp token | X | - | - | - | X |
| A2 từ chối đăng nhập | - | X | X | X | - |

R1/R5 thành công; R2 sai mật khẩu; R3 username không tồn tại; R4 tài khoản không được phép xác thực. R4 phụ thuộc cấu hình user details và dữ liệu inactive.

### 4.2 Tạo hồ sơ bệnh án

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1 token role DOCTOR hợp lệ | Y | N | Y | Y | Y | Y |
| C2 doctorId tồn tại | Y | - | N | Y | Y | Y |
| C3 patientId khác null | Y | - | Y | N | Y | Y |
| C4 patient tồn tại | Y | - | - | - | N | Y |
| C5 diagnosis NotBlank | Y | - | Y | Y | Y | N |
| A1 tạo, HTTP 200 | X | - | - | - | - | - |
| A2 từ chối xác thực/phân quyền | - | X | - | - | - | - |
| A3 lỗi validation, HTTP 400 | - | - | - | X | - | X |
| A4 không tìm thấy tài nguyên, HTTP 404 | - | - | X | - | X | - |

### 4.3 Tạo đơn thuốc

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 | R7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| C1 token role DOCTOR hợp lệ | Y | N | Y | Y | Y | Y | Y |
| C2 doctorId tồn tại | Y | - | N | Y | Y | Y | Y |
| C3 patientId khác null và tồn tại | Y | - | Y | N | N | Y | Y |
| C4 appointmentId khác null và tồn tại | Y | - | Y | Y | Y | N | N |
| C5 medication/dosage/frequency hợp lệ | Y | - | Y | Y | Y | Y | N |
| A1 tạo, HTTP 200 | X | - | - | - | - | - | - |
| A2 từ chối xác thực/phân quyền | - | X | - | - | - | - | - |
| A3 lỗi validation, HTTP 400 | - | - | - | X | - | X | X |
| A4 không tìm thấy tài nguyên, HTTP 404 | - | - | X | - | X | - | - |

### 4.4 Số test case tối thiểu cho bảng quyết định

Mỗi luật của bảng quyết định tương ứng một test case, vì luật chính là một tổ hợp điều kiện cần được chứng minh.

| API | Số luật | TC tối thiểu nếu kiểm thử độc lập | TC phát sinh thêm khi gộp với EP |
|---|---:|---:|---:|
| Login | 4 (R1–R4; R5 trùng R1) | 4 | 0 |
| Tạo hồ sơ bệnh án | 6 | 6 | 0 |
| Tạo đơn thuốc | 7 | 7 | 0 |

Khi làm riêng, bảng quyết định cần đúng 4 / 6 / 7 case. Khi gộp với EP thì không phát sinh case mới, vì mỗi luật ánh xạ một-một vào một lớp (hoặc cặp lớp) đã được thiết kế — bảng quyết định vì thế dùng để chứng minh không thiếu tổ hợp, không phải để tăng số case.

## 5. Ma trận quyền (RBAC)

| Actor / tình huống | Login | Medical records POST | Prescriptions POST | Kết quả dự kiến |
|---|---:|---:|---:|---|
| Chưa xác thực | Được gọi | Không được phép | Không được phép | Login 200 nếu thông tin đúng; API Doctor 401 |
| DOCTOR có token hợp lệ | Được gọi | Được phép theo role | Được phép theo role | 200 khi request/dữ liệu hợp lệ |
| PATIENT có token hợp lệ | Được gọi | Không được phép | Không được phép | 403 |
| ADMIN có token hợp lệ | Được gọi | Không được phép theo annotation hiện tại | Không được phép theo annotation hiện tại | 403 |
| Token sai/không xác thực được | Được gọi như dữ liệu login | Không được phép | Không được phép | 401 |
| Token DOCTOR nhưng doctorId thuộc bác sĩ khác | — | Cần kiểm tra quyền sở hữu | Cần kiểm tra quyền sở hữu | Cần seed đặc biệt; chưa xác định implementation có ràng buộc bản ghi |

Các endpoint Doctor chỉ có `@PreAuthorize("hasRole('DOCTOR')")`; việc đối chiếu doctorId với danh tính trong token cần kiểm thử riêng, không suy ra từ role.

## 6. Ma trận phủ kỹ thuật

| Nhóm kiểm thử | Login (26) | Medical records (32) | Prescriptions (36) |
|---|---|---|---|
| EP hợp lệ và không hợp lệ | Username/password, user active | doctorId, patientId, diagnosis, token | doctor/patient/appointment ID, 3 trường NotBlank, token |
| BVA có căn cứ | Rỗng/blank; không có biên độ dài | Diagnosis 0/1 ký tự không trắng; null/thiếu | 3 trường NotBlank; duration tùy chọn/0/âm/lớn |
| Decision table | Thành công, sai thông tin, user inactive | Auth, tài nguyên, validation | Auth, tài nguyên, validation |
| RBAC | — | Anonymous/DOCTOR/PATIENT/ADMIN | Anonymous/DOCTOR/PATIENT |
| Kiểu dữ liệu / JSON | Malformed JSON, sai kiểu, trường thiếu/null | Malformed JSON, ID/field sai kiểu | ID/field sai kiểu, malformed JSON |
| Trường tùy chọn | — | Bỏ qua/null/nội dung dài | Bỏ qua/null/duration ngoài miền nghiệp vụ |

## 7. Tổng hợp test suite

| File | Dải TC_ID | Số case | Nội dung |
|---|---|---:|---|
| `exports/testcases-login.csv` | TC-LGN-01..26 | 26 | Xác thực, đầu vào, độ dài, SQL injection, Unicode, media type và trường role thừa |
| `exports/testcases-medical-records.csv` | TC-MRC-01..32 | 32 | Tạo hồ sơ, auth/RBAC, ID biên, validation, payload và độ dài diagnosis |
| `exports/testcases-prescriptions.csv` | TC-PRS-01..36 | 36 | Tạo đơn, validation, duration nghiệp vụ/kiểu, quan hệ lịch hẹn, RBAC và độ dài |

Tổng cộng: **94 test case**.

Mỗi CSV dùng cùng 10 cột, UTF-8 có BOM, dấu phẩy và quote kép cho toàn bộ trường. Mỗi case có cờ Postman ở cột `Ghi chú`. Những case cần cấu hình dữ liệu đặc biệt được gắn `Không`; các request còn lại có thể gửi trực tiếp trong Postman.

### 7.1 Bảng tổng hợp số test case tối thiểu

| API | EP tối thiểu | BVA bổ sung | Bảng quyết định (nếu riêng) | Tổng tối thiểu (đã gộp) | CSV hiện có | Kết luận |
|---|---:|---:|---:|---:|---:|---|
| Login | 13 | 0 | 4 | **13** | 26 | Đủ, dư 13 |
| Tạo hồ sơ bệnh án | 19 | 1 | 6 | **20** | 32 | Đủ, dư 12 |
| Tạo đơn thuốc | 33 | 2 | 7 | **35** | 36 | Đủ, dư 1 |
| **Tổng** | 65 | 3 | 17 | **68** | 94 | Đủ, dư 26 |

## 8. Cách thực thi và tiêu chí ghi nhận

1. Khởi chạy backend và DB test; xác nhận seed, endpoint base URL, role token, Doctor/Patient/Appointment ID.
2. Với login, gửi JSON trong `Body` raw; thành công phải trả đủ token và thông tin user, lỗi không được cấp token.
3. Với hai endpoint Doctor, đặt `Authorization: Bearer {{DOCTOR_TOKEN}}`, `Content-Type: application/json`; truyền doctorId trên query string.
4. Gửi từng test case bằng Postman Runner/Newman, không dùng lại dữ liệu tạo mới nếu nó làm sai điều kiện của case tiếp theo. Hồ sơ/đơn thuốc thành công có thể tạo dữ liệu mới.
5. So sánh status với `Kỳ vọng HTTP`, xác minh `Kỳ vọng nội dung` và kiểm tra không phát sinh resource với request bị từ chối.
6. Lưu status/thực tế, response body, thời điểm và môi trường; ghi nhận khác biệt thay vì sửa kỳ vọng sau khi chạy mà không có căn cứ.

Các status `400/401/403/404` được dự kiến theo DTO, Spring Security và `GlobalExceptionHandler`. Lỗi binding/JSON và truy cập tài nguyên có thể khác theo cấu hình triển khai; cần xác nhận trên môi trường đích.

## 9. Chỗ đặc tả chưa chốt

- LoginRequest không khai báo `@NotBlank` hoặc giới hạn độ dài. Status với trường null/thiếu và cách kiểm tra user inactive cần xác nhận.
- ID không có `@Min/@Max/@Positive`; `0` hoặc số âm chưa có quy tắc nghiệp vụ rõ ràng.
- Medical record chỉ bắt buộc patientId và diagnosis; giới hạn độ dài/cấu trúc cho diagnosis và các trường mô tả chưa định nghĩa.
- Prescription duration không có min/max; chưa rõ duration bằng 0/âm, đơn vị thời hạn, giới hạn tối đa và ý nghĩa khi không gửi.
- Prescription service có nhánh appointmentId null nhưng DTO khai báo `@NotNull`; luồng thường sẽ bị validation chặn trước service.
- Quy tắc bác sĩ chỉ truy cập hồ sơ/đơn thuốc của bệnh nhân được phân công chưa được xác nhận; controller nhận doctorId từ query string.
- Tính nhất quán giữa patientId, appointmentId và doctorId (cùng một lịch khám) chưa có kiểm tra được xác nhận trong đặc tả.
- Quy tắc duplicate, idempotency, audit log và quyền xóa/sửa bản ghi chưa xác định trong phạm vi API POST.
- Response lỗi từ ID không tồn tại và lỗi JSON cần xác nhận thực tế; không suy luận chỉ từ thông điệp.

## 10. Cách chạy từng file CSV bằng Postman/Newman

Chạy từng API riêng, giới hạn đúng folder của request tương ứng trong collection `HMS`:

```bash
newman run postman/HMS.postman_collection.json -e env.json -d exports/testcases-login.csv --folder "Auth"
newman run postman/HMS.postman_collection.json -e env.json -d exports/testcases-medical-records.csv --folder "Medical Records"
newman run postman/HMS.postman_collection.json -e env.json -d exports/testcases-prescriptions.csv --folder "Prescriptions"
```

Collection cần có các request tương ứng và raw JSON body `{{body}}`; cấu hình `baseUrl` và token trong `env.json`. Newman đọc mỗi dòng CSV như một iteration. Đặt request body ở **Body → raw → JSON** với nội dung `{{body}}`. Các trường `Request` mô tả method/endpoint cho case; collection folder phải trỏ tới endpoint đó. `doctorId` trong URL có thể dùng `{{doctorId}}` (giá trị biến môi trường/collection) hoặc giá trị query cụ thể của case. `username` và `password` là biến Postman có thể dùng trong JSON raw như `{"username":"{{username}}","password":"{{password}}"}`; khi chạy CSV, payload kiểm thử của từng dòng được truyền qua cột `Body` → `{{body}}` để mỗi case có thể chứa cả dữ liệu sai/thiếu/null.

Trong test script của mỗi request, đọc cột kỳ vọng bằng:

```javascript
pm.response.to.have.status(Number(pm.iterationData.get("Kỳ vọng HTTP")));
```

Nếu collection dùng script chung thì thêm assertion trên vào request; tên header phải khớp chính xác dấu cách/Unicode. Case `Chạy được trên Postman: Không` cần tạo seed hoặc điều kiện token trước khi chạy, sau đó cập nhật đúng fixture/token cho iteration tương ứng.
