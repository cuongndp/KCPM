package com.hospital.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.management.model.Appointment;
import com.hospital.management.model.Prescription;
import com.hospital.management.repository.AppointmentRepository;
import com.hospital.management.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    private static final String APPOINTMENT_REMINDER_TOPIC = "appointment-reminders";
    private static final String PRESCRIPTION_NOTIFICATION_TOPIC = "prescription-notifications";
    private static final String FOLLOW_UP_ALERT_TOPIC = "follow-up-alerts";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendAppointmentReminder(Appointment appointment) {
        try {
            String message = objectMapper.writeValueAsString(appointment);
            kafkaTemplate.send(APPOINTMENT_REMINDER_TOPIC, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send appointment reminder", e);
        }
    }

    public void sendPrescriptionNotification(Prescription prescription) {
        try {
            String message = objectMapper.writeValueAsString(prescription);
            kafkaTemplate.send(PRESCRIPTION_NOTIFICATION_TOPIC, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send prescription notification", e);
        }
    }

    public void sendFollowUpAlert(Appointment appointment) {
        try {
            String message = objectMapper.writeValueAsString(appointment);
            kafkaTemplate.send(FOLLOW_UP_ALERT_TOPIC, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send follow-up alert", e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void checkAppointmentReminders() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.toLocalDate().atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.toLocalDate().atTime(23, 59, 59);

        List<Appointment> upcomingAppointments = appointmentRepository.findAll().stream()
                .filter(a -> a.getAppointmentDateTime().isAfter(startOfTomorrow))
                .filter(a -> a.getAppointmentDateTime().isBefore(endOfTomorrow))
                .filter(a -> !a.getReminderSent())
                .filter(a -> a.getStatus() == Appointment.Status.SCHEDULED || a.getStatus() == Appointment.Status.CONFIRMED)
                .toList();

        for (Appointment appointment : upcomingAppointments) {
            sendAppointmentReminder(appointment);
            appointment.setReminderSent(true);
            appointmentRepository.save(appointment);
        }
    }

    @Scheduled(fixedRate = 7200000) // Run every 2 hours
    public void checkPrescriptionNotifications() {
        List<Prescription> prescriptions = prescriptionRepository.findAll().stream()
                .filter(p -> !p.getNotificationSent())
                .toList();

        for (Prescription prescription : prescriptions) {
            sendPrescriptionNotification(prescription);
            prescription.setNotificationSent(true);
            prescriptionRepository.save(prescription);
        }
    }

    @Scheduled(fixedRate = 86400000) // Run daily
    public void checkFollowUpAlerts() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        
        List<Appointment> completedAppointments = appointmentRepository.findAll().stream()
                .filter(a -> a.getAppointmentDateTime().isBefore(threeDaysAgo))
                .filter(a -> a.getStatus() == Appointment.Status.COMPLETED)
                .filter(a -> a.getFollowUpRequired())
                .toList();

        for (Appointment appointment : completedAppointments) {
            sendFollowUpAlert(appointment);
        }
    }
}