# Thiết kế kiểm thử Black-box — KCPM (3 API Admin)

Phạm vi gồm ba API quản trị: xóa khoa, cập nhật trạng thái làm việc của bác sĩ và tạo hồ sơ bác sĩ. Thiết kế được xây dựng theo bố cục của tài liệu mẫu `blackbox-design(1).md`, đồng thời đối chiếu với controller, DTO, service, entity và cấu hình bảo mật trong source code `KCPM.zip`.

Đây là **kỳ vọng thiết kế** dựa trên source code. Cần chạy trên môi trường test để xác nhận status code và response thực tế, đặc biệt với các trường hợp binding sai kiểu, dữ liệu liên kết và lỗi cơ sở dữ liệu.

## 1. Phạm vi, quy ước và dữ liệu kiểm thử

### 1.1 Danh sách API

| API | Endpoint | Xác thực | Thành công dự kiến |
|---|---|---|---|
| Xóa khoa | `DELETE /api/admin/departments/{departmentId}` | Bearer token role `ADMIN` | `200`, body `"Department deleted successfully"` |
| Cập nhật trạng thái bác sĩ | `PATCH /api/admin/doctors/{doctorId}/availability` | Bearer token role `ADMIN` | `200`, trả về đối tượng Doctor đã cập nhật |
| Tạo hồ sơ bác sĩ | `POST /api/admin/doctors?departmentId={id}` | Bearer token role `ADMIN` | `200`, trả về đối tượng Doctor vừa tạo |

EP = phân vùng tương đương; BVA = giá trị biên; DT = bảng quyết định; RBAC = phân quyền theo vai trò. `V` là lớp hợp lệ, `X` là lớp không hợp lệ; `Y/N/-` trong bảng quyết định lần lượt là có/không/không xét.

### 1.2 Căn cứ từ source code

| Thành phần | Quy tắc đọc được từ source |
|---|---|
| `AdminController` | Cả ba endpoint đều có `@PreAuthorize("hasRole('ADMIN')")` |
| `SecurityConfig` | `/api/admin/**` yêu cầu role `ADMIN`; ứng dụng dùng JWT và session stateless |
| `DoctorAvailabilityRequest` | `available` có `@NotNull`; không có `@Min`, `@Max` hoặc ràng buộc khác |
| `CreateDoctorRequest` | `userId` có `@NotNull`; `specialization` có `@NotBlank`; `qualification`, `experience` không có annotation validation |
| `AdminService.deleteDepartment` | Tìm khoa theo ID; không tìm thấy thì ném `ResourceNotFoundException`; tìm thấy thì gọi `departmentRepository.delete(department)` |
| `AdminService.createDoctorWithDepartment` | Tìm User; User phải có role `DOCTOR`; không được có hồ sơ Doctor trước đó; tìm Department; tạo Doctor với `available=true`, `consultationFee=0.0` |
| `DoctorService.updateAvailability` | Tìm Doctor theo ID; không tìm thấy thì ném `ResourceNotFoundException`; cập nhật `available` và lưu |

### 1.3 Dữ liệu kiểm thử cần chuẩn bị

| Dữ liệu | Mục đích |
|---|---|
| Token ADMIN hợp lệ | Gọi cả ba API |
| Token DOCTOR hợp lệ | Kiểm tra RBAC |
| Token PATIENT hợp lệ | Kiểm tra RBAC |
| Department đang tồn tại và chưa có Doctor | Kiểm thử xóa khoa an toàn |
| Department đang có Doctor | Kiểm tra ảnh hưởng xóa dây chuyền |
| Department ID không tồn tại | Kiểm thử 404 |
| User role DOCTOR chưa có hồ sơ Doctor | Kiểm thử tạo hồ sơ thành công |
| User role DOCTOR đã có hồ sơ Doctor | Kiểm thử trùng hồ sơ |
| User role PATIENT hoặc ADMIN | Kiểm thử sai role khi tạo Doctor |
| Doctor đang tồn tại | Kiểm thử cập nhật availability |
| Doctor ID không tồn tại | Kiểm thử 404 |

