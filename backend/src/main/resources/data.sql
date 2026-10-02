-- Initialize database with default department
INSERT INTO departments (name, description, active, created_at, updated_at) 
VALUES ('General Medicine', 'General medical consultations and treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Insert sample departments
INSERT INTO departments (name, description, active, created_at, updated_at) 
VALUES 
('Cardiology', 'Heart and cardiovascular treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Orthopedics', 'Bone and joint treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pediatrics', 'Child healthcare', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Neurology', 'Brain and nervous system treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dermatology', 'Skin treatments', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;