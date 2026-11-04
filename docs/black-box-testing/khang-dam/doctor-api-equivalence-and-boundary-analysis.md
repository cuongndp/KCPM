# Phân hoạch lớp tương đương và phân tích giá trị biên

## 1. Phạm vi và quy ước

Tài liệu áp dụng kỹ thuật **Phân hoạch lớp tương đương (Equivalence Partitioning – EP)** và **Phân tích giá trị biên (Boundary Value Analysis – BVA)** cho năm API bác sĩ được khai báo trong Postman Collection `KhangDam.postman_collection(1).json`.

Nội dung gồm các bảng phân hoạch lớp tương đương và bảng giá trị biên cho từng API; không thiết kế test case chi tiết và không viết mã kiểm thử.

### 1.1. Danh sách API trong phạm vi

| STT | Phương thức | Endpoint | Tham số / dữ liệu chính |
|---|---|---|---|
| 1 | GET | `/api/doctor/profile` | Query `userId` |
| 2 | GET | `/api/doctor/appointments` | Query `doctorId` |
| 3 | PUT | `/api/doctor/appointments/{appointmentId}/status` | Path `appointmentId`; query `status` |
| 4 | GET | `/api/doctor/appointments/upcoming` | Query `doctorId` |
| 5 | POST | `/api/doctor/medical-records` | Query `doctorId`; JSON body gồm `patientId`, `diagnosis`, `symptoms`, `consultationNotes`, `labResults`, `recommendations` |

### 1.2. Quy ước phân tích

- Phân hoạch dựa trên method, URL, path variable, query parameter, Authorization và body được thể hiện trong Postman Collection.
- Các request trong Collection sử dụng Bearer token qua biến `{{DOCTOR_TOKEN}}`.
- URL cơ sở được cấu hình bằng biến `{{baseUrl}}`. Trong môi trường kiểm thử đã trao đổi, giá trị được sử dụng là `http://localhost:8080`.
- Tag `V` biểu thị lớp hợp lệ/được API chấp nhận theo cấu trúc request hoặc điều kiện dữ liệu được mô tả.
- Tag `X` biểu thị lớp đầu vào không hợp lệ hoặc trường hợp cần bị từ chối theo hợp đồng API.
- Tag được đánh số riêng trong từng API.
- ID trong Collection là các giá trị tra cứu (`userId`, `doctorId`, `appointmentId`, `patientId`). Collection không cung cấp kiểu dữ liệu Java/DB, miền min/max nghiệp vụ hoặc validation annotation. Vì vậy không tự gán giới hạn số học cho ID.
- Collection không liệt kê đầy đủ các giá trị enum của `status`, quy tắc chuyển trạng thái, độ dài tối thiểu/tối đa của các trường văn bản hoặc quy tắc thời gian của lịch hẹn sắp tới. Các nội dung này được ghi rõ là cần xác minh bằng source code hoặc tài liệu API.
- Các response mẫu `200 OK` trong Collection là bằng chứng cho request mẫu đã lưu, không thay thế cho quy tắc nghiệp vụ tổng quát.

## 2. GET `/api/doctor/profile`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `userId` (query) | Có truyền `userId` theo định dạng ID mà backend chấp nhận. Collection minh họa `userId=2`; response mẫu là `200 OK` và trả về đối tượng hồ sơ bác sĩ. | V1 | Không truyền tham số `userId` trong query string. | X1 |
| `userId` (query) | ID tham chiếu đến hồ sơ bác sĩ có tồn tại; dữ liệu trả về phải tương ứng với ID được truy vấn. | V2 | Giá trị rỗng `userId=`. | X2 |
| `userId` (query) | ID có định dạng và kiểu dữ liệu được backend hỗ trợ. | V3 | Giá trị không thể chuyển đổi sang kiểu ID, ví dụ `abc`, `1.5`; giá trị vượt miền kiểu dữ liệu nếu backend có giới hạn. | X3 |
| `userId` (query) | ID hợp lệ về định dạng nhưng không tìm thấy hồ sơ bác sĩ tương ứng. | V4* | — | — |
| `Authorization` | Gửi Bearer token hợp lệ và có quyền truy cập API hồ sơ bác sĩ. Collection dùng `{{DOCTOR_TOKEN}}`. | V5 | Không gửi Authorization hoặc token sai/hết hạn/không đủ quyền. | X4 |

