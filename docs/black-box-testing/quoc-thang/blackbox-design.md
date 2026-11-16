# Thiết kế kiểm thử Black-box — HMS

> **Trạng thái:** Thiết kế cho hai API `POST /api/auth/register` và `POST /api/admin/departments`, được trình bày theo cùng các mục của tài liệu mẫu. Tài liệu `whitebox-design.docx` đã cung cấp mô tả `AuthService.login`, `MedicalRecordService.createMedicalRecord` và `PrescriptionService.createPrescription`; nội dung đó không nêu request schema hay controller/service của hai endpoint trong phạm vi hiện tại. Trong workspace hiện chưa có SRS v1.2, OpenAPI hoặc DTO/controller/service của register và departments. Vì vậy bảng dưới đây là bộ thiết kế có điều kiện: không tự đặt tên trường, độ dài, role, mã HTTP hay response. Các phần đánh dấu **[CẦN ĐỐI CHIẾU]** phải được thay bằng dữ liệu thật trước khi phát hành CSV và chạy Postman. Không có kết quả Pass/Fail nào được giả định.

## 1. Phạm vi, quy ước và dữ liệu kiểm thử

### 1.1 Phạm vi API

| API | Endpoint | Xác thực | Thành công dự kiến |
|---|---|---|---|
| Register | `POST /api/auth/register` | **[CẦN ĐỐI CHIẾU]** Không suy ra public chỉ từ tên endpoint | Status và response theo SRS/OpenAPI; hiện chưa xác nhận |
| Departments | `POST /api/admin/departments` | **[CẦN ĐỐI CHIẾU]** Path có `/admin/`, nhưng quyền phải lấy từ Spring Security/controller | Status và response theo SRS/OpenAPI; hiện chưa xác nhận |

Hai API này thay thế phạm vi ba API cũ. Những dữ liệu, fixture và số case của login, medical records, prescriptions không còn được dùng trong thiết kế này.

### 1.2 Quy ước

- **EP**: phân vùng tương đương; `V` là lớp hợp lệ, `X` là lớp không hợp lệ.
- **BVA**: phân tích giá trị biên; chỉ áp dụng giới hạn được quy định trong SRS, OpenAPI, annotation validation hoặc service.
- **DT**: bảng quyết định; `Y/N/–` lần lượt là có/không/không xét.
- Mỗi case EP không hợp lệ chỉ chứa **một điều kiện không hợp lệ**; mọi trường và fixture còn lại phải hợp lệ.
- Mỗi lớp lỗi riêng trong hợp đồng (ví dụ thiếu, `null`, rỗng, chỉ khoảng trắng) được tính thành một case nếu mục tiêu là phủ từng lớp đó.
- Sau khi lập danh sách factor, số case EP tối thiểu là:

  **TC_min_EP = max(số lớp hợp lệ của từng factor) + tổng số lớp không hợp lệ của mọi factor.**

  Công thức giả định các lớp hợp lệ có thể ghép trong cùng request. Nếu có điều kiện không thể đồng thời thỏa mãn, phải thêm case và ghi rõ lý do.
- Một case BVA/DT được tính là đã phủ nếu nó trùng đúng điều kiện và kết quả cần chứng minh của case EP. Chỉ cộng case BVA/DT chưa được EP đại diện.
- Không coi mã HTTP trong tài liệu này là đã xác nhận. Lỗi validation, binding, authentication, authorization, duplicate và media type cần so với hợp đồng API và response thực tế.

### 1.3 Fixture cần chuẩn bị

| Fixture | Dùng cho | Điều kiện chuẩn bị |
|---|---|---|
| `REG-NEW` | Đăng ký hợp lệ | Body hợp lệ theo schema; mọi trường unique dùng giá trị chưa tồn tại |
| `REG-DUP-{key}` | Đăng ký duplicate | Tạo sẵn tài khoản có cùng giá trị ở đúng trường được SRS quy định unique; các trường khác hợp lệ |
| `REG-ROLE` | Role/privilege khi đăng ký | Chỉ dùng nếu request có trường role/quyền hoặc SRS quy định chống tự gán quyền; chuẩn bị role được phép và role không được phép |
| `ADMIN-AUTH` | Tạo department hợp lệ | Token của actor thực sự được phép theo security config; body hợp lệ, key unique |
| `NONADMIN-AUTH` | Kiểm thử RBAC department | Token hợp lệ của actor đã xác thực nhưng không có quyền tạo department |
| `DEP-DUP-{key}` | Department duplicate | Tạo sẵn department có cùng giá trị ở đúng trường unique; các trường khác hợp lệ |
| `DEP-REF-{state}` | Trường tham chiếu của department | Chỉ dùng nếu request có reference ID; chuẩn bị ID hợp lệ, ID không tồn tại, và quan hệ không hợp lệ nếu SRS phân biệt |

