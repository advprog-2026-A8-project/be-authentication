package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PublicProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String bio;
    private String role;
    private String kycStatus;
    private Long successfulTransactionCount;
}
