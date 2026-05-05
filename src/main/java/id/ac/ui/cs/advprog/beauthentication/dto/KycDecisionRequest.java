package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

@Data
public class KycDecisionRequest {
    private Long userId;
    private String decision;
}
