# Thiết kế kiểm thử EP, BVA và Decision Table cho 2 API Admin

**Dự án:** Hospital Management System  
**Phạm vi kiểm thử:**
1. `DELETE /api/admin/departments/{departmentId}`
2. `PATCH /api/admin/doctors/{doctorId}/availability`

**Phạm vi loại trừ:** Không kiểm thử các API GET.

> Tài liệu được xây dựng theo cấu trúc và quy ước của tài liệu mẫu `kiem-thu-3-api-post.md`, đồng thời đối chiếu source code trong ZIP: `AdminController`, `AdminService`, `DoctorService`, `DoctorAvailabilityRequest`, `Department`, `Doctor` và repository liên quan.

> **Quy ước:** EP dùng `V` (hợp lệ), `X` (không hợp lệ). BVA dùng tag `B1, B2, ...`. Decision Table dùng `C` (Condition), `A` (Action), `R` (Rule). `Y` = có/đúng, `N` = không/sai, `-` = không ảnh hưởng/không xét.

---

# 1. DELETE /api/admin/departments/{departmentId}

## 1.1. Thông tin API và dữ liệu đầu vào

- **Method:** `DELETE`
- **URL:** `/api/admin/departments/{departmentId}`
- **Authorization:** Bearer Token có role `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`).
- **Path Variable:** `departmentId` kiểu `Long`.
- **Request Body:** Không có.

Luồng xử lý theo source:

1. Controller gọi `adminService.deleteDepartment(departmentId)`.
2. Service gọi `getDepartmentById(departmentId)`.
3. Nếu không tìm thấy Department, service ném `ResourceNotFoundException("Department", departmentId)`.
4. Nếu tìm thấy, service gọi `departmentRepository.delete(department)`.
5. Nếu xóa thành công, controller trả HTTP `200 OK` với nội dung `Department deleted successfully`.

**Điểm cần chú ý từ model:** `Department.doctors` được khai báo `@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)`. Vì vậy việc xóa Department có liên quan đến hành vi cascade/orphan removal đối với Doctor. Cần kiểm thử trên database test để xác nhận kết quả thực tế và tránh xóa nhầm dữ liệu liên quan.

Source không khai báo kiểm tra riêng rằng Department phải không có Doctor trước khi xóa.

## 1.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| departmentId (Path) | ID Department tồn tại trong database | V1 | (a) ID không tồn tại<br>(b) Path variable không phải số Long<br>(c) Thiếu path segment / URL sai cấu trúc | X1, X2, X3 |
| Authorization | Bearer Token hợp lệ, role ADMIN | V2 | (a) Không gửi token<br>(b) Token sai hoặc hết hạn<br>(c) Token hợp lệ nhưng role không phải ADMIN | X4, X5, X6 |

**Căn cứ code:**
- Controller gắn `@PreAuthorize("hasRole('ADMIN')")`.
- `departmentId` được nhận dưới dạng `Long`.
- Service tìm Department bằng `departmentRepository.findById(...)`; không tìm thấy thì ném `ResourceNotFoundException`.
- Service không có điều kiện nghiệp vụ kiểm tra Department đang có Doctor hay không trước khi gọi delete.

**Lưu ý:** `departmentId = 0` hoặc số âm không được DTO/controller khai báo là vi phạm `@Min`/`@Positive`. Vì vậy không tự kết luận đây là lỗi biên số học; cần kiểm tra theo kết quả có/không có bản ghi tương ứng trong database.

## 1.3. Bảng phân tích giá trị biên (BVA)

Source chỉ quy định `departmentId` là kiểu `Long`, không khai báo `@Min`, `@Max`, `@Positive` hoặc miền ID hợp lệ cụ thể. Vì vậy không có căn cứ xác định biên số học min/max cho ID.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| departmentId | Không quy định | Không quy định | Không quy định | ID Department đang tồn tại | Không quy định | Không quy định | Không quy định | B1–B7: chưa xác định được biên số học |

**Kiểm tra bổ sung theo EP:**
- ID Department tồn tại.
- ID Department không tồn tại.
- Giá trị path không chuyển đổi được sang Long, ví dụ `abc`.
- Token thiếu/sai/hết hạn hoặc sai role.

Không gọi `0` là `min-` hay `min` vì source không khai báo miền ID bắt đầu từ 1.

