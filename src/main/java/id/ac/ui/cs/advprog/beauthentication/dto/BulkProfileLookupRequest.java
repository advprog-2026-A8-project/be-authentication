package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkProfileLookupRequest {
    private List<UUID> userIds;
}
