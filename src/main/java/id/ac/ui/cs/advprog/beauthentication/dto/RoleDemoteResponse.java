package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RoleDemoteResponse {
    private UUID userId;
    private String oldRole;
    private String newRole;
}