Giá trị seed/ID cụ thể chưa có trong tài liệu nguồn hiện tại. Khi chạy, lấy fixture từ DB test và không giả định ID tuần tự. Tạo giá trị unique riêng cho mỗi iteration hoặc reset dữ liệu để các case không làm ảnh hưởng lẫn nhau.

### 1.4 Hợp đồng request cần điền từ SRS/DTO

| API | Thông tin bắt buộc phải ghi trước khi chốt case |
|---|---|
| Register | Tên và kiểu từng trường; bắt buộc/tùy chọn; validation; định danh unique; chính sách password; role/trạng thái mặc định; client có được gửi role không; xác thực; response; status thành công/lỗi |
| Departments | Tên và kiểu từng trường; bắt buộc/tùy chọn; validation/độ dài; mã/tên unique; trường reference; role được phép; quy tắc duplicate; response; status thành công/lỗi |
| Cả hai | Content-Type; cách xử lý body rỗng, JSON malformed, JSON root sai kiểu, trường thừa; schema lỗi; giới hạn payload nếu có |

## 2. EP — Bảng phân vùng tương đương

Các placeholder `{field}` bên dưới có nghĩa là **từng field thật trong DTO**, không phải tên trường được suy đoán. Sau khi có DTO, thay từng placeholder bằng tên trường và đánh mã case cụ thể.

### Register — EP

| Trường/điều kiện | Lớp hợp lệ | Lớp không hợp lệ / mã lớp | Case đại diện cần tạo |
|---|---|---|---|
| Mỗi trường bắt buộc `{field}` | V1: đúng kiểu và thỏa toàn bộ rule của field | X1: thiếu; X2: `null`; X3: sai kiểu; nếu là string `@NotBlank`, X4: rỗng, X5: chỉ khoảng trắng; thêm lớp format/range nếu SRS có quy định | Một case âm cho mỗi X, chỉ thay đổi `{field}` |
| Mỗi trường tùy chọn `{field}` | V1: không gửi; V2: giá trị hợp lệ | X1: sai kiểu/format/range nếu có rule; `null` là lớp riêng nếu hợp đồng phân biệt | Một case cho mỗi lớp X được DTO/SRS quy định |
| Mỗi định danh unique (nếu có) | V1: giá trị chưa được dùng | X1: trùng giá trị đã có | Một case cho từng key unique độc lập, ví dụ không gộp hai key trùng trong cùng request |
| Password/credential (nếu có) | V1: đáp ứng chính sách được đặc tả | X1…Xn: từng vi phạm policy được SRS phân biệt; thiếu/null/blank chỉ tính nếu là lớp riêng | Một case cho mỗi rule riêng; không tự đặt min length hay độ phức tạp |
| Role/quyền gửi từ client (nếu có) | V1: role mặc định/role được phép theo hợp đồng | X1: role đặc quyền không được phép hoặc giá trị role không hợp lệ | Kiểm tra response và dữ liệu lưu; bỏ mục này nếu DTO không có trường tương ứng |
| Request body | V1: JSON object đúng schema | X1: body rỗng; X2: JSON malformed; X3: root `null`; X4: root array/scalar | Một case cho từng lỗi parser/binding cần phân biệt trong hợp đồng |
| Content-Type | V1: media type được hỗ trợ | X1: media type không hỗ trợ | Một case riêng; status cần xác nhận |
| Xác thực | V1: actor đúng policy | X1: thiếu/sai token hoặc role nếu endpoint được bảo vệ | Chỉ đưa vào suite bắt buộc sau khi xác nhận security config |

**TC_min_EP Register:** chưa thể ghi số cụ thể khi chưa biết số field và constraint. Gọi `F_REG` là các field thật; `Xᵢ` là số lớp invalid của field/factor i; `Vᵢ` là số lớp valid. Khi có schema:

**TC_min_EP_REG = max(Vᵢ) + ΣXᵢ**, bao gồm các field, uniqueness, security và các lớp body/Content-Type độc lập được yêu cầu.

