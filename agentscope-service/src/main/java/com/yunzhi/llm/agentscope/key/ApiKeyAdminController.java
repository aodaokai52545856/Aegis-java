package com.yunzhi.llm.agentscope.key;

import com.yunzhi.llm.agentscope.key.dto.ApiKeyCreateRequest;
import com.yunzhi.llm.agentscope.key.dto.ApiKeyResponse;
import com.yunzhi.llm.agentscope.key.dto.ApiKeyUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/keys")
@RequiredArgsConstructor
public class ApiKeyAdminController {

    private final ApiKeyRepository apiKeyRepository;

    @GetMapping
    public List<ApiKeyResponse> list() {
        return apiKeyRepository.findAll().stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiKeyResponse create(@Valid @RequestBody ApiKeyCreateRequest request) {
        ApiKeyEntity entity = new ApiKeyEntity();
        entity.setLabel(request.getLabel());
        entity.setProvider(request.getProvider());
        entity.setSecret(request.getSecret());
        entity.setEnabled(request.isEnabled());
        entity.setPriority(request.getPriority());
        return ApiKeyResponse.from(apiKeyRepository.save(entity));
    }

    @PutMapping("/{id}")
    public ApiKeyResponse update(@PathVariable Long id, @RequestBody ApiKeyUpdateRequest request) {
        ApiKeyEntity entity = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Key not found"));
        if (request.getLabel() != null) {
            entity.setLabel(request.getLabel());
        }
        if (request.getSecret() != null) {
            entity.setSecret(request.getSecret());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getPriority() != null) {
            entity.setPriority(request.getPriority());
        }
        return ApiKeyResponse.from(apiKeyRepository.save(entity));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!apiKeyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Key not found");
        }
        apiKeyRepository.deleteById(id);
    }
}
