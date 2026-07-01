package com.yunzhi.llm.agentscope.key;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long> {

    List<ApiKeyEntity> findByProviderAndEnabledTrueOrderByPriorityDescIdAsc(ApiKeyProvider provider);

    java.util.Optional<ApiKeyEntity> findByProviderAndLabel(ApiKeyProvider provider, String label);
}