Không tính một rule giả định là case bắt buộc. Mỗi lớp X được giữ riêng chỉ khi SRS/DTO hoặc yêu cầu kiểm thử đòi phủ riêng lớp đó.

### Departments — EP

| Trường/điều kiện | Lớp hợp lệ | Lớp không hợp lệ / mã lớp | Case đại diện cần tạo |
|---|---|---|---|
| Mỗi trường bắt buộc `{field}` | V1: đúng kiểu và thỏa rule field | X1: thiếu; X2: `null`; X3: sai kiểu; nếu string `@NotBlank`, X4: rỗng, X5: chỉ khoảng trắng; thêm format/range theo DTO | Một case âm cho mỗi X; các field còn lại hợp lệ |
| Mỗi trường tùy chọn `{field}` | V1: không gửi; V2: giá trị hợp lệ | X1: sai kiểu/format/range nếu có constraint | Một case riêng cho mỗi lớp invalid được quy định |
| Mỗi key unique (nếu có) | V1: chưa tồn tại | X1: duplicate đúng key | Một case cho từng key unique; chuẩn bị `DEP-DUP-{key}` |
| Mỗi ID tham chiếu (nếu có) | V1: ID đúng kiểu, tồn tại và được phép dùng | X1: thiếu/null nếu bắt buộc; X2: sai kiểu; X3: không tồn tại; X4: tồn tại nhưng không hợp lệ về quan hệ nếu SRS quy định | Một case cho từng lớp và từng quan hệ độc lập |
| Role/authorization | V1: role được phép theo controller/security config | X1: anonymous/token sai; X2: token hợp lệ nhưng role không được phép | Tách xác thực và phân quyền nếu contract cho expected khác nhau |
| Request body | V1: JSON object đúng schema | X1: body rỗng; X2: JSON malformed; X3: root `null`; X4: root array/scalar | Một case cho từng lớp lỗi được yêu cầu phân biệt |
| Content-Type | V1: media type được hỗ trợ | X1: media type không hỗ trợ | Một case riêng; xác nhận status thực tế |

**TC_min_EP Departments:** chưa thể ghi số cụ thể khi chưa có DTO/SRS và ma trận quyền. Gọi `F_DEP` là tập field/factor thật:

**TC_min_EP_DEP = max(Vᵢ) + ΣXᵢ**, bao gồm field, duplicate, reference, RBAC và body/content type khi chúng là yêu cầu độc lập.

Không mặc định endpoint chỉ cho ADMIN chỉ vì path chứa `/admin/`; quyền phải được lấy từ implementation/SRS.

### 2.1 Số test case tối thiểu để phủ 100% lớp tương đương

Quy tắc tính: một test hợp lệ có thể đại diện đồng thời các lớp hợp lệ nếu chúng kết hợp được trong cùng request; mỗi lớp không hợp lệ được kiểm tra bằng một case riêng, chỉ sai một điều kiện. Vì vậy:

**TC_min_EP = max(Vᵢ) + ΣXᵢ**

Trong đó `Vᵢ` là số lớp hợp lệ của factor i; `Xᵢ` là số lớp không hợp lệ riêng biệt của factor i. Các lớp body/Content-Type/RBAC/uniqueness chỉ được cộng nếu là điều kiện được đặc tả và chưa được đại diện bằng factor khác.

#### Register

| Factor trong request | Lớp hợp lệ | Số lớp hợp lệ | Lớp không hợp lệ theo EP hiện tại | Số lớp không hợp lệ |
|---|---|---:|---|---:|
| Mỗi trường bắt buộc là string `@NotBlank` | Một giá trị hợp lệ | 1 | Thiếu, `null`, sai kiểu, rỗng, chỉ khoảng trắng | 5 mỗi trường |
| Mỗi trường bắt buộc chỉ `@NotNull` | Một giá trị hợp lệ | 1 | Thiếu, `null`, sai kiểu; rỗng chỉ tính nếu có rule | 3 mỗi trường |
| Mỗi trường tùy chọn | Không gửi; gửi giá trị hợp lệ | 2 | Sai kiểu/format/range theo constraint | `xᵢ` theo DTO |
| Mỗi key unique | Giá trị chưa tồn tại | 1 | Giá trị trùng | 1 mỗi key unique |
| Password/credential | Giá trị hợp lệ theo policy | `vᵢ` theo policy | Từng lớp vi phạm policy độc lập | `xᵢ` theo SRS |
| Quyền truy cập | Actor được phép | Theo policy | Actor không xác thực / role không được phép (nếu phân biệt) | 1 hoặc 2 theo policy |
| Request body | JSON object hợp lệ | 1 | Body rỗng; malformed JSON; root null; root array/scalar | 4 theo các lớp đang mô hình hóa |
| Content-Type | Media type hỗ trợ | 1 | Media type không hỗ trợ | 1 |

