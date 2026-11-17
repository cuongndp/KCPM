# Thiết kế kiểm thử Black-box — 2 API lịch hẹn (HMS)

Phạm vi gồm hai API thao tác trên một lịch hẹn: bác sĩ đổi trạng thái và bệnh nhân xoá lịch hẹn. Thiết kế dựa trên hai collection Postman `Khang Đàm.postman_collection.json` và `Phúc Cường.postman_collection.json`, response mẫu đã lưu trong collection và quy ước Spring Boot/Security của backend. Chưa đối chiếu được với mã nguồn (file `KCPM.zip` không đọc được trong workspace này), nên các mã `400/401/403/404/409` là kỳ vọng theo quy ước, phải chạy trên môi trường test để xác nhận.

## 1. Phạm vi, quy ước và dữ liệu kiểm thử

| API | Endpoint | Xác thực | Thành công dự kiến |
|---|---|---|---|
| Doctor — đổi trạng thái | `PUT /api/doctor/appointments/{appointmentId}/status?status={STATUS}` | Bearer token role `DOCTOR` | `200`, trả Appointment với `status` mới và `updatedAt` đổi |
| Patient — xoá lịch hẹn | `DELETE /api/patient/appointments/{appointmentId}` | Bearer token role `PATIENT` | `200` hoặc `204` (chưa chốt — collection không lưu response mẫu) |

Trạng thái hợp lệ theo mô tả collection: `SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`. Hai API chỉ xuất hiện trong collection `Khang Đàm` (request 1 tại dòng 349–511, request 2 tại dòng 651–733); collection `Phúc Cường` chỉ có `GET /api/patient/appointments?patientId={id}`.

EP = phân vùng tương đương; BVA = giá trị biên; DT = bảng quyết định; RBAC = phân quyền theo vai trò. `V` là lớp hợp lệ, `X` là lớp không hợp lệ; `Y/N/-` trong bảng quyết định lần lượt là có/không/không xét. Mã HTTP trong CSV là một số nguyên để assert tự động. Những kết quả phụ thuộc môi trường hoặc dữ liệu được ghi chú, không coi là Pass trước khi chạy.

| Loại dữ liệu | Seed mẫu khi `app.seed.enabled=true` |
|---|---|
| DOCTOR | username `2`, password `2`; DB mới: `doctorId=1` |
| PATIENT | username `3`, password `3`; DB mới: `patientId=1` |
| PATIENT thứ hai | Cần seed thêm để kiểm thử quyền sở hữu (`patientId` khác) |
| DOCTOR thứ hai | Cần seed thêm để kiểm thử bác sĩ không phụ trách lịch hẹn |
| Appointment | Tạo bằng `POST /api/patient/appointments?patientId=<pid>` với token PATIENT; dùng `id` trả về |

Token lấy bằng `POST /api/auth/login` với cặp username/password tương ứng. Nếu DB không phải bản mới, lấy `doctorId` thật bằng `GET /api/doctor/profile?userId=2` và `patientId` thật bằng `GET /api/patient/profile?userId=3`. Mỗi test case thay đổi trạng thái hoặc xoá lịch hẹn cần một appointment riêng, vì hai API này làm thay đổi dữ liệu.

## 2. EP — Bảng phân vùng tương đương