Không mặc định ID bằng `1` luôn tồn tại. Cần lấy ID thực tế từ database hoặc API GET tương ứng trước khi chạy. Các test case có thể làm thay đổi hoặc xóa dữ liệu phải dùng fixture riêng, tránh ảnh hưởng những case chạy sau.

## 2. EP — Bảng phân vùng tương đương

| API | Trường / điều kiện | Lớp hợp lệ | Lớp không hợp lệ / mã |
|---|---|---|---|
| DELETE departments | `departmentId` | ID khoa tồn tại (V1) | ID không tồn tại (X1); ID bằng 0/âm (X2, nếu không có bản ghi); sai kiểu path (X3); thiếu path segment (X4) |
| DELETE departments | Authorization | Bearer token hợp lệ, role ADMIN (V2) | Không có token (X5); token sai/hết hạn (X6); role DOCTOR/PATIENT (X7) |
| DELETE departments | Khoa có Doctor liên kết | Khoa tồn tại (V3) | Entity Department có quan hệ `@OneToMany(cascade = ALL, orphanRemoval = true)` với Doctor; cần kiểm tra xóa khoa có kéo theo hồ sơ bác sĩ hay không (X8 – rủi ro dữ liệu) |
| PATCH availability | `doctorId` | ID Doctor tồn tại (V1) | ID không tồn tại (X1); ID bằng 0/âm (X2, nếu không có bản ghi); sai kiểu path (X3) |
| PATCH availability | `available` | Boolean `true` (V2), Boolean `false` (V3) | Thiếu (X4), `null` (X5), chuỗi/number/object thay Boolean (X6) |
| PATCH availability | Authorization | Bearer token hợp lệ, role ADMIN (V4) | Không có token (X7); token sai/hết hạn (X8); role DOCTOR/PATIENT (X9) |
| POST doctors | `departmentId` query | ID Department tồn tại (V1) | Thiếu query (X1); sai kiểu Long (X2); ID không tồn tại (X3) |
| POST doctors | `userId` | User tồn tại, role DOCTOR, chưa có Doctor profile (V2) | Thiếu/null (X4); User không tồn tại (X5); User role PATIENT/ADMIN (X6); User đã có Doctor profile (X7) |
| POST doctors | `specialization` | Chuỗi có ít nhất một ký tự không trắng (V3) | Rỗng (X8); chỉ khoảng trắng (X9); thiếu (X10); null (X11) |
| POST doctors | `qualification` | Chuỗi có nội dung hoặc không gửi/null (V4) | DTO không khai báo validation cho trường này |
| POST doctors | `experience` | Integer hoặc không gửi/null (V5) | Sai kiểu dữ liệu (X12); số âm/giá trị lớn chưa có quy tắc min/max trong DTO |
| POST doctors | Authorization | Bearer token hợp lệ, role ADMIN (V6) | Không có token (X13); token sai/hết hạn (X14); role DOCTOR/PATIENT (X15) |

**Lưu ý:** `departmentId` của API POST là `@RequestParam Long` nhưng không có `@NotNull`/`@Min` trong controller. `userId` và `specialization` là hai trường có validation rõ ràng trong DTO. Không tự đặt giới hạn cho `experience` hoặc độ dài chuỗi vì source hiện tại không quy định.

### 2.1 Số test case tối thiểu để phủ lớp tương đương

Quy tắc thiết kế: một case hợp lệ có thể đại diện lớp hợp lệ của nhiều trường; mỗi case không hợp lệ chỉ thay đổi một điều kiện không hợp lệ, các điều kiện còn lại giữ hợp lệ. Những case cần dữ liệu đặc biệt (ví dụ user đã có Doctor profile) phải chuẩn bị fixture riêng.

| API | Nhóm lớp không hợp lệ cần kiểm tra | Số case không hợp lệ dự kiến | Case hợp lệ tối thiểu | Tổng EP tối thiểu dự kiến |
|---|---|---:|---:|---:|
| DELETE departments | ID không tồn tại, ID 0/âm, path sai kiểu/thiếu, thiếu token, token sai/hết hạn, role sai, rủi ro cascade | 8 | 1 | **9** |
| PATCH availability | ID không tồn tại, ID 0/âm, path sai kiểu, body thiếu/null/sai kiểu, thiếu token, token sai/hết hạn, role sai | 9 | 2 (`true`, `false`) | **11** |
| POST doctors | Thiếu/sai/không tồn tại departmentId, userId thiếu/null/không tồn tại/sai role/trùng profile, specialization rỗng/blank/thiếu/null, experience sai kiểu, auth | 14 | 1 | **15** |

