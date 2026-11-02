# Phân hoạch lớp tương đương và phân tích giá trị biên

## 1. Phạm vi và quy ước

Tài liệu áp dụng kỹ thuật **Phân hoạch lớp tương đương (EP)** và **Phân tích giá trị biên (BVA)** cho năm API trong collection Postman **Hải Bằng**.

- Phạm vi gồm: `GET /api/doctor/medical-records`, `POST /api/doctor/prescriptions`, `GET /api/doctor/prescriptions`, `GET /api/patient/profile`, `GET /api/patient/departments`.
- Phân hoạch dựa trên tham số controller, DTO `PrescriptionRequest` và xử lý trong service của dự án.
- Giả định request đã có xác thực hợp lệ với vai trò phù hợp (`DOCTOR` hoặc `PATIENT`); không phân hoạch token/quyền truy cập.
- `doctorId`, `userId`, `patientId`, `appointmentId` là ID kiểu `Long`. Mã nguồn không đặt miền min/max nghiệp vụ cho các ID; việc tìm thấy bản ghi phụ thuộc dữ liệu trong database. Vì vậy không gán BVA số học min/max cho ID.
- Tag `V` biểu thị lớp hợp lệ/được API chấp nhận; tag `X` biểu thị lớp đầu vào không hợp lệ hoặc bị nghiệp vụ từ chối. Tag được đánh số riêng trong từng API.

## 2. GET `/api/doctor/medical-records`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `doctorId` (query, bắt buộc) | ID dạng `Long` có hồ sơ y tế do bác sĩ đó tạo: trả về danh sách hồ sơ tìm được. | V1 | Thiếu `doctorId`: request không đáp ứng tham số bắt buộc. | X1 |
| `doctorId` (query, bắt buộc) | ID dạng `Long` không có hồ sơ y tế khớp; service chỉ truy vấn danh sách nên trả về danh sách rỗng. | V2 | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X2 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `doctorId` | Không có miền min/max nghiệp vụ; đây là ID dùng để truy vấn hồ sơ, không phải miền số liên tục. | Không áp dụng BVA số học; trường hợp ID không có hồ sơ được phân hoạch bằng EP. |

## 3. POST `/api/doctor/prescriptions`

### Bảng phân hoạch lớp tương đương

| Đầu vào/điều kiện | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `doctorId` (query, bắt buộc) | ID `Long` của bác sĩ tồn tại. | V1 | Thiếu `doctorId` hoặc giá trị không chuyển đổi được sang `Long`/vượt miền `Long`. | X1 |
| `doctorId` (query, bắt buộc) | — | — | ID `Long` không có bác sĩ tương ứng: service không tìm thấy bác sĩ. | X2 |
| `patientId` (body, bắt buộc) | ID `Long` của bệnh nhân tồn tại. | V2 | Thiếu/null hoặc giá trị body không chuyển đổi được sang `Long`. | X3 |
| `patientId` (body, bắt buộc) | — | — | ID `Long` không có bệnh nhân tương ứng: service không tìm thấy bệnh nhân. | X4 |
| `appointmentId` (body, bắt buộc theo `@NotNull`) | ID `Long` của lịch hẹn tồn tại; đơn thuốc được liên kết với lịch hẹn đó. | V3 | Thiếu/null hoặc giá trị body không chuyển đổi được sang `Long`. | X5 |
| `appointmentId` (body, bắt buộc theo `@NotNull`) | — | — | ID `Long` không có lịch hẹn tương ứng: service không tìm thấy lịch hẹn. | X6 |
| `medication` (body, bắt buộc) | Chuỗi có nội dung, không null/rỗng/chỉ chứa khoảng trắng. | V4 | Null, chuỗi rỗng hoặc chỉ chứa khoảng trắng (`@NotBlank`). | X7 |
| `dosage` (body, bắt buộc) | Chuỗi có nội dung, không null/rỗng/chỉ chứa khoảng trắng. | V5 | Null, chuỗi rỗng hoặc chỉ chứa khoảng trắng (`@NotBlank`). | X8 |
| `frequency` (body, bắt buộc) | Chuỗi có nội dung, không null/rỗng/chỉ chứa khoảng trắng. | V6 | Null, chuỗi rỗng hoặc chỉ chứa khoảng trắng (`@NotBlank`). | X9 |
| `duration` (body, tùy chọn) | Số nguyên `Integer` hoặc null; DTO không khai báo `@Min`/`@Max`. | V7 | Giá trị không chuyển đổi được sang `Integer` hoặc vượt miền biểu diễn `Integer`. | X10 |
| `instructions` (body, tùy chọn) | Có thể bỏ trống, null hoặc truyền chuỗi; DTO không khai báo ràng buộc validation. | V8 | Không có lớp không hợp lệ nghiệp vụ được khai báo cho nội dung trường này. | — |
| Body | JSON đúng cú pháp và có các trường bắt buộc hợp lệ. | V9 | JSON sai cú pháp hoặc body không thể chuyển đổi sang DTO. | X11 |

