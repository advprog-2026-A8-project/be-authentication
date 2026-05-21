package id.ac.ui.cs.advprog.beauthentication.service;

import id.ac.ui.cs.advprog.beauthentication.dto.AccountStatusUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.BulkProfileLookupResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.JastiperStatsUpdateResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycDecisionResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.KycSubmissionRequest;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleDemoteResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.RoleUpgradeResponse;
import id.ac.ui.cs.advprog.beauthentication.dto.UpdateProfileRequest;
import id.ac.ui.cs.advprog.beauthentication.model.UserProfile;

import java.util.List;
import java.util.UUID;

public interface IProfileService {
    UserProfile getByUsername(String username);
    UserProfile getByIdentifier(UUID id, String username, String email);
    List<UserProfile> getAllJastiperProfiles();
    List<UserProfile> getAllProfiles();
    BulkProfileLookupResponse bulkLookupByIds(List<UUID> userIds);
    RoleUpgradeResponse upgradeRoleToJastiper(UUID userId);
    RoleDemoteResponse demoteRoleToTitiper(UUID userId);
    KycDecisionResponse decideKyc(UUID userId, String decision);
    JastiperStatsUpdateResponse incrementSuccessfulTransactionCount(UUID userId, Long delta);
    AccountStatusUpdateResponse updateAccountStatus(UUID userId, String status);
    UserProfile updateMyProfile(String principalIdentifier, UpdateProfileRequest request);
    UserProfile submitKyc(String principalIdentifier, KycSubmissionRequest request);
}
