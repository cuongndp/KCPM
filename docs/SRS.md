# Tài liệu đặc tả yêu cầu phần mềm (SRS)

## 1. Giới thiệu

### 1.1 Mục đích
Tài liệu này mô tả yêu cầu chức năng, dữ liệu, giao diện và chất lượng của Hospital Management System (HMS). Đây là cơ sở để phát triển, kiểm thử và nghiệm thu. Nội dung được đối chiếu với mã nguồn hiện có; các mục ghi **Cần xác nhận** hoặc **Đề xuất** chưa được xem là chức năng đã triển khai.

### 1.2 Phạm vi sản phẩm
HMS là ứng dụng web gồm giao diện React và API Spring Boot, hỗ trợ quy trình quản lý bệnh nhân, bác sĩ, khoa/phòng, lịch hẹn, hồ sơ khám và đơn thuốc. Backend sử dụng PostgreSQL; cấu hình còn có Redis và Kafka. Các vai trò ứng dụng là Bệnh nhân, Bác sĩ và Quản trị viên.

Hệ thống hiện không phải hồ sơ bệnh án điện tử được chứng nhận, không thay thế tư vấn/y lệnh của nhân viên y tế và chưa có chức năng thanh toán được xác nhận trong mã nguồn.

### 1.3 Thuật ngữ
- **HMS**: Hospital Management System.
- **RBAC**: Kiểm soát truy cập dựa trên vai trò.
- **JWT**: JSON Web Token dùng cho xác thực API.
- **SRS**: Software Requirements Specification.

### 1.4 Đối tượng đọc
Chủ dự án, người phát triển backend/frontend, người kiểm thử, người triển khai và bên nghiệm thu.

## 2. Tổng quan hệ thống

### 2.1 Nhóm người dùng
| Vai trò | Mục tiêu | Quyền truy cập chính |
|---|---|---|
| Khách chưa đăng nhập | Tạo tài khoản hoặc đăng nhập | Đăng ký, đăng nhập |
| Bệnh nhân | Quản lý lịch hẹn và xem thông tin khám của mình | Hồ sơ, bác sĩ khả dụng, lịch hẹn, bệnh sử, đơn thuốc |
| Bác sĩ | Theo dõi lịch khám và ghi nhận kết quả khám | Hồ sơ, lịch hẹn, hồ sơ bệnh án, đơn thuốc |
| Quản trị viên | Quản lý danh mục khoa/phòng và bác sĩ | Khoa/phòng, danh sách bác sĩ, ứng viên bác sĩ, trạng thái khả dụng |

### 2.2 Môi trường và nền tảng
- Frontend: React 18; chạy trong trình duyệt web hiện đại.
- Backend: Java 21, Spring Boot 3.2; API REST.
- Cơ sở dữ liệu: PostgreSQL.
- Thành phần cấu hình/tích hợp: Redis, Kafka, Docker và Kubernetes.
- Yêu cầu chạy cụ thể phụ thuộc cấu hình môi trường và `docker-compose.yml`.

### 2.3 Giới hạn và phụ thuộc
- Người dùng cần kết nối mạng tới frontend, API và các dịch vụ dữ liệu.
- CORS hiện cho phép origin `http://localhost:3000`; triển khai khác môi trường cần cấu hình origin tương ứng.
- Hệ thống phụ thuộc vào PostgreSQL; Redis/Kafka cần được xác nhận là bắt buộc cho từng luồng nghiệp vụ.
- Quy định pháp lý về dữ liệu sức khỏe, thời hạn lưu trữ và quy trình vận hành cần được chủ dự án xác nhận theo nơi triển khai.

## 3. Yêu cầu chức năng

Các yêu cầu dưới đây mô tả hành vi mục tiêu dựa trên chức năng hiện thấy. Cột trạng thái phản ánh dấu vết trong mã nguồn, không thay thế kiểm thử nghiệm thu.