Đặt `nNB` là số trường bắt buộc `@NotBlank`, `nNN` là số trường bắt buộc chỉ `@NotNull`, `xOPT` là tổng lớp lỗi của trường tùy chọn, `xPOLICY` là tổng lớp vi phạm policy, `xUNIQUE` là số key unique, `xAUTH` là số lớp truy cập không hợp lệ được phân biệt, `xBODY` là số lớp body cần phủ, và `xCT` là số lớp Content-Type cần phủ. Khi các loại trường và policy đã được xác nhận:

```text
TC_min_REG = max(Vᵢ) + 5×nNB + 3×nNN + xOPT + xPOLICY
             + xUNIQUE + xAUTH + xBODY + xCT
```

Với mô hình body hiện tại, `xBODY=4` nếu hợp đồng yêu cầu tách cả bốn lớp; `xCT=1` nếu cần kiểm thử media type không hỗ trợ; `xAUTH=0` nếu register public, hoặc 1–2 nếu chính sách bảo vệ endpoint phân biệt lớp. **Chưa thể thay các biến bằng số cuối cùng** khi chưa có danh sách field và validation của SRS/DTO. Ví dụ, `5×nNB` là phép đếm có điều kiện cho mỗi string `@NotBlank`, không khẳng định API hiện có trường nào như vậy.

#### Departments

| Factor trong request | Lớp hợp lệ | Số lớp hợp lệ | Lớp không hợp lệ theo EP hiện tại | Số lớp không hợp lệ |
|---|---|---:|---|---:|
| Mỗi trường bắt buộc là string `@NotBlank` | Một giá trị hợp lệ | 1 | Thiếu, `null`, sai kiểu, rỗng, chỉ khoảng trắng | 5 mỗi trường |
| Mỗi trường bắt buộc chỉ `@NotNull` | Một giá trị hợp lệ | 1 | Thiếu, `null`, sai kiểu; rỗng chỉ tính nếu có rule | 3 mỗi trường |
| Mỗi trường tùy chọn | Không gửi; gửi giá trị hợp lệ | 2 | Sai kiểu/format/range theo constraint | `xᵢ` theo DTO |
| Mỗi key unique (nếu có) | Giá trị chưa tồn tại | 1 | Giá trị trùng | 1 mỗi key unique |
| Mỗi ID tham chiếu (nếu có) | ID đúng kiểu, tồn tại và được phép dùng | 1 | Thiếu/null nếu bắt buộc; sai kiểu; không tồn tại; quan hệ không hợp lệ nếu được phân biệt | 2–4 mỗi ID, theo DTO/SRS |
| Quyền truy cập | Actor có quyền | 1 | Chưa xác thực; token sai/hết hạn; role đã xác thực nhưng không đủ quyền | 1–3 theo security contract |
| Request body | JSON object hợp lệ | 1 | Body rỗng; malformed JSON; root null; root array/scalar | 4 theo các lớp đang mô hình hóa |
| Content-Type | Media type hỗ trợ | 1 | Media type không hỗ trợ | 1 |

Đặt `nNB`, `nNN`, `xOPT`, `xUNIQUE`, `xREF`, `xRBAC`, `xBODY`, `xCT` tương tự trên, với `xREF` là tổng số lớp invalid của các reference ID. Khi có schema:

```text
TC_min_DEP = max(Vᵢ) + 5×nNB + 3×nNN + xOPT + xUNIQUE
             + xREF + xRBAC + xBODY + xCT
```

Với mô hình hiện tại, `xBODY=4`, `xCT=1` chỉ khi các lớp này được yêu cầu tách. `xRBAC` có thể là 0 nếu không áp dụng hoặc 1–3 theo policy; `xREF`/`xUNIQUE` bằng 0 nếu endpoint không có các điều kiện đó. **Số test case cuối cùng vẫn TBD**, không được lấy tổng các cột có điều kiện như yêu cầu đã xác nhận.

#### Bảng kết quả tối thiểu hiện tại

