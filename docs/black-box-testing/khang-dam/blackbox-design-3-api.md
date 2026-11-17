# Thiết kế kiểm thử Black-box — HMS

Phạm vi gồm ba API thao tác trên lịch hẹn và hồ sơ bác sĩ: đổi trạng thái lịch hẹn (doctor), huỷ lịch hẹn (patient) và xoá bác sĩ (admin). Thiết kế dựa trên collection `Khang Đàm.postman_collection.json`, ba file kết quả chạy `Khang Đàm.postman_test_run*.json` (29 iteration), tài liệu white-box `exports/ThietKe-KiemThu-Whitebox-Appointments.html` và mã nguồn `AppointmentService`/`PatientAppointmentController`. Đây là kỳ vọng thiết kế; phần đã xác nhận từ mã nguồn hoặc lần chạy được ghi rõ, phần còn lại phải chạy trên môi trường test để chốt.

Tài liệu này thay thế `exports/blackbox-design-appointment-apis.md` (chỉ có hai API lịch hẹn) bằng cách bổ sung API xoá bác sĩ và hiệu chỉnh các lớp tương đương theo mã nguồn thực tế.

## 1. Phạm vi, quy ước và dữ liệu kiểm thử

| API | Endpoint | Xác thực | Thành công dự kiến |
|---|---|---|---|
| Doctor — đổi trạng thái | `PUT /api/doctor/appointments/{appointmentId}/status?status={STATUS}` | Bearer token role `DOCTOR` | `200`, trả Appointment với `status` mới (đã xác nhận ở iteration 1: `appointmentId=1`, `status=CONFIRMED`) |
| Patient — huỷ lịch hẹn | `DELETE /api/patient/appointments/{appointmentId}` | Bearer token role `PATIENT` | `200` kèm body chuỗi `Appointment cancelled successfully` (xác nhận từ `PatientAppointmentController` dòng 73–78) |
| Admin — xoá bác sĩ | `DELETE /api/admin/doctors/{doctorId}` | Bearer token role `ADMIN` | `200` (chưa có response mẫu); `doctorId` không tồn tại trả `404` (đã quan sát ở iteration 8) |

Trạng thái hợp lệ của Appointment theo mô tả collection: `SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`. API bác sĩ nhận `status` trên query string, API bệnh nhân và API admin không có body. `cancelAppointment` không xoá bản ghi mà gán `status=CANCELLED`; hành vi này quyết định cách kiểm chứng ở mục 6 và 8.

EP = phân vùng tương đương; BVA = giá trị biên; DT = bảng quyết định; RBAC = phân quyền theo vai trò. `V` là lớp hợp lệ, `X` là lớp không hợp lệ; `Y/N/-` trong bảng quyết định lần lượt là có/không/không xét. Mã HTTP trong CSV là một số nguyên để assert tự động. Những kết quả phụ thuộc môi trường hoặc dữ liệu được ghi chú, không coi là Pass trước khi chạy.

| Loại dữ liệu | Seed mẫu khi `app.seed.enabled=true` |
|---|---|
| ADMIN | username `1`, password `1` |
| DOCTOR | username `2`, password `2`; DB mới: `doctorId=1` |
| PATIENT | username `3`, password `3`; DB mới: `patientId=1` |
| doctor2 (IDOR) | username `doctor2`, password `Passw0rd123`; `doctorId=2` |
| patient2 (IDOR) | username `patient2`, password `Passw0rd123`; `patientId=2` |
| Appointment | `appointmentId=1` thuộc `patientId=1` và `doctorId=1`, tạo bằng `POST /api/patient/appointments?patientId=<pid>` với token PATIENT |
| Doctor không tồn tại | `doctorId=2147483647` (đã dùng ở lần chạy admin) |

Token lấy bằng `POST /api/auth/login` với cặp username/password tương ứng. Nếu DB không phải bản mới, lấy `doctorId` thật bằng `GET /api/doctor/profile?userId=2` và `patientId` thật bằng `GET /api/patient/profile?userId=3`. Ba API đều làm thay đổi dữ liệu, nên mỗi test case thành công cần một appointment hoặc doctor riêng; appointment đã `CANCELLED` không dùng lại cho case thành công của API bác sĩ.

