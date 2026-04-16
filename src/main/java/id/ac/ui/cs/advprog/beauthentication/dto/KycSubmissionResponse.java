package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KycSubmissionResponse {
    private String fullName;
    private String identityDocumentUrl;
    private String socialMediaUrl;
    private String kycStatus;
}