\* Collection không mô tả rõ response khi ID không có hồ sơ tương ứng; cần xác minh service/exception handler để kết luận API trả `404`, `200` với nội dung rỗng hay mã khác.

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `userId` | Collection chỉ minh họa `userId=2`; không khai báo kiểu dữ liệu, min/max hoặc miền ID nghiệp vụ. | Không đủ căn cứ áp dụng BVA số học cho ID. Phân biệt ID tồn tại và không tồn tại bằng EP. |
| `userId` | Không truyền tham số, tham số rỗng, sai định dạng. | Đây là các lớp tương đương đầu vào; kiểm tra riêng bằng EP, không phải mốc min/max số học. |
| `Authorization` | Token hợp lệ / không gửi token / token sai hoặc hết hạn. | Phân hoạch theo trạng thái xác thực; không áp dụng BVA số học. |

## 3. GET `/api/doctor/appointments`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `doctorId` (query) | Có truyền `doctorId` theo định dạng ID mà backend chấp nhận. Collection minh họa `doctorId=2`; response mẫu là `200 OK` với một phần tử lịch hẹn. | V1 | Không truyền tham số `doctorId`. | X1 |
| `doctorId` (query) | ID thuộc bác sĩ tồn tại và có lịch hẹn; API trả về danh sách lịch hẹn phù hợp. | V2 | Giá trị rỗng `doctorId=`. | X2 |
| `doctorId` (query) | ID thuộc bác sĩ tồn tại nhưng không có lịch hẹn; nếu hợp đồng API trả danh sách thì kết quả có thể là mảng rỗng `[]`. | V3 | Giá trị không thể chuyển đổi sang kiểu ID, ví dụ `abc`, `1.5`; giá trị vượt miền kiểu dữ liệu nếu có. | X3 |
| `doctorId` (query) | ID đúng định dạng nhưng không có bác sĩ tương ứng. | V4* | — | — |
| `Authorization` | Bearer token hợp lệ và có quyền truy cập lịch hẹn. | V5 | Không gửi Authorization hoặc token sai/hết hạn/không đủ quyền. | X4 |

\* Collection không xác định cách API xử lý ID bác sĩ không tồn tại. Không được đồng nhất trường hợp bác sĩ không tồn tại với trường hợp bác sĩ tồn tại nhưng chưa có lịch hẹn.

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `doctorId` | Không có khai báo min/max hoặc kiểu dữ liệu trong Collection. | Không áp dụng BVA số học khi chưa xem Controller/DTO và kiểu cột DB. |
| Số lượng lịch hẹn | Response mẫu chứa một lịch hẹn. Collection không khai báo giới hạn số lượng kết quả hoặc phân trang. | Có thể kiểm tra dữ liệu rỗng, một bản ghi và nhiều bản ghi như các lớp dữ liệu; chưa thể xem đó là giới hạn min/max của API. |
| `Authorization` | Token hợp lệ / không gửi token / token sai hoặc hết hạn. | Phân hoạch theo trạng thái xác thực; không áp dụng BVA số học. |

