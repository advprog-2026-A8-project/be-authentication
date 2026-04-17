package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
}