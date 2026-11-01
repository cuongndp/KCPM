# Báo cáo rà soát API — Hải Bằng — Tuần 03

## 1. Thông tin chung

- **Người thực hiện:** Hải Bằng
- **Công cụ:** Postman
- **API rà soát:**
  1. `GET /api/doctor/medical-records`
  2. `POST /api/doctor/prescriptions`
- **Mục tiêu:** Rà soát phản hồi API, tham số đầu vào, body, headers, xác thực và các trường hợp dữ liệu phù hợp.
- **Môi trường:** `HMS Local` *(xác nhận lại nếu bạn sử dụng environment khác).*
- **Thời điểm thực hiện:** 23/10/2026 (theo thời gian hiển thị trên ảnh bằng chứng).

> Báo cáo này được lập theo cấu trúc file mẫu và danh sách 21 ảnh bằng chứng bạn cung cấp. Tên file ảnh được đối chiếu theo ảnh thư mục Windows. Status code và response cụ thể cần được điền từ nội dung từng ảnh Postman; không suy đoán kết quả từ tên file. Chỉ xác nhận bug sau khi đối chiếu yêu cầu nghiệp vụ và/hoặc mã nguồn.

## 2. Cấu hình request quan sát/kiểm tra

### 2.1. GET `/api/doctor/medical-records`

- **Method:** `GET`
- **URL dự kiến:** `{{baseUrl}}/api/doctor/medical-records`
- **Query parameter:** `doctorId`
- **Headers:** Kiểm tra các header theo cấu hình request thực tế.
- **Authorization:** Có các trường hợp kiểm thử không token và token không hợp lệ; xác nhận loại token và cấu hình thực tế trong Postman.
- **Response:** Ghi status code và body đúng như Postman hiển thị cho từng test case ở mục 3.

### 2.2. POST `/api/doctor/prescriptions`

- **Method:** `POST`
- **URL dự kiến:** `{{baseUrl}}/api/doctor/prescriptions`
- **Query parameter:** `doctorId`
- **Body:** `PrescriptionRequest` dạng JSON (ghi lại cấu trúc body thực tế đang sử dụng trong Postman).
- **Headers:** Kiểm tra `Content-Type: application/json` nếu gửi JSON và các header cần thiết khác.
- **Authorization:** Có các trường hợp kiểm thử không token và token không hợp lệ; xác nhận cấu hình thực tế trong Postman.
- **Response:** Ghi status code và body đúng như Postman hiển thị cho từng test case ở mục 4.

## 3. Kết quả kiểm thử — GET medical records

| Test ID | Trường hợp | Kết quả thực tế trên Postman | Nhận xét ban đầu |
|---|---|---|---|
| GET-01 | `doctorId=1` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Đối chiếu dữ liệu trả về với dữ liệu thử nghiệm mong đợi. |
| GET-02 | `doctorId=99999` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh hành vi khi ID không tồn tại; không kết luận chỉ từ tên ảnh. |
| GET-03 | `doctorId=abc` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Kiểm tra xử lý tham số sai định dạng. |
| GET-04 | Thiếu `doctorId` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Kiểm tra cách API xử lý query parameter bị thiếu. |
| GET-05 | `doctorId` âm | Chưa ghi nhận status code/body trong bản tổng hợp này. | Đối chiếu với quy tắc hợp lệ của ID. |
| GET-06 | `doctorId=0` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Đối chiếu với quy tắc hợp lệ của ID. |
| GET-07 | Không có token | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh theo cấu hình bảo mật thực tế. |
| GET-08 | Token không hợp lệ | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh theo yêu cầu xác thực và phân quyền. |

### 3.1. Tổng kết API GET

- **Số trường hợp có ảnh bằng chứng:** 8.
- Các ảnh được đặt tên để thể hiện lần lượt ID hợp lệ, ID không tồn tại, sai định dạng, thiếu tham số, ID âm, ID bằng 0, không token và token không hợp lệ.
- Cần bổ sung status code và response từ Postman trước khi hoàn thiện kết luận.
- Không kết luận bug chỉ dựa trên status code nếu chưa đối chiếu hợp đồng API và yêu cầu nghiệp vụ.

