package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class RoleUpgradeRequest {
    private UUID userId;
}
