import api from './api';

export const adminService = {
  getAllDepartments: async () => {
    const response = await api.get('/admin/departments');
    return response.data;
  },

  createDepartment: async (departmentData) => {
    const response = await api.post('/admin/departments', departmentData);
    return response.data;
  },

  deleteDepartment: async (departmentId) => {
    const response = await api.delete(`/admin/departments/${departmentId}`);
    return response.data;
  },

  getAllDoctors: async () => {
    const response = await api.get('/admin/doctors');
    return response.data;
  },

  getDoctorCandidates: async () => {
    const response = await api.get('/admin/users/doctor-candidates');
    return response.data;
  },

  createDoctor: async (doctorData, departmentId) => {
    const response = await api.post(`/admin/doctors?departmentId=${departmentId}`, doctorData);
    return response.data;
  },

  deleteDoctor: async (doctorId) => {
    const response = await api.delete(`/admin/doctors/${doctorId}`);
    return response.data;
  },

  setDoctorAvailability: async (doctorId, available) => {
    const response = await api.patch(`/admin/doctors/${doctorId}/availability`, { available });
    return response.data;
  }
};