## 4. Kết quả kiểm thử — POST prescriptions

| Test ID | Trường hợp theo tên ảnh | Kết quả thực tế trên Postman | Nhận xét ban đầu |
|---|---|---|---|
| POST-01 | Gửi request thành công | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác nhận response và dữ liệu đơn thuốc được tạo/trả về theo yêu cầu. |
| POST-02 | Không tìm thấy bệnh nhân | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh thông báo và hành vi khi bệnh nhân không tồn tại. |
| POST-03 | Không tìm thấy lịch hẹn | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh thông báo và hành vi khi lịch hẹn không tồn tại. |
| POST-04 | JSON không hợp lệ | Chưa ghi nhận status code/body trong bản tổng hợp này. | Kiểm tra xử lý body không phải JSON hợp lệ. |
| POST-05 | Thiếu `patientId` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Đối chiếu trường bắt buộc với `PrescriptionRequest` và hợp đồng API. |
| POST-06 | Thiếu `appointmentId` | Chưa ghi nhận status code/body trong bản tổng hợp này. | Đối chiếu trường bắt buộc với `PrescriptionRequest` và hợp đồng API. |
| POST-07 | Thiếu medication | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh validation cho thông tin thuốc. |
| POST-08 | Thiếu dosage | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh validation cho liều dùng. |
| POST-09 | Thiếu frequency | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh validation cho tần suất dùng thuốc. |
| POST-10 | Thiếu instructions | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh trường này có bắt buộc theo yêu cầu/API hay không. |
| POST-11 | Duration không hợp lệ | Chưa ghi nhận status code/body trong bản tổng hợp này. | Đối chiếu với quy tắc hợp lệ của thời lượng điều trị. |
| POST-12 | Không có token | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh theo cấu hình bảo mật thực tế. |
| POST-13 | Token không hợp lệ | Chưa ghi nhận status code/body trong bản tổng hợp này. | Xác minh theo yêu cầu xác thực và phân quyền. |

### 4.1. Tổng kết API POST

- **Số trường hợp có ảnh bằng chứng:** 13.
- Các tên ảnh cho thấy phạm vi gồm tạo đơn thuốc, dữ liệu bệnh nhân/lịch hẹn không tồn tại, body/field đầu vào và xác thực.
- Cần bổ sung status code, response thực tế và nhận xét sau khi mở từng ảnh Postman.
- Phân biệt JSON không hợp lệ với JSON hợp lệ nhưng thiếu trường hoặc sai kiểu dữ liệu.
- Chỉ xác nhận lỗi sau khi đối chiếu hợp đồng API, DTO/validation và quy tắc nghiệp vụ.

## 5. Vấn đề cần xác minh

### Issue 1 — Xử lý query parameter `doctorId`

- **Test case liên quan:** GET-02 đến GET-06, tùy kết quả thực tế.
- **Kết quả quan sát:** Bổ sung status code và response từ Postman.
- **Kết quả mong đợi:** Đối chiếu tài liệu API, yêu cầu nghiệp vụ hoặc quy ước lỗi của dự án.
- **Nhận định:** Nếu chưa đủ căn cứ, ghi “cần xác minh”, không tự kết luận bug.

### Issue 2 — Validation của `PrescriptionRequest`

- **Test case liên quan:** POST-04 đến POST-11.
- **Kết quả quan sát:** Bổ sung status code và response thực tế.
- **Kết quả mong đợi:** Đối chiếu các trường bắt buộc và quy tắc nghiệp vụ của đơn thuốc.
- **Nhận định:** Phân biệt JSON không hợp lệ với JSON hợp lệ nhưng thiếu trường/sai kiểu dữ liệu.

### Issue 3 — Xác thực và phân quyền

- **Test case liên quan:** GET-07, GET-08, POST-12 và POST-13.
- **Kết quả quan sát:** Bổ sung status code và response thực tế cho từng trường hợp.
- **Nhận định:** Kiểm tra token và quyền truy cập theo cấu hình thực tế; không kết luận lỗi phân quyền chỉ dựa trên tên ảnh.

