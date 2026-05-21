package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class AccountStatusUpdateRequest {
    private UUID userId;
    private String status;
}
