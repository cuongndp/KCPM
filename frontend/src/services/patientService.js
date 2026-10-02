import api from './api';

export const patientService = {
  getPatientProfile: async (userId) => {
    const response = await api.get(`/patient/profile?userId=${userId}`);
    return response.data;
  },

  getMedicalHistory: async (patientId) => {
    const response = await api.get(`/patient/medical-history?patientId=${patientId}`);
    return response.data;
  },

  getPrescriptions: async (patientId) => {
    const response = await api.get(`/patient/prescriptions?patientId=${patientId}`);
    return response.data;
  }
};