package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private String kycStatus;
}