| API | Trường | Lớp hợp lệ | Lớp không hợp lệ / mã |
|---|---|---|---|
| Doctor status | `appointmentId` (path) | ID lịch hẹn tồn tại, số nguyên dương (V1) | Thiếu/rỗng (X1), không phải số (X2), không tồn tại (X3), `0` hoặc âm (X4) |
| Doctor status | `status` (query) | Một trong 5 giá trị enum, đúng chữ hoa (V2) | Thiếu tham số (X5), rỗng `status=` (X6), giá trị lạ (X7), chữ thường (X8), có khoảng trắng (X9) |
| Doctor status | Authorization | Bearer hợp lệ, role `DOCTOR` (V3) | Không token (X10), token sai/hết hạn (X11), role `PATIENT` (X12), role `ADMIN` (X13) |
| Doctor status | Quyền sở hữu | Lịch hẹn thuộc bác sĩ trong token (V4) | Lịch hẹn của bác sĩ khác (X14; cần xác nhận có kiểm tra) |
| Doctor status | Chuyển trạng thái | Chuyển từ trạng thái hiện tại sang trạng thái cho phép (V5) | Từ `CANCELLED`/`COMPLETED` quay lại `CONFIRMED` (X15; cần chốt nghiệp vụ) |
| Patient delete | `appointmentId` (path) | Lịch hẹn tồn tại và thuộc bệnh nhân trong token (V1) | Thiếu/rỗng (X1), không phải số (X2), không tồn tại (X3), `0` hoặc âm (X4), lịch hẹn của bệnh nhân khác (X5) |
| Patient delete | Authorization | Bearer hợp lệ, role `PATIENT` (V2) | Không token (X6), token sai/hết hạn (X7), role `DOCTOR` (X8), role `ADMIN` (X9) |
| Patient delete | Trạng thái lịch hẹn | Lịch hẹn còn hiệu lực để xoá (V3) | Xoá lại lịch đã xoá/đã `CANCELLED` (X10; kỳ vọng `404`/`409` hoặc idempotent — chưa chốt) |

Ghi chú: hai API không có request body. Trường thừa trong body (nếu gửi) không thuộc phạm vi black-box của thiết kế này.

### 2.1 Số test case tối thiểu để phủ 100% lớp tương đương

Quy tắc: một test case hợp lệ có thể đồng thời đại diện lớp hợp lệ của **mọi** trường; mỗi test case không hợp lệ chỉ được chứa **một** giá trị không hợp lệ, các trường còn lại giữ giá trị hợp lệ.

Công thức: **TC_min = max(số lớp hợp lệ của một trường) + tổng số lớp không hợp lệ của tất cả trường**.

| API | Trường (lớp hợp lệ / không hợp lệ) | max lớp hợp lệ | Tổng lớp không hợp lệ | TC tối thiểu |
|---|---|---:|---:|---:|
| Doctor status | appointmentId (1/4), status (1/5), token (1/4), quyền sở hữu (1/1), chuyển trạng thái (1/1) | 1 | 15 | **16** |
| Patient delete | appointmentId (1/5), token (1/4), trạng thái lịch hẹn (1/1) | 1 | 10 | **11** |

Như vậy cần tối thiểu **16 + 11 = 27** test case để phủ 100% lớp tương đương. Phần dư nên dành cho kiểm tra hợp đồng response, độ bền dữ liệu và các tổ hợp RBAC — không lặp lại lớp đã phủ.

## 3. BVA — Bảng giá trị biên

| API / trường | Biên xác định được | Giá trị cần thử | Giới hạn chưa được đặc tả |
|---|---|---|---|
| Doctor status `appointmentId` | Kiểu `Long`, ID dương; biên dưới `0/1` | `0`, `1`, ID không tồn tại, `2147483647`, `9223372036854775808` | Không có `@Min/@Positive/@Max`; không suy đoán giới hạn nghiệp vụ |
| Doctor status `status` | Enum 5 giá trị; không có miền số | 5 giá trị hợp lệ + chuỗi rỗng, chuỗi lạ, khác hoa/thường | Không có ràng buộc độ dài; giá trị phải khớp chính xác tên enum |
| Patient delete `appointmentId` | `Long`; biên `0/1` | `0`, `1`, ID không tồn tại, Long max, tràn Long | Không có `@Min/@Positive` |
| Patient delete trạng thái | Trạng thái kết thúc: `CANCELLED`, `COMPLETED`, `NO_SHOW` | Xoá lần đầu, xoá lần hai, xoá lịch đã huỷ | Hành vi idempotent chưa chốt |
| Cả hai API | Không có body, không có trường độ dài | — | Không áp dụng BVA chuỗi/ngày giờ |

