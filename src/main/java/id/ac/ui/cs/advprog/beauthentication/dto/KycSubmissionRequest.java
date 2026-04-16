package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

@Data
public class KycSubmissionRequest {
    private String fullName;
    private String identityDocumentUrl;
    private String socialMediaUrl;
}