Các con số trên là số case thiết kế tối thiểu theo các lớp đã liệt kê, không phải số lượng test case bắt buộc duy nhất. Có thể tăng số lượng để kiểm tra response body, dữ liệu liên kết và tính toàn vẹn dữ liệu.

## 3. BVA — Bảng giá trị biên

| API / trường | Biên xác định được từ source | Giá trị cần thử | Giới hạn chưa được đặc tả |
|---|---|---|---|
| DELETE `departmentId` | Kiểu `Long`, không có `@Min/@Max/@Positive` | ID tồn tại; `0`; `-1`; ID rất lớn trong miền Long; chuỗi không phải số | Không có miền ID hợp lệ được khai báo; ID tồn tại là điều kiện tra cứu DB |
| PATCH `doctorId` | Kiểu `Long`, không có `@Min/@Max/@Positive` | ID tồn tại; `0`; `-1`; ID rất lớn; chuỗi không phải số | Không có ràng buộc số học trong DTO/controller |
| PATCH `available` | Boolean bắt buộc `@NotNull` | `true`, `false`, `null`, bỏ field | Boolean chỉ có hai giá trị hợp lệ; không có biên số |
| POST `departmentId` | Query parameter kiểu `Long` | ID tồn tại; `0`; `-1`; thiếu; chuỗi không phải số | Không có `@Min/@Max`; kiểm tra tồn tại trong service |
| POST `userId` | `@NotNull`, kiểu `Long` | `null`, bỏ field, ID tồn tại, ID không tồn tại, `0`, `-1` | `@NotNull` không đồng nghĩa với `@Positive`; không có giới hạn số học |
| POST `specialization` | `@NotBlank` | `""`, `"   "`, `"A"`, `"AB"`, chuỗi thông thường | Không có giới hạn độ dài tối đa |
| POST `experience` | `Integer`, không có validation min/max | Bỏ field, `null`, `-1`, `0`, `1`, số dương lớn, vượt miền Integer, sai kiểu | Chưa có quy tắc nghiệp vụ về số năm kinh nghiệm tối thiểu/tối đa |
| POST `qualification` | Chuỗi tùy chọn, không có validation | Bỏ field, `null`, `""`, chuỗi có nội dung, chuỗi dài | Không có min/max hoặc quy tắc nội dung |

### 3.1 Số test case BVA bổ sung

Vì phần lớn trường ID không có giới hạn số học và `experience` không có min/max trong DTO, không thể kết luận một cặp min/max nghiệp vụ từ source. BVA tập trung vào ranh giới `@NotNull`, `@NotBlank` và Boolean.

| API | Miền có biên xác định | Giá trị biên | Case BVA cần bảo đảm |
|---|---|---|---|
| DELETE departments | Không có ràng buộc min/max; ID là Long | ID hợp lệ, 0, -1, ID lớn | Các giá trị này được bao phủ trong EP/kiểm tra kiểu; không đặt min/max giả định |
| PATCH availability | Boolean bắt buộc | `true`, `false`, `null` | 3 giá trị; `true` và `false` là hai lớp hợp lệ, null là không hợp lệ |
| POST doctors | `userId @NotNull` | ID có giá trị, null/thiếu | Bao phủ trong EP |
| POST doctors | `specialization @NotBlank` | 0 ký tự không trắng; 1 ký tự không trắng | `""`, `"   "`, `"A"` |
| POST doctors | `experience` Integer tùy chọn | -1, 0, 1, số lớn | Kiểm tra thăm dò; không xem -1/0 là sai nếu chưa có quy tắc nghiệp vụ |

## 4. Bảng quyết định (Decision Table)