## 1.4. Bảng quyết định (Decision Table)

### Conditions / Actions

- C1: Bearer Token hợp lệ và có role ADMIN?
- C2: departmentId chuyển đổi được sang Long?
- C3: Department có tồn tại trong database?
- C4: Department có Doctor liên kết hay không? (Điều kiện quan sát bổ sung để kiểm tra cascade; source không dùng điều kiện này để chặn xóa.)
- A1: Xóa Department và trả HTTP 200.
- A2: Từ chối do xác thực/phân quyền.
- A3: Từ chối do lỗi binding/path variable.
- A4: Báo không tìm thấy Department.
- A5: Ghi nhận/kiểm tra ảnh hưởng cascade đến Doctor liên kết.

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| C1. Token ADMIN hợp lệ | Y | N | Y | Y | Y |
| C2. departmentId là Long hợp lệ về kiểu | Y | - | N | Y | Y |
| C3. Department tồn tại | Y | - | - | N | Y |
| C4. Có Doctor liên kết | N | - | - | - | Y |
| A1. Xóa Department, trả 200 | X | - | - | - | X |
| A2. Từ chối xác thực/phân quyền | - | X | - | - | - |
| A3. Lỗi binding/path variable | - | - | X | - | - |
| A4. Báo không tìm thấy Department | - | - | - | X | - |
| A5. Kiểm tra tác động cascade | - | - | - | - | X |

**Diễn giải Rules:**
- R1: Token ADMIN hợp lệ, ID là Long và Department tồn tại, không có Doctor liên kết → xóa Department.
- R2: Token thiếu/sai/hết hạn hoặc role không phải ADMIN → từ chối.
- R3: departmentId không chuyển đổi được sang Long → lỗi binding trước khi vào service.
- R4: departmentId hợp lệ về kiểu nhưng không có bản ghi → `ResourceNotFoundException`.
- R5: Department có Doctor liên kết → source vẫn gọi `departmentRepository.delete(department)`; cần chạy trên database test để xác nhận cascade/orphan removal và dữ liệu bị ảnh hưởng.

**Lưu ý khi chạy:** Không tự đặt trước status code cho `ResourceNotFoundException` hoặc lỗi ràng buộc database nếu chưa kiểm tra exception handler. Ghi nhận status và response thực tế từ Postman.

---

# 2. PATCH /api/admin/doctors/{doctorId}/availability

## 2.1. Thông tin API và dữ liệu đầu vào

- **Method:** `PATCH`
- **URL:** `/api/admin/doctors/{doctorId}/availability`
- **Authorization:** Bearer Token có role `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`).
- **Path Variable:** `doctorId` kiểu `Long`.
- **Content-Type:** `application/json`
- **Request Body mẫu:**

```json
{
  "available": true
}
```

`DoctorAvailabilityRequest` quy định:
- `available`: kiểu `Boolean`.
- Có annotation `@NotNull(message = "Trạng thái làm việc là bắt buộc")`.
- Không khai báo `@Min`, `@Max` hoặc giới hạn khác vì đây là Boolean.

Luồng xử lý theo source:

1. Controller nhận `doctorId` và request body có `@Valid`.
2. Gọi `doctorService.updateAvailability(doctorId, request.getAvailable())`.
3. Service tìm Doctor bằng `getDoctorById(doctorId)`.
4. Không tìm thấy Doctor thì ném `ResourceNotFoundException("Doctor", doctorId)`.
5. Nếu tìm thấy, service gọi `doctor.setAvailable(available)` rồi `doctorRepository.save(doctor)`.
6. Controller trả HTTP `200 OK` cùng đối tượng Doctor đã cập nhật.

## 2.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ | Tag |
|---|---|---|---|---|
| doctorId (Path) | ID Doctor tồn tại trong database | V1 | (a) ID không tồn tại<br>(b) Path variable không phải số Long<br>(c) URL thiếu doctorId | X1, X2, X3 |
| available (Body) | `true` – bác sĩ sẵn sàng làm việc | V2 | `null`/thiếu trường `available` | X4 |
| available (Body) | `false` – bác sĩ không sẵn sàng làm việc | V3 | Giá trị không phải Boolean, ví dụ chuỗi `"yes"` hoặc số `1` | X5 |
| Authorization | Bearer Token hợp lệ, role ADMIN | V4 | (a) Không gửi token<br>(b) Token sai/hết hạn<br>(c) Token role khác ADMIN | X6, X7, X8 |

