package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String bio;
    private String role;
    private String accountStatus;
    private String kycStatus;
    private Long successfulTransactionCount;
}
