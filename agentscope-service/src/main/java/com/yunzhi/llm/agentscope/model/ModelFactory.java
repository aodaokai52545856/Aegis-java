package com.yunzhi.llm.agentscope.model;

import com.yunzhi.llm.agentscope.config.AegisProperties;
import com.yunzhi.llm.agentscope.key.AllocatedKey;
import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelFactory {

    private final AegisProperties aegisProperties;

    public Model createDeepSeekModel(AllocatedKey allocatedKey, String modelId) {
        String resolvedModel = modelId != null && !modelId.isBlank()
                ? modelId
                : aegisProperties.getDeepseek().getDefaultModel();

        return OpenAIChatModel.builder()
                .apiKey(allocatedKey.secret())
                .baseUrl(aegisProperties.getDeepseek().getBaseUrl())
                .modelName(resolvedModel)
                .stream(true)
                .formatter(new DeepSeekFormatter())
                .build();
    }
}
