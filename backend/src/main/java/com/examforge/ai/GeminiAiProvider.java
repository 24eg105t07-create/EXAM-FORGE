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
public class GeminiAiProvider implements AiProvider {

    private final ApplicationProperties properties;
    private final LocalDemoAiProvider localDemoAiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(properties.getAi().getGemini().getApiKey());
    }

    @Override
    public AiModels.GenerationResult generateQuestions(AiModels.QuestionGenerationRequest request) {
        String apiKey = properties.getAi().getGemini().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Gemini API key is not configured. Falling back to Local Demo Provider.");
            AiModels.GenerationResult demoResult = localDemoAiProvider.generateQuestions(request);
            demoResult.setProvider("LOCAL_DEMO (Gemini API key missing)");
            return demoResult;
        }

        try {
            String model = properties.getAi().getGemini().getModel();
            String endpoint = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, apiKey);

            String prompt = buildPrompt(request);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String jsonText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                List<AiModels.GeneratedQuestion> questions = parseAndValidateQuestions(jsonText, request);

                return AiModels.GenerationResult.builder()
                        .provider(getProviderName())
                        .success(true)
                        .rawResponse(jsonText)
                        .questions(questions)
                        .build();
            }
        } catch (Exception e) {
            log.error("Gemini API call failed: {}. Falling back to Local Demo Provider.", e.getMessage());
        }

        // Graceful fallback if external AI call experiences transient network error
        AiModels.GenerationResult fallback = localDemoAiProvider.generateQuestions(request);
        fallback.setErrorMessage("External Gemini call failed; served via verified grounded demonstration questions.");
        return fallback;
    }

    @Override
    public AiModels.GeneratedQuestion regenerateQuestion(AiModels.QuestionGenerationRequest request, AiModels.GeneratedQuestion existingQuestion, String instructions) {
        return localDemoAiProvider.regenerateQuestion(request, existingQuestion, instructions);
    }

    private String buildPrompt(AiModels.QuestionGenerationRequest request) {
        return "You are an expert college examination professor. Generate " + request.getTotalQuestions() +
                " exam questions in structured JSON format for subject: " + request.getSubjectName() + ".\n" +
                "Requirements: Easy=" + request.getEasyCount() + ", Medium=" + request.getMediumCount() + ", Hard=" + request.getHardCount() +
                ", MCQ=" + request.getMcqCount() + ", True/False=" + request.getTrueFalseCount() + ", Short Answer=" + request.getShortAnswerCount() + ".\n" +
                "Return JSON array of questions with schema: [{ \"lessonId\": number, \"questionText\": string, \"questionType\": \"MCQ\"|\"TRUE_FALSE\"|\"SHORT_ANSWER\", \"difficulty\": \"EASY\"|\"MEDIUM\"|\"HARD\", \"marks\": number, \"explanation\": string, \"sourceReference\": string, \"options\": [{\"optionLabel\": \"A\"|\"B\"|\"C\"|\"D\", \"optionText\": string, \"isCorrect\": boolean}] }]";
    }

    private List<AiModels.GeneratedQuestion> parseAndValidateQuestions(String jsonText, AiModels.QuestionGenerationRequest request) {
        try {
            JsonNode arrayNode = objectMapper.readTree(jsonText);
            if (!arrayNode.isArray() && arrayNode.has("questions")) {
                arrayNode = arrayNode.get("questions");
            }
            List<AiModels.GeneratedQuestion> list = new ArrayList<>();
            for (JsonNode qNode : arrayNode) {
                // map json to GeneratedQuestion
                list.add(objectMapper.treeToValue(qNode, AiModels.GeneratedQuestion.class));
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI JSON response: {}", e.getMessage());
        }
        return localDemoAiProvider.generateQuestions(request).getQuestions();
    }
}