## 2. EP — Bảng phân vùng tương đương

| API | Trường | Lớp hợp lệ | Lớp không hợp lệ / mã |
|---|---|---|---|
| Doctor status | `appointmentId` (path) | ID lịch hẹn tồn tại, số nguyên dương (V1) | Thiếu/rỗng (X1), không phải số (X2), không tồn tại (X3), `0` hoặc âm (X4) |
| Doctor status | `status` (query) | Một trong 5 giá trị enum, đúng chữ hoa (V2) | Thiếu tham số (X5), rỗng `status=` (X6), giá trị lạ (X7), chữ thường (X8), có khoảng trắng (X9) |
| Doctor status | Authorization | Bearer hợp lệ, role `DOCTOR` (V3) | Không token (X10), token sai/hết hạn (X11), role `PATIENT` (X12), role `ADMIN` (X13) |
| Doctor status | Quyền sở hữu | Lịch hẹn thuộc bác sĩ trong token (V4) | Lịch hẹn của bác sĩ khác (X14; kỳ vọng `403`) |
| Doctor status | Chuyển trạng thái | Chuyển sang trạng thái được phép (V5) | Từ `CANCELLED`/`COMPLETED` quay lại `CONFIRMED` (X15; kỳ vọng `409`) |
| Patient cancel | `appointmentId` (path) | Lịch hẹn tồn tại và thuộc bệnh nhân trong token (V1) | Thiếu/rỗng (X1), không phải số (X2), không tồn tại (X3), `0` hoặc âm (X4), lịch hẹn của bệnh nhân khác (X5; kỳ vọng `403`) |
| Patient cancel | Authorization | Bearer hợp lệ, role `PATIENT` (V2) | Không token (X6), token sai/hết hạn (X7), role `DOCTOR` (X8), role `ADMIN` (X9) |
| Patient cancel | Trạng thái lịch hẹn | Huỷ lần đầu và huỷ lại đều trả `200` (V3 — idempotent, xác nhận từ service) | Không có lớp không hợp lệ: `cancelAppointment` không đọc trạng thái cũ |
| Admin delete | `doctorId` (path) | ID bác sĩ tồn tại, số nguyên dương (V1) | Thiếu/rỗng (X1), không phải số (X2), không tồn tại (X3), `0` hoặc âm (X4) |
| Admin delete | Authorization | Bearer hợp lệ, role `ADMIN` (V2) | Không token (X5), token sai/hết hạn (X6), role `DOCTOR` (X7), role `PATIENT` (X8) |
| Admin delete | Dữ liệu liên quan | Bác sĩ không còn ràng buộc chặn xoá (V3) | Bác sĩ còn lịch hẹn/hồ sơ bệnh án (X9; kỳ vọng `409` hoặc `500` — chưa xác nhận) |
| Admin delete | Xoá lặp | Xoá lần đầu thành công (V4) | Xoá lại bác sĩ đã xoá (X10; kỳ vọng `404`) |

Ghi chú: X14, X15, X5 (patient) và X9 (admin) là **lớp kỳ vọng theo yêu cầu nghiệp vụ**, không suy ra từ mã nguồn hiện tại. `AppointmentService` chỉ nhận `appointmentId`, không đối chiếu danh tính trong token và gán thẳng trạng thái mới, nên bốn lớp này hiện được dự đoán sẽ trả `200` — chúng là case phát hiện defect/IDOR chứ không phải case hồi quy.

### 2.1 Số test case tối thiểu để phủ 100% lớp tương đương

Quy tắc: một test case hợp lệ có thể đồng thời đại diện lớp hợp lệ của **mọi** trường; mỗi test case không hợp lệ chỉ được chứa **một** giá trị không hợp lệ, các trường còn lại giữ giá trị hợp lệ.

Công thức: **TC_min = max(số lớp hợp lệ của một trường) + tổng số lớp không hợp lệ của tất cả trường**.