## 6. Bằng chứng Postman

> Các liên kết ảnh dưới đây dùng đường dẫn tương đối `evidence/<tên-file>.png`. Để hiển thị đúng trên GitHub, đặt file báo cáo tại `docs/api-review/hai-bang/` và các ảnh tại `docs/api-review/hai-bang/evidence/`. Phần mở rộng `.png` được thêm theo cột Type hiển thị là PNG File trong Windows.

### 6.1. API GET `/api/doctor/medical-records`

**GET-01 — `doctorId=1`**

![GET-01 doctorId=1](evidence/get-01-doctorId-1.png)

**GET-02 — `doctorId=99999`**

![GET-02 doctorId=99999](evidence/get-02-doctorId-99999.png)

**GET-03 — `doctorId=abc`**

![GET-03 doctorId abc](evidence/get-03-doctorId-abc.png)

**GET-04 — Thiếu `doctorId`**

![GET-04 thiếu doctorId](evidence/get-04-missing-doctorId.png)

**GET-05 — `doctorId` âm**

![GET-05 doctorId âm](evidence/get-05-doctorId-negative.png)

**GET-06 — `doctorId=0`**

![GET-06 doctorId 0](evidence/get-06-doctorId-0.png)

**GET-07 — Không có token**

![GET-07 không có token](evidence/get-07-no-token.png)

**GET-08 — Token không hợp lệ**

![GET-08 token không hợp lệ](evidence/get-08-invalid-token.png)

### 6.2. API POST `/api/doctor/prescriptions`

**POST-01 — Thành công**

![POST-01 success](evidence/post-01-success.png)

**POST-02 — Không tìm thấy bệnh nhân**

![POST-02 patient not found](evidence/post-02-patient-not-found.png)

**POST-03 — Không tìm thấy lịch hẹn**

![POST-03 appointment not found](evidence/post-03-appointment-not-found.png)

**POST-04 — JSON không hợp lệ**

![POST-04 invalid JSON](evidence/post-04-invalid-json.png)

**POST-05 — Thiếu `patientId`**

![POST-05 thiếu patientId](evidence/post-05-missing-patientid.png)

**POST-06 — Thiếu `appointmentId`**

![POST-06 thiếu appointmentId](evidence/post-06-missing-appointmentid.png)

**POST-07 — Thiếu medication**

![POST-07 thiếu medication](evidence/post-07-missing-medication.png)

**POST-08 — Thiếu dosage**

![POST-08 thiếu dosage](evidence/post-08-missing-dosage.png)

**POST-09 — Thiếu frequency**

![POST-09 thiếu frequency](evidence/post-09-missing-frequency.png)

**POST-10 — Thiếu instructions**

![POST-10 thiếu instructions](evidence/post-10-missing-instructions.png)

**POST-11 — Duration không hợp lệ**

![POST-11 invalid duration](evidence/post-11-invalid-duration.png)

**POST-12 — Không có token**

![POST-12 không có token](evidence/post-12-no-token.png)

**POST-13 — Token không hợp lệ**

![POST-13 token không hợp lệ](evidence/post-13-invalid-token.png)

## 7. Liên kết công việc

- **Jira task:** [Bổ sung link task Jira]
- **GitHub branch:** [Bổ sung tên nhánh cá nhân]
- **GitHub commit:** [Bổ sung link commit sau khi commit và push]
- **Pull Request:** [Bổ sung link PR nếu nhóm sử dụng PR]

## 8. Kết luận

- Đã lập báo cáo cho **2 API**, gồm **8 trường hợp GET** và **13 trường hợp POST** theo danh sách ảnh bằng chứng được cung cấp.
- Các đường dẫn ảnh được viết tương đối từ file báo cáo tới thư mục `evidence/`.
- Cần điền status code và response từ từng ảnh Postman trước khi gửi leader review.
- Đây là báo cáo rà soát ban đầu; các bất thường chỉ được xác nhận là bug sau khi đối chiếu yêu cầu nghiệp vụ và/hoặc mã nguồn.
- **Trạng thái leader review:** Chờ leader review/xác nhận.
