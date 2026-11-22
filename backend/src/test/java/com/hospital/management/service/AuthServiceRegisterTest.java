package com.hospital.management.service;

import com.hospital.management.dto.RegistrationRequest;
import com.hospital.management.model.Patient;
import com.hospital.management.model.User;
import com.hospital.management.repository.PatientRepository;
import com.hospital.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * White-box unit tests for AuthService.register.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void WB_REG_01_throwsWhenUsernameAlreadyExists() {
        RegistrationRequest request = request("doctor", null);
        when(userRepository.existsByUsername("new-user")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any(User.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void WB_REG_02_throwsWhenEmailAlreadyExists() {
        RegistrationRequest request = request("doctor", null);
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("new-user@example.test")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void WB_REG_03_savesOnlyUserWhenRoleIsNotPatient() {
        RegistrationRequest request = request("doctor", null);
        allowNewAccount();
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        assertEquals(User.Role.DOCTOR, userCaptor.getValue().getRole());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void WB_REG_04_createsPatientWithoutDateOfBirth() {
        RegistrationRequest request = request("PATIENT", null);
        allowNewAccount();
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);

        verify(userRepository).save(userCaptor.capture());
        verify(patientRepository).save(patientCaptor.capture());

        assertEquals(User.Role.PATIENT, userCaptor.getValue().getRole());
        assertNull(patientCaptor.getValue().getDateOfBirth());
        assertSame(userCaptor.getValue(), patientCaptor.getValue().getUser());
    }

    @Test
    void WB_REG_05_createsPatientWithDateOfBirth() {
        RegistrationRequest request = request("patient", "2000-05-20");
        allowNewAccount();
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(patientCaptor.capture());

        assertEquals(
                LocalDate.of(2000, 5, 20),
                patientCaptor.getValue().getDateOfBirth()
        );
    }

    @Test
    void WB_REG_06_throwsWhenRoleIsUnknown() {
        RegistrationRequest request = request("SUPERUSER", null);
        allowNewAccount();
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never()).save(any(User.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void WB_REG_07_throwsWhenPatientDateOfBirthIsMalformed() {
        RegistrationRequest request = request("PATIENT", "20/05/2000");
        allowNewAccount();
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(
                DateTimeParseException.class,
                () -> authService.register(request)
        );

        verify(userRepository).save(any(User.class));
        verify(patientRepository, never()).save(any(Patient.class));
    }

    private RegistrationRequest request(String role, String dateOfBirth) {
        RegistrationRequest request = mock(RegistrationRequest.class);

        when(request.getUsername()).thenReturn("new-user");
        when(request.getEmail()).thenReturn("new-user@example.test");
        when(request.getPassword()).thenReturn("plain-password");
        when(request.getRole()).thenReturn(role);
        when(request.getDateOfBirth()).thenReturn(dateOfBirth);

        return request;
    }

    private void allowNewAccount() {
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("new-user@example.test")).thenReturn(false);
    }
}