### 4.1 DELETE `/api/admin/departments/{departmentId}`

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| C1 Token hợp lệ và role ADMIN | Y | N | Y | Y | Y |
| C2 Department tồn tại | Y | - | N | Y | Y |
| C3 Department có Doctor liên kết | N | - | - | Y | N |
| A1 Xóa thành công, HTTP 200 | X | - | - | X | X |
| A2 Từ chối xác thực/phân quyền | - | X | - | - | - |
| A3 Không tìm thấy, HTTP 404 | - | - | X | - | - |
| A4 Xóa khoa có liên kết và kiểm tra cascade | - | - | - | X | - |

R1 là trường hợp xóa khoa tồn tại, không có Doctor liên kết. R4 là trường hợp khoa có Doctor liên kết: source entity khai báo cascade `ALL` và `orphanRemoval=true`, vì vậy phải xác minh dữ liệu Doctor liên quan có bị xóa theo hay không. Không coi đây là hành vi mong muốn trước khi xác nhận yêu cầu nghiệp vụ.

### 4.2 PATCH `/api/admin/doctors/{doctorId}/availability`

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1 Token hợp lệ và role ADMIN | Y | N | Y | Y | Y | Y |
| C2 Doctor tồn tại | Y | - | N | Y | Y | Y |
| C3 `available=true` | Y | - | - | N | - | - |
| C4 `available=false` | N | - | - | Y | - | - |
| C5 `available` null/thiếu/sai kiểu | N | - | - | N | Y | Y |
| A1 Cập nhật thành công, HTTP 200 | X | - | - | X | - | - |
| A2 Từ chối xác thực/phân quyền | - | X | - | - | - | - |
| A3 Không tìm thấy Doctor, HTTP 404 | - | - | X | - | - | - |
| A4 Validation/binding error, HTTP 400 dự kiến | - | - | - | - | X | X |

R1 và R4 kiểm tra cả hai trạng thái Boolean. R5/R6 đại diện cho body không thỏa `@NotNull` hoặc không bind được về Boolean.

### 4.3 POST `/api/admin/doctors?departmentId={id}`

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 | R7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| C1 Token hợp lệ và role ADMIN | Y | N | Y | Y | Y | Y | Y |
| C2 Department tồn tại | Y | - | N | Y | Y | Y | Y |
| C3 User tồn tại | Y | - | - | N | Y | Y | Y |
| C4 User có role DOCTOR | Y | - | - | - | N | Y | Y |
| C5 User chưa có Doctor profile | Y | - | - | - | - | N | Y |
| C6 `userId` khác null | Y | - | - | - | Y | Y | Y |
| C7 `specialization` NotBlank | Y | - | - | - | Y | Y | N |
| A1 Tạo Doctor, HTTP 200 | X | - | - | - | - | - | - |
| A2 Từ chối xác thực/phân quyền | - | X | - | - | - | - | - |
| A3 Không tìm thấy Department/User, HTTP 404 | - | - | X | X | - | - | - |
| A4 User không phải DOCTOR, HTTP 400 | - | - | - | - | X | - | - |
| A5 User đã có Doctor profile, HTTP 409 | - | - | - | - | - | X | - |
| A6 Validation, HTTP 400 | - | - | - | - | - | - | X |

Bảng trên thể hiện các nhánh nghiệp vụ chính. Trường hợp thiếu/null `userId`, thiếu/null/blank `specialization`, thiếu/sai kiểu `departmentId` cần có case riêng trong EP để kiểm tra validation và binding.

### 4.4 Số test case tối thiểu cho bảng quyết định

| API | Số luật chính | TC tối thiểu nếu kiểm thử độc lập | TC phát sinh thêm khi gộp với EP |
|---|---:|---:|---:|
| DELETE departments | 4 luật có ý nghĩa (R1–R4; R5 là đối chứng không có liên kết) | 5 | 0 nếu đã có case tương ứng |
| PATCH availability | 5 luật có ý nghĩa | 6 | 0 nếu đã có case tương ứng |
| POST doctors | 6 luật nghiệp vụ chính | 7 | 0 nếu đã có case tương ứng |

