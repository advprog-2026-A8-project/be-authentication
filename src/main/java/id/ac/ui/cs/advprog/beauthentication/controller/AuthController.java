package id.ac.ui.cs.advprog.beauthentication.controller;

import id.ac.ui.cs.advprog.beauthentication.dto.LoginRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.LoginResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RegisterRequest;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;
import id.ac.ui.cs.advprog.beauthentication.service.AuthService;
import id.ac.ui.cs.advprog.beauthentication.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            UserProfile user = authService.register(request);
            return ResponseEntity.ok("Registrasi berhasil! ID Pengguna: " + user.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try{
            UserProfile user = authService.login(request);

            // buat token JWT dari username
            String token = jwtUtil.generateToken(user.getUsername());

            // return token dalam format JSON
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (IllegalArgumentException e) {
            // return status 401 Unauthorized jika gagal login
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}