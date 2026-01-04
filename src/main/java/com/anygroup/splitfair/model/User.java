package com.anygroup.splitfair.model;

import com.anygroup.splitfair.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String userName;

    @Column(nullable = false, unique = true, length = 250)
    private String email;

    @Column(nullable = true)
    private String password;

    private String avatar;

    @Column(name = "bank_code")
    private String bankCode; // VCB, BIDV, ACB...

    @Column(name = "bank_account_no")
    private String bankAccountNo;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    private Instant createdTime = Instant.now();
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}