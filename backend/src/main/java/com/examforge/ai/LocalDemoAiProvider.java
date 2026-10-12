package com.examforge.ai;

import com.examforge.entity.Difficulty;
import com.examforge.entity.QuestionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
public class LocalDemoAiProvider implements AiProvider {

    @Override
    public String getProviderName() {
        return "LOCAL_DEMO";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiModels.GenerationResult generateQuestions(AiModels.QuestionGenerationRequest request) {
        log.info("Generating {} questions using Local Demonstration Provider (No external AI key required)", request.getTotalQuestions());

        List<AiModels.GeneratedQuestion> generatedList = new ArrayList<>();

        // Distribute requested question counts across lessons and difficulties
        int easyRemaining = request.getEasyCount();
        int mediumRemaining = request.getMediumCount();
        int hardRemaining = request.getHardCount();

        int mcqRemaining = request.getMcqCount();
        int tfRemaining = request.getTrueFalseCount();
        int saRemaining = request.getShortAnswerCount();

        int questionIndex = 1;

        for (AiModels.LessonContext lesson : request.getLessons()) {
            int targetForLesson = lesson.getTargetQuestionCount();
            if (targetForLesson <= 0) targetForLesson = Math.max(1, request.getTotalQuestions() / request.getLessons().size());

            for (int i = 0; i < targetForLesson && generatedList.size() < request.getTotalQuestions(); i++) {
                // Determine difficulty
                Difficulty diff;
                if (easyRemaining > 0) {
                    diff = Difficulty.EASY;
                    easyRemaining--;
                } else if (mediumRemaining > 0) {
                    diff = Difficulty.MEDIUM;
                    mediumRemaining--;
                } else {
                    diff = Difficulty.HARD;
                    hardRemaining--;
                }

                // Determine question type
                QuestionType qType;
                if (mcqRemaining > 0) {
                    qType = QuestionType.MCQ;
                    mcqRemaining--;
                } else if (tfRemaining > 0) {
                    qType = QuestionType.TRUE_FALSE;
                    tfRemaining--;
                } else if (saRemaining > 0) {
                    qType = QuestionType.SHORT_ANSWER;
                    saRemaining--;
                } else {
                    qType = QuestionType.MCQ;
                }

                AiModels.GeneratedQuestion q = buildDeterministicQuestion(
                        questionIndex++,
                        lesson,
                        request.getSubjectName(),
                        diff,
                        qType,
                        request.getMarksPerQuestion()
                );
                generatedList.add(q);
            }
        }

        // Fill any remainder to reach exact total
        while (generatedList.size() < request.getTotalQuestions()) {
            AiModels.LessonContext fallbackLesson = request.getLessons().get(generatedList.size() % request.getLessons().size());
            generatedList.add(buildDeterministicQuestion(
                    questionIndex++,
                    fallbackLesson,
                    request.getSubjectName(),
                    Difficulty.MEDIUM,
                    QuestionType.MCQ,
                    request.getMarksPerQuestion()
            ));
        }

        return AiModels.GenerationResult.builder()
                .provider(getProviderName())
                .success(true)
                .rawResponse("{\"status\":\"success\",\"mode\":\"LOCAL_DEMO_PROVIDER\",\"count\":" + generatedList.size() + "}")
                .questions(generatedList)
                .build();
    }

    @Override
    public AiModels.GeneratedQuestion regenerateQuestion(AiModels.QuestionGenerationRequest request, AiModels.GeneratedQuestion existingQuestion, String instructions) {
        AiModels.LessonContext lesson = request.getLessons().stream()
                .filter(l -> l.getLessonId().equals(existingQuestion.getLessonId()))
                .findFirst()
                .orElse(request.getLessons().getFirst());

        AiModels.GeneratedQuestion regenerated = buildDeterministicQuestion(
                999,
                lesson,
                request.getSubjectName(),
                existingQuestion.getDifficulty(),
                existingQuestion.getQuestionType(),
                existingQuestion.getMarks()
        );
        regenerated.setQuestionText(regenerated.getQuestionText() + " (Revised: " + (instructions != null ? instructions : "Alternative focus") + ")");
        return regenerated;
    }

    private AiModels.GeneratedQuestion buildDeterministicQuestion(
            int idx,
            AiModels.LessonContext lesson,
            String subject,
            Difficulty diff,
            QuestionType qType,
            BigDecimal marks
    ) {
        String title = lesson.getTitle().toLowerCase();
        String sourceRef = "Lesson: " + lesson.getTitle() + " (Unit " + lesson.getUnitNumber() + ")";

        if (qType == QuestionType.MCQ) {
            return buildMcq(idx, title, lesson.getLessonId(), diff, marks, sourceRef);
        } else if (qType == QuestionType.TRUE_FALSE) {
            return buildTrueFalse(idx, title, lesson.getLessonId(), diff, marks, sourceRef);
        } else {
            return buildShortAnswer(idx, title, lesson.getLessonId(), diff, marks, sourceRef);
        }
    }

    private AiModels.GeneratedQuestion buildMcq(int idx, String title, Long lessonId, Difficulty diff, BigDecimal marks, String sourceRef) {
        if (title.contains("sql") || title.contains("relational") || title.contains("query")) {
            return switch (idx % 4) {
                case 1 -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("Which of the following SQL clauses is specifically used to filter rows AFTER group aggregations have occurred?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("The HAVING clause filters groups created by the GROUP BY clause, whereas the WHERE clause filters rows prior to aggregation.")
                        .sourceReference(sourceRef + " - SQL Aggregations and Clauses")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "WHERE", false),
                                new AiModels.GeneratedOption("B", "HAVING", true),
                                new AiModels.GeneratedOption("C", "GROUP BY", false),
                                new AiModels.GeneratedOption("D", "ORDER BY", false)
                        )).build();
                case 2 -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("In relational database design, which normal form enforces that all non-key attributes are fully functionally dependent on the entire primary key?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("Second Normal Form (2NF) eliminates partial dependency of non-prime attributes on candidate keys.")
                        .sourceReference(sourceRef + " - Relational Database Normalization")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "First Normal Form (1NF)", false),
                                new AiModels.GeneratedOption("B", "Second Normal Form (2NF)", true),
                                new AiModels.GeneratedOption("C", "Third Normal Form (3NF)", false),
                                new AiModels.GeneratedOption("D", "Boyce-Codd Normal Form (BCNF)", false)
                        )).build();
                case 3 -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("Which type of relational join returns all rows from the left table along with matching rows from the right table, filling with NULL when no match exists?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("LEFT OUTER JOIN preserves every record from the left relation, matching available entries on the right.")
                        .sourceReference(sourceRef + " - Relational Algebra and Joins")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "INNER JOIN", false),
                                new AiModels.GeneratedOption("B", "LEFT OUTER JOIN", true),
                                new AiModels.GeneratedOption("C", "FULL OUTER JOIN", false),
                                new AiModels.GeneratedOption("D", "CROSS JOIN", false)
                        )).build();
                default -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("What does the integrity constraint 'ON DELETE CASCADE' enforce in a foreign key relationship?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("ON DELETE CASCADE automatically deletes all dependent child rows when the corresponding referenced parent row is deleted.")
                        .sourceReference(sourceRef + " - Referential Integrity Constraints")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "Prevents deletion of the referenced row", false),
                                new AiModels.GeneratedOption("B", "Deletes dependent child rows automatically", true),
                                new AiModels.GeneratedOption("C", "Sets foreign keys in child rows to NULL", false),
                                new AiModels.GeneratedOption("D", "Generates a compiler warning on drop", false)
                        )).build();
            };
        } else {
            // Transactions / Concurrency control or generic
            return switch (idx % 3) {
                case 1 -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("Which ACID property ensures that all transaction operations either complete entirely or leave the database completely unaltered?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("Atomicity guarantees all-or-nothing execution for database transactions.")
                        .sourceReference(sourceRef + " - ACID Properties Overview")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "Atomicity", true),
                                new AiModels.GeneratedOption("B", "Consistency", false),
                                new AiModels.GeneratedOption("C", "Isolation", false),
                                new AiModels.GeneratedOption("D", "Durability", false)
                        )).build();
                case 2 -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("What does the Strict Two-Phase Locking (Strict 2PL) protocol guarantee regarding schedules?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("Strict 2PL guarantees both serializability and strict (cascadeless) recovery by holding exclusive locks until transaction commit/abort.")
                        .sourceReference(sourceRef + " - Concurrency Control Protocols")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "Conflict serializability and absence of cascading aborts", true),
                                new AiModels.GeneratedOption("B", "Guaranteed immunity to all deadlocks", false),
                                new AiModels.GeneratedOption("C", "Immediate validation without locking overhead", false),
                                new AiModels.GeneratedOption("D", "Read uncommitted isolation levels", false)
                        )).build();
                default -> AiModels.GeneratedQuestion.builder()
                        .lessonId(lessonId)
                        .questionText("Which component of a DBMS writes log records to stable storage during checkpointing to assure Durability?")
                        .questionType(QuestionType.MCQ)
                        .difficulty(diff)
                        .marks(marks)
                        .explanation("The Recovery Manager writes write-ahead logs (WAL) to ensure updates persist after failures.")
                        .sourceReference(sourceRef + " - Crash Recovery and WAL")
                        .options(List.of(
                                new AiModels.GeneratedOption("A", "Query Optimizer", false),
                                new AiModels.GeneratedOption("B", "Recovery Manager", true),
                                new AiModels.GeneratedOption("C", "Storage Allocator", false),
                                new AiModels.GeneratedOption("D", "Data Dictionary Cache", false)
                        )).build();
            };
        }
    }

    private AiModels.GeneratedQuestion buildTrueFalse(int idx, String title, Long lessonId, Difficulty diff, BigDecimal marks, String sourceRef) {
        boolean isTrue = (idx % 2 == 0);
        String text = isTrue
                ? "In SQL, primary key columns cannot accept NULL values and must uniquely identify each row."
                : "A dirty read occurs when a transaction reads committed data that was modified by an aborted transaction.";
        String exp = isTrue
                ? "Primary keys implicitly mandate NOT NULL and UNIQUE constraints."
                : "A dirty read occurs when a transaction reads uncommitted modifications made by another concurrent transaction.";

        return AiModels.GeneratedQuestion.builder()
                .lessonId(lessonId)
                .questionText(text)
                .questionType(QuestionType.TRUE_FALSE)
                .difficulty(diff)
                .marks(marks)
                .explanation(exp)
                .sourceReference(sourceRef)
                .options(List.of(
                        new AiModels.GeneratedOption("A", "True", isTrue),
                        new AiModels.GeneratedOption("B", "False", !isTrue)
                )).build();
    }

    private AiModels.GeneratedQuestion buildShortAnswer(int idx, String title, Long lessonId, Difficulty diff, BigDecimal marks, String sourceRef) {
        return AiModels.GeneratedQuestion.builder()
                .lessonId(lessonId)
                .questionText("Briefly explain how a Write-Ahead Log (WAL) ensures durability in database transaction recovery.")
                .questionType(QuestionType.SHORT_ANSWER)
                .difficulty(diff)
                .marks(marks)
                .explanation("WAL mandates that any change log record must be flushed to stable storage before the corresponding dirty page is written to disk, enabling REDO/UNDO after a crash.")
                .sourceReference(sourceRef + " - Write Ahead Logging")
                .options(Collections.emptyList())
                .build();
    }
}