Bảng quyết định dùng để chứng minh các nhánh chính đã được xét. Không cộng trùng case nếu cùng một test case đã bao phủ điều kiện EP tương ứng.

## 5. Ma trận quyền (RBAC)

| Actor / tình huống | DELETE departments | PATCH availability | POST doctors | Kết quả dự kiến |
|---|---:|---:|---:|---|
| Chưa xác thực | Không được phép | Không được phép | Không được phép | `401` |
| ADMIN có token hợp lệ | Được phép | Được phép | Được phép | Tiếp tục kiểm tra dữ liệu nghiệp vụ |
| DOCTOR có token hợp lệ | Không được phép | Không được phép | Không được phép | `403` |
| PATIENT có token hợp lệ | Không được phép | Không được phép | Không được phép | `403` |
| Token sai/hết hạn | Không được phép | Không được phép | Không được phép | `401` dự kiến |

Căn cứ: `SecurityConfig` áp dụng `.requestMatchers("/api/admin/**").hasRole("ADMIN")`; cả ba method trong `AdminController` cũng có `@PreAuthorize("hasRole('ADMIN')")`. Cần xác nhận status thực tế trên môi trường chạy.

## 6. Ma trận phủ kỹ thuật

| Nhóm kiểm thử | DELETE departments | PATCH availability | POST doctors |
|---|---|---|---|
| EP hợp lệ/không hợp lệ | ID tồn tại/không tồn tại; token và role | Doctor ID; Boolean true/false/null/thiếu/sai kiểu; token và role | departmentId, userId, role User, trùng Doctor profile, specialization |
| BVA có căn cứ | ID 0/âm/ID lớn; không có min/max khai báo | Boolean true/false/null; ID 0/âm | userId null/thiếu; specialization 0/1 ký tự không trắng; experience -1/0/1 (thăm dò) |
| Decision table | Xóa thành công, không tìm thấy, auth, khoa có Doctor liên kết | true/false, doctor không tồn tại, auth, validation | Tạo thành công, department/user không tồn tại, sai role, trùng profile, validation |
| RBAC | Anonymous/ADMIN/DOCTOR/PATIENT | Anonymous/ADMIN/DOCTOR/PATIENT | Anonymous/ADMIN/DOCTOR/PATIENT |
| Kiểu dữ liệu / JSON | Path ID sai kiểu | Path ID sai kiểu; body malformed; available sai kiểu | Query ID sai kiểu; userId/experience sai kiểu; body malformed |
| Tính toàn vẹn dữ liệu | Kiểm tra khoa và Doctor sau khi xóa | Kiểm tra giá trị available sau PATCH | Kiểm tra Doctor mới, liên kết User/Department, giá trị mặc định |

## 7. Danh sách test case đề xuất

Các mã dưới đây là danh sách khung để triển khai thành CSV/Postman. Kỳ vọng HTTP là dự kiến theo source; trường hợp binding sai kiểu hoặc ràng buộc database phải chạy thực tế để chốt.

### 7.1 DELETE `/api/admin/departments/{departmentId}`

| TC_ID | Trường hợp kiểm thử | Dữ liệu / điều kiện | Kỳ vọng HTTP | Kỳ vọng nội dung |
|---|---|---|---:|---|
| DEL-DEP-01 | Xóa khoa tồn tại, không có Doctor liên kết | ADMIN; ID khoa hợp lệ | 200 | `"Department deleted successfully"` |
| DEL-DEP-02 | Xóa khoa không tồn tại | ADMIN; ID chưa có trong DB | 404 | Error response báo không tìm thấy Department |
| DEL-DEP-03 | ID bằng 0 | ADMIN; `departmentId=0` | 404 dự kiến | Không tìm thấy Department |
| DEL-DEP-04 | ID âm | ADMIN; `departmentId=-1` | 404 dự kiến | Không tìm thấy Department |
| DEL-DEP-05 | Path ID sai kiểu | ADMIN; `/departments/abc` | 400 dự kiến | Lỗi chuyển đổi path variable |
| DEL-DEP-06 | Không truyền token | Không Authorization | 401 dự kiến | Từ chối xác thực |
| DEL-DEP-07 | Token DOCTOR | Bearer DOCTOR_TOKEN | 403 dự kiến | Từ chối phân quyền |
| DEL-DEP-08 | Token PATIENT | Bearer PATIENT_TOKEN | 403 dự kiến | Từ chối phân quyền |
| DEL-DEP-09 | Khoa có Doctor liên kết | ADMIN; khoa có ít nhất một Doctor | 200 hoặc lỗi DB tùy cascade thực tế | Kiểm tra khoa và các Doctor liên quan sau thao tác; cần xác nhận yêu cầu nghiệp vụ |