| ID | Yêu cầu | Vai trò | Trạng thái theo mã nguồn |
|---|---|---|---|
| FR-01 | Người dùng có thể đăng ký tài khoản với thông tin định danh và vai trò hợp lệ. | Khách | Có API đăng ký; cần kiểm thử quy tắc hợp lệ và cấp quyền |
| FR-02 | Người dùng có thể đăng nhập và nhận thông tin xác thực để gọi API được bảo vệ. | Tất cả | Có API đăng nhập và bộ lọc JWT |
| FR-03 | Bệnh nhân có thể xem hồ sơ cá nhân. | Bệnh nhân | Có API hồ sơ |
| FR-04 | Bệnh nhân có thể xem danh sách bác sĩ khả dụng. | Bệnh nhân | Có API |
| FR-05 | Bệnh nhân có thể xem lịch hẹn của mình. | Bệnh nhân | Có API; cần xác nhận ràng buộc quyền sở hữu dữ liệu |
| FR-06 | Bệnh nhân có thể tạo lịch hẹn với bác sĩ, thời điểm và lý do phù hợp. | Bệnh nhân | Có API đặt lịch; quy tắc trùng lịch/còn chỗ cần xác nhận |
| FR-07 | Bệnh nhân có thể hủy lịch hẹn được phép hủy. | Bệnh nhân | Có API hủy; điều kiện thời gian và quyền sở hữu cần xác nhận |
| FR-08 | Bệnh nhân có thể xem lịch sử khám và đơn thuốc. | Bệnh nhân | Có API; cần xác nhận ràng buộc quyền sở hữu |
| FR-09 | Bác sĩ có thể xem hồ sơ cá nhân, lịch hẹn và lịch hẹn sắp tới. | Bác sĩ | Có API; cần xác nhận ràng buộc quyền sở hữu |
| FR-10 | Bác sĩ có thể cập nhật trạng thái lịch hẹn. Trạng thái gồm SCHEDULED, CONFIRMED, COMPLETED, CANCELLED và NO_SHOW. | Bác sĩ | Có API cập nhật trạng thái |
| FR-11 | Bác sĩ có thể ghi và xem hồ sơ khám, gồm chẩn đoán, triệu chứng, ghi chú, kết quả xét nghiệm và khuyến nghị. | Bác sĩ | Có API tạo/xem |
| FR-12 | Bác sĩ có thể tạo và xem đơn thuốc gồm thuốc, liều dùng, tần suất, thời hạn và hướng dẫn. | Bác sĩ | Có API tạo/xem |
| FR-13 | Quản trị viên có thể xem/tạo/xóa khoa hoặc phòng ban. | Quản trị viên | Có API xem/tạo/xóa; chưa thấy API cập nhật |
| FR-14 | Quản trị viên có thể xem danh sách bác sĩ, xem ứng viên và thêm bác sĩ vào khoa/phòng. | Quản trị viên | Có API tương ứng |
| FR-15 | Quản trị viên có thể xóa bác sĩ và bật/tắt trạng thái khả dụng. | Quản trị viên | Có API tương ứng |
| FR-16 | Hệ thống chỉ cho phép truy cập API theo vai trò được cấp. | Tất cả | Có phân quyền theo vai trò; kiểm thử truy cập chéo dữ liệu vẫn cần thiết |
| FR-17 | Hệ thống gửi nhắc lịch, thông báo đơn thuốc hoặc nhắc tái khám. | Bệnh nhân | Chưa xác nhận có luồng gửi thông báo hoạt động; trường dữ liệu Kafka không đủ chứng minh chức năng |
| FR-18 | Hệ thống quản lý hóa đơn và thanh toán. | Bệnh nhân/Quản trị viên | Chưa thấy mô hình/API thanh toán trong mã nguồn |

