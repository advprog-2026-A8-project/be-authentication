package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class BulkProfileLookupResponse {
    private List<UserLookupSummaryResponse> users;
    private List<UUID> notFoundIds;
}