**Căn cứ code:**
- `DoctorAvailabilityRequest.available` có kiểu `Boolean` và `@NotNull`.
- Controller dùng `@Valid @RequestBody`.
- Controller yêu cầu role ADMIN.
- Service tìm Doctor trước khi cập nhật; không tìm thấy thì ném `ResourceNotFoundException`.
- Không có ràng buộc nào khác đối với `available`; cả `true` và `false` đều là giá trị hợp lệ.

**Lưu ý:** Trường `available` bị thiếu hoặc gửi `null` phải vi phạm `@NotNull` trong luồng validation thông thường. Giá trị sai kiểu có thể bị từ chối trong bước parse JSON/binding trước khi vào service.

## 2.3. Bảng phân tích giá trị biên (BVA)

`available` là Boolean, chỉ có hai giá trị hợp lệ `true` và `false`; không có miền số học min/max. Có thể kiểm thử đầy đủ hai giá trị Boolean và giá trị null/thiếu theo validation.

`doctorId` là Long nhưng source không khai báo min/max/positive nên không tự đặt biên số học.

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| doctorId | Không quy định | Không quy định | Không quy định | ID Doctor tồn tại | Không quy định | Không quy định | Không quy định | B1–B7: chưa xác định biên số học |
| available | Không áp dụng | `false` | Không áp dụng | `true` hoặc `false` | Không áp dụng | `true` | Không áp dụng | B8–B10: kiểm tra Boolean và null |

**Bộ giá trị kiểm thử cho available:**

| Tag | Giá trị | Kỳ vọng theo validation |
|---|---|---|
| B8 | `null` | Không hợp lệ do `@NotNull` |
| B9 | `false` | Hợp lệ |
| B10 | `true` | Hợp lệ |
| B11 | Thiếu thuộc tính `available` | Không hợp lệ do giá trị sau binding là null |
| B12 | `"yes"` hoặc `1` | Không phải Boolean hợp lệ; kiểm tra lỗi parse/binding |

Các tag B8–B12 là bộ kiểm tra giá trị của Boolean và trường bắt buộc, không phải 7 điểm biên số học min-/max-.

## 2.4. Bảng quyết định (Decision Table)

### Conditions / Actions

- C1: Bearer Token hợp lệ và role ADMIN?
- C2: doctorId chuyển đổi được sang Long?
- C3: Doctor có tồn tại?
- C4: available có khác null?
- C5: available có phải giá trị Boolean hợp lệ?
- A1: Cập nhật available và trả HTTP 200.
- A2: Từ chối do xác thực/phân quyền.
- A3: Từ chối do lỗi binding/path variable hoặc JSON.
- A4: Từ chối validation do available null/thiếu.
- A5: Báo không tìm thấy Doctor.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1. Token ADMIN hợp lệ | Y | N | Y | Y | Y | Y |
| C2. doctorId là Long hợp lệ về kiểu | Y | - | N | Y | Y | Y |
| C3. Doctor tồn tại | Y | - | - | N | Y | Y |
| C4. available khác null | Y | - | - | - | N | Y |
| C5. available là Boolean hợp lệ | Y | - | - | - | - | N |
| A1. Cập nhật và trả 200 | X | - | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | - | - | - |
| A3. Lỗi binding/JSON | - | - | X | - | - | X |
| A4. Validation available | - | - | - | - | X | - |
| A5. Không tìm thấy Doctor | - | - | - | X | - | - |

**Diễn giải Rules:**
- R1: Token ADMIN hợp lệ, doctorId là Long, Doctor tồn tại và available là Boolean `true` hoặc `false` → cập nhật thành công.
- R2: Token thiếu/sai/hết hạn hoặc role không phải ADMIN → từ chối.
- R3: doctorId không chuyển đổi được sang Long → lỗi binding.
- R4: doctorId đúng kiểu nhưng Doctor không tồn tại → `ResourceNotFoundException`.
- R5: available bị thiếu hoặc null → validation thất bại do `@NotNull`.
- R6: available gửi sai kiểu JSON → lỗi parse/binding, không cập nhật Doctor.

