package in.vikramaditya.ctrlpluscvapi.controller;

import in.vikramaditya.ctrlpluscvapi.dto.AuthResponse;
import in.vikramaditya.ctrlpluscvapi.dto.RegisterRequest;
import in.vikramaditya.ctrlpluscvapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static in.vikramaditya.ctrlpluscvapi.util.AppConstants.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping(AUTH_CONTROLLER)
public class AuthController {

    private final AuthService authService;

    @PostMapping(REGISTER)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
            log.info("Inside AuthController - register(): {}", request);
            AuthResponse response = authService.register(request);
            log.info("Response from service:{}",request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(VERIFY_EMAIL)
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        log.info("Inside AuthController - verifyEmail(): {}", token);
        authService.verifyEmail(token);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Email verified successfully"));
    }
}