| API | `max(Vᵢ)` | Tổng lớp không hợp lệ `ΣXᵢ` | Công thức | TC tối thiểu để phủ 100% EP |
|---|---:|---:|---|---:|
| Register | TBD theo DTO | `5×nNB + 3×nNN + xOPT + xPOLICY + xUNIQUE + xAUTH + xBODY + xCT` | `max(Vᵢ) + ΣXᵢ` | **TBD — thiếu SRS/DTO** |
| Departments | TBD theo DTO | `5×nNB + 3×nNN + xOPT + xUNIQUE + xREF + xRBAC + xBODY + xCT` | `max(Vᵢ) + ΣXᵢ` | **TBD — thiếu SRS/DTO** |
| **Tổng** | — | — | `TC_min_REG + TC_min_DEP` | **TBD** |

Đây là mục tính tối thiểu theo phương pháp phủ EP; không phải số case đã chạy. Khi có SRS/DTO, thay từng biến bằng số đếm thực, dẫn nguồn constraint, sau đó ghi ra số nguyên cuối cùng và map từng lớp X sang `TC_ID`. Nếu các biểu diễn sai kiểu/body đã bị framework gộp và SRS không yêu cầu phân biệt, cập nhật lại số lớp thay vì giữ nguyên công thức minh họa.

### 2.2 Bảng đếm lớp EP theo field để hoàn tất sau khi có DTO

| API | Field/factor thật | Lớp hợp lệ V | Lớp không hợp lệ X | Số case X | Căn cứ SRS/DTO |
|---|---|---|---:|---:|---|
| Register | `{điền từ DTO}` | `{liệt kê}` | `{liệt kê}` | TBD | `{mục SRS / class / annotation}` |
| Register | `{mỗi unique/security/body factor}` | `{liệt kê}` | `{liệt kê}` | TBD | `{căn cứ}` |
| Departments | `{điền từ DTO}` | `{liệt kê}` | `{liệt kê}` | TBD | `{mục SRS / class / annotation}` |
| Departments | `{unique/reference/RBAC/body factor}` | `{liệt kê}` | `{liệt kê}` | TBD | `{căn cứ}` |

## 3. BVA — Bảng giá trị biên

| API / trường | Biên cần lấy từ nguồn | Giá trị cần thử | Giới hạn chưa được đặc tả |
|---|---|---|---|
| Register — string có `@NotBlank` | Ranh giới blank/không blank | `""`, chuỗi whitespace, một ký tự không trắng, giá trị hợp lệ thông thường | Không suy ra max length nếu DTO/SRS không có `@Size`/giới hạn DB |
| Register — string có min/max length | `minLength`, `maxLength` theo SRS/DTO | Với mỗi biên hữu hạn: `min−1`, `min`, `min+1`, `max−1`, `max`, `max+1` (bỏ giá trị không biểu diễn được) | Chưa có số biên nếu thiếu constraint |
| Register — password/credential | Policy thực tế | Giá trị ngay dưới, tại và trên từng ngưỡng; tổ hợp thiếu class nếu policy quy định | Không tự đặt policy |
| Register — định danh unique | Trạng thái “chưa tồn tại/đã tồn tại” | Giá trị mới và giá trị duplicate | Đây là biên trạng thái EP/DT, không phải biên độ dài |
| Departments — string có `@NotBlank` | Ranh giới blank/không blank | `""`, whitespace, một ký tự không trắng, giá trị hợp lệ thông thường | Không tự đặt max length |
| Departments — field có min/max length | Min/max trong SRS/DTO | `min−1`, `min`, `min+1`, `max−1`, `max`, `max+1` | Chưa xác định cho đến khi biết constraint |
| Departments — numeric/ID | Ranh giới số học nếu có `@Min/@Max/@Positive` | Giá trị ngay dưới/tại/trên min và max; ID tồn tại/không tồn tại theo rule | Không suy `min=1` chỉ từ `@NotNull` |
| Departments — reference | Bản ghi tham chiếu hợp lệ/không hợp lệ | ID tồn tại, ID không tồn tại; quan hệ sai nếu SRS có quy định | Không có trường reference thì không áp dụng |
| Cả hai — body size | Giới hạn request nếu có | Ngay dưới/tại/trên max payload nếu server/SRS quy định | Payload lớn không được gọi là BVA khi không có max được đặc tả |

### 3.1 Số test case tối thiểu cho BVA

Quy tắc tính: mỗi giá trị biên được xác định cần một test input; giá trị đã có trong EP không cộng lại nếu nó chứng minh đúng cùng điều kiện.