**Hai trường hợp cập nhật quan trọng cần có trong CSV:**
- Đổi `available` từ `true` sang `false`.
- Đổi `available` từ `false` sang `true`.

Cần xác nhận dữ liệu Doctor sau request để đảm bảo trạng thái đã được lưu, không chỉ dựa vào status HTTP.

---

# 3. Decision Table cho điều kiện tạo đơn thuốc

> Đây là Decision Table bổ sung theo yêu cầu công việc. API tạo đơn thuốc không nằm trong hai API phụ trách chính của tài liệu này. Bảng dưới đây được đối chiếu theo `PrescriptionRequest`, `DoctorController` và `PrescriptionService` trong source.

## 3.1. Conditions / Actions

- C1: Token hợp lệ và role DOCTOR?
- C2: Doctor tồn tại?
- C3: Patient tồn tại?
- C4: Appointment tồn tại?
- C5: patientId và appointmentId không null?
- C6: medication, dosage và frequency đều không blank?
- A1: Tạo Prescription.
- A2: Từ chối xác thực/phân quyền.
- A3: Từ chối validation dữ liệu.
- A4: Báo không tìm thấy tài nguyên.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1. Token DOCTOR hợp lệ | Y | N | Y | Y | Y | Y |
| C2. Doctor tồn tại | Y | - | N | Y | Y | Y |
| C3. Patient tồn tại | Y | - | - | N | Y | Y |
| C4. Appointment tồn tại | Y | - | - | - | N | Y |
| C5. Các ID bắt buộc khác null | Y | - | Y | Y | Y | Y |
| C6. medication/dosage/frequency hợp lệ | Y | - | Y | Y | Y | N |
| A1. Tạo Prescription | X | - | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | - | - | - |
| A3. Từ chối validation | - | - | - | - | - | X |
| A4. Báo không tìm thấy tài nguyên | - | - | X | X | X | - |

**Diễn giải:** Chỉ tạo đơn thuốc khi quyền truy cập hợp lệ, các đối tượng Doctor/Patient/Appointment tồn tại, các ID bắt buộc có giá trị và các trường medication, dosage, frequency không blank.

**Lưu ý theo source:** `PrescriptionRequest` khai báo `@NotNull` cho `patientId`, `appointmentId`; `@NotBlank` cho `medication`, `dosage`, `frequency`. `duration` không có `@Min`/`@Max`; `instructions` là tùy chọn.

---

# 4. Tổng kết phạm vi EP, BVA và Decision Table

| API / Nội dung | EP – Phân vùng tương đương | BVA – Giá trị biên | Decision Table |
|---|---|---|---|
| DELETE /api/admin/departments/{departmentId} | departmentId tồn tại/không tồn tại/sai kiểu; token ADMIN hợp lệ/không hợp lệ | Chưa có min/max số học cho Long ID | Quyền ADMIN, binding ID, Department tồn tại, kiểm tra cascade |
| PATCH /api/admin/doctors/{doctorId}/availability | doctorId tồn tại/không tồn tại/sai kiểu; available true/false/null/sai kiểu; token ADMIN | Boolean có hai giá trị hợp lệ true/false; null/thiếu là không hợp lệ | Quyền ADMIN, Doctor tồn tại, available hợp lệ |
| Tạo đơn thuốc (bảng bổ sung) | Các ID bắt buộc, trường medication/dosage/frequency, quyền DOCTOR | Theo các annotation `@NotNull`, `@NotBlank`; duration không có min/max | Quyền, Doctor, Patient, Appointment, trường bắt buộc |

---

# 5. Danh sách test case cần đưa vào CSV

Các test case dưới đây là danh sách thiết kế ban đầu. Khi chạy Postman, cập nhật Actual Status, Actual Result và Pass/Fail theo kết quả thực tế.

## 5.1. DELETE Department

| TC ID | Nhóm | Dữ liệu / Điều kiện | Kết quả mong đợi |
|---|---|---|---|
| DEL_DEP_01 | EP hợp lệ | Token ADMIN, departmentId tồn tại, không có Doctor liên kết | Xóa thành công, HTTP 200 và thông báo thành công |
| DEL_DEP_02 | EP không hợp lệ | Token ADMIN, departmentId không tồn tại | Không xóa; báo không tìm thấy Department |
| DEL_DEP_03 | EP sai kiểu | departmentId = `abc` | Request bị từ chối do lỗi chuyển kiểu Path Variable |
| DEL_DEP_04 | Authorization | Không gửi token | Bị từ chối truy cập |
| DEL_DEP_05 | Authorization | Token hết hạn hoặc sai | Bị từ chối xác thực |
| DEL_DEP_06 | Authorization | Token hợp lệ nhưng role không phải ADMIN | Bị từ chối phân quyền |
| DEL_DEP_07 | Quan hệ dữ liệu | Department đang có Doctor liên kết | Kiểm tra hành vi cascade/orphan removal và dữ liệu liên quan trên DB test |

