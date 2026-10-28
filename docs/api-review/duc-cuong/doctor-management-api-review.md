Doctor Management API Review

1. Mục tiêu

Rà soát ban đầu hai API quản lý bác sĩ bằng Postman.

GET /api/admin/doctors

POST /api/admin/doctors

2. Môi trường kiểm thử

Công cụ: Postman

Base URL: http://localhost:8080

Ngày kiểm thử: 28/10/2026

3. Kết quả GET /api/admin/doctors

Test case

Status code thực tế

Kết quả

GET có token hợp lệ

Điền kết quả

Ghi response thực tế

GET không có token

Điền kết quả

Ghi response thực tế

4. Kết quả POST /api/admin/doctors

Test case

Status code thực tế

Kết quả

Tài khoản đã có hồ sơ bác sĩ

409

API trả thông báo tài khoản đã có hồ sơ bác sĩ

POST không có token

Điền kết quả

Ghi response thực tế

POST thiếu userId

Điền kết quả

Ghi response thực tế

5. Bằng chứng

evidence/GET-001-valid-token.png

evidence/GET-002-no-token.png

evidence/POST-002-existing-doctor-profile.png

evidence/POST-003-no-token.png

evidence/POST-004-missing-required-field.png

6. Nhận xét

Kết quả kiểm thử được ghi nhận theo phản hồi thực tế của API. Cần đối chiếu tài liệu hoặc yêu cầu nghiệp vụ trước khi kết luận API có lỗi.

7. Kết luận

Hoàn thiện kết luận sau khi các trường hợp kiểm thử đã được thực hiện và bằng chứng được lưu đầy đủ.
