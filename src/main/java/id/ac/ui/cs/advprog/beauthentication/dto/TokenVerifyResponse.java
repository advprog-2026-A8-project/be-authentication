package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenVerifyResponse {
    private String subject;
    private String role;
    private long expiresAt;
}