### 7.2 PATCH `/api/admin/doctors/{doctorId}/availability`

| TC_ID | Trường hợp kiểm thử | Dữ liệu / điều kiện | Kỳ vọng HTTP | Kỳ vọng nội dung |
|---|---|---|---:|---|
| PATCH-AVL-01 | Chuyển trạng thái sang available | ADMIN; Doctor tồn tại; `{"available":true}` | 200 | Doctor trả về có `available=true` |
| PATCH-AVL-02 | Chuyển trạng thái sang unavailable | ADMIN; Doctor tồn tại; `{"available":false}` | 200 | Doctor trả về có `available=false` |
| PATCH-AVL-03 | Doctor không tồn tại | ADMIN; ID không có trong DB | 404 | Error response báo không tìm thấy Doctor |
| PATCH-AVL-04 | ID bằng 0 | ADMIN; `doctorId=0` | 404 dự kiến | Không tìm thấy Doctor |
| PATCH-AVL-05 | ID âm | ADMIN; `doctorId=-1` | 404 dự kiến | Không tìm thấy Doctor |
| PATCH-AVL-06 | Thiếu field available | Body `{}` | 400 | Validation failed |
| PATCH-AVL-07 | available null | `{"available":null}` | 400 | Validation failed |
| PATCH-AVL-08 | available là chuỗi | `{"available":"true"}` | 400 dự kiến | Lỗi binding/JSON |
| PATCH-AVL-09 | available là số | `{"available":1}` | 400 dự kiến | Lỗi binding/JSON |
| PATCH-AVL-10 | Body JSON không hợp lệ | Ví dụ `{available:true` | 400 dự kiến | Lỗi parse JSON |
| PATCH-AVL-11 | Không truyền token | Không Authorization | 401 dự kiến | Từ chối xác thực |
| PATCH-AVL-12 | Token DOCTOR | Bearer DOCTOR_TOKEN | 403 dự kiến | Từ chối phân quyền |
| PATCH-AVL-13 | Token PATIENT | Bearer PATIENT_TOKEN | 403 dự kiến | Từ chối phân quyền |

### 7.3 POST `/api/admin/doctors?departmentId={id}`

Body hợp lệ mẫu:

```json
{
  "userId": 2,
  "specialization": "Cardiology",
  "qualification": "MD",
  "experience": 5
}
```

`userId=2` chỉ là ví dụ minh họa; phải thay bằng User có role DOCTOR chưa có Doctor profile trong database test.