## 5.2. PATCH Doctor Availability

| TC ID | Nhóm | Dữ liệu / Điều kiện | Kết quả mong đợi |
|---|---|---|---|
| PATCH_AVA_01 | EP hợp lệ | Token ADMIN, doctorId tồn tại, `available: true` | Cập nhật thành công, response Doctor có available = true |
| PATCH_AVA_02 | EP hợp lệ | Token ADMIN, doctorId tồn tại, `available: false` | Cập nhật thành công, response Doctor có available = false |
| PATCH_AVA_03 | EP không hợp lệ | doctorId không tồn tại, available = true | Không cập nhật; báo không tìm thấy Doctor |
| PATCH_AVA_04 | EP sai kiểu ID | doctorId = `abc` | Request bị từ chối do lỗi chuyển kiểu |
| PATCH_AVA_05 | Validation | Body `{}` | Validation thất bại vì thiếu available |
| PATCH_AVA_06 | Validation | Body `{"available": null}` | Validation thất bại do `@NotNull` |
| PATCH_AVA_07 | Binding | Body `{"available": "yes"}` | Bị từ chối do giá trị không phải Boolean |
| PATCH_AVA_08 | Binding | Body `{"available": 1}` | Bị từ chối nếu JSON binder không chuyển số thành Boolean |
| PATCH_AVA_09 | Authorization | Không gửi token | Bị từ chối truy cập |
| PATCH_AVA_10 | Authorization | Token hợp lệ nhưng role không phải ADMIN | Bị từ chối phân quyền |
| PATCH_AVA_11 | Authorization | Token sai hoặc hết hạn | Bị từ chối xác thực |

---

# 6. Hướng dẫn CSV và Postman Collection Runner

## 6.1. Cấu trúc cột CSV đề xuất

Dùng các cột sau để thống nhất giữa bảng thiết kế và dữ liệu chạy Postman:

| Cột | Ý nghĩa |
|---|---|
| TC_ID | Mã test case |
| API | Tên API |
| Method | DELETE/PATCH |
| Endpoint | URL hoặc URL có biến |
| Test_Group | EP/BVA/DecisionTable/Authorization |
| Input_Data | Dữ liệu đầu vào |
| Expected_Result | Kết quả mong đợi |
| Expected_Status | HTTP status mong đợi nếu đã xác định |
| Actual_Status | HTTP status thực tế |
| Actual_Result | Nội dung response thực tế |
| Test_Result | PASS/FAIL |
| Notes | Ghi chú hoặc lỗi phát hiện |

Không tự điền status code cho các exception chưa xác nhận từ exception handler. Sau lần chạy đầu, căn cứ response thực tế của ứng dụng để hoàn thiện `Expected_Status`.

## 6.2. Biến Postman đề xuất

- `baseUrl`: địa chỉ backend đang chạy.
- `adminToken`: Bearer Token của tài khoản ADMIN.
- `doctorToken`: Token của tài khoản không có role ADMIN để kiểm tra phân quyền.
- `departmentId`: ID Department test.
- `departmentIdNotFound`: ID Department không tồn tại.
- `doctorId`: ID Doctor test.
- `doctorIdNotFound`: ID Doctor không tồn tại.

## 6.3. Request mẫu

### DELETE Department

```http
DELETE {{baseUrl}}/api/admin/departments/{{departmentId}}
Authorization: Bearer {{adminToken}}
```

Không gửi request body.

### PATCH Doctor Availability

