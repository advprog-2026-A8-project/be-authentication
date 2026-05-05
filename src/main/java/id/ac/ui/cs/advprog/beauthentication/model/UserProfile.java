package id.ac.ui.cs.advprog.beauthentication.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String accountStatus = AccountStatus.ACTIVE.name();

    @Column(nullable = false)
    private String kycStatus = KycStatus.NOT_SUBMITTED.name();

    @Column(name = "kyc_identity_document_url")
    private String kycIdentityDocumentUrl;

    @Column(name = "kyc_social_media_url")
    private String kycSocialMediaUrl;

    @Column(name = "successful_transaction_count", nullable = false)
    private Long successfulTransactionCount = 0L;
}