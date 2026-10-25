import api from './api';

export const doctorService = {
  getActiveDepartments: async () => {
    const response = await api.get('/patient/departments');
    return response.data;
  },

  getAvailableDoctors: async (departmentId) => {
    const response = await api.get('/patient/doctors/available', { params: { departmentId } });
    return response.data;
  },

  getDoctorProfile: async (userId) => {
    const response = await api.get(`/doctor/profile?userId=${userId}`);
    return response.data;
  },

  createMedicalRecord: async (doctorId, medicalRecordData) => {
    const response = await api.post(`/doctor/medical-records?doctorId=${doctorId}`, medicalRecordData);
    return response.data;
  },

  getMedicalRecords: async (doctorId) => {
    const response = await api.get(`/doctor/medical-records?doctorId=${doctorId}`);
    return response.data;
  },

  createPrescription: async (doctorId, prescriptionData) => {
    const response = await api.post(`/doctor/prescriptions?doctorId=${doctorId}`, prescriptionData);
    return response.data;
  },

  getPrescriptions: async (doctorId) => {
    const response = await api.get(`/doctor/prescriptions?doctorId=${doctorId}`);
    return response.data;
  }

};