| API | Trường (lớp hợp lệ / không hợp lệ) | max lớp hợp lệ | Tổng lớp không hợp lệ | TC tối thiểu |
|---|---|---|---:|---:|
| Doctor status | appointmentId (1/4), status (1/5), token (1/4), sở hữu (1/1), chuyển trạng thái (1/1) | 1 | 15 | **16** |
| Patient cancel | appointmentId (1/5), token (1/4), trạng thái (1/0) | 1 | 9 | **10** |
| Admin delete | doctorId (1/4), token (1/4), dữ liệu liên quan (1/1), xoá lặp (1/1) | 1 | 10 | **11** |

Như vậy cần tối thiểu **16 / 10 / 11** test case là phủ 100% các lớp tương đương, tổng **37**. Ba file CSV hiện có 11 / 10 / 8 case, tức thiếu 5 case ở API bác sĩ và 3 case ở API admin; API bệnh nhân đã đủ số lượng nhưng thiếu case IDOR và token sai.

## 3. BVA — Bảng giá trị biên

| API / trường | Biên xác định được | Giá trị cần thử | Giới hạn chưa được đặc tả |
|---|---|---|---|
| Doctor status `appointmentId` | `Long`, ID dương; biên dưới `0/1` | `0`, `1`, ID không tồn tại, `9223372036854775807`, `9223372036854775808` | Không có `@Min/@Positive/@Max`; không suy đoán giới hạn nghiệp vụ |
| Doctor status `status` | Enum 5 giá trị, không có miền số | 5 giá trị hợp lệ + rỗng, giá trị lạ, khác hoa/thường, có khoảng trắng | Không có ràng buộc độ dài; binding enum của Spring quyết định `400` hay `500` |
| Patient cancel `appointmentId` | `Long`; biên `0/1` | `0`, `1`, ID không tồn tại, Long max, tràn Long | Không có `@Min/@Positive` |
| Patient cancel trạng thái | Trạng thái kết thúc: `CANCELLED`, `COMPLETED`, `NO_SHOW` | Huỷ lần đầu, huỷ lần hai trên cùng appointment | Idempotent đã xác nhận: lần hai vẫn `200`, không có `409` |
| Admin delete `doctorId` | `Long`; biên `0/1` | `0`, `1`, ID không tồn tại, `2147483647`, Long max, tràn Long | Không có `@Min/@Positive`; ngưỡng ID không tồn tại đã quan sát là `404` |
| Cả ba API | Không có body, không có trường độ dài | — | Không áp dụng BVA chuỗi/ngày giờ |

Các trường hợp không có ràng buộc nguồn được đánh dấu “chưa chốt”; không tạo giả định min/max. `0` và `1` đã nằm trong bảng EP nên BVA chỉ bổ sung các giá trị vượt miền `Long`.

### 3.1 Số test case tối thiểu cho BVA

Quy tắc: mỗi miền có biên xác định cần tối thiểu bốn giá trị min, min+1, max-1, max. Với ID kiểu `Long`, biên thực tế chỉ có `0/1` và hai đầu miền `Long`; giá trị ID không tồn tại không phải biên số học mà là lớp EP.

| API | Miền và giá trị biên | Số giá trị biên | Đã phủ bởi EP | Case BVA bổ sung |
|---|---|---:|---:|---:|
| Doctor status | appointmentId {0, 1, Long max, tràn Long} | 4 | 2 | **2** (Long max, tràn Long) |
| Patient cancel | appointmentId {0, 1, Long max, tràn Long} | 4 | 2 | **2** (Long max, tràn Long) |
| Admin delete | doctorId {0, 1, Long max, tràn Long} | 4 | 2 | **2** (Long max, tràn Long) |

Tràn `Long` kỳ vọng `400` do lỗi binding kiểu dữ liệu; `2147483647` đã được dùng ở lần chạy admin và trả `404`, nên không lặp lại giá trị này trong CSV mới.

## 4. Bảng quyết định (Decision Table)

