package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountStatusUpdateResponse {
    private Long userId;
    private String oldStatus;
    private String newStatus;
}
