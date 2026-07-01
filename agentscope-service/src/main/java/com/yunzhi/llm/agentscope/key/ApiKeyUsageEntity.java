package com.yunzhi.llm.agentscope.key;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "api_key_usages", indexes = {
        @Index(name = "idx_usage_key_id", columnList = "apiKeyId"),
        @Index(name = "idx_usage_request_id", columnList = "requestId")
})
public class ApiKeyUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long apiKeyId;

    @Column(nullable = false, length = 64)
    private String requestId;

    @Column(length = 128)
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String model;

    @Column(nullable = false, length = 16)
    private String mode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