### 4.1 Đổi trạng thái lịch hẹn (Doctor)

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 | R7 |
|---|---:|---:|---:|---:|---:|---:|---:|
| C1 token role DOCTOR hợp lệ | Y | N | Y | Y | Y | Y | Y |
| C2 appointmentId tồn tại | Y | - | N | Y | Y | Y | Y |
| C3 status thuộc enum | Y | - | Y | N | Y | Y | Y |
| C4 chuyển trạng thái hợp lệ | Y | - | - | - | N | Y | Y |
| C5 lịch hẹn thuộc bác sĩ trong token | Y | - | - | - | - | N | Y |
| A1 cập nhật, HTTP 200, body có status mới | X | - | - | - | - | - | X |
| A2 từ chối xác thực/phân quyền, 401/403 | - | X | - | - | - | X | - |
| A3 validation, HTTP 400 | - | - | - | X | - | - | - |
| A4 không tìm thấy tài nguyên, HTTP 404 | - | - | X | - | - | - | - |
| A5 xung đột trạng thái, HTTP 409 | - | - | - | - | X | - | - |

R2 tách thành hai case thực thi: không token (`401`) và sai role (`403`). C4 và C5 là điều kiện **kỳ vọng**: mã nguồn hiện không kiểm tra, nên R5/R6 dự kiến trả `200` và phải được ghi nhận là defect thay vì sửa kỳ vọng. R7 là ca hồi quy bắt buộc: đổi trạng thái phải trả đúng lịch hẹn, không tạo bản ghi mới.

### 4.2 Huỷ lịch hẹn (Patient)

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1 token role PATIENT hợp lệ | Y | N | Y | Y | Y | Y |
| C2 appointmentId tồn tại | Y | - | N | Y | Y | Y |
| C3 lịch hẹn thuộc bệnh nhân trong token | Y | - | - | N | Y | Y |
| C4 lịch hẹn đã CANCELLED | N | - | - | - | Y | N |
| A1 huỷ thành công, 200 kèm body chuỗi | X | - | - | - | X | X |
| A2 từ chối xác thực/phân quyền, 401/403 | - | X | - | X | - | - |
| A3 không tìm thấy tài nguyên, 404 | - | - | X | - | - | - |

R5 là hành vi idempotent đã xác nhận từ `cancelAppointment` (luôn gán `CANCELLED` và trả `200`), thuộc lớp hợp lệ chứ không phải xung đột. R4 là IDOR: mã nguồn không đối chiếu `patientId` trong token, dự kiến trả `200` — case này phải fail theo kỳ vọng `403`.

### 4.3 Xoá bác sĩ (Admin)

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1 token role ADMIN hợp lệ | Y | N | Y | Y | Y | Y |
| C2 doctorId tồn tại | Y | - | N | Y | Y | Y |
| C3 bác sĩ chưa bị xoá | Y | - | - | Y | N | Y |
| C4 không còn dữ liệu liên quan chặn xoá | Y | - | - | Y | Y | N |
| A1 xoá thành công, 200 | X | - | - | - | - | - |
| A2 từ chối xác thực/phân quyền, 401/403 | - | X | - | - | - | - |
| A3 không tìm thấy tài nguyên, 404 | - | - | X | - | X | - |
| A4 xung đột ràng buộc dữ liệu, 409/500 | - | - | - | - | - | X |

R2 tách thành không token (`401`) và sai role (`403`). C4 chưa được xác nhận: nếu backend xoá cứng bác sĩ còn lịch hẹn, kết quả có thể là `500` từ ràng buộc khoá ngoại thay vì `409`. R6 là ca xoá lặp, kỳ vọng `404` theo quan sát `doctorId=2147483647`.

### 4.4 Số test case tối thiểu cho bảng quyết định

Mỗi luật của bảng quyết định tương ứng một test case, vì luật chính là một tổ hợp điều kiện cần được chứng minh.

| API | Số luật | TC tối thiểu nếu kiểm thử độc lập | TC phát sinh thêm khi gộp với EP |
|---|---:|---:|---:|
| Doctor status | 7 (R1–R7; R2 tách 401/403) | 8 | 0 |
| Patient cancel | 6 | 6 | 0 |
| Admin delete | 6 (R2 tách 401/403) | 7 | 0 |