Các trường hợp không có ràng buộc nguồn được đánh dấu “chưa chốt”; không tạo giả định min/max.

### 3.1 Số test case tối thiểu cho BVA

| API | Miền và giá trị biên | Số giá trị biên | Đã phủ bởi EP | Case BVA bổ sung |
|---|---|---:|---:|---:|
| Doctor status | appointmentId {0, 1, Long max, tràn Long} | 4 | 2 | **2** (Long max, tràn Long) |
| Patient delete | appointmentId {0, 1, Long max, tràn Long} | 4 | 2 | **2** (Long max, tràn Long) |

`0` và `1` đã nằm trong lớp EP tương ứng, nên BVA chỉ thêm **4** case. Tràn `Long` kỳ vọng `400` do lỗi binding kiểu dữ liệu; cần xác nhận thực tế.

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

R2 tách thành hai case thực thi: không token (`401`) và sai role (`403`). R7 là ca hồi quy bắt buộc: đổi trạng thái phải trả về đúng lịch hẹn, không tạo bản ghi mới.

### 4.2 Xoá lịch hẹn (Patient)

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| C1 token role PATIENT hợp lệ | Y | N | Y | Y | Y |
| C2 appointmentId tồn tại | Y | - | N | Y | Y |
| C3 lịch hẹn thuộc bệnh nhân trong token | Y | - | - | N | Y |
| C4 lịch hẹn còn xoá được | Y | - | - | - | N |
| A1 xoá thành công, 200/204 | X | - | - | - | - |
| A2 từ chối xác thực/phân quyền, 401/403 | - | X | - | X | - |
| A3 không tìm thấy tài nguyên, 404 | - | - | X | - | - |
| A4 xung đột/đã xoá, 409 hoặc 404 | - | - | - | - | X |

R5 phụ thuộc quyết định idempotency của backend; nếu backend xoá mềm và trả `204` cho lần hai thì R5 thuộc lớp hợp lệ và phải sửa bảng trước khi chạy.

### 4.3 Số test case tối thiểu cho bảng quyết định

Mỗi luật của bảng quyết định tương ứng một test case, vì luật chính là một tổ hợp điều kiện cần được chứng minh.

| API | Số luật | TC tối thiểu nếu kiểm thử độc lập | TC phát sinh thêm khi gộp với EP |
|---|---:|---:|---:|
| Doctor status | 7 (R1–R7; R2 tách 401/403) | 8 | 0 |
| Patient delete | 5 | 5 | 0 |

Khi gộp với EP, các luật ánh xạ một-một vào lớp đã thiết kế nên không phát sinh case mới; bảng quyết định dùng để chứng minh không thiếu tổ hợp điều kiện.

## 5. Ma trận quyền (RBAC)

| Actor / tình huống | PUT doctor status | DELETE patient appointment | Kết quả dự kiến |
|---|---:|---:|---|
| Chưa xác thực | Được gọi | Được gọi | `401` |
| DOCTOR có token hợp lệ | Được phép theo role | Không được phép | `200` / `403` |
| PATIENT có token hợp lệ | Không được phép | Được phép theo role | `403` / `200` hoặc `204` |
| ADMIN có token hợp lệ | Không được phép theo annotation hiện tại | Không được phép | `403` |
| Token sai/hết hạn | Không được phép | Không được phép | `401` |
| DOCTOR cập nhật lịch hẹn của bác sĩ khác | Cần kiểm tra quyền sở hữu | — | Chưa xác định: `403` nếu có kiểm tra, `200` nếu chỉ kiểm tra role |
| PATIENT xoá lịch hẹn của bệnh nhân khác | — | Cần kiểm tra quyền sở hữu | Chưa xác định: `403`/`404` nếu có kiểm tra, `200` nếu không (lỗ hổng IDOR) |

