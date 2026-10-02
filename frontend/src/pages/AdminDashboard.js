import React, { useState, useEffect } from 'react';
import { Layout, Menu, Card, Button, Modal, Form, Input, Select, message, Tabs, Table, Switch } from 'antd';
import { 
  DashboardOutlined, 
  MedicineBoxOutlined,
  TeamOutlined,
  LogoutOutlined 
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import { adminService } from '../services/adminService';

const { Header, Content, Sider } = Layout;
const { Option } = Select;

const AdminDashboard = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [departments, setDepartments] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [doctorCandidates, setDoctorCandidates] = useState([]);
  const [departmentModalVisible, setDepartmentModalVisible] = useState(false);
  const [doctorModalVisible, setDoctorModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [availabilityUpdatingId, setAvailabilityUpdatingId] = useState(null);
  const [deletingDoctorId, setDeletingDoctorId] = useState(null);
  const [departmentForm] = Form.useForm();
  const [doctorForm] = Form.useForm();
  const navigate = useNavigate();

  useEffect(() => {
    loadAdminData();
  }, []);

  const loadAdminData = async () => {
    setLoading(true);
    const [departmentsResult, doctorsResult, candidatesResult] = await Promise.allSettled([
      adminService.getAllDepartments(),
      adminService.getAllDoctors(),
      adminService.getDoctorCandidates()
    ]);

    if (departmentsResult.status === 'fulfilled') {
      setDepartments(departmentsResult.value);
    } else {
      message.error(departmentsResult.reason?.response?.data?.message || 'Không thể tải danh sách khoa');
    }

    if (doctorsResult.status === 'fulfilled') {
      setDoctors(doctorsResult.value);
    } else {
      message.error(doctorsResult.reason?.response?.data?.message || 'Không thể tải danh sách bác sĩ');
    }

    if (candidatesResult.status === 'fulfilled') {
      setDoctorCandidates(candidatesResult.value);
    } else {
      message.error(candidatesResult.reason?.response?.data?.message || 'Không thể tải danh sách tài khoản bác sĩ');
    }

    setLoading(false);
  };

  const handleCreateDepartment = async (values) => {
    try {
      await adminService.createDepartment(values);
      message.success('Đã thêm khoa thành công');
      setDepartmentModalVisible(false);
      departmentForm.resetFields();
      loadAdminData();
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể thêm khoa');
    }
  };

  const handleDeleteDepartment = async (departmentId) => {
    try {
      await adminService.deleteDepartment(departmentId);
      message.success('Đã xóa khoa thành công');
      loadAdminData();
    } catch (error) {
      message.error('Không thể xóa khoa');
    }
  };

  const handleCreateDoctor = async (values) => {
    try {
      await adminService.createDoctor(values);
      message.success('Đã thêm bác sĩ thành công');
      setDoctorModalVisible(false);
      doctorForm.resetFields();
      await loadAdminData();
    } catch (error) {
      message.error(error.response?.data?.message || error.response?.data?.error || 'Không thể thêm bác sĩ');
    }
  };

  const handleDeleteDoctor = async (doctorId) => {
    try {
      setDeletingDoctorId(doctorId);
      await adminService.deleteDoctor(doctorId);
      setDoctors(currentDoctors => currentDoctors.filter(doctor => doctor.id !== doctorId));
      message.success('Đã xóa bác sĩ thành công');
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể xóa bác sĩ');
    } finally {
      setDeletingDoctorId(null);
    }
  };

  const handleAvailabilityChange = async (doctorId, available) => {
    try {
      setAvailabilityUpdatingId(doctorId);
      const updatedDoctor = await adminService.setDoctorAvailability(doctorId, available);
      setDoctors(currentDoctors => currentDoctors.map(doctor => (
        doctor.id === doctorId ? { ...doctor, available: updatedDoctor.available } : doctor
      )));
      message.success(updatedDoctor.available ? 'Bác sĩ đã được đặt là đang làm việc' : 'Bác sĩ đã được chuyển sang nghỉ');
    } catch (error) {
      message.error(error.response?.data?.message || 'Không thể cập nhật trạng thái làm việc');
    } finally {
      setAvailabilityUpdatingId(null);
    }
  };

  const handleLogout = () => {
    authService.logout();
    navigate('/login');
  };

  const menuItems = [
    { key: '1', icon: <DashboardOutlined />, label: 'Tổng quan' },
    { key: '2', icon: <TeamOutlined />, label: 'Các khoa' },
    { key: '3', icon: <MedicineBoxOutlined />, label: 'Các bác sĩ' },
    { key: '4', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: handleLogout },
  ];

  const departmentColumns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: 'Tên khoa', dataIndex: 'name', key: 'name' },
    { title: 'Mô tả', dataIndex: 'description', key: 'description' },
    { title: 'Trạng thái', dataIndex: 'active', key: 'active', render: (active) => (active ? 'Đang hoạt động' : 'Tạm dừng') },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <div>
          <Button size="small" onClick={() => handleDeleteDepartment(record.id)} danger>
            Xóa
          </Button>
        </div>
      )
    }
  ];

  const doctorColumns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: 'Họ tên', dataIndex: ['user', 'firstName'], key: 'name', render: (_, record) => `${record.user?.firstName} ${record.user?.lastName}` },
    { title: 'Khoa', dataIndex: ['department', 'name'], key: 'department' },
    { title: 'Chuyên môn', dataIndex: 'specialization', key: 'specialization' },
    { title: 'Kinh nghiệm', dataIndex: 'experience', key: 'experience' },
    {
      title: 'Đang làm việc',
      dataIndex: 'available',
      key: 'available',
      render: (available, record) => (
        <Switch
          checked={available}
          checkedChildren="Có"
          unCheckedChildren="Không"
          loading={availabilityUpdatingId === record.id}
          onChange={(nextAvailable) => handleAvailabilityChange(record.id, nextAvailable)}
        />
      )
    },
    {
      title: 'Thao tác',
      key: 'actions',
      render: (_, record) => (
        <div>
          <Button
            size="small"
            danger
            loading={deletingDoctorId === record.id}
            onClick={() => Modal.confirm({
              title: 'Xác nhận xóa bác sĩ',
              content: `Bạn có chắc muốn xóa ${record.user?.firstName || 'bác sĩ này'}?`,
              okText: 'Xóa',
              cancelText: 'Hủy',
              okButtonProps: { danger: true },
              onOk: () => handleDeleteDoctor(record.id)
            })}
          >
            Xóa
          </Button>
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
          <h2 style={{ padding: '0 24px', margin: 0 }}>Bảng điều khiển quản trị viên</h2>
        </Header>
        <Content style={{ margin: '16px' }}>
          <div style={{ padding: 24, background: '#fff', minHeight: 360 }}>
            <Tabs defaultActiveKey="1">
              <Tabs.TabPane tab="Tổng quan" key="1">
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: 16 }}>
                  <Card title="Tổng số khoa" bordered={false}>
                    <h3>{departments.length}</h3>
                  </Card>
                  <Card title="Tổng số bác sĩ" bordered={false}>
                    <h3>{doctors.length}</h3>
                  </Card>
                  <Card title="Bác sĩ đang làm việc" bordered={false}>
                    <h3>{doctors.filter(d => d.available).length}</h3>
                  </Card>
                </div>
              </Tabs.TabPane>

              <Tabs.TabPane tab="Các khoa" key="2">
                <Button 
                  type="primary" 
                  onClick={() => setDepartmentModalVisible(true)}
                  style={{ marginBottom: 16 }}
                >
                  Thêm khoa
                </Button>
                <Table
                  dataSource={departments}
                  columns={departmentColumns}
                  rowKey="id"
                  loading={loading}
                />
              </Tabs.TabPane>

              <Tabs.TabPane tab="Các bác sĩ" key="3">
                <Button 
                  type="primary" 
                  onClick={() => setDoctorModalVisible(true)}
                  style={{ marginBottom: 16 }}
                >
                  Thêm bác sĩ
                </Button>
                <Table
                  dataSource={doctors}
                  columns={doctorColumns}
                  rowKey="id"
                  loading={loading}
                />
              </Tabs.TabPane>
            </Tabs>
          </div>
        </Content>
      </Layout>

      <Modal
        title="Thêm khoa"
        open={departmentModalVisible}
        onCancel={() => setDepartmentModalVisible(false)}
        footer={null}
      >
        <Form form={departmentForm} onFinish={handleCreateDepartment} layout="vertical">
          <Form.Item name="name" label="Tên khoa" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>Thêm khoa</Button>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="Thêm bác sĩ"
        open={doctorModalVisible}
        onCancel={() => setDoctorModalVisible(false)}
        footer={null}
      >
        <Form form={doctorForm} onFinish={handleCreateDoctor} layout="vertical">
          <Form.Item name="userId" label="Tài khoản bác sĩ" rules={[{ required: true, message: 'Vui lòng chọn tài khoản bác sĩ' }]}>
            <Select
              placeholder="Chọn tài khoản bác sĩ"
              showSearch
              optionFilterProp="label"
              notFoundContent="Chưa có tài khoản chờ tạo hồ sơ. Đăng ký tài khoản với vai trò Bác sĩ trước."
              options={doctorCandidates.map(candidate => ({
                value: candidate.id,
                label: `#${candidate.id} - ${candidate.firstName} ${candidate.lastName} (${candidate.username})`
              }))}
            />
          </Form.Item>
          <Form.Item name="departmentId" label="Khoa" rules={[{ required: true }]}>
            <Select>
              {departments.map(dept => (
                <Option key={dept.id} value={dept.id}>{dept.name}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="specialization" label="Chuyên môn" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="qualification" label="Bằng cấp">
            <Input />
          </Form.Item>
          <Form.Item name="experience" label="Kinh nghiệm (năm)">
            <Input type="number" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>Thêm bác sĩ</Button>
          </Form.Item>
        </Form>
      </Modal>
    </Layout>
  );
};

export default AdminDashboard;