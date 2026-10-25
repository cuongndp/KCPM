-- Khoi tao danh muc khoa bang tieng Viet; cac bang nghiep vu khac de nguoi dung tu tao.
INSERT INTO departments (name, description, active, created_at, updated_at)
VALUES
('Y học tổng quát', 'Khám và tư vấn sức khỏe tổng quát', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tim mạch', 'Khám và điều trị bệnh tim mạch', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chỉnh hình', 'Khám và điều trị bệnh về xương khớp', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Nhi khoa', 'Chăm sóc và điều trị sức khỏe trẻ em', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Thần kinh', 'Khám và điều trị bệnh về thần kinh', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Da liễu', 'Khám và điều trị bệnh về da', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tai Mũi Họng', 'Khám và điều trị bệnh về tai, mũi và họng', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Chuyen lien ket bac si sang ten khoa tieng Viet truoc khi xoa khoa tieng Anh trung lap.
UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Y học tổng quát')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'General Medicine');

UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Tim mạch')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'Cardiology');

UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Chỉnh hình')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'Orthopedics');

UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Nhi khoa')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'Pediatrics');

UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Thần kinh')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'Neurology');

UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Da liễu')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'Dermatology');

DELETE FROM departments
WHERE name IN ('General Medicine', 'Cardiology', 'Orthopedics', 'Pediatrics', 'Neurology', 'Dermatology');