Khi làm riêng, bảng quyết định cần đúng 8 / 6 / 7 case. Khi gộp với EP thì không phát sinh case mới, vì mỗi luật ánh xạ một-một vào một lớp (hoặc cặp lớp) đã được thiết kế — bảng quyết định vì thế dùng để chứng minh không thiếu tổ hợp, không phải để tăng số case.

## 5. Ma trận quyền (RBAC)

| Actor / tình huống | PUT doctor status | DELETE patient appointment | DELETE admin doctor | Kết quả dự kiến |
|---|---:|---:|---:|---|
| Chưa xác thực | Được gọi | Được gọi | Được gọi | `401` |
| DOCTOR có token hợp lệ | Được phép theo role | Không được phép | Không được phép | `200` / `403` / `403` |
| PATIENT có token hợp lệ | Không được phép | Được phép theo role | Không được phép | `403` / `200` / `403` |
| ADMIN có token hợp lệ | Không được phép theo annotation hiện tại | Không được phép | Được phép theo role | `403` / `403` / `200` |
| Token sai/hết hạn | Không được phép | Không được phép | Không được phép | `401` |
| DOCTOR cập nhật lịch hẹn của bác sĩ khác | Không kiểm tra ở service | — | — | Dự kiến `200` — IDOR đã xác nhận ở mức mã nguồn |
| PATIENT huỷ lịch hẹn của bệnh nhân khác | — | Không kiểm tra ở service | — | Dự kiến `200` — IDOR đã xác nhận ở mức mã nguồn |
| ADMIN xoá bác sĩ còn lịch hẹn | — | — | Cần xác nhận ràng buộc dữ liệu | `409` hoặc `500` — chưa xác định |

Ba endpoint chỉ có ràng buộc role ở mức controller (`@PreAuthorize("hasRole(...)")`); cả `updateAppointmentStatus` và `cancelAppointment` chỉ nhận ID tài nguyên, không nhận danh tính từ token, nên việc đối chiếu quyền sở hữu phải kiểm thử riêng và hiện được dự đoán sẽ thất bại. Đây là nhóm case ưu tiên cao nhất vì ảnh hưởng trực tiếp đến dữ liệu bệnh nhân và hồ sơ bác sĩ.

## 6. Ma trận phủ kỹ thuật

| Nhóm kiểm thử | Doctor status (18) | Patient cancel (12) | Admin delete (13) |
|---|---|---|---|
| EP hợp lệ và không hợp lệ | appointmentId, status enum, token, sở hữu, chuyển trạng thái | appointmentId, token, trạng thái idempotent | doctorId, token, dữ liệu liên quan, xoá lặp |
| BVA có căn cứ | ID `0/1`, Long max, tràn Long | ID `0/1`, Long max, tràn Long | ID `0/1`, Long max, tràn Long |
| Decision table | Auth, tài nguyên, validation, xung đột, sở hữu | Auth, tài nguyên, sở hữu, idempotency | Auth, tài nguyên, ràng buộc dữ liệu, xoá lặp |
| RBAC | Anonymous/DOCTOR/PATIENT/ADMIN, bác sĩ khác | Anonymous/PATIENT/DOCTOR/ADMIN, bệnh nhân khác | Anonymous/ADMIN/DOCTOR/PATIENT |
| Kiểu dữ liệu / path | ID không phải số, ID rỗng, enum sai kiểu | ID không phải số, ID rỗng | ID không phải số, ID rỗng |
| Hợp đồng response | `status` trong body khớp tham số, `updatedAt` đổi | `200` kèm body `Appointment cancelled successfully`; lịch hẹn chuyển `CANCELLED` trong danh sách | Mã `200`, body thành công; bác sĩ không còn trong danh sách |
| Tác động phụ | Không tạo appointment mới; danh sách của bác sĩ đổi trạng thái | Lịch hẹn vẫn tồn tại với `CANCELLED`; không xoá bản ghi liên quan | Lịch hẹn/hồ sơ của bác sĩ bị xoá bị xử lý thế nào; không xoá tài khoản user ngoài phạm vi |

