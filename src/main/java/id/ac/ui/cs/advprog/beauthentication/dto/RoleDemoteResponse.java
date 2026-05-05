package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleDemoteResponse {
    private Long userId;
    private String oldRole;
    private String newRole;
}