> `PrescriptionRequest` khai báo `@NotNull` cho `patientId`, `appointmentId`; `@NotBlank` cho `medication`, `dosage`, `frequency`. `duration` và `instructions` không có annotation validation. Service kiểm tra sự tồn tại của bác sĩ, bệnh nhân và lịch hẹn trước khi lưu đơn thuốc. Mã nguồn không khai báo giới hạn min/max nghiệp vụ cho `duration`, vì vậy không tự xem số âm hoặc 0 là lỗi validation.

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `doctorId`, `patientId`, `appointmentId` | Không có miền min/max nghiệp vụ; đây là ID tra cứu bản ghi. | Không áp dụng BVA số học; ID tồn tại/không tồn tại được phân hoạch bằng EP. |
| `medication` | `null`, `""`, `" "` và chuỗi có nội dung như `"A"`. | `null`, rỗng và chỉ khoảng trắng không hợp lệ (`X7`); chuỗi có nội dung hợp lệ (`V4`). Không có giới hạn độ dài được khai báo. |
| `dosage` | `null`, `""`, `" "` và chuỗi có nội dung như `"1"`. | `null`, rỗng và chỉ khoảng trắng không hợp lệ (`X8`); chuỗi có nội dung hợp lệ (`V5`). Không có giới hạn độ dài được khai báo. |
| `frequency` | `null`, `""`, `" "` và chuỗi có nội dung như `"1"`. | `null`, rỗng và chỉ khoảng trắng không hợp lệ (`X9`); chuỗi có nội dung hợp lệ (`V6`). Không có giới hạn độ dài được khai báo. |
| `duration` | `-1`, `0`, `1`, `2147483647`, `2147483648`. | DTO không khai báo min/max nghiệp vụ; `-1`, `0`, `1` đều là giá trị `Integer` có thể bind. `2147483648` vượt miền `Integer` và không bind được. |
| `instructions` | Không truyền, `null`, `""`, chuỗi có nội dung. | Không khai báo bắt buộc hoặc giới hạn độ dài; không áp dụng BVA min/max. |

## 4. GET `/api/doctor/prescriptions`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `doctorId` (query, bắt buộc) | ID dạng `Long` có đơn thuốc: trả về danh sách đơn thuốc tìm được. | V1 | Thiếu `doctorId`: request không đáp ứng tham số bắt buộc. | X1 |
| `doctorId` (query, bắt buộc) | ID dạng `Long` không có đơn thuốc khớp; service chỉ truy vấn danh sách nên trả về danh sách rỗng. | V2 | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X2 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `doctorId` | Không có miền min/max nghiệp vụ; đây là ID dùng để truy vấn đơn thuốc, không phải miền số liên tục. | Không áp dụng BVA số học; trường hợp ID không có đơn thuốc được phân hoạch bằng EP. |

## 5. GET `/api/patient/profile`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `userId` (query, bắt buộc) | ID dạng `Long` có hồ sơ bệnh nhân tương ứng: trả về thông tin hồ sơ bệnh nhân. | V1 | Thiếu `userId`: request không đáp ứng tham số bắt buộc. | X1 |
| `userId` (query, bắt buộc) | — | — | ID dạng `Long` không có hồ sơ bệnh nhân tương ứng: service ném lỗi không tìm thấy bệnh nhân. | X2 |
| `userId` (query, bắt buộc) | — | — | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X3 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `userId` | Không có miền min/max nghiệp vụ; đây là ID dùng để tìm hồ sơ bệnh nhân. | Không áp dụng BVA số học; ID không có hồ sơ được phân hoạch bằng EP. |

## 6. GET `/api/patient/departments`

### Bảng phân hoạch lớp tương đương

API không nhận query parameter hoặc request body. Controller gọi service lấy danh sách khoa đang hoạt động.

| Đầu vào/điều kiện | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| Request | Không truyền query/body; API trả danh sách khoa có trạng thái active. Danh sách có thể rỗng nếu không có khoa đang hoạt động. | V1 | Không có lớp dữ liệu đầu vào không hợp lệ được khai báo vì API không nhận tham số. | — |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| Request | API không có tham số đầu vào và không có giới hạn min/max. | Không áp dụng BVA. |