Khác biệt quan trọng so với thiết kế hai API trước: API bệnh nhân **không xoá** bản ghi mà huỷ mềm, nên bước kiểm chứng phải là `GET /api/patient/appointments?patientId=<pid>` thấy phần tử với `status=CANCELLED`, không phải "không còn trong danh sách".

## 7. Tổng hợp test suite

| File | Dải TC_ID | Số case | Nội dung |
|---|---|---:|---|
| `exports/testcases-doctor-appointment-status.csv` | TC-DAS-01..18 | 18 | Đổi trạng thái, enum, ID biên, RBAC, sở hữu, chuyển trạng thái, hợp đồng response |
| `exports/testcases-patient-appointment-cancel.csv` | TC-PDC-01..12 | 12 | Huỷ lịch hẹn, ID biên, RBAC, IDOR, idempotency, body chuỗi |
| `exports/testcases-admin-delete-doctor.csv` | TC-ADR-01..13 | 13 | Xoá bác sĩ, ID biên, RBAC, xoá lặp, ràng buộc dữ liệu liên quan |

Tổng cộng: **43 test case**. Ba file CSV này chưa tồn tại trong workspace; đây là đề xuất cấu trúc. Cột tối thiểu phải khớp script hiện có của collection: `TestCase_ID`, `appointmentId` hoặc `doctorId`, `Expected_HTTP_Status`, `status` (chỉ API bác sĩ), và thêm `Kỳ vọng nội dung`, `Ghi chú`.

### 7.1 Bảng tổng hợp số test case tối thiểu

| API | EP tối thiểu | BVA bổ sung | Bảng quyết định (nếu riêng) | Tổng tối thiểu (đã gộp) | CSV hiện có | Kết luận |
|---|---:|---:|---:|---:|---:|---|
| Doctor status | 16 | 2 | 8 | **18** | 11 | Thiếu 7; thiếu RBAC, sở hữu, biên ID |
| Patient cancel | 10 | 2 | 6 | **12** | 10 | Thiếu 2; thiếu token sai, IDOR, biên ID |
| Admin delete | 11 | 2 | 7 | **13** | 8 | Thiếu 5; thiếu token sai, xoá lặp, ràng buộc dữ liệu |
| **Tổng** | 37 | 6 | 21 | **43** | 29 | Thiếu 14 case so với mức tối thiểu |

## 8. Cách thực thi và tiêu chí ghi nhận

1. Khởi chạy backend và DB test; xác nhận `baseUrl`, token DOCTOR/PATIENT/ADMIN, `doctorId`, `patientId` và tạo trước appointment cho từng case.
2. Đặt `Authorization: Bearer {{DOCTOR_TOKEN}}`, `{{PATIENT_TOKEN}}` hoặc token admin; ba API không gửi body, không cần `Content-Type`.
3. Với API bác sĩ, truyền `status` trên query string — hợp đồng này đã được xác nhận bằng lần chạy `PUT /api/doctor/appointments/1/status?status=CONFIRMED` trả `200` kèm body.
4. Mỗi case thay đổi dữ liệu dùng một appointment hoặc doctor mới; không tái sử dụng appointment đã `CANCELLED` cho case thành công của API bác sĩ.
5. So sánh status với `Expected_HTTP_Status`, rồi assert thêm nội dung: API bác sĩ phải trả `status` bằng giá trị gửi lên; API bệnh nhân phải trả đúng chuỗi `Appointment cancelled successfully` và lịch hẹn xuất hiện với `CANCELLED` trong danh sách; API admin phải làm bác sĩ biến mất khỏi danh sách và trả `404` khi xoá lại.
6. Với case bị từ chối, kiểm tra dữ liệu không đổi; với case IDOR, xác nhận không có thay đổi nào trên lịch hẹn của bệnh nhân khác.
7. Lưu status thực tế, response body, thời điểm và môi trường; ghi nhận khác biệt thay vì sửa kỳ vọng sau khi chạy mà không có căn cứ.