## 4. PUT `/api/doctor/appointments/{appointmentId}/status`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `appointmentId` (path) | Path variable được truyền đúng vị trí và có giá trị ID mà backend chấp nhận. Collection dùng biến `{{appointmentId}}`. | V1 | URL thiếu path segment `appointmentId` hoặc không khớp route. | X1 |
| `appointmentId` (path) | ID tham chiếu đến lịch hẹn tồn tại và thuộc phạm vi được phép cập nhật. | V2 | ID đúng định dạng nhưng không tồn tại hoặc không thuộc phạm vi được phép cập nhật. | X2 |
| `appointmentId` (path) | ID có định dạng và kiểu dữ liệu được backend hỗ trợ. | V3 | ID không thể chuyển đổi sang kiểu dữ liệu backend yêu cầu, ví dụ chữ hoặc số thập phân; vượt miền kiểu dữ liệu nếu có. | X3 |
| `status` (query) | Có truyền query parameter `status`. Collection minh họa `status=CONFIRMED`. | V4 | Không truyền `status` hoặc truyền chuỗi rỗng `status=`. | X4 |
| `status` (query) | Giá trị status thuộc tập trạng thái được backend cho phép. | V5 | Giá trị status không thuộc tập trạng thái hợp lệ, ví dụ `INVALID_STATUS` (chỉ là dữ liệu thử, không phải enum được xác nhận). | X5 |
| `status` kết hợp trạng thái hiện tại | Cặp trạng thái hiện tại và trạng thái đích được phép chuyển theo quy tắc nghiệp vụ. | V6 | Cặp trạng thái hiện tại và trạng thái đích không được phép chuyển. | X6* |
| `Authorization` | Bearer token hợp lệ và có quyền cập nhật trạng thái lịch hẹn. | V7 | Không gửi token, token sai/hết hạn hoặc không đủ quyền cập nhật. | X7 |

\* Collection chỉ có request mẫu cập nhật sang `CONFIRMED`; không cung cấp state machine hoặc các chuyển trạng thái được phép. Cần kiểm tra Service/enum để xác định các trường hợp cụ thể.

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `appointmentId` | Collection dùng biến `{{appointmentId}}`, không nêu kiểu dữ liệu hoặc miền min/max. | Không đủ căn cứ BVA số học; ID tồn tại/không tồn tại được phân hoạch bằng EP. |
| `status` | Collection chỉ có giá trị mẫu `CONFIRMED`, không có danh sách enum đầy đủ. | Chưa xác định được phần tử đầu/cuối của tập enum. Cần lấy enum hoặc API schema trước khi xác định biên. |
| Chuyển trạng thái | Không có mô tả trạng thái nguồn và trạng thái đích được phép. | Không thể xác định biên nghiệp vụ của state transition chỉ từ Collection. |
| `Authorization` | Token hợp lệ / thiếu token / token sai hoặc hết hạn. | Phân hoạch theo trạng thái xác thực; không áp dụng BVA số học. |

## 5. GET `/api/doctor/appointments/upcoming`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `doctorId` (query) | Có truyền `doctorId` theo định dạng ID mà backend chấp nhận. Collection minh họa `doctorId=2`; response mẫu là `200 OK` với mảng rỗng `[]`. | V1 | Không truyền tham số `doctorId`. | X1 |
| `doctorId` (query) | ID thuộc bác sĩ tồn tại và có lịch hẹn thỏa điều kiện “upcoming”. | V2 | Giá trị rỗng `doctorId=`. | X2 |
| `doctorId` (query) | ID thuộc bác sĩ tồn tại nhưng không có lịch hẹn sắp tới; response mẫu cho phép ghi nhận trường hợp danh sách rỗng `[]`. | V3 | Giá trị không thể chuyển đổi sang kiểu ID, ví dụ `abc`, `1.5`; giá trị vượt miền kiểu dữ liệu nếu có. | X3 |
| `doctorId` (query) | ID đúng định dạng nhưng không tìm thấy bác sĩ tương ứng. | V4* | — | — |
| `Authorization` | Bearer token hợp lệ và có quyền truy cập lịch hẹn sắp tới. | V5 | Không gửi Authorization hoặc token sai/hết hạn/không đủ quyền. | X4 |

