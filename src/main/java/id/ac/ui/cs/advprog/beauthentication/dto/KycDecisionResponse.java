package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class KycDecisionResponse {
    private UUID userId;
    private String oldKycStatus;
    private String newKycStatus;
    private String oldRole;
    private String newRole;
}
