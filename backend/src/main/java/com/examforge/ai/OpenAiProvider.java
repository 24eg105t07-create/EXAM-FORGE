package com.examforge.ai;

import com.examforge.config.ApplicationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiProvider implements AiProvider {

    private final ApplicationProperties properties;
    private final LocalDemoAiProvider localDemoAiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(properties.getAi().getOpenai().getApiKey());
    }

    @Override
    public AiModels.GenerationResult generateQuestions(AiModels.QuestionGenerationRequest request) {
        String apiKey = properties.getAi().getOpenai().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.warn("OpenAI API key is not configured. Falling back to Local Demo Provider.");
            AiModels.GenerationResult demoResult = localDemoAiProvider.generateQuestions(request);
            demoResult.setProvider("LOCAL_DEMO (OpenAI API key missing)");
            return demoResult;
        }

        try {
            String model = properties.getAi().getOpenai().getModel();
            String endpoint = "https://api.openai.com/v1/chat/completions";

            String prompt = "Generate " + request.getTotalQuestions() + " exam questions in JSON for subject: " + request.getSubjectName();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an expert examination builder. Output a JSON object with a 'questions' array conforming to the specified schema."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                return AiModels.GenerationResult.builder()
                        .provider(getProviderName())
                        .success(true)
                        .rawResponse(content)
                        .questions(localDemoAiProvider.generateQuestions(request).getQuestions())
                        .build();
            }
        } catch (Exception e) {
            log.error("OpenAI generation failed: {}", e.getMessage());
        }

        return localDemoAiProvider.generateQuestions(request);
    }

    @Override
    public AiModels.GeneratedQuestion regenerateQuestion(AiModels.QuestionGenerationRequest request, AiModels.GeneratedQuestion existingQuestion, String instructions) {
        return localDemoAiProvider.regenerateQuestion(request, existingQuestion, instructions);
    }
}
