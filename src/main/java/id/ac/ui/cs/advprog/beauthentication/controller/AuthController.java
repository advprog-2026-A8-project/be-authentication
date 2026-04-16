package id.ac.ui.cs.advprog.beauthentication.controller;

import id.ac.ui.cs.advprog.beauthentication.dto.ApiResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.LoginResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterResponse;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.service.AuthService;
import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    private boolean isAuthenticationFailure(String message) {
        return "Email tidak ditemukan!".equals(message) || "Password salah!".equals(message);
    }

    private RegisterResponse toRegisterResponse(UserProfile user) {
        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getKycStatus()
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        try {
            UserProfile user = authService.register(request);
            // Respons sukses berformat JSON
            return ResponseEntity.ok(new ApiResponse<>("Registrasi berhasil!", toRegisterResponse(user)));
        } catch (IllegalArgumentException e) {
            // Respons gagal berformat JSON
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            UserProfile user = authService.login(request);
            String token = jwtUtil.generateToken(user.getEmail());
            
            return ResponseEntity.ok(new ApiResponse<>("Login berhasil!", new LoginResponse(token)));
        } catch (IllegalArgumentException e) {
            HttpStatus status = isAuthenticationFailure(e.getMessage())
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity.status(status)
                    .body(new ApiResponse<>(e.getMessage(), null));
        }
    }
}