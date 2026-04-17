package id.ac.ui.cs.advprog.beauthentication.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkProfileLookupRequest {
    private List<Long> userIds;
}
