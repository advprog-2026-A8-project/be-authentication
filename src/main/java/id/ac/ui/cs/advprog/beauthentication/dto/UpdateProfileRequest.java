package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String fullName;
    private String phoneNumber;
    private String bio;
}
