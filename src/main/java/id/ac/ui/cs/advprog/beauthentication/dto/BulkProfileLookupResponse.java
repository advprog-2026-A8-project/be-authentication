package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkProfileLookupResponse {
    private List<UserLookupSummaryResponse> users;
    private List<Long> notFoundIds;
}
