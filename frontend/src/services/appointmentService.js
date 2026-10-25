import api from './api';

export const appointmentService = {
  getPatientAppointments: async (patientId) => {
    const response = await api.get(`/patient/appointments?patientId=${patientId}`);
    return response.data;
  },

  getDoctorAppointments: async (doctorId) => {
    const response = await api.get(`/doctor/appointments?doctorId=${doctorId}`);
    return response.data;
  },

  bookAppointment: async (patientId, appointmentData) => {
    const response = await api.post(`/patient/appointments?patientId=${patientId}`, appointmentData);
    return response.data;
  },

  cancelAppointment: async (appointmentId) => {
    const response = await api.delete(`/patient/appointments/${appointmentId}`);
    return response.data;
  },

  updateAppointmentStatus: async (appointmentId, status) => {
    const response = await api.put(`/doctor/appointments/${appointmentId}/status?status=${status}`);
    return response.data;
  },

  getUpcomingAppointments: async (doctorId) => {
    const response = await api.get(`/doctor/appointments/upcoming?doctorId=${doctorId}`);
    return response.data;
  }
};