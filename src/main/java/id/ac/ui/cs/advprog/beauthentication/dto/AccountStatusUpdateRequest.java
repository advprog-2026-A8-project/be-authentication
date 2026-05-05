package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

@Data
public class AccountStatusUpdateRequest {
    private Long userId;
    private String status;
}
