package in.vikramaditya.ctrlpluscvapi.service;

import in.vikramaditya.ctrlpluscvapi.document.User;
import in.vikramaditya.ctrlpluscvapi.dto.AuthResponse;
import in.vikramaditya.ctrlpluscvapi.dto.LoginRequest;
import in.vikramaditya.ctrlpluscvapi.dto.RegisterRequest;
import in.vikramaditya.ctrlpluscvapi.exception.ResourceExistsException;
import in.vikramaditya.ctrlpluscvapi.repository.UserRepository;
import in.vikramaditya.ctrlpluscvapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;

    public AuthResponse register(RegisterRequest request) {
        log.info("Inside AuthService: register() {}", request);

        if(userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceExistsException("User already exists with this email");
        }
        User newUser = toDocument(request);
        userRepository.save(newUser);
        sendVerificationEmail(newUser);
        return toResponse(newUser);
    }

    private void sendVerificationEmail(User newUser) {
        log.info("Inside AuthService - sendVerificationEmail(): {}", newUser);
        try {
            String link = appBaseUrl + "/api/auth/verify-email?token=" + newUser.getVerificationToken();
            String html = "<div style='font-family:Arial, sans-serif; max-width:600px; margin:20px auto; padding:20px; border:1px solid #eaeaea; border-radius:10px;'>"
                            + "<h2 style='color:#0d6efd; margin-bottom:10px;'>Verify your email</h2>"
                            + "<p>Hi " + newUser.getName() + ", please confirm your email to continue.</p>"
                            + "<p style='margin:16px 0;'>"
                            + "<a href='" + link + "' "
                            + "style='display:inline-block; padding:10px 16px; background:#0d6efd; color:white; text-decoration:none; border-radius:6px;'>"
                            + "Verify Email</a>"
                            + "</p>"
                            + "<p>Or copy this link:</p>"
                            + "<p style='word-break:break-all;'>" + link + "</p>"
                            + "<p style='color:#777; font-size:12px; margin-top:20px;'>This link expires in 24 hours.</p>"
                            + "</div>";
            emailService.sendHtmlEmail(newUser.getEmail(), "Verify your email", html);

        } catch (Exception e) {
            log.error("Exception occurred at sendVerificationEmail(): {}",e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }
    private AuthResponse toResponse(User newUser) {
        return AuthResponse.builder()
                .id(newUser.getId())
                .name(newUser.getName())
                .email(newUser.getEmail())
                .profileImageUrl(newUser.getProfileImageUrl())
                .emailVerified(newUser.isEmailVerified())
                .subscriptionPlan(newUser.getSubscriptionPlan())
                .createdAt(newUser.getCreatedAt())
                .updatedAt(newUser.getUpdatedAt())
                .build();
    }
    private User toDocument(RegisterRequest request) {
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .profileImageUrl(request.getProfileImageUrl())
                .subscriptionPlan("Basic")
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationExpires(LocalDateTime.now().plusHours(24))
                .build();
    }

    public void verifyEmail(String token) {
        log.info("Inside AuthService verifyEmail(): {}", token);
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(()-> new RuntimeException("Invalid or expired verification token"));

        if(user.getVerificationExpires() != null && user.getVerificationExpires().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification token has expired. Please request new one");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpires(null);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User existingUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new UsernameNotFoundException("Invalid email or password"));
        if(!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new UsernameNotFoundException("Invalid email or Password");
        }
        if(!existingUser.isEmailVerified()) {
            throw new RuntimeException("Please verify your email before logging in");
        }
        String token = jwtUtil.generateToken(existingUser.getId());
        AuthResponse response = toResponse(existingUser);
        response.setToken(token);
        return response;
    }


}