### 3.1 API nghiệp vụ hiện có
| Nhóm | Phương thức và đường dẫn | Mô tả |
|---|---|---|
| Auth | `POST /api/auth/login` | Đăng nhập |
| Auth | `POST /api/auth/register` | Đăng ký |
| Patient | `GET /api/patient/profile` | Xem hồ sơ bệnh nhân |
| Patient | `GET /api/patient/doctors/available` | Xem bác sĩ khả dụng |
| Patient | `GET /api/patient/appointments` | Xem lịch hẹn |
| Patient | `POST /api/patient/appointments` | Đặt lịch hẹn |
| Patient | `DELETE /api/patient/appointments/{appointmentId}` | Hủy lịch hẹn |
| Patient | `GET /api/patient/medical-history` | Xem lịch sử khám |
| Patient | `GET /api/patient/prescriptions` | Xem đơn thuốc |
| Doctor | `GET /api/doctor/profile` | Xem hồ sơ bác sĩ |
| Doctor | `GET /api/doctor/appointments` | Xem lịch hẹn |
| Doctor | `GET /api/doctor/appointments/upcoming` | Xem lịch sắp tới |
| Doctor | `PUT /api/doctor/appointments/{appointmentId}/status` | Cập nhật trạng thái lịch |
| Doctor | `POST /api/doctor/medical-records` | Tạo hồ sơ khám |
| Doctor | `GET /api/doctor/medical-records` | Xem hồ sơ khám |
| Doctor | `POST /api/doctor/prescriptions` | Tạo đơn thuốc |
| Doctor | `GET /api/doctor/prescriptions` | Xem đơn thuốc |
| Admin | `GET /api/admin/departments` | Xem khoa/phòng |
| Admin | `POST /api/admin/departments` | Tạo khoa/phòng |
| Admin | `DELETE /api/admin/departments/{departmentId}` | Xóa khoa/phòng |
| Admin | `GET /api/admin/doctors` | Xem bác sĩ |
| Admin | `GET /api/admin/users/doctor-candidates` | Xem ứng viên bác sĩ |
| Admin | `POST /api/admin/doctors?departmentId={id}` | Thêm bác sĩ vào khoa/phòng |
| Admin | `DELETE /api/admin/doctors/{doctorId}` | Xóa bác sĩ |
| Admin | `PATCH /api/admin/doctors/{doctorId}/availability` | Cập nhật khả dụng |

## 4. Yêu cầu dữ liệu

### 4.1 Thực thể chính
- **User**: tên đăng nhập, mật khẩu đã mã hóa, email, họ tên, số điện thoại, vai trò và trạng thái hoạt động.
- **Patient**: hồ sơ bệnh nhân gắn với tài khoản người dùng.
- **Doctor**: hồ sơ bác sĩ, chuyên môn/trạng thái và liên kết tới khoa/phòng.
- **Department**: thông tin khoa/phòng.
- **Appointment**: bệnh nhân, bác sĩ, thời gian, trạng thái, lý do, ghi chú và dấu thời gian.
- **MedicalRecord**: bệnh nhân, bác sĩ, chẩn đoán, triệu chứng, ghi chú khám, kết quả xét nghiệm và khuyến nghị.
- **Prescription**: bệnh nhân, bác sĩ, lịch hẹn liên quan, thuốc, liều lượng, tần suất, thời hạn và hướng dẫn.

### 4.2 Quy tắc dữ liệu
- Các khóa định danh phải duy nhất và các quan hệ bắt buộc phải tham chiếu bản ghi tồn tại.
- Mật khẩu không được trả về API hoặc lưu ở dạng rõ.
- Dữ liệu sức khỏe chỉ được truy cập bởi người dùng có thẩm quyền và phải gắn được với bệnh nhân liên quan.
- Thời điểm hẹn được lưu dưới dạng ngày giờ; múi giờ chuẩn hóa khi triển khai là **Cần xác nhận**.
- Chính sách lưu trữ, xóa, sao lưu, phục hồi và nhật ký truy cập dữ liệu sức khỏe là **Cần xác nhận**.

## 5. Yêu cầu giao diện ngoài

### 5.1 Giao diện người dùng
Ứng dụng web cần cung cấp màn hình đăng nhập/đăng ký và khu vực riêng theo vai trò: bệnh nhân, bác sĩ, quản trị viên. Giao diện gọi API backend và hiển thị trạng thái thành công/lỗi phù hợp.

### 5.2 Giao diện phần mềm
Frontend giao tiếp với backend qua HTTP REST/JSON. API được bảo vệ sử dụng JWT theo cơ chế backend cấu hình. Backend kết nối PostgreSQL; Redis/Kafka chỉ được xem là tích hợp bắt buộc khi các luồng sử dụng chúng được xác nhận.

### 5.3 Giao diện truyền thông
Các endpoint phải chỉ chấp nhận nguồn gốc frontend được cấu hình. Môi trường triển khai thực tế phải dùng HTTPS và không được để bí mật triển khai trong mã nguồn.

## 6. Yêu cầu phi chức năng

