package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KycDecisionResponse {
    private Long userId;
    private String oldKycStatus;
    private String newKycStatus;
    private String oldRole;
    private String newRole;
}