| API | Miền có biên đã xác định | Số giá trị biên hiện biết | Đã phủ EP | BVA bổ sung |
|---|---|---:|---:|---:|
| Register | Chưa có SRS/DTO nên chưa xác định field/min/max | Chưa xác định | Chưa xác định | **TBD** |
| Departments | Chưa có SRS/DTO nên chưa xác định field/min/max | Chưa xác định | Chưa xác định | **TBD** |
| **Tổng** | — | — | — | **TBD** |

Công thức sau khi điền: **BVA_add = số giá trị biên được yêu cầu − số giá trị đã được case EP đại diện chính xác**. Không thể ghi `0` là kết luận phủ BVA khi chưa biết constraint.

## 4. Bảng quyết định (Decision Table)

Các luật bên dưới mô tả tổ hợp điều kiện ở mức endpoint. Khi có SRS/OpenAPI, thay expected outcome bằng status và schema lỗi chính thức. Nếu một luật không áp dụng (ví dụ không có unique key/reference), bỏ luật và ghi lý do.

### 4.1 Register

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| Body đúng schema và qua validation | Y | N | Y | Y | Y |
| Định danh unique chưa được sử dụng (nếu có) | Y | – | N | Y | Y |
| Actor được phép gọi endpoint theo policy | Y | Y | Y | N | Y |
| Client gửi role/privilege bị cấm (nếu field tồn tại) | N | – | – | – | Y |
| A1: tạo một tài khoản mới, trả response hợp đồng | X | – | – | – | – |
| A2: từ chối validation, không tạo tài khoản | – | X | – | – | – |
| A3: từ chối duplicate, không tạo tài khoản thứ hai | – | – | X | – | – |
| A4: từ chối authentication/authorization | – | – | – | X | – |
| A5: bỏ qua/từ chối role không được phép; không cấp privilege | – | – | – | – | X |

- R1: success với định danh mới và request hợp lệ.
- R2: chỉ một field/constraint sai; không trộn lỗi validation.
- R3: duplicate của một key unique; các trường khác hợp lệ.
- R4: chỉ dùng nếu policy endpoint có giới hạn actor.
- R5: chỉ dùng nếu payload cho phép gửi role/quyền; nếu không có trường này thì bỏ.

**Số luật:** tối đa 5 trong bảng; số luật bắt buộc bằng các điều kiện thực có theo SRS. **TC tối thiểu nếu kiểm thử DT độc lập:** số luật áp dụng. **Case DT tăng thêm khi gộp EP:** số luật áp dụng chưa có case EP tương ứng; điền sau khi chốt schema.

### 4.2 Departments

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| Actor được phép tạo department | Y | N | Y | Y | Y | Y |
| Request qua validation | Y | – | N | Y | Y | Y |
| Key unique chưa tồn tại (nếu có) | Y | – | Y | N | Y | Y |
| ID tham chiếu hợp lệ (nếu có) | Y | – | Y | Y | N | Y |
| Body không cố gán quyền/field bị cấm (nếu có) | Y | – | Y | Y | Y | N |
| A1: tạo department, trả resource theo hợp đồng | X | – | – | – | – | – |
| A2: từ chối authentication/authorization; không tạo | – | X | – | – | – | – |
| A3: từ chối validation; không tạo | – | – | X | – | – | – |
| A4: từ chối duplicate; không tạo bản ghi thứ hai | – | – | – | X | – | – |
| A5: từ chối reference không hợp lệ | – | – | – | – | X | – |
| A6: từ chối/loại bỏ field quyền bị cấm | – | – | – | – | – | X |

- R1: actor có quyền, body hợp lệ, key mới và reference hợp lệ nếu có.
- R2: tách anonymous/token không hợp lệ khỏi token hợp lệ nhưng sai role nếu contract trả kết quả khác nhau.
- R3: đúng một constraint payload không hợp lệ.
- R4/R5/R6 chỉ bắt buộc nếu SRS/DTO có uniqueness/reference/field quyền tương ứng.

**Số luật:** tối đa 6; số luật bắt buộc chỉ xác định sau khi đối chiếu SRS và security config. **TC tối thiểu nếu kiểm thử DT độc lập:** số luật áp dụng. **Case DT tăng thêm khi gộp EP:** các luật áp dụng nhưng chưa có EP case tương ứng.

### 4.3 Số test case tối thiểu cho bảng quyết định