| TC_ID | Trường hợp kiểm thử | Dữ liệu / điều kiện | Kỳ vọng HTTP | Kỳ vọng nội dung |
|---|---|---|---:|---|
| POST-DOC-01 | Tạo Doctor hợp lệ | ADMIN; department tồn tại; User role DOCTOR chưa có profile | 200 | Doctor được tạo, liên kết đúng User/Department |
| POST-DOC-02 | Không truyền departmentId | Bỏ query parameter | 400 dự kiến | Lỗi thiếu request parameter |
| POST-DOC-03 | departmentId sai kiểu | `departmentId=abc` | 400 dự kiến | Lỗi chuyển đổi kiểu |
| POST-DOC-04 | Department không tồn tại | ADMIN; ID chưa có trong DB | 404 | Error response báo không tìm thấy Department |
| POST-DOC-05 | userId không tồn tại | Body có ID User chưa có | 404 | Error response báo không tìm thấy User |
| POST-DOC-06 | userId null | `{"userId":null,"specialization":"Cardiology"}` | 400 | Validation failed |
| POST-DOC-07 | Thiếu userId | Bỏ field `userId` | 400 | Validation failed |
| POST-DOC-08 | User không có role DOCTOR | Chọn User role PATIENT | 400 | Thông báo tài khoản không có vai trò bác sĩ |
| POST-DOC-09 | User role ADMIN | Chọn User role ADMIN | 400 | Thông báo tài khoản không có vai trò bác sĩ |
| POST-DOC-10 | User đã có Doctor profile | Chọn User DOCTOR đã được cấp hồ sơ | 409 | Thông báo tài khoản đã có hồ sơ bác sĩ |
| POST-DOC-11 | specialization rỗng | `specialization=""` | 400 | Validation failed |
| POST-DOC-12 | specialization chỉ có khoảng trắng | `specialization="   "` | 400 | Validation failed |
| POST-DOC-13 | Thiếu specialization | Bỏ field | 400 | Validation failed |
| POST-DOC-14 | specialization null | `specialization=null` | 400 | Validation failed |
| POST-DOC-15 | qualification không gửi | Bỏ field qualification | 200 dự kiến | Tạo thành công nếu các dữ liệu bắt buộc hợp lệ |
| POST-DOC-16 | experience không gửi | Bỏ field experience | 200 dự kiến | Tạo thành công; source không đánh dấu bắt buộc |
| POST-DOC-17 | experience bằng 0 | `experience=0` | 200 dự kiến | Không có validation min trong DTO |
| POST-DOC-18 | experience âm | `experience=-1` | 200 dự kiến | Source chưa khai báo min; ghi nhận để xác nhận quy tắc nghiệp vụ |
| POST-DOC-19 | experience sai kiểu | `experience="five"` | 400 dự kiến | Lỗi binding/JSON |
| POST-DOC-20 | Không truyền token | Không Authorization | 401 dự kiến | Từ chối xác thực |
| POST-DOC-21 | Token DOCTOR | Bearer DOCTOR_TOKEN | 403 dự kiến | Từ chối phân quyền |
| POST-DOC-22 | Token PATIENT | Bearer PATIENT_TOKEN | 403 dự kiến | Từ chối phân quyền |

## 8. Tiêu chí xác minh dữ liệu sau kiểm thử

| API | Sau khi request thành công | Sau khi request thất bại |
|---|---|---|
| DELETE departments | Department không còn tồn tại; kiểm tra riêng các Doctor thuộc khoa để xác định ảnh hưởng cascade | Department vẫn tồn tại nếu request bị từ chối hoặc không tìm thấy |
| PATCH availability | Trường `available` trong DB đổi đúng giá trị gửi lên | Giá trị `available` không bị thay đổi |
| POST doctors | Có Doctor mới; `user_id` và `department_id` đúng; `specialization`, `qualification`, `experience` đúng; `available=true`; `consultationFee=0.0` | Không tạo Doctor mới và không tạo liên kết sai |

## 9. Cách thực thi và tiêu chí ghi nhận

1. Khởi chạy backend và database test; xác nhận `baseUrl`, token ADMIN/DOCTOR/PATIENT và các ID fixture.
2. Tạo dữ liệu test riêng: khoa không có Doctor, khoa có Doctor, Department ID không tồn tại, User DOCTOR chưa có profile, User DOCTOR đã có profile, User PATIENT/ADMIN và Doctor hiện có.
3. Trong Postman, đặt `Authorization: Bearer {{ADMIN_TOKEN}}` và `Content-Type: application/json` cho các request có body.
4. Với DELETE, truyền `departmentId` trên path; với PATCH, truyền `doctorId` trên path và JSON body; với POST, truyền `departmentId` ở query string và JSON body theo DTO.
5. Chạy từng nhóm test bằng Postman Runner/Newman. Các case tạo/xóa dữ liệu nên dùng dữ liệu độc lập hoặc reset DB giữa các lần chạy.
6. So sánh status với kỳ vọng, kiểm tra response body và xác minh database sau mỗi case quan trọng.
7. Lưu request, response, status thực tế, thời điểm, môi trường và ảnh evidence. Nếu kết quả khác kỳ vọng, ghi nhận bug thay vì tự đổi expected result khi chưa có căn cứ.