Hai endpoint chỉ có ràng buộc role ở mức controller (`hasRole`); việc đối chiếu `appointmentId` với danh tính trong token phải kiểm thử riêng, không suy ra từ role. Đây là nhóm case ưu tiên cao nhất vì ảnh hưởng trực tiếp đến dữ liệu bệnh nhân.

## 6. Ma trận phủ kỹ thuật

| Nhóm kiểm thử | Doctor status (16) | Patient delete (11) |
|---|---|---|
| EP hợp lệ và không hợp lệ | appointmentId, status enum, token, sở hữu, chuyển trạng thái | appointmentId, token, trạng thái lịch hẹn |
| BVA có căn cứ | ID `0/1`, Long max, tràn Long | ID `0/1`, Long max, tràn Long |
| Decision table | Auth, tài nguyên, validation, xung đột, sở hữu | Auth, tài nguyên, sở hữu, idempotency |
| RBAC | Anonymous/DOCTOR/PATIENT/ADMIN, bác sĩ khác | Anonymous/PATIENT/DOCTOR/ADMIN, bệnh nhân khác |
| Kiểu dữ liệu / path | ID không phải số, ID rỗng | ID không phải số, ID rỗng |
| Hợp đồng response | `status` trong body khớp tham số, `updatedAt` đổi | Mã 200/204, body rỗng hoặc object, dữ liệu không còn truy vấn được |
| Tác động phụ | Không tạo appointment mới; danh sách của bác sĩ đổi trạng thái | Lịch hẹn biến mất khỏi danh sách bệnh nhân; không xoá bản ghi liên quan |

## 7. Tổng hợp test suite đề xuất

| File | Dải TC_ID | Số case | Nội dung |
|---|---|---:|---|
| `exports/testcases-doctor-appointment-status.csv` | TC-DAS-01..16 | 16+ | Đổi trạng thái, enum, ID biên, RBAC, sở hữu, chuyển trạng thái, hợp đồng response |
| `exports/testcases-patient-appointment-delete.csv` | TC-PDC-01..11 | 11+ | Xoá lịch hẹn, ID biên, RBAC, IDOR, idempotency, mã trả về |

Hai file CSV này chưa tồn tại trong workspace; đây là đề xuất cấu trúc. Cột tối thiểu phải khớp script hiện có của collection: `TestCase_ID`, `appointmentId`, `Expected_HTTP_Status`, `status` (chỉ API bác sĩ), và thêm `Kỳ vọng nội dung`, `Ghi chú`.

Tổng cộng tối thiểu **27 test case** cho EP, cộng 4 case BVA và các case hợp đồng response.

### 7.1 Bảng tổng hợp số test case tối thiểu

| API | EP tối thiểu | BVA bổ sung | Bảng quyết định (nếu riêng) | Tổng tối thiểu (đã gộp) | CSV hiện có | Kết luận |
|---|---:|---:|---:|---:|---:|---|
| Doctor status | 16 | 2 | 8 | **18** | 0 | Cần bổ sung toàn bộ |
| Patient delete | 11 | 2 | 5 | **13** | 0 | Cần bổ sung toàn bộ |
| **Tổng** | 27 | 4 | 13 | **31** | 0 | Chưa có CSV cho hai API này |

## 8. Cách thực thi và tiêu chí ghi nhận

1. Khởi chạy backend và DB test; xác nhận `baseUrl`, token DOCTOR/PATIENT, `doctorId`, `patientId` và tạo trước appointment cho từng case.
2. Đặt `Authorization: Bearer {{DOCTOR_TOKEN}}` hoặc `{{PATIENT_TOKEN}}`; hai API không gửi body, không cần `Content-Type`.
3. Với API bác sĩ, truyền `status` trên query string đúng như collection hiện tại. Nếu backend khai báo `@RequestBody`, request sẽ trả `400/415` — phải chốt hợp đồng trước khi chạy CSV.
4. Mỗi case thay đổi dữ liệu dùng một appointment mới; không tái sử dụng appointment đã `CANCELLED` cho case thành công.
5. So sánh status với `Expected_HTTP_Status`, rồi assert thêm: API bác sĩ phải trả `status` bằng giá trị gửi lên; API bệnh nhân phải xác nhận lịch hẹn không còn trong `GET /api/patient/appointments?patientId=`.
6. Với case bị từ chối, kiểm tra dữ liệu không đổi; với case IDOR, xác nhận không có thay đổi nào trên lịch hẹn của bệnh nhân khác.
7. Lưu status thực tế, response body, thời điểm và môi trường; ghi nhận khác biệt thay vì sửa kỳ vọng sau khi chạy mà không có căn cứ.

