package com.yunzhi.llm.agentscope.key;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "api_keys")
public class ApiKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApiKeyProvider provider;

    @Column(nullable = false, length = 512)
    private String secret;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int priority = 0;

    @Column(nullable = false)
    private long totalRequests = 0;

    private Instant lastUsedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