### 9.1 Cấu hình Postman đề xuất

| Request | Method | URL | Body |
|---|---|---|---|
| Delete Department | DELETE | `{{baseUrl}}/api/admin/departments/{{departmentId}}` | none |
| Update Doctor Availability | PATCH | `{{baseUrl}}/api/admin/doctors/{{doctorId}}/availability` | raw JSON `{{body}}` |
| Create Doctor | POST | `{{baseUrl}}/api/admin/doctors?departmentId={{departmentId}}` | raw JSON `{{body}}` |

Test script chung để so sánh HTTP status khi chạy CSV:

```javascript
const expectedStatus = Number(pm.iterationData.get("ExpectedStatus"));

pm.test("HTTP status matches expected", function () {
    pm.response.to.have.status(expectedStatus);
});
```

Tên cột trong CSV phải khớp chính xác với tên đọc trong script. Với case không thể chạy tự động (cần tạo fixture, kiểm tra cascade hoặc xác minh database), ghi rõ điều kiện chuẩn bị trong cột ghi chú.

## 10. Những điểm cần xác nhận với yêu cầu nghiệp vụ

- **Xóa khoa có Doctor liên kết:** `Department.doctors` khai báo `cascade = CascadeType.ALL` và `orphanRemoval = true`. Cần xác nhận việc xóa khoa có được phép xóa luôn hồ sơ Doctor hay phải chặn xóa khi còn Doctor.
- **Experience:** DTO chỉ dùng `Integer`, không có `@Min` hoặc `@Max`; chưa có căn cứ kết luận số năm kinh nghiệm âm hoặc quá lớn phải bị từ chối.
- **Specialization:** Có `@NotBlank` nhưng không có giới hạn độ dài tối đa.
- **Department ID và Doctor ID:** Không có `@Positive`, `@Min` hoặc `@Max`; ID bằng 0/âm thường sẽ dẫn đến không tìm thấy bản ghi, nhưng cần xác nhận bằng DB thực tế.
- **Qualification:** Không có validation; chưa có quy định độ dài hoặc bắt buộc.
- **Tạo Doctor:** Service đặt mặc định `available=true` và `consultationFee=0.0`; cần xác minh response và DB có đúng giá trị.
- **Response lỗi:** `ResourceNotFoundException` được xử lý thành 404; `ResponseStatusException` giữ status đã chỉ định; validation thành 400. Lỗi binding/path/query và lỗi database cần chạy thực tế để xác nhận.
- **Audit log:** Trong phạm vi ba method được xem, chưa thấy logic ghi audit log riêng. Nếu yêu cầu hệ thống bắt buộc log thao tác thêm/xóa/cập nhật thì cần kiểm thử ở phần khác hoặc xác nhận implementation.

## 11. Tổng kết phạm vi kiểm thử

| API | EP / BVA trọng tâm | Decision Table trọng tâm | RBAC |
|---|---|---|---|
| DELETE departments | ID tồn tại/không tồn tại; ID biên; khoa có Doctor liên kết | Xóa thành công, không tìm thấy, auth, cascade | ADMIN được phép; Anonymous/DOCTOR/PATIENT bị từ chối |
| PATCH availability | Doctor ID; `available=true/false/null/thiếu/sai kiểu` | Hai trạng thái cập nhật, không tìm thấy, auth, validation | ADMIN được phép; Anonymous/DOCTOR/PATIENT bị từ chối |
| POST doctors | departmentId, userId, role User, duplicate profile, specialization, experience | Tạo thành công, Department/User không tồn tại, sai role, trùng profile, validation | ADMIN được phép; Anonymous/DOCTOR/PATIENT bị từ chối |

**Kết luận:** Bộ thiết kế bao phủ ba API Admin theo các lớp tương đương, giá trị biên có căn cứ từ DTO/entity, bảng quyết định, phân quyền và xác minh dữ liệu. Các quy tắc chưa được source code quy định — đặc biệt là cascade khi xóa khoa và miền hợp lệ của `experience` — được đánh dấu để xác nhận, không tự đặt thành tiêu chí bắt buộc.