| API | Số luật trong khung | Số luật có thể áp dụng | TC tối thiểu khi DT độc lập | TC tăng thêm khi gộp EP |
|---|---:|---:|---:|---:|
| Register | 5 | TBD theo field/policy thực tế | TBD | TBD |
| Departments | 6 | TBD theo field/policy thực tế | TBD | TBD |

Không lấy số luật khung làm số luật bắt buộc. Khi loại các điều kiện không có trong SRS, đánh số lại các luật còn lại và ánh xạ từng luật vào Case ID.

## 5. Ma trận quyền (RBAC)

| Actor / tình huống | Register | Departments | Kỳ vọng cần xác nhận |
|---|---|---|---|
| Chưa xác thực | Được gọi nếu endpoint public; nếu không thì bị chặn | Thử gọi không token | Lấy policy từ security config; không suy ra chỉ từ URL |
| Actor đã xác thực, role được phép | Chỉ áp dụng nếu register yêu cầu auth | Được gọi nếu role này được cấp quyền | Thành công khi body hợp lệ |
| Actor đã xác thực, role không được phép | Theo policy register | Không được phép nếu policy hạn chế | Expected status/error theo security config |
| Token sai/hết hạn | Theo policy endpoint | Từ chối nếu endpoint cần auth | Tách lỗi authentication khỏi sai role nếu contract phân biệt |
| Body cố gán role/privilege | Chỉ nếu request có field tương ứng | Chỉ nếu request có field tương ứng | Không được cấp quyền ngoài policy; assert cả response và DB |
| Actor không có quyền nhưng body sai | Thiết kế để không che mất nhánh cần kiểm tra | Dùng payload hợp lệ khi test RBAC | Với ưu tiên xử lý giữa auth và validation, ghi đúng kết quả thực tế/contract |

Role cụ thể (ADMIN, STAFF hoặc role khác), quyền của endpoint và status chưa thể chốt nếu chưa đọc `@PreAuthorize`, cấu hình filter chain và SRS.

## 6. Ma trận phủ kỹ thuật

| Nhóm kiểm thử | Register | Departments |
|---|---|---|
| EP hợp lệ | Body đủ trường hợp lệ; định danh mới | Body đủ trường hợp hợp lệ; actor có quyền; key mới; reference hợp lệ nếu có |
| EP không hợp lệ | Thiếu/null/blank/sai kiểu/format theo từng field thực tế | Thiếu/null/blank/sai kiểu/format theo từng field thực tế |
| Duplicate | Mỗi định danh được SRS xác định là unique | Mỗi mã/tên được SRS xác định là unique |
| BVA | Ranh giới string/password/number theo constraints thực tế | Ranh giới field, ID và length theo constraints thực tế |
| Decision table | Thành công, validation, duplicate, access, role giả mạo nếu có | Thành công, auth/RBAC, validation, duplicate, reference, field quyền nếu có |
| JSON/binding | Body rỗng, JSON lỗi, root sai kiểu, field sai kiểu | Body rỗng, JSON lỗi, root sai kiểu, field sai kiểu |
| Content-Type | Media type hợp lệ/không hỗ trợ theo contract | Media type hợp lệ/không hỗ trợ theo contract |
| Không phát sinh side effect | Invalid/duplicate/unauthorized không tạo tài khoản ngoài ý muốn | Invalid/duplicate/unauthorized không tạo department ngoài ý muốn |
| Bảo mật response | Không lộ password/secret; role persisted đúng policy | Không lộ dữ liệu nhạy cảm; role/owner không bị mass assignment nếu có |

## 7. Tổng hợp test suite

### 7.1 File dự kiến

| File | Dải TC_ID dự kiến | Số case | Nội dung |
|---|---|---:|---|
| `testcases-register.csv` | `TC-REG-01..N` | Chưa chốt | EP, BVA, DT, body/content type và RBAC/security theo schema thực tế |
| `testcases-departments.csv` | `TC-DEP-01..M` | Chưa chốt | EP, BVA, DT, duplicate/reference và RBAC theo schema thực tế |
| **Tổng** | — | **Chưa chốt** | Sẽ tính sau khi lập bảng field/factor tại §2.3 |

CSV sẽ dùng cùng cấu trúc 10 cột, UTF-8 BOM, dấu phẩy và quote kép toàn bộ trường theo quy ước của bộ test ban đầu. Cột ghi chú phân biệt case gửi trực tiếp được với case cần fixture đặc biệt. CSV chưa được tạo trong bước này vì chưa có schema request để điền body chính xác.

### 7.2 Bảng tổng hợp mức tối thiểu