Các status `400/401/403/404` được dự kiến theo Spring Security và `GlobalExceptionHandler`; `409` chỉ áp dụng nếu backend có kiểm tra chuyển trạng thái. Cần xác nhận trên môi trường đích.

## 9. Chỗ đặc tả chưa chốt

- Chưa xác nhận `status` là query param hay JSON body. Collection dùng query; nếu controller dùng DTO body thì toàn bộ case của API bác sĩ sai hợp đồng.
- Chưa rõ mã thành công của `DELETE`: `200` kèm body, `204` không body, hay `200` kèm thông báo. Collection không lưu response mẫu.
- Chưa rõ backend xoá cứng, xoá mềm hay chuyển `status=CANCELLED`; điều này quyết định case xoá lần hai và cách kiểm chứng.
- Chưa xác nhận có kiểm tra quyền sở hữu: bác sĩ khác cập nhật lịch hẹn và bệnh nhân khác xoá lịch hẹn là hai lỗ hổng IDOR tiềm năng.
- Quy tắc chuyển trạng thái chưa được đặc tả: có cho phép `COMPLETED → CONFIRMED`, `CANCELLED → SCHEDULED`, cập nhật lịch hẹn trong quá khứ hay không.
- API `GET /api/patient/appointments/{appointmentId}` (xem chi tiết) không được kiểm thử ở collection nào; tên request `api/patient/appointments/{{appointmentId}}` lại là `DELETE`, dễ nhầm phạm vi.
- Biến `appointmentId` và `PATIENT_TOKEN` không khai báo trong `variable` của collection; chạy ngoài Runner sẽ gửi nguyên chuỗi `{{appointmentId}}`.
- Chưa có cơ chế chống gọi lặp (idempotency), audit log hay thông báo cho bệnh nhân khi bác sĩ đổi trạng thái.

## 10. Cách chạy từng file CSV bằng Postman/Newman

Collection `Khang Đàm` hiện là danh sách phẳng, không có folder, nên cần tách hai request vào folder `Appointments` trước khi chạy theo folder:

```bash
newman run postman/KhangDam.postman_collection.json -e env.json \
  -d exports/testcases-doctor-appointment-status.csv --folder "Appointments"
newman run postman/KhangDam.postman_collection.json -e env.json \
  -d exports/testcases-patient-appointment-delete.csv --folder "Appointments"
```

Nếu chưa tách folder, chạy từ Postman Runner và chọn đúng request, hoặc tạo collection riêng chỉ chứa request tương ứng — chạy cả collection với một file CSV sẽ gửi sai dữ liệu cho các request khác.

Script hiện tại đọc `pm.iterationData`, nên chỉ hoạt động trong Runner/Newman; chạy tay trong Postman app sẽ cho `expectedStatus = NaN` và fail. Trong test script, assert thêm nội dung:

```javascript
pm.test("Body status matches request", function () {
    pm.expect(pm.response.json().status).to.eql(pm.iterationData.get("status"));
});
```

Cấu hình `baseUrl`, `DOCTOR_TOKEN`, `PATIENT_TOKEN`, `appointmentId` trong `env.json`; mỗi iteration có thể override `appointmentId` và `status` từ CSV. Case cần seed đặc biệt (bác sĩ khác, bệnh nhân khác, lịch hẹn đã huỷ) phải chuẩn bị trước và ghi rõ trong cột `Ghi chú` là `Không` chạy được ngay.
