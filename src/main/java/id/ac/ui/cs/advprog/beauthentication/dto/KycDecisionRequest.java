package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class KycDecisionRequest {
    private UUID userId;
    private String decision;
}
