import React, { useState, useEffect } from 'react';
import { Layout, Menu, Card, Button, Modal, Form, Input, DatePicker, Select, message, Tabs } from 'antd';
import { 
  DashboardOutlined, 
  CalendarOutlined, 
  FileTextOutlined, 
  MedicineBoxOutlined,
  LogoutOutlined 
} from '@ant-design/icons';
import { useNavigate, Outlet } from 'react-router-dom';
import { authService } from '../services/authService';
import { appointmentService } from '../services/appointmentService';
import { patientService } from '../services/patientService';
import { doctorService } from '../services/doctorService';
import { formatLocalDate, formatLocalDateTime } from '../utils/dateTime';

const { Header, Content, Sider } = Layout;
const { Option } = Select;

const PatientDashboard = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [appointments, setAppointments] = useState([]);
  const [medicalHistory, setMedicalHistory] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [patientId, setPatientId] = useState(null);
  const [bookingModalVisible, setBookingModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const selectedDepartmentId = Form.useWatch('departmentId', form);
  const navigate = useNavigate();

  const user = authService.getCurrentUser();

  useEffect(() => {
    loadPatientData();
  }, []);

  useEffect(() => {
    if (!selectedDepartmentId) {
      setDoctors([]);
      return undefined;
    }

    let isCurrentDepartment = true;
    setDoctors([]);
    doctorService.getAvailableDoctors(selectedDepartmentId)
      .then(availableDoctors => {
        if (isCurrentDepartment) setDoctors(availableDoctors);
      })
      .catch(() => {
        if (isCurrentDepartment) message.error('Không thể tải danh sách bác sĩ của khoa');
      });

    return () => {
      isCurrentDepartment = false;
    };
  }, [selectedDepartmentId]);

  const loadPatientData = async () => {
    try {
      setLoading(true);
      const patient = await patientService.getPatientProfile(user.userId);
      setPatientId(patient.id);
      const results = await Promise.allSettled([
        appointmentService.getPatientAppointments(patient.id),
        patientService.getMedicalHistory(patient.id),
        patientService.getPrescriptions(patient.id),
        doctorService.getActiveDepartments()
      ]);
      const setters = [setAppointments, setMedicalHistory, setPrescriptions, setDepartments];
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          setters[index](result.value);
        } else {
          message.error(result.reason?.response?.data?.message || 'Không thể tải dữ liệu bệnh nhân');
        }
      });
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể tải hồ sơ bệnh nhân');
    } finally {
      setLoading(false);
    }
  };

  const handleBookAppointment = async (values) => {
    try {
      await appointmentService.bookAppointment(patientId, {
        ...values,
        appointmentDateTime: values.appointmentDateTime.format('YYYY-MM-DDTHH:mm:ss')
      });
      message.success('Đã đặt lịch hẹn thành công');
      setBookingModalVisible(false);
      form.resetFields();
      loadPatientData();
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể đặt lịch hẹn');
    }
  };

  const handleCancelAppointment = async (appointmentId) => {
    try {
      await appointmentService.cancelAppointment(appointmentId);
      message.success('Đã hủy lịch hẹn');
      loadPatientData();
    } catch (error) {
      message.error('Không thể hủy lịch hẹn');
    }
  };

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  const menuItems = [
    { key: '1', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '2', icon: <CalendarOutlined />, label: 'Lịch hẹn' },
    { key: '3', icon: <FileTextOutlined />, label: 'Lịch sử khám' },
    { key: '4', icon: <MedicineBoxOutlined />, label: 'Đơn thuốc' },
    { key: '5', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: handleLogout },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ height: 32, margin: 16, background: 'rgba(255, 255, 255, 0.2)' }} />
        <Menu theme="dark" mode="inline" items={menuItems} />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: '#fff' }}>
          <h2 style={{ padding: '0 24px', margin: 0 }}>Bảng điều khiển bệnh nhân</h2>
        </Header>
        <Content style={{ margin: '16px' }}>
          <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
            <Tabs defaultActiveKey="1">
              <Tabs.TabPane tab="Tổng quan" key="1">
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 16 }}>
                  <Card title="Tổng số lịch hẹn" bordered={false}>
                    <h3>{appointments.length}</h3>
                  </Card>
                  <Card title="Hồ sơ y tế" bordered={false}>
                    <h3>{medicalHistory.length}</h3>
                  </Card>
                  <Card title="Đơn thuốc đang dùng" bordered={false}>
                    <h3>{prescriptions.length}</h3>
                  </Card>
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane tab="Lịch hẹn" key="2">
                <Button 
                  type="primary" 
                  onClick={() => setBookingModalVisible(true)}
                  style={{ marginBottom: 16 }}
                >
                  Đặt lịch hẹn mới
                </Button>
                <div style={{ display: 'grid', gap: 16 }}>
                  {appointments.map(appointment => (
                    <Card key={appointment.id} title={`Lịch hẹn với BS. ${appointment.doctor?.user?.firstName}`}>
                      <p><strong>Thời gian:</strong> {formatLocalDateTime(appointment.appointmentDateTime)}</p>
                      <p><strong>Trạng thái:</strong> {appointment.status}</p>
                      <p><strong>Lý do:</strong> {appointment.reason}</p>
                      {appointment.status === 'SCHEDULED' && (
                        <Button 
                          danger 
                          onClick={() => handleCancelAppointment(appointment.id)}
                        >
                          Hủy lịch
                        </Button>
                      )}
                    </Card>
                  ))}
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane tab="Lịch sử khám" key="3">
                <div style={{ display: 'grid', gap: 16 }}>
                  {medicalHistory.map(record => (
                    <Card key={record.id} title={`Lần khám ngày ${formatLocalDate(record.createdAt)}`}>
                      <p><strong>Bác sĩ:</strong> BS. {record.doctor?.user?.firstName}</p>
                      <p><strong>Chẩn đoán:</strong> {record.diagnosis}</p>
                      <p><strong>Triệu chứng:</strong> {record.symptoms}</p>
                      <p><strong>Ghi chú:</strong> {record.consultationNotes}</p>
                    </Card>
                  ))}
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane tab="Đơn thuốc" key="4">
                <div style={{ display: 'grid', gap: 16 }}>
                  {prescriptions.map(prescription => (
                    <Card key={prescription.id} title={prescription.medication}>
                      <p><strong>Bác sĩ:</strong> BS. {prescription.doctor?.user?.firstName}</p>
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
        title="Đặt lịch hẹn"
        open={bookingModalVisible}
        onCancel={() => setBookingModalVisible(false)}
        footer={null}
      >
        <Form
          form={form}
          onFinish={handleBookAppointment}
          layout="vertical"
        >
          <Form.Item
            name="departmentId"
            label="Chọn khoa"
            rules={[{ required: true, message: 'Vui lòng chọn khoa' }]}
          >
            <Select placeholder="Chọn khoa">
              {departments.map(department => (
                <Option key={department.id} value={department.id}>
                  {department.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="doctorId"
            label="Chọn bác sĩ"
            rules={[{ required: true, message: 'Vui lòng chọn bác sĩ' }]}
          >
            <Select
              placeholder={selectedDepartmentId ? 'Chọn bác sĩ' : 'Vui lòng chọn khoa trước'}
              disabled={!selectedDepartmentId}
              notFoundContent={selectedDepartmentId ? 'Khoa chưa có bác sĩ khả dụng' : 'Vui lòng chọn khoa trước'}
            >
              {doctors.map(doctor => (
                <Option key={doctor.id} value={doctor.id}>
                  BS. {doctor.user?.firstName} {doctor.user?.lastName} - {doctor.specialization}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="appointmentDateTime"
            label="Ngày và giờ khám"
            rules={[{ required: true, message: 'Vui lòng chọn ngày và giờ khám' }]}
          >
            <DatePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            name="reason"
            label="Lý do khám"
            rules={[{ required: true, message: 'Vui lòng nhập lý do khám' }]}
          >
            <Input.TextArea rows={3} />
          </Form.Item>

          <Form.Item
            name="notes"
            label="Ghi chú thêm"
          >
            <Input.TextArea rows={2} />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              Đặt lịch hẹn
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
};

export default PatientDashboard;