\* Collection không mô tả response khi `doctorId` không tồn tại.

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `doctorId` | Không có khai báo kiểu dữ liệu, min/max hoặc miền ID nghiệp vụ. | Không áp dụng BVA số học khi chưa xác minh source code. |
| Thời điểm lịch hẹn | Tên endpoint có từ `upcoming`, nhưng Collection không thể hiện tham số thời gian hoặc điều kiện lọc chính xác. | Chưa đủ căn cứ xác định ngưỡng thời gian. Cần kiểm tra service/repository query. Nếu điều kiện lọc được xác nhận là thời điểm sau hiện tại, có thể dùng `T0−1 giây`, `T0`, `T0+1 giây` với `T0` cố định. |
| Số lượng lịch hẹn sắp tới | Response mẫu là `[]`; Collection không công bố giới hạn số lượng kết quả. | Không xác định được min/max số bản ghi; có thể kiểm tra rỗng, một và nhiều bản ghi như các tình huống dữ liệu. |
| `Authorization` | Token hợp lệ / thiếu token / token sai hoặc hết hạn. | Phân hoạch theo trạng thái xác thực; không áp dụng BVA số học. |

## 6. POST `/api/doctor/medical-records`

### Cấu trúc request theo Collection

- Method: `POST`
- Endpoint: `/api/doctor/medical-records`
- Query parameter: `doctorId=2`
- Authorization: Bearer `{{DOCTOR_TOKEN}}`
- Header: `Content-Type: application/json`, `Accept: application/json`
- Body mẫu:
  - `patientId`
  - `diagnosis`
  - `symptoms`
  - `consultationNotes`
  - `labResults`
  - `recommendations`

### Bảng phân hoạch lớp tương đương

| Đầu vào | Lớp tương đương hợp lệ/được chấp nhận | Tag | Lớp tương đương không hợp lệ/bị từ chối | Tag |
|---|---|---|---|---|
| `doctorId` (query) | Có truyền `doctorId` theo định dạng backend chấp nhận; Collection minh họa `doctorId=2`. | V1 | Không truyền, để rỗng hoặc truyền giá trị không chuyển đổi được sang kiểu ID. | X1 |
| `doctorId` (query) | ID thuộc bác sĩ tồn tại và được phép tạo hồ sơ bệnh án. | V2 | ID không tồn tại hoặc bác sĩ không được phép thực hiện thao tác theo chính sách backend. | X2* |
| `patientId` (JSON body) | Body có `patientId` theo định dạng ID backend chấp nhận; Collection dùng biến `{{patientId}}`. | V3 | Thiếu/null `patientId`, sai kiểu hoặc ID không có bệnh nhân tương ứng. | X3* |
| `diagnosis` (JSON body) | Truyền chuỗi chẩn đoán theo body mẫu của Collection. | V4 | Thiếu/null hoặc sai kiểu dữ liệu nếu DTO/backend yêu cầu trường này. | X4* |
| `symptoms` (JSON body) | Truyền chuỗi triệu chứng theo body mẫu của Collection. | V5 | Thiếu/null hoặc sai kiểu dữ liệu nếu DTO/backend yêu cầu trường này. | X5* |
| `consultationNotes` (JSON body) | Truyền chuỗi ghi chú tư vấn theo body mẫu của Collection. | V6 | Thiếu/null hoặc sai kiểu dữ liệu nếu DTO/backend yêu cầu trường này. | X6* |
| `labResults` (JSON body) | Truyền chuỗi kết quả xét nghiệm theo body mẫu của Collection. | V7 | Thiếu/null hoặc sai kiểu dữ liệu nếu DTO/backend yêu cầu trường này. | X7* |
| `recommendations` (JSON body) | Truyền chuỗi khuyến nghị theo body mẫu của Collection. | V8 | Thiếu/null hoặc sai kiểu dữ liệu nếu DTO/backend yêu cầu trường này. | X8* |
| Toàn bộ JSON body | JSON đúng cú pháp, là object và có các thuộc tính theo body mẫu. | V9 | JSON sai cú pháp, body không phải object hoặc kiểu dữ liệu thuộc tính không phù hợp. | X9 |
| `Authorization` | Bearer token hợp lệ và có quyền tạo hồ sơ bệnh án. | V10 | Không gửi token, token sai/hết hạn hoặc không đủ quyền. | X10 |

