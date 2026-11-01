# Phân hoạch lớp tương đương và phân tích giá trị biên

## 1. Phạm vi và quy ước

Tài liệu áp dụng kỹ thuật **Phân hoạch lớp tương đương (EP)** và **Phân tích giá trị biên (BVA)** cho sáu API bệnh nhân trong collection Postman Phúc Cường. Nội dung chỉ gồm các bảng phân hoạch và giá trị biên; không thiết kế test case hay viết mã kiểm thử.

- Phân hoạch dựa trên tham số của controller, ràng buộc của `AppointmentRequest` và xử lý trong service.
- Giả định request đã có thông tin xác thực hợp lệ với vai trò `PATIENT`; không phân hoạch token, quyền truy cập hoặc lỗi hệ thống bên ngoài phạm vi đầu vào của sáu API.
- `patientId`, `doctorId`, `departmentId`, `appointmentId` là ID kiểu `Long`. Mã nguồn không đặt miền min/max nghiệp vụ cho các ID; việc chấp nhận nghiệp vụ phụ thuộc vào bản ghi có tồn tại hay không. Vì vậy không gán BVA số học min/max cho ID.
- Với thời điểm đặt lịch, gọi `T0` là thời điểm `BusinessTime.now()` tại lúc đánh giá. Dùng bước một giây để thể hiện giá trị sát ngưỡng theo định dạng ngày giờ trong collection; khi áp dụng cần cố định `T0` để tránh phụ thuộc thời gian thực.
- Tag `V` biểu thị lớp hợp lệ/được API chấp nhận; tag `X` biểu thị lớp đầu vào không hợp lệ hoặc bị nghiệp vụ từ chối. Tag được đánh số riêng trong từng API.

## 2. GET `/api/patient/doctors/available`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `departmentId` (query, tùy chọn) | Không truyền tham số: lấy danh sách bác sĩ đang khả dụng ở mọi khoa. | V1 | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X1 |
| `departmentId` (query, tùy chọn) | ID khoa dạng `Long` thuộc khoa đang hoạt động: lọc bác sĩ khả dụng thuộc khoa đó; danh sách có thể rỗng nếu không có bác sĩ phù hợp. | V2 | — | — |
| `departmentId` (query, tùy chọn) | ID dạng `Long` nhưng không tìm được khoa/bác sĩ phù hợp (ví dụ ID không tồn tại, khoa không hoạt động, ID 0/âm): API vẫn chấp nhận và trả danh sách rỗng. | V3 | — | — |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `departmentId` | Không có ngưỡng min/max nghiệp vụ; đây là ID dùng để tra cứu, không phải miền số liên tục. Không áp dụng BVA số học. | Tình huống không truyền tham số và lớp ID không khớp đã được phân hoạch bằng EP. |

## 3. GET `/api/patient/appointments`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `patientId` (query, bắt buộc) | ID dạng `Long` có lịch hẹn: trả về danh sách lịch hẹn tìm được. | V1 | Thiếu `patientId`: request không đáp ứng tham số bắt buộc. | X1 |
| `patientId` (query, bắt buộc) | ID dạng `Long` không có lịch hẹn khớp; gồm bệnh nhân chưa có lịch hoặc ID không tồn tại. Service chỉ truy vấn danh sách, nên API trả về danh sách rỗng thay vì kiểm tra sự tồn tại của bệnh nhân. | V2 | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X2 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `patientId` | Không có miền min/max nghiệp vụ; kết quả phụ thuộc vào lịch hẹn khớp với ID, không phụ thuộc một khoảng số liên tục. | Không áp dụng BVA số học; ID không khớp được phân hoạch bằng EP. |

## 4. POST `/api/patient/appointments`

### Bảng phân hoạch lớp tương đương

| Đầu vào/điều kiện | Lớp tương đương hợp lệ | Tag | Lớp tương đương không hợp lệ | Tag |
|---|---|---|---|---|
| `patientId` (query, bắt buộc) | ID `Long` của bệnh nhân tồn tại. | V1 | Thiếu tham số hoặc giá trị không chuyển đổi được sang `Long`/vượt miền `Long`. | X1 |
| `patientId` (query, bắt buộc) | — | — | ID `Long` không có bệnh nhân tương ứng (bao gồm ID 0/âm nếu không có bản ghi): service không tìm thấy bệnh nhân. | X2 |
| `doctorId` (body, bắt buộc) | ID `Long` của bác sĩ tồn tại và đang khả dụng. | V2 | Thiếu/null hoặc giá trị body không chuyển đổi được sang `Long`. | X3 |
| `doctorId` (body, bắt buộc) | — | — | ID `Long` không có bác sĩ tương ứng. | X4 |
| `doctorId` (body, bắt buộc) | — | — | Bác sĩ tồn tại nhưng không khả dụng. | X5 |
| `departmentId` (body, bắt buộc) | ID `Long` của khoa tồn tại và đang hoạt động. | V3 | Thiếu/null hoặc giá trị body không chuyển đổi được sang `Long`. | X6 |
| `departmentId` (body, bắt buộc) | — | — | ID `Long` không có khoa tương ứng. | X7 |
| `departmentId` (body, bắt buộc) | — | — | Khoa tồn tại nhưng không hoạt động. | X8 |
| `doctorId` kết hợp `departmentId` | Bác sĩ thuộc đúng khoa đã chọn. | V4 | Bác sĩ thuộc khoa khác với khoa đã chọn. | X9 |
| `appointmentDateTime` (body, bắt buộc) | Thời điểm hẹn sau `T0`. | V5 | Thiếu/null hoặc ngày giờ không được chuyển đổi đúng kiểu. | X10 |
| `appointmentDateTime` (body, bắt buộc) | — | — | Thời điểm hẹn trước hoặc đúng `T0`; service chỉ chấp nhận thời điểm strictly sau hiện tại. | X11 |
| Slot của bác sĩ tại `appointmentDateTime` | Slot chưa bị đặt trùng. | V6 | Slot đã có lịch xung đột. Mã nguồn hiện ném `RuntimeException`, được xử lý thành HTTP 500 bởi handler tổng quát. | X12 |
| `reason`, `notes` (body, tùy chọn) | Có thể bỏ trống hoặc truyền chuỗi; không khai báo giới hạn độ dài hay ràng buộc nghiệp vụ. | V7 | Không có lớp không hợp lệ nghiệp vụ được khai báo cho nội dung chuỗi. | — |