| API | EP tối thiểu | BVA bổ sung | Decision table nếu chạy riêng | Tổng tối thiểu sau gộp | CSV hiện có | Kết luận |
|---|---:|---:|---:|---:|---:|---|
| Register | TBD | TBD | TBD | **TBD** | Chưa tạo | Chưa thể kết luận đủ trước khi có SRS/DTO |
| Departments | TBD | TBD | TBD | **TBD** | Chưa tạo | Chưa thể kết luận đủ trước khi có SRS/DTO |
| **Tổng** | TBD | TBD | TBD | **TBD** | — | Chưa thể chốt |

Sau khi có schema, tổng gộp được tính như sau: **TC_total_min = TC_min_EP + BVA_add chưa trùng EP + DT_add chưa trùng EP/BVA**. Mỗi số phải truy ngược được về lớp hoặc luật và Case ID; không dùng một con số tổng không có bảng phân rã.

## 8. Cách thực thi và tiêu chí ghi nhận

1. Đối chiếu SRS v1.2, OpenAPI, request/response DTO, controller, service, security config và exception handler; điền bảng field/factor và expected status.
2. Chuẩn bị DB test và fixture theo §1.3; xác nhận base URL, actor/role, key unique và reference ID thực tế.
3. Chạy riêng từng file qua đúng folder/request Postman; mỗi CSV row là một iteration, body được truyền từ cột `Body` hoặc biến tương đương theo collection.
4. Request hợp lệ: assert status theo contract, schema response, dữ liệu tạo đúng một lần và giá trị lưu đúng; Register phải kiểm tra role/trạng thái theo policy và không lộ secret trái hợp đồng.
5. Request bị từ chối: assert status/error code theo contract, xác minh không tạo resource ngoài ý muốn và fixture không bị thay đổi ngoài quy tắc.
6. Với duplicate, xác minh dữ liệu ban đầu còn nguyên và không phát sinh bản ghi thứ hai. Với RBAC, dùng body hợp lệ để tập trung kiểm tra quyền.
7. Lưu cho từng case: status thực tế, response body đã loại secret/PII, thời điểm, môi trường/build, fixture, Pass/Fail/Blocked và defect ID. Không sửa expected sau khi thấy kết quả nếu chưa có căn cứ đặc tả.

Các status `400/401/403/409/415` chỉ là những dạng thường gặp, không được xem là expected cho API này cho đến khi xác nhận trên hợp đồng hoặc môi trường test.

## 9. Chỗ đặc tả chưa chốt

- Request schema của cả hai endpoint chưa có trong workspace: tên field, kiểu, bắt buộc/tùy chọn và validation chưa thể xác định.
- Chưa biết register có public không, có tạo role mặc định nào, client có thể gửi role hay không, và trường nào dùng làm định danh unique.
- Chưa biết policy password, format email/username/định danh hoặc giới hạn độ dài.
- Chưa biết departments có mã/tên unique, trạng thái, trưởng khoa hoặc field tham chiếu nào; quy tắc duplicate/reference chưa xác nhận.
- Chưa xác nhận role nào được gọi `/api/admin/departments`; không suy luận RBAC chỉ từ path.
- Chưa có status thành công, error status và schema response/error cho từng endpoint.
- Chưa xác nhận hành vi JSON malformed, binding sai kiểu, body rỗng và Content-Type không hỗ trợ.
- Chưa có dữ liệu seed, collection/environment Postman và môi trường chạy nên chưa thể ghi Pass/Fail hoặc defect.

## 10. Cách chạy bằng Postman/Newman

Sau khi collection và environment đã có request đúng schema, tạo mỗi request trong folder riêng. Thay `<collection.json>`, `<env.json>` và tên folder bằng đúng file trong dự án:

```bash
newman run <collection.json> -e <env.json> -d exports/testcases-register.csv --folder "Register"
newman run <collection.json> -e <env.json> -d exports/testcases-departments.csv --folder "Departments"
```

Trong Postman, cấu hình `Body → raw → JSON` để đọc body từ biến/cột CSV theo collection. Test script có thể assert status bằng tên cột expected sau khi chốt header CSV:

```javascript
pm.response.to.have.status(Number(pm.iterationData.get("Expected HTTP")));
```

Không chạy CSV cuối cùng khi cột body và expected vẫn còn placeholder. Case cần token, duplicate hoặc reference đặc biệt phải có fixture trước iteration; không đánh dấu Pass nếu fixture chưa được chuẩn bị.
