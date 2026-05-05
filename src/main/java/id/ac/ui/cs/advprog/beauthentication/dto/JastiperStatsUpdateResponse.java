package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JastiperStatsUpdateResponse {
    private Long userId;
    private Long oldCount;
    private Long newCount;
}
