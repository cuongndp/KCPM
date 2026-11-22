package com.hospital.management.service;

import com.hospital.management.dto.LoginRequest;
import com.hospital.management.dto.LoginResponse;
import com.hospital.management.model.User;
import com.hospital.management.repository.PatientRepository;
import com.hospital.management.repository.UserRepository;
import com.hospital.management.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("WB-LGN-01 — đăng nhập thành công trả token")
    void loginReturnsTokenWhenUserExists() {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(5L);
        user.setUsername("2");
        user.setRole(User.Role.DOCTOR);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findByUsername("2")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("2", "DOCTOR")).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("2", "password"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getUsername()).isEqualTo("2");
        assertThat(response.getRole()).isEqualTo("DOCTOR");
    }

    @Test
    @DisplayName("WB-LGN-02 — không tìm thấy user sau khi xác thực")
    void loginThrowsWhenUserIsMissingAfterAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "password")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }
}