Các status `400/401/403/404` được dự kiến theo Spring Security, binding enum và `GlobalExceptionHandler`; `409` chỉ áp dụng nếu backend có kiểm tra chuyển trạng thái hoặc ràng buộc dữ liệu. Lần chạy trước ghi 15/110 test fail, tập trung ở khâu so mã HTTP; nguyên nhân chưa chốt vì file run chỉ lưu boolean, không lưu mã và body từng iteration.

## 9. Chỗ đặc tả chưa chốt

- `AdminController`/`DoctorController` chưa được đọc: chưa rõ mã và body thành công của `DELETE /api/admin/doctors/{id}`, cách xoá (cứng, mềm hay chuyển `available=false`) và mã khi bác sĩ còn lịch hẹn.
- Chưa xác nhận cách bind `status`: nếu controller nhận enum trực tiếp, giá trị lạ có thể trả `400`; nếu nhận chuỗi rồi `valueOf`, có thể trả `500`. Lần chạy API bác sĩ có 7 iteration lệch mã, nghi thuộc nhóm này.
- Quyền sở hữu đã xác nhận **không** được kiểm tra ở tầng service cho cả hai API lịch hẹn; cần chốt đây là defect bắt buộc sửa hay rủi ro được chấp nhận, và mã trả về mong muốn (`403` hay `404` để tránh lộ sự tồn tại).
- Quy tắc chuyển trạng thái chưa được đặc tả: có cho phép `COMPLETED → CONFIRMED`, `CANCELLED → SCHEDULED`, cập nhật lịch hẹn trong quá khứ hay không.
- Huỷ lịch hẹn là huỷ mềm và idempotent; chưa rõ có thông báo cho bệnh nhân/bác sĩ, audit log hay chặn huỷ lịch đã `COMPLETED`.
- API `GET /api/patient/appointments/{appointmentId}` (xem chi tiết) không được kiểm thử ở collection nào; tên request `api/patient/appointments/{{appointmentId}}` lại là `DELETE`, dễ nhầm phạm vi.
- Biến `appointmentId` và `PATIENT_TOKEN` không khai báo trong `variable` của collection; chạy ngoài Runner sẽ gửi nguyên chuỗi `{{appointmentId}}`.
- Ảnh hưởng của việc xoá bác sĩ lên lịch hẹn, hồ sơ bệnh án và đơn thuốc liên quan chưa có case nào kiểm tra.

## 10. Cách chạy từng file CSV bằng Postman/Newman

Collection `Khang Đàm` hiện là danh sách phẳng, không có folder, nên cần tách ba request vào folder `Doctor Appointment Status`, `Patient Appointment Cancel`, `Admin Doctor` trước khi chạy theo folder:

```bash
newman run postman/KhangDam.postman_collection.json -e env.json \
  -d exports/testcases-doctor-appointment-status.csv --folder "Doctor Appointment Status"
newman run postman/KhangDam.postman_collection.json -e env.json \
  -d exports/testcases-patient-appointment-cancel.csv --folder "Patient Appointment Cancel"
newman run postman/KhangDam.postman_collection.json -e env.json \
  -d exports/testcases-admin-delete-doctor.csv --folder "Admin Doctor"
```

Nếu chưa tách folder, chạy từ Postman Runner và chọn đúng request, hoặc tạo collection riêng chỉ chứa request tương ứng — chạy cả collection với một file CSV sẽ gửi sai dữ liệu cho các request khác.

Script hiện tại đọc `pm.iterationData`, nên chỉ hoạt động trong Runner/Newman; chạy tay trong Postman app sẽ cho `expectedStatus = NaN` và fail. Trong test script, assert thêm nội dung:

```javascript
pm.test("Body status matches request", function () {
    pm.expect(pm.response.json().status).to.eql(pm.iterationData.get("status"));
});
```

Cấu hình `baseUrl`, `DOCTOR_TOKEN`, `PATIENT_TOKEN`, token admin, `appointmentId`, `doctorId` trong `env.json`; mỗi iteration có thể override ID và `status` từ CSV. Case cần seed đặc biệt (bác sĩ khác, bệnh nhân khác, lịch hẹn đã huỷ, bác sĩ còn dữ liệu liên quan) phải chuẩn bị trước và ghi rõ trong cột `Ghi chú` là `Không` chạy được ngay.
