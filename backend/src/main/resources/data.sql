-- Insert seed departments
INSERT INTO departments (name, description, active, created_at, updated_at) 
VALUES 
('Y học tổng quát', 'Tư vấn và điều trị y tế tổng quát', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tim mạch', 'Khám và điều trị các bệnh tim mạch', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Orthopedics', 'Bone and joint treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pediatrics', 'Child healthcare', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Neurology', 'Brain and nervous system treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dermatology', 'Skin treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Move existing doctors and preserve their history before removing duplicate seed departments.
UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Y học tổng quát')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'General Medicine');

UPDATE doctors
SET department_id = (SELECT id FROM departments WHERE name = 'Tim mạch')
WHERE department_id IN (SELECT id FROM departments WHERE name = 'Cardiology');

DELETE FROM departments WHERE name IN ('General Medicine', 'Cardiology');