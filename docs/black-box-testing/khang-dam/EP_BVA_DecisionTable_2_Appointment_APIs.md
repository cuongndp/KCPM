# Thiết kế kiểm thử EP, BVA và Decision Table cho 2 API Appointment

**Dự án:** Hospital Management System

**Phạm vi:**
1. `PUT /api/doctor/appointments/{appointmentId}/status`
2. `DELETE /api/patient/appointments/{appointmentId}`

> **Nguồn thiết kế:** Hai Postman Collection đã cung cấp. Collection xác nhận method, URL, tham số, Bearer Token và một số phản hồi mẫu. Chưa có source Controller/Service/DTO hoặc tài liệu đầy đủ về quy tắc chuyển trạng thái. Nội dung chưa được chứng minh được đánh dấu **TBD/Cần xác minh**, không tự đặt giới hạn hay nghiệp vụ.
>
> **Quy ước:** EP dùng `V` (hợp lệ), `X` (không hợp lệ). BVA dùng `B` theo vị trí biên. Decision Table dùng `C` (Condition), `A` (Action), `R` (Rule); `Y` = có/đúng, `N` = không/sai, `-` = không xét.

---

# 1. PUT /api/doctor/appointments/{appointmentId}/status

## 1.1. Thông tin API và dữ liệu đầu vào

- **Method:** `PUT`
- **URL:** `/api/doctor/appointments/{appointmentId}/status?status={status}`
- **Authorization:** Bearer Token `{{DOCTOR_TOKEN}}`.
- **Path Parameter:** `appointmentId` — ID lịch hẹn.
- **Query Parameter:** `status` — trạng thái muốn cập nhật.
- **Body:** Không có trong request mẫu.
- **Response mẫu:** `200 OK`, dữ liệu trả về có trạng thái `CONFIRMED`.

Collection chưa cung cấp danh sách enum trạng thái, quy tắc chuyển trạng thái, điều kiện lịch hẹn thuộc bác sĩ hiện tại hoặc status code cho từng lỗi.

## 1.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ / cần kiểm tra | Tag |
|---|---|---|---|---|
| `appointmentId` | ID lịch hẹn tồn tại và bác sĩ có quyền cập nhật | V1 | Không tồn tại; không phải số; rỗng/thiếu; bằng 0/âm; lịch không thuộc bác sĩ hiện tại | X1–X5 |
| `status` | Trạng thái backend hỗ trợ và cho phép chuyển từ trạng thái hiện tại | V2 | Trạng thái không hỗ trợ; rỗng; thiếu; null; chuyển đổi không được phép | X6–X10 |
| Authorization | Bearer Token hợp lệ có quyền bác sĩ | V3 | Thiếu token; token sai/hết hạn; token không có role/quyền phù hợp | X11–X13 |

**Lưu ý:** Collection chỉ có ví dụ `status=CONFIRMED`; không chứng minh đây là toàn bộ enum. ID 0/âm và quyền sở hữu cần đối chiếu source.

## 1.3. Bảng phân tích giá trị biên (BVA)

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| `appointmentId` | Chưa quy định | Chưa quy định | Chưa quy định | ID lịch hẹn tồn tại (ví dụ `1`) | Chưa quy định | Chưa quy định | Chưa quy định | B1–B7: chưa xác định miền số |
| `status` | Không áp dụng biên số | Không áp dụng | Không áp dụng | `CONFIRMED` (giá trị mẫu) | Chưa quy định | Chưa quy định | Chưa quy định | B8–B14: chưa xác định enum/độ dài |

Có thể kiểm tra ID thiếu/rỗng/sai kiểu/0/âm và status thiếu/rỗng/null/không hỗ trợ như kiểm thử EP. Đây không phải min/max đã được đặc tả. Không tự kết luận ID nhỏ nhất là 1 hoặc status có độ dài tối đa.

## 1.4. Bảng quyết định (Decision Table)

**Conditions / Actions**

- C1: Bearer Token hợp lệ và có quyền bác sĩ?
- C2: `appointmentId` hợp lệ và lịch hẹn tồn tại?
- C3: Bác sĩ hiện tại có quyền cập nhật lịch hẹn?
- C4: `status` thuộc tập trạng thái backend hỗ trợ?
- C5: Chuyển từ trạng thái hiện tại sang trạng thái yêu cầu có được phép?
- A1: Cập nhật trạng thái thành công.
- A2: Từ chối xác thực/phân quyền.
- A3: Từ chối dữ liệu đầu vào.
- A4: Không tìm thấy lịch hẹn.
- A5: Từ chối chuyển trạng thái theo nghiệp vụ.

| Condition / Action | R1 | R2 | R3 | R4 | R5 | R6 |
|---|---:|---:|---:|---:|---:|---:|
| C1. Token bác sĩ hợp lệ | Y | N | Y | Y | Y | Y |
| C2. ID hợp lệ, lịch hẹn tồn tại | Y | - | N | Y | Y | Y |
| C3. Bác sĩ có quyền cập nhật | Y | - | - | N | Y | Y |
| C4. Status được hỗ trợ | Y | - | - | - | N | Y |
| C5. Chuyển trạng thái được phép | Y | - | - | - | - | N |
| A1. Cập nhật thành công | X | - | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | X | - | - |
| A3. Từ chối dữ liệu đầu vào | - | - | X | - | X | - |
| A4. Không tìm thấy lịch hẹn | - | - | X* | - | - | - |
| A5. Từ chối chuyển trạng thái | - | - | - | - | - | X |

`X*`: ID không tồn tại có thể được xử lý thành lỗi không tìm thấy; mã HTTP cần xác nhận. ID sai định dạng có thể bị từ chối ở bước binding.

**Diễn giải Rules:**
- R1: Token hợp lệ, lịch tồn tại, bác sĩ có quyền, status hỗ trợ và chuyển đổi hợp lệ → cập nhật.
- R2: Token thiếu/sai → từ chối.
- R3: ID không hợp lệ/không tìm thấy → không cập nhật.
- R4: Người gọi không có quyền cập nhật lịch → từ chối; status cụ thể cần xác minh.
- R5: Status không thuộc tập giá trị backend hỗ trợ → từ chối.
- R6: Status được hỗ trợ nhưng chuyển đổi từ trạng thái hiện tại không hợp lệ → từ chối.

**Cần xác minh:** danh sách trạng thái, ma trận chuyển đổi, status code từng lỗi và cách kiểm tra quyền sở hữu lịch hẹn.

---

# 2. DELETE /api/patient/appointments/{appointmentId}

## 2.1. Thông tin API và dữ liệu đầu vào

- **Method:** `DELETE`
- **URL:** `/api/patient/appointments/{appointmentId}`
- **Authorization:** Bearer Token `{{PATIENT_TOKEN}}`.
- **Path Parameter:** `appointmentId` — ID lịch hẹn cần hủy.
- **Query Parameter:** Không có trong request mẫu.
- **Body:** Không có trong request mẫu.
- **Mô tả collection:** Hủy lịch hẹn bằng cách chuyển trạng thái sang `CANCELLED`, không phải xóa cứng bản ghi.
- **Response thành công mẫu:** `200 OK`, nội dung `Appointment cancelled successfully`.
- **Response không tìm thấy:** `404 Not Found` nếu `appointmentId` không tồn tại.

## 2.2. Bảng phân vùng tương đương (EP)

| Biến đầu vào | Lớp hợp lệ | Tag | Lớp không hợp lệ / cần kiểm tra | Tag |
|---|---|---|---|---|
| `appointmentId` | ID lịch hẹn tồn tại và bệnh nhân hiện tại được phép hủy | V1 | Không tồn tại; không phải số; rỗng/thiếu; bằng 0/âm; lịch không thuộc bệnh nhân hiện tại | X1–X5 |
| Authorization | Bearer Token hợp lệ của bệnh nhân | V2 | Thiếu token; token sai/hết hạn; token không có role/quyền bệnh nhân | X6–X8 |
| Trạng thái hiện tại | Lịch hẹn đang ở trạng thái cho phép hủy | V3 | Đã hủy hoặc trạng thái không cho phép hủy; quy tắc cụ thể chưa có trong collection | X9 |

Collection xác nhận 404 cho ID không tồn tại và 200 cho thao tác hủy mẫu. Chưa nêu trạng thái nào được phép hủy, thời hạn hủy hoặc response khi hủy lịch không thuộc bệnh nhân/đã hủy.

## 2.3. Bảng phân tích giá trị biên (BVA)

| Biến đầu vào | min- | min | min+ | nominal | max- | max | max+ | Tag biên |
|---|---|---|---|---|---|---|---|---|
| `appointmentId` | Chưa quy định | Chưa quy định | Chưa quy định | ID lịch hẹn tồn tại (ví dụ `1`) | Chưa quy định | Chưa quy định | Chưa quy định | B1–B7: chưa xác định miền số |

Thử path parameter thiếu/rỗng/sai kiểu/0/âm là kiểm thử EP/kiểu dữ liệu. Không tự đặt min=1 hoặc max. Hủy lần thứ hai và lịch không thuộc bệnh nhân là kiểm thử nghiệp vụ/quyền, không phải BVA số học.

## 2.4. Bảng quyết định (Decision Table)

**Conditions / Actions**

