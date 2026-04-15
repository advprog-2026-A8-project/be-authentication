package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
