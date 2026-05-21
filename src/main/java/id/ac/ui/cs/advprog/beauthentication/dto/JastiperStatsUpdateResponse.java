package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class JastiperStatsUpdateResponse {
    private UUID userId;
    private Long oldCount;
    private Long newCount;
}