\* Postman Collection cho thấy các trường trong body mẫu nhưng không xác định trường nào bắt buộc, nullable, có giá trị mặc định hay bị ràng buộc nghiệp vụ. Vì vậy các lớp X2–X8 là trường hợp cần xác minh với DTO/validation/service; không kết luận chắc chắn rằng backend phải từ chối nếu chưa có căn cứ.

### Bảng giá trị biên

| Đầu vào | Giá trị biên | Kết luận |
|---|---|---|
| `doctorId`, `patientId` | Collection không khai báo kiểu dữ liệu hoặc min/max nghiệp vụ. | Không áp dụng BVA số học cho ID khi chưa xem Controller/DTO và kiểu cột DB. |
| `diagnosis` | Không có minLength/maxLength trong Collection. | Chưa đủ căn cứ xác định biên độ dài. Cần xem DTO/validation trước khi chọn `min−1`, `min`, `min+1`, `max−1`, `max`, `max+1`. |
| `symptoms` | Không có minLength/maxLength trong Collection. | Chưa đủ căn cứ xác định biên độ dài. |
| `consultationNotes` | Không có minLength/maxLength trong Collection. | Chưa đủ căn cứ xác định biên độ dài. |
| `labResults` | Không có minLength/maxLength trong Collection. | Chưa đủ căn cứ xác định biên độ dài. |
| `recommendations` | Không có minLength/maxLength trong Collection. | Chưa đủ căn cứ xác định biên độ dài. |
| JSON body | Collection cung cấp một JSON object mẫu; không cung cấp giới hạn kích thước body. | Có thể kiểm tra JSON hợp lệ/sai cú pháp bằng EP; chưa xác định được biên kích thước request. |
| `Authorization` | Token hợp lệ / thiếu token / token sai hoặc hết hạn. | Phân hoạch theo trạng thái xác thực; không áp dụng BVA số học. |

## 7. Tổng hợp những giới hạn cần xác minh

| API | Thông tin chưa được Collection xác định | Nguồn cần kiểm tra để chốt |
|---|---|---|
| GET `/api/doctor/profile` | Kiểu dữ liệu và miền ID; hành vi khi ID không tồn tại; quyền xem hồ sơ theo ID. | Controller, Service, Entity/DB, Security configuration, exception handler. |
| GET `/api/doctor/appointments` | Kiểu dữ liệu ID; khác biệt giữa bác sĩ không tồn tại và bác sĩ không có lịch hẹn; giới hạn/phân trang danh sách. | Controller, Service, Repository/query, Entity/DB. |
| PUT `/api/doctor/appointments/{appointmentId}/status` | Kiểu ID; tập giá trị status; trạng thái chuyển đổi được phép; quyền cập nhật. | Controller, enum trạng thái, Service/state transition, Security configuration. |
| GET `/api/doctor/appointments/upcoming` | Định nghĩa chính xác “upcoming”, mốc thời gian, timezone và điều kiện lọc. | Service và Repository/query. |
| POST `/api/doctor/medical-records` | Trường bắt buộc/nullable; kiểu và miền ID; min/max độ dài từng trường; quy tắc liên kết bác sĩ-bệnh nhân; kích thước body. | `MedicalRecordRequest`/DTO, validation annotations, Service, Entity/DB, Security configuration. |

## 8. Kết luận

Từ Postman Collection có thể xác định chắc chắn năm endpoint, HTTP method, các path/query parameter, cơ chế Bearer token và cấu trúc body mẫu của API tạo hồ sơ bệnh án. Collection cũng lưu response mẫu `200 OK` cho cả năm request.

Tuy nhiên, Collection không thay thế tài liệu API hoặc source code. Các giới hạn số học của ID, giới hạn độ dài trường văn bản, tập enum `status`, quy tắc chuyển trạng thái, điều kiện thời gian của endpoint `upcoming` và chính sách phân quyền theo từng đối tượng chưa được thể hiện. Do đó, tài liệu không tự đặt các giá trị min/max hoặc quy tắc validation không có căn cứ; các nội dung đó được liệt kê tại mục 7 để xác minh trước khi thiết kế BVA chi tiết.