- C1: Bearer Token hợp lệ và có quyền bệnh nhân?
- C2: `appointmentId` hợp lệ và lịch hẹn tồn tại?
- C3: Lịch hẹn thuộc bệnh nhân đang đăng nhập?
- C4: Trạng thái hiện tại cho phép hủy?
- A1: Chuyển trạng thái sang `CANCELLED`, trả HTTP 200.
- A2: Từ chối xác thực/phân quyền.
- A3: Từ chối dữ liệu đầu vào.
- A4: Trả 404 khi không tìm thấy lịch hẹn.
- A5: Từ chối hủy theo nghiệp vụ.

| Condition / Action | R1 | R2 | R3 | R4 | R5 |
|---|---:|---:|---:|---:|---:|
| C1. Token bệnh nhân hợp lệ | Y | N | Y | Y | Y |
| C2. ID hợp lệ, lịch tồn tại | Y | - | N | Y | Y |
| C3. Lịch thuộc bệnh nhân | Y | - | - | N | Y |
| C4. Trạng thái cho phép hủy | Y | - | - | - | N |
| A1. Chuyển sang CANCELLED, 200 | X | - | - | - | - |
| A2. Từ chối xác thực/phân quyền | - | X | - | X | - |
| A3. Từ chối dữ liệu đầu vào | - | - | X | - | - |
| A4. Không tìm thấy, 404 | - | - | X* | - | - |
| A5. Từ chối theo nghiệp vụ | - | - | - | - | X |

`X*`: Collection nêu rõ ID không tồn tại trả 404. ID sai định dạng/thiếu cần xác nhận response thực tế.

**Diễn giải Rules:**
- R1: Token hợp lệ, lịch tồn tại, thuộc bệnh nhân và được phép hủy → chuyển sang `CANCELLED`, trả 200.
- R2: Token thiếu/sai/hết hạn → từ chối.
- R3: Lịch hẹn không tồn tại → 404 theo collection.
- R4: Lịch không thuộc bệnh nhân đang đăng nhập → từ chối; status cụ thể cần xác minh.
- R5: Trạng thái hiện tại không cho phép hủy (ví dụ đã hủy trước đó) → cần xác minh backend.

---

# 3. Tổng kết phạm vi EP, BVA và Decision Table

| API | EP | BVA | Decision Table |
|---|---|---|---|
| PUT `/api/doctor/appointments/{appointmentId}/status` | appointmentId, status, token/role | Chưa công bố giới hạn min/max ID hoặc enum/độ dài status | Token, lịch tồn tại, quyền bác sĩ, status và chuyển đổi |
| DELETE `/api/patient/appointments/{appointmentId}` | appointmentId, token/role, trạng thái lịch | Chưa công bố giới hạn min/max ID | Token, lịch tồn tại, thuộc bệnh nhân, trạng thái cho phép hủy |

# 4. Những điểm cần xác nhận khi chạy Postman

1. Đặt `{{baseUrl}}` thành địa chỉ backend thực tế, ví dụ `http://localhost:8080`.
2. Thiết lập `DOCTOR_TOKEN` và `PATIENT_TOKEN` bằng token test hợp lệ; không ghi token thật vào CSV hoặc Git.
3. Dùng appointment ID tồn tại và đúng chủ thể cho test thành công.
4. Với PUT, xác nhận enum trạng thái và ma trận chuyển trạng thái từ source.
5. Với DELETE, xác nhận điều kiện hủy (chủ sở hữu, trạng thái hiện tại, thời hạn nếu có).
6. Status chưa được collection xác nhận được ghi `TBD` trong CSV. Thay bằng expected status đã xác minh trước khi đánh giá Pass/Fail.
7. Không dùng dữ liệu bệnh nhân thật trong evidence; che token và thông tin cá nhân trước khi commit.

# 5. Cấu trúc file kiểm thử và bằng chứng

- `PUT_doctor_appointment_status.csv`: dữ liệu kiểm thử API cập nhật trạng thái lịch hẹn.
- `DELETE_patient_appointment.csv`: dữ liệu kiểm thử API bệnh nhân hủy lịch hẹn.
- `evidence/`: ảnh Postman theo TestCaseID.
- `results/`: JSON export từ Postman Collection Runner sau khi chạy thật.

CSV chứa `TBD` ở những expected status chưa có căn cứ. Không chạy đối chiếu Pass/Fail tự động cho các dòng này trước khi xác minh và điền status cụ thể.

## 6. Bằng chứng kiểm thử

| Test Case | Tên ảnh đề xuất |
|---|---|
| PUT | `evidence/PUT-001.png`|
| DEL| `evidence/DEL-001.png` |

Mỗi ảnh nên thể hiện method/URL, tham số cần thiết, status code và response. Che Bearer Token, thông tin cá nhân và dữ liệu nhạy cảm. Sau khi chạy Collection Runner, export JSON vào `results/`. Chỉ ghi Actual Status, Pass/Fail sau khi có kết quả chạy thật.
