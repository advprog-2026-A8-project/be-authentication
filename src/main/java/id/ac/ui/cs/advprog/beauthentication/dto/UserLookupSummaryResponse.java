package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLookupSummaryResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String kycStatus;
}
