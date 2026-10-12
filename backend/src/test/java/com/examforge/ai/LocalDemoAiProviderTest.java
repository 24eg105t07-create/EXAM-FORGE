package com.examforge.ai;

import com.examforge.entity.Difficulty;
import com.examforge.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalDemoAiProviderTest {

    private final LocalDemoAiProvider provider = new LocalDemoAiProvider();

    @Test
    @DisplayName("Should generate exact requested total questions adhering to blueprint distribution")
    void testLocalGenerationCountAndBreakdown() {
        AiModels.QuestionGenerationRequest request = AiModels.QuestionGenerationRequest.builder()
                .subjectName("Database Management Systems")
                .subjectCode("CS301")
                .totalQuestions(20)
                .easyCount(6)
                .mediumCount(10)
                .hardCount(4)
                .mcqCount(14)
                .trueFalseCount(4)
                .shortAnswerCount(2)
                .marksPerQuestion(BigDecimal.valueOf(1.0))
                .lessons(List.of(
                        AiModels.LessonContext.builder().lessonId(1L).title("Relational Model and SQL").targetQuestionCount(10).extractedDocumentContent("SQL DDL DML").build(),
                        AiModels.LessonContext.builder().lessonId(2L).title("Transactions and Concurrency Control").targetQuestionCount(10).extractedDocumentContent("ACID 2PL").build()
                ))
                .build();

        AiModels.GenerationResult result = provider.generateQuestions(request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("LOCAL_DEMO", result.getProvider());
        assertEquals(20, result.getQuestions().size(), "Must generate exactly 20 questions");

        // Verify MCQ options structure
        long mcqCount = result.getQuestions().stream().filter(q -> q.getQuestionType() == QuestionType.MCQ).count();
        assertTrue(mcqCount >= 10, "MCQs should be represented properly");

        for (AiModels.GeneratedQuestion q : result.getQuestions()) {
            assertNotNull(q.getQuestionText());
            assertNotNull(q.getDifficulty());
            assertNotNull(q.getExplanation());
            assertNotNull(q.getSourceReference());

            if (q.getQuestionType() == QuestionType.MCQ) {
                assertEquals(4, q.getOptions().size(), "MCQs must have 4 options");
                long correctCount = q.getOptions().stream().filter(AiModels.GeneratedOption::isCorrect).count();
                assertEquals(1, correctCount, "MCQs must have exactly one correct option");
            } else if (q.getQuestionType() == QuestionType.TRUE_FALSE) {
                assertEquals(2, q.getOptions().size(), "True/False must have 2 options");
                long correctCount = q.getOptions().stream().filter(AiModels.GeneratedOption::isCorrect).count();
                assertEquals(1, correctCount, "True/False must have exactly one correct option");
            }
        }
    }
}