| ID | Yêu cầu |
|---|---|
| NFR-01 | Xác thực và phân quyền phải được áp dụng cho mọi API nghiệp vụ; API công khai chỉ giới hạn ở đăng nhập/đăng ký và endpoint được chủ động công bố. |
| NFR-02 | Mật khẩu phải được băm bằng thuật toán thích hợp; token và thông tin nhạy cảm không được ghi vào log hoặc trả về ngoài chủ thể được phép. |
| NFR-03 | Dữ liệu y tế và thông tin định danh phải được bảo mật khi truyền và khi lưu; thời hạn lưu/xóa cần tuân thủ quy định nơi vận hành. |
| NFR-04 | Hệ thống phải xác minh quyền truy cập theo cả vai trò lẫn bản ghi thuộc người dùng hiện tại, không chỉ dựa trên ID truyền từ client. **Cần kiểm thử/đối chiếu triển khai.** |
| NFR-05 | API phải kiểm tra dữ liệu đầu vào và trả lỗi nhất quán, không để lộ stack trace hoặc thông tin nội bộ cho người dùng. |
| NFR-06 | Cấu hình môi trường và bí mật (mật khẩu DB, khóa JWT, thông tin dịch vụ) phải nằm ngoài repository và được quản lý bằng secret manager/biến môi trường. |
| NFR-07 | Cơ sở dữ liệu cần có quy trình sao lưu và kiểm thử khôi phục định kỳ. Tần suất/RPO/RTO là **Cần xác nhận**. |
| NFR-08 | Thời gian phản hồi, tải đồng thời, khả năng sẵn sàng và kích thước dữ liệu mục tiêu là **Cần xác nhận** trước nghiệm thu hiệu năng. |
| NFR-09 | Giao diện phải sử dụng được trên màn hình máy tính và thiết bị di động phổ biến; yêu cầu hỗ trợ trình duyệt cụ thể là **Cần xác nhận**. |
| NFR-10 | Các luồng đăng nhập, phân quyền, đặt/hủy lịch, tạo hồ sơ khám và tạo đơn thuốc phải có kiểm thử tự động hoặc kịch bản nghiệm thu. |

## 7. Quy tắc nghiệp vụ và điều kiện nghiệm thu

1. Người dùng chưa xác thực không được gọi API thuộc vai trò.
2. Bệnh nhân không được đọc hoặc thay đổi hồ sơ, lịch hẹn, bệnh sử hay đơn thuốc của bệnh nhân khác.
3. Bác sĩ chỉ được thao tác trên lịch hẹn/hồ sơ/đơn thuốc trong phạm vi được phân công; quy tắc phân công cần được xác nhận.
4. Lịch hẹn mới không được trùng với lịch bác sĩ hoặc vi phạm giờ làm việc; thuật toán và quy tắc cần được xác nhận.
5. Chỉ người có quyền mới được thay đổi trạng thái lịch; các chuyển trạng thái hợp lệ cần được chủ nghiệp vụ xác nhận.
6. Dữ liệu đăng ký thiếu hoặc sai định dạng phải bị từ chối và thông báo lỗi có thể xử lý.
7. Mọi thay đổi liên quan hồ sơ sức khỏe cần truy vết được người thực hiện và thời điểm; hiện trạng audit log cần được xác nhận.
8. Nghiệm thu phải kiểm thử tối thiểu từng vai trò, quyền truy cập chéo đối tượng, các luồng lỗi và tính toàn vẹn dữ liệu.

## 8. Ngoài phạm vi và câu hỏi cần xác nhận

- Thanh toán viện phí, bảo hiểm, hóa đơn và hoàn tiền.
- Gửi SMS/email/push notification và lịch nhắc tự động.
- Quản lý tồn kho thuốc, xét nghiệm, chẩn đoán hình ảnh và nhập viện.
- Quy trình duyệt bác sĩ, sửa thông tin khoa/phòng và quản lý người dùng tổng quát.
- Quy tắc hủy lịch, thời gian đệm, trùng lịch, múi giờ và ngày nghỉ.
- Yêu cầu tuân thủ pháp lý, lưu trữ/đồng ý/chia sẻ hồ sơ sức khỏe theo khu vực triển khai.
- Mục tiêu tải, hiệu năng, độ sẵn sàng, sao lưu và khôi phục.

## 9. Tham chiếu

- Mã nguồn controller, model và cấu hình Spring Security trong `backend/src/main/java/com/hospital/management/`.
- Cấu hình ứng dụng trong `backend/src/main/resources/`.
- Hướng dẫn chạy và công nghệ trong `README.md`.