```http
PATCH {{baseUrl}}/api/admin/doctors/{{doctorId}}/availability
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

Body:

```json
{
  "available": false
}
```

## 6.4. Script kiểm tra cơ bản trong Postman

Có thể đặt script sau trong tab **Tests** của request PATCH để xác nhận response thành công và trạng thái trả về:

```javascript
pm.test("Response status is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Doctor availability matches input", function () {
    const response = pm.response.json();
    const expected = pm.iterationData.get("available");

    if (expected !== undefined && response.available !== undefined) {
        pm.expect(response.available).to.eql(
            expected === true || expected === "true"
        );
    }
});
```

Với request DELETE:

```javascript
pm.test("Delete request returns success", function () {
    pm.response.to.have.status(200);
});

pm.test("Response confirms department deletion", function () {
    pm.expect(pm.response.text()).to.include(
        "Department deleted successfully"
    );
});
```

Khi chạy CSV, bảo đảm các cột trong CSV trùng với tên biến được đọc trong script và request URL.

## 6.5. Xuất kết quả JSON

Trong Collection Runner:
1. Chọn Collection chứa hai request.
2. Chọn file CSV làm Data file.
3. Chạy collection.
4. Kiểm tra tổng số iteration, passed và failed.
5. Export kết quả run dưới dạng JSON.
6. Lưu JSON kết quả vào thư mục evidence/report của project.

Tên file gợi ý: `admin-department-availability-run.json`.

---

# 7. Tổ chức tài liệu và ảnh minh chứng trên GitHub

Cấu trúc thư mục gợi ý:

```text
docs/
└── api-testing/
    └── admin-department-availability/
        ├── admin-department-availability-ep-bva-decision-table.md
        ├── test-cases.csv
        ├── postman/
        │   └── admin-department-availability.postman_collection.json
        └── evidence/
            ├── 01-delete-department-success.png
            ├── 02-delete-department-not-found.png
            ├── 03-delete-department-unauthorized.png
            ├── 04-patch-availability-true.png
            ├── 05-patch-availability-false.png
            ├── 06-patch-availability-validation.png
            ├── 07-patch-availability-forbidden.png
            └── admin-department-availability-run.json
```

**Ảnh minh chứng nên thể hiện:**
- Request URL, method, Params/Body và Authorization (che token).
- Status code và response body.
- Kết quả Collection Runner.
- Kết quả export JSON.
- Với PATCH, nên có ảnh thể hiện cả hai trạng thái `true` và `false`.

**Không đưa token thật, mật khẩu hoặc thông tin nhạy cảm vào ảnh/CSV/JSON commit lên GitHub.**

---

# 8. Tiêu chí nghiệm thu

| STT | Tiêu chí | Minh chứng |
|---|---|---|
| 1 | Có bảng EP cho DELETE Department | Mục 1.2 |
| 2 | Có bảng BVA cho DELETE Department | Mục 1.3 |
| 3 | Có bảng EP cho PATCH Availability | Mục 2.2 |
| 4 | Có bảng BVA cho PATCH Availability | Mục 2.3 |
| 5 | Có Decision Table cho hai API phụ trách | Mục 1.4 và 2.4 |
| 6 | Có Decision Table điều kiện tạo đơn thuốc theo yêu cầu bổ sung | Mục 3 |
| 7 | CSV khớp với các test case đã thiết kế | File `test-cases.csv` |
| 8 | Postman Collection chạy được với CSV | Collection và kết quả Runner |
| 9 | Có file JSON kết quả thực thi | File JSON export từ Runner |
| 10 | Có ảnh minh chứng và tài liệu trên GitHub | Thư mục `evidence/` và commit/push |

---

# 9. Những điểm cần xác nhận khi chạy Postman

1. **DELETE Department:** chạy với Department không có Doctor liên kết trước; sau đó kiểm tra riêng Department có Doctor liên kết trên database test để xác nhận cascade/orphan removal.
2. **DELETE Department:** xác nhận status thực tế khi Department không tồn tại và khi ID sai định dạng.
3. **PATCH Availability:** chạy cả `true` và `false`, sau đó kiểm tra response và dữ liệu đã lưu.
4. **PATCH Availability:** chạy body thiếu `available`, null và sai kiểu.
5. **Authorization:** kiểm tra token ADMIN, token role khác ADMIN và request không có token.
6. Không đánh dấu test case PASS chỉ dựa trên kết quả mong đợi; phải chạy request và ghi Actual Status/Actual Result.
7. Không sử dụng dữ liệu thật có thể ảnh hưởng hệ thống đang dùng; ưu tiên database test và tài khoản test.

