package com.aiservice.platform.aiplatform.service;

import com.aiservice.platform.aiplatform.config.PromptConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PromptRegistry")
class PromptRegistryTest {

    @Autowired
    private PromptRegistry promptRegistry;

    @Nested
    @DisplayName("getSystemPrompt()")
    class GetSystemPrompt {

        @Test
        @DisplayName("should load InsightTube prompt from classpath")
        void shouldLoadInsightTubePrompt() {
            String prompt = promptRegistry.getSystemPrompt("insighttube");
            assertNotNull(prompt);
            assertTrue(prompt.contains("YouTube"), "Prompt should mention YouTube");
            assertTrue(prompt.contains("timestamp"), "Prompt should mention timestamps");
            assertTrue(prompt.length() > 100, "Prompt should be substantial");
        }

        @Test
        @DisplayName("should load PDFMind prompt from classpath")
        void shouldLoadPdfMindPrompt() {
            String prompt = promptRegistry.getSystemPrompt("pdfmind");
            assertNotNull(prompt);
            assertTrue(prompt.contains("PDF"), "Prompt should mention PDF");
            assertTrue(prompt.contains("page"), "Prompt should mention pages");
        }

        @Test
        @DisplayName("should return default prompt for unknown app")
        void shouldReturnDefaultForUnknown() {
            String prompt = promptRegistry.getSystemPrompt("unknown-app");
            assertNotNull(prompt);
            assertTrue(prompt.contains("unknown-app"));
        }
    }

    @Nested
    @DisplayName("getConfig()")
    class GetConfig {

        @Test
        @DisplayName("should return InsightTube config with correct chunk strategy")
        void shouldReturnInsightTubeConfig() {
            PromptConfig.AppPromptConfig config = promptRegistry.getConfig("insighttube");
            assertNotNull(config);
            assertEquals("transcript-by-timestamp", config.getChunkStrategy());
            assertEquals(10, config.getTopK());
            assertEquals(0.7, config.getSimilarityThreshold(), 0.01);
            assertEquals("timestamp", config.getCitationFormat());
            assertEquals("gemini-3-flash-preview", config.getModel());
        }

        @Test
        @DisplayName("should return PDFMind config with correct settings")
        void shouldReturnPdfMindConfig() {
            PromptConfig.AppPromptConfig config = promptRegistry.getConfig("pdfmind");
            assertNotNull(config);
            assertEquals("semantic-with-page-boundary", config.getChunkStrategy());
            assertEquals(15, config.getTopK());
            assertEquals(0.75, config.getSimilarityThreshold(), 0.01);
            assertEquals("page-number", config.getCitationFormat());
            assertTrue(config.getMetadataFields().contains("page_number"));
            assertTrue(config.getMetadataFields().contains("chapter"));
        }

        @Test
        @DisplayName("should throw for unregistered app")
        void shouldThrowForUnregistered() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> promptRegistry.getConfig("nonexistent")
            );
            assertTrue(ex.getMessage().contains("nonexistent"));
        }
    }

    @Nested
    @DisplayName("isRegistered()")
    class IsRegistered {

        @Test
        @DisplayName("should return true for registered apps")
        void shouldReturnTrueForRegistered() {
            assertTrue(promptRegistry.isRegistered("insighttube"));
            assertTrue(promptRegistry.isRegistered("pdfmind"));
        }

        @Test
        @DisplayName("should return false for unregistered apps")
        void shouldReturnFalseForUnregistered() {
            assertFalse(promptRegistry.isRegistered("unknown-app"));
        }
    }

    @Nested
    @DisplayName("getRegisteredApps()")
    class GetRegisteredApps {

        @Test
        @DisplayName("should return all registered app IDs")
        void shouldReturnAllApps() {
            var apps = promptRegistry.getRegisteredApps();
            assertEquals(2, apps.size());
            assertTrue(apps.contains("insighttube"));
            assertTrue(apps.contains("pdfmind"));
        }
    }
}
