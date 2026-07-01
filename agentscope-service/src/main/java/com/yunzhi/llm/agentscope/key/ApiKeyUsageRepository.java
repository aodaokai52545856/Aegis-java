package com.yunzhi.llm.agentscope.key;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyUsageRepository extends JpaRepository<ApiKeyUsageEntity, Long> {
}