> Các trường `doctorId`, `departmentId` và `appointmentDateTime` có `@NotNull`. `reason` và `notes` không có ràng buộc validation được khai báo. Với ID bác sĩ không khả dụng và slot bị trùng, service hiện ném `RuntimeException` nên phản hồi thực tế là HTTP 500; đây là hành vi của mã nguồn hiện tại, không phải kết quả mong muốn về mặt thiết kế API.

### Bảng giá trị biên

| Đầu vào | `min-` / sát dưới ngưỡng | `min` / tại ngưỡng | `min+` / sát trên ngưỡng | Giá trị đại diện hợp lệ | `max-`, `max`, `max+` | Tag / kết luận |
|---|---|---|---|---|---|---|
| `appointmentDateTime` | `T0 − 1 giây` — không hợp lệ (`X11`) | `T0` — không hợp lệ vì điều kiện là `> T0` (`B1`) | `T0 + 1 giây` — hợp lệ nếu các điều kiện khác thỏa mãn (`B2`) | `T0 + 1 ngày` (`B3`) | Không có ngưỡng trên được khai báo; không áp dụng | `X11` (min-), B1 (min), B2 (min+), B3 (nominal) |
| `patientId`, `doctorId`, `departmentId` | Không xác định | Không xác định | Không xác định | ID tồn tại phù hợp nghiệp vụ | Không xác định | Không áp dụng BVA: ID là khóa tra cứu, không có miền min/max nghiệp vụ. |
| `reason`, `notes` | Không xác định | Không xác định | Không xác định | Chuỗi tùy chọn | Không xác định | Không áp dụng BVA: mã nguồn không khai báo giới hạn độ dài. |

## 5. DELETE `/api/patient/appointments/{appointmentId}`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `appointmentId` (path, bắt buộc) | ID dạng `Long` của lịch hẹn tồn tại: service cập nhật trạng thái lịch thành `CANCELLED`. | V1 | Thiếu path segment hoặc giá trị không chuyển đổi được sang `Long`/vượt miền `Long`. | X1 |
| `appointmentId` (path, bắt buộc) | — | — | ID dạng `Long` không có lịch hẹn tương ứng: service không tìm thấy lịch. | X2 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `appointmentId` | Không có ngưỡng min/max nghiệp vụ; đây là ID tra cứu lịch hẹn. | Không áp dụng BVA số học; ID tồn tại/không tồn tại được phân hoạch bằng EP. |

## 6. GET `/api/patient/medical-history`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `patientId` (query, bắt buộc) | ID dạng `Long` có hồ sơ khám: trả về danh sách hồ sơ tìm được. | V1 | Thiếu `patientId`: request không đáp ứng tham số bắt buộc. | X1 |
| `patientId` (query, bắt buộc) | ID dạng `Long` không có hồ sơ khám khớp; gồm bệnh nhân chưa có hồ sơ hoặc ID không tồn tại. Service chỉ truy vấn danh sách nên trả về danh sách rỗng. | V2 | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X2 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `patientId` | Không có miền min/max nghiệp vụ; kết quả phụ thuộc hồ sơ khám khớp với ID, không phụ thuộc một khoảng số liên tục. | Không áp dụng BVA số học; ID không khớp được phân hoạch bằng EP. |

## 7. GET `/api/patient/prescriptions`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `patientId` (query, bắt buộc) | ID dạng `Long` có đơn thuốc: trả về danh sách đơn thuốc tìm được. | V1 | Thiếu `patientId`: request không đáp ứng tham số bắt buộc. | X1 |
| `patientId` (query, bắt buộc) | ID dạng `Long` không có đơn thuốc khớp; gồm bệnh nhân chưa có đơn hoặc ID không tồn tại. Service chỉ truy vấn danh sách nên trả về danh sách rỗng. | V2 | Giá trị không chuyển đổi được sang `Long` hoặc vượt miền biểu diễn `Long`: request bị từ chối khi binding. | X2 |

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `patientId` | Không có miền min/max nghiệp vụ; kết quả phụ thuộc đơn thuốc khớp với ID, không phụ thuộc một khoảng số liên tục. | Không áp dụng BVA số học; ID không khớp được phân hoạch bằng EP. |
