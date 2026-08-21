package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.config.PromptConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads system prompts from classpath:prompts/{appId}/system-prompt.txt
 * and caches them in memory.
 *
 * This allows each application to define its AI personality without
 * changing AI Platform code.
 *
 * Directory structure:
 *   src/main/resources/prompts/
 *       insighttube/
 *           system-prompt.txt
 *           chunk-config.yaml    (optional, overrides application.yaml)
 *       pdfmind/
 *           system-prompt.txt
 *           chunk-config.yaml
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptRegistry {

    private final PromptConfig promptConfig;

    /** appId → loaded system prompt text */
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (String appId : promptConfig.getRegisteredApps()) {
            loadPrompt(appId);
        }
        log.info("PromptRegistry initialized with apps: {}", promptConfig.getRegisteredApps());
    }

    /**
     * Get the system prompt for an application.
     * Loads from prompts/{appId}/system-prompt.txt on first access.
     */
    public String getSystemPrompt(String appId) {
        return promptCache.computeIfAbsent(appId, this::loadPrompt);
    }

    /**
     * Get the full prompt config for an application.
     */
    public PromptConfig.AppPromptConfig getConfig(String appId) {
        return promptConfig.getConfigForApp(appId);
    }

    /**
     * Check if an application is registered.
     */
    public boolean isRegistered(String appId) {
        return promptConfig.isAppRegistered(appId);
    }

    /**
     * Get all registered application IDs.
     */
    public java.util.Set<String> getRegisteredApps() {
        return promptConfig.getRegisteredApps();
    }

    /**
     * Reload prompt from disk (for development/hot-reload).
     */
    public String reloadPrompt(String appId) {
        promptCache.remove(appId);
        return getSystemPrompt(appId);
    }

    private String loadPrompt(String appId) {
        String path = "prompts/" + appId + "/system-prompt.txt";
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (resource.exists()) {
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                log.info("Loaded system prompt for app '{}' ({} chars)", appId, content.length());
                return content;
            } else {
                log.warn("No system-prompt.txt found at {} — using default prompt", path);
                return getDefaultPrompt(appId);
            }
        } catch (IOException e) {
            log.error("Failed to load system prompt for app '{}' from {}", appId, path, e);
            return getDefaultPrompt(appId);
        }
    }

    private String getDefaultPrompt(String appId) {
        return "You are a helpful AI assistant for the " + appId + " application. "
                + "Answer questions accurately and concisely based on the provided context.";
    }
}
