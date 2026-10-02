import React, { useState, useEffect } from 'react';
import { Layout, Menu, Card, Button, Modal, Form, Input, Select, message, Tabs, Table } from 'antd';
import { 
  DashboardOutlined, 
  CalendarOutlined, 
  FileTextOutlined, 
  MedicineBoxOutlined,
  LogoutOutlined 
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import { appointmentService } from '../services/appointmentService';
import { doctorService } from '../services/doctorService';
import { formatLocalDateTime } from '../utils/dateTime';

const { Header, Content, Sider } = Layout;
const { Option } = Select;

const DoctorDashboard = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [appointments, setAppointments] = useState([]);
  const [upcomingAppointments, setUpcomingAppointments] = useState([]);
  const [medicalRecords, setMedicalRecords] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [patients, setPatients] = useState([]);
  const [doctorId, setDoctorId] = useState(null);
  const [medicalRecordModalVisible, setMedicalRecordModalVisible] = useState(false);
  const [prescriptionModalVisible, setPrescriptionModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [medicalRecordForm] = Form.useForm();
  const [prescriptionForm] = Form.useForm();
  const navigate = useNavigate();

  const user = authService.getCurrentUser();

  useEffect(() => {
    loadDoctorData();
  }, []);

  const loadDoctorData = async () => {
    try {
      setLoading(true);
      const doctor = await doctorService.getDoctorProfile(user.userId);
      setDoctorId(doctor.id);
      const results = await Promise.allSettled([
        appointmentService.getDoctorAppointments(doctor.id),
        appointmentService.getUpcomingAppointments(doctor.id),
        doctorService.getMedicalRecords(doctor.id),
        doctorService.getPrescriptions(doctor.id)
      ]);
      const setters = [setAppointments, setUpcomingAppointments, setMedicalRecords, setPrescriptions];
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          setters[index](result.value);
        } else {
          message.error(result.reason?.response?.data?.message || 'Không thể tải dữ liệu bác sĩ');
        }
      });

      const appointmentResult = results[0];
      const patientsFromAppointments = appointmentResult.status === 'fulfilled'
        ? Array.from(
            new Map(
              appointmentResult.value
                .filter(appointment => appointment.patient)
                .map(appointment => [appointment.patient.id, appointment.patient])
            ).values()
          )
        : [];
      setPatients(patientsFromAppointments);
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể tải hồ sơ bác sĩ');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateMedicalRecord = async (values) => {
    try {
      await doctorService.createMedicalRecord(doctorId, values);
      message.success('Đã tạo hồ sơ y tế thành công');
      setMedicalRecordModalVisible(false);
      medicalRecordForm.resetFields();
      loadDoctorData();
    } catch (error) {
      message.error('Không thể tạo hồ sơ y tế');
    }
  };

  const handleCreatePrescription = async (values) => {
    try {
      await doctorService.createPrescription(doctorId, values);
      message.success('Đã tạo đơn thuốc thành công');
      setPrescriptionModalVisible(false);
      prescriptionForm.resetFields();
      loadDoctorData();
    } catch (error) {
      message.error('Không thể tạo đơn thuốc');
    }
  };

  const handleUpdateAppointmentStatus = async (appointmentId, status) => {
    try {
      await appointmentService.updateAppointmentStatus(appointmentId, status);
      message.success('Đã cập nhật trạng thái cuộc hẹn');
      loadDoctorData();
    } catch (error) {
      message.error('Không thể cập nhật trạng thái cuộc hẹn');
    }
  };

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  const menuItems = [
    { key: '1', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '2', icon: <CalendarOutlined />, label: 'Cuộc hẹn' },
    { key: '3', icon: <FileTextOutlined />, label: 'Hồ sơ y tế' },
    { key: '4', icon: <MedicineBoxOutlined />, label: 'Đơn thuốc' },
    { key: '5', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: handleLogout },
  ];

  const appointmentColumns = [
    { title: 'Bệnh nhân', dataIndex: ['patient', 'user', 'firstName'], key: 'patient' },
    { title: 'Thời gian', dataIndex: 'appointmentDateTime', key: 'date', render: formatLocalDateTime },
    { title: 'Trạng thái', dataIndex: 'status', key: 'status' },
    { title: 'Lý do', dataIndex: 'reason', key: 'reason' },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <div>
          {['SCHEDULED', 'CONFIRMED'].includes(record.status) && (
            <Button
              size="small"
              type={record.status === 'CONFIRMED' ? 'primary' : 'default'}
              onClick={() => handleUpdateAppointmentStatus(
                record.id,
                record.status === 'SCHEDULED' ? 'CONFIRMED' : 'COMPLETED'
              )}
            >
              {record.status === 'SCHEDULED' ? 'Xác nhận' : 'Hoàn tất'}
            </Button>
          )}
        </div>
      )
    }
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ height: 32, margin: 16, background: 'rgba(255, 255, 255, 0.2)' }} />
        <Menu theme="dark" mode="inline" items={menuItems} />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: '#fff' }}>
          <h2 style={{ padding: '0 24px', margin: 0 }}>Bảng điều khiển bác sĩ</h2>
        </Header>
        <Content style={{ margin: '16px' }}>
          <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
            <Tabs defaultActiveKey="1">
              <Tabs.TabPane tab="Tổng quan" key="1">
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 16 }}>
                  <Card title="Cuộc hẹn hôm nay" bordered={false}>
                    <h3>{upcomingAppointments.length}</h3>
                  </Card>
                  <Card title="Tổng số cuộc hẹn" bordered={false}>
                    <h3>{appointments.length}</h3>
                  </Card>
                  <Card title="Hồ sơ y tế" bordered={false}>
                    <h3>{medicalRecords.length}</h3>
                  </Card>
                  <Card title="Đơn thuốc" bordered={false}>
                    <h3>{prescriptions.length}</h3>
                  </Card>
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane tab="Cuộc hẹn" key="2">
                <Table
                  dataSource={appointments}
                  columns={appointmentColumns}
                  rowKey="id"
                  loading={loading}
                />
              </Tabs.TabPane>

              <Tabs.TabPane tab="Hồ sơ y tế" key="3">
                <Button 
                  type="primary" 
                  onClick={() => setMedicalRecordModalVisible(true)}
                  style={{ marginBottom: 16 }}
                >
                  Tạo hồ sơ y tế
                </Button>
                <div style={{ display: 'grid', gap: 16 }}>
                  {medicalRecords.map(record => (
                      <Card key={record.id} title={`Bệnh nhân: ${record.patient?.user?.firstName}`}>
                      <p><strong>Chẩn đoán:</strong> {record.diagnosis}</p>
                      <p><strong>Triệu chứng:</strong> {record.symptoms}</p>
                      <p><strong>Ghi chú tư vấn:</strong> {record.consultationNotes}</p>
                      <p><strong>Kết quả xét nghiệm:</strong> {record.labResults}</p>
                      <p><strong>Khuyến nghị:</strong> {record.recommendations}</p>
                    </Card>
                  ))}
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane tab="Đơn thuốc" key="4">
                <Button 
                  type="primary" 
                  onClick={() => setPrescriptionModalVisible(true)}
                  style={{ marginBottom: 16 }}
                >
                  Tạo đơn thuốc
                </Button>
                <div style={{ display: 'grid', gap: 16 }}>
                  {prescriptions.map(prescription => (
                    <Card key={prescription.id} title={prescription.medication}>
                      <p><strong>Bệnh nhân:</strong> {prescription.patient?.user?.firstName}</p>
                      <p><strong>Liều lượng:</strong> {prescription.dosage}</p>
                      <p><strong>Tần suất:</strong> {prescription.frequency}</p>
                      <p><strong>Thời lượng:</strong> {prescription.duration} ngày</p>
                      <p><strong>Hướng dẫn:</strong> {prescription.instructions}</p>
                    </Card>
                  ))}
                </div>
              </Tabs.TabPane>

            </Tabs>
          </div>
        </Content>
      </Layout>

      <Modal
        title="Tạo hồ sơ y tế"
        open={medicalRecordModalVisible}
        onCancel={() => setMedicalRecordModalVisible(false)}
        footer={null}
      >
        <Form form={medicalRecordForm} onFinish={handleCreateMedicalRecord} layout="vertical">
          <Form.Item name="patientId" label="Bệnh nhân" rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}>
            <Select placeholder="Chọn bệnh nhân" showSearch optionFilterProp="label">
              {patients.map(patient => (
                <Option
                  key={patient.id}
                  value={patient.id}
                  label={`${patient.id} ${patient.user?.firstName || ''} ${patient.user?.lastName || ''}`}
                >
                  #{patient.id} - {patient.user?.firstName} {patient.user?.lastName}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="diagnosis" label="Chẩn đoán" rules={[{ required: true }]}>
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="symptoms" label="Triệu chứng">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="consultationNotes" label="Ghi chú tư vấn">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="labResults" label="Kết quả xét nghiệm">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="recommendations" label="Khuyến nghị">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>Lưu hồ sơ</Button>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Tạo đơn thuốc"
        open={prescriptionModalVisible}
        onCancel={() => setPrescriptionModalVisible(false)}
        footer={null}
      >
        <Form form={prescriptionForm} onFinish={handleCreatePrescription} layout="vertical">
          <Form.Item name="patientId" label="Bệnh nhân" rules={[{ required: true, message: 'Vui lòng chọn bệnh nhân' }]}>
            <Select placeholder="Chọn bệnh nhân" showSearch optionFilterProp="label">
              {patients.map(patient => (
                <Option
                  key={patient.id}
                  value={patient.id}
                  label={`${patient.id} ${patient.user?.firstName || ''} ${patient.user?.lastName || ''}`}
                >
                  #{patient.id} - {patient.user?.firstName} {patient.user?.lastName}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="appointmentId" label="Cuộc hẹn" rules={[{ required: true, message: 'Vui lòng chọn cuộc hẹn' }]}>
            <Select placeholder="Chọn cuộc hẹn" showSearch optionFilterProp="label">
              {appointments.map(appointment => (
                <Option
                  key={appointment.id}
                  value={appointment.id}
                  label={`#${appointment.id} ${appointment.patient?.user?.firstName || ''} ${appointment.patient?.user?.lastName || ''}`}
                >
                  #{appointment.id} - {appointment.patient?.user?.firstName} {appointment.patient?.user?.lastName} - {formatLocalDateTime(appointment.appointmentDateTime)}
                </Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="medication" label="Thuốc" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="dosage" label="Liều lượng" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="frequency" label="Tần suất" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="duration" label="Thời lượng (ngày)">
            <Input type="number" />
          </Form.Item>
          <Form.Item name="instructions" label="Hướng dẫn sử dụng">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>Lưu đơn thuốc</Button>
          </Form.Item>
        </Form>
      </Modal>

    </Layout>
  );
};

export default DoctorDashboard;