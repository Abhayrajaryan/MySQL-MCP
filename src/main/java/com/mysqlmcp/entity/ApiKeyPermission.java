package com.mysqlmcp.entity;

import com.mysqlmcp.enums.DatabasePermission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "api_key_permissions")
@Getter
@Setter
@NoArgsConstructor
public class ApiKeyPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DatabasePermission permission;
}