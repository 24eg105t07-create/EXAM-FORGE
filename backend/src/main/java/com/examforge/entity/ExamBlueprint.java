package com.examforge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_blueprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamBlueprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "max_marks", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "easy_percentage", nullable = false)
    private Integer easyPercentage;

    @Column(name = "medium_percentage", nullable = false)
    private Integer mediumPercentage;

    @Column(name = "hard_percentage", nullable = false)
    private Integer hardPercentage;

    @Column(name = "mcq_count", nullable = false)
    private Integer mcqCount;

    @Column(name = "true_false_count", nullable = false)
    private Integer trueFalseCount;

    @Column(name = "short_answer_count", nullable = false)
    private Integer shortAnswerCount;

    @Column(name = "marks_per_question", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal marksPerQuestion = BigDecimal.valueOf(1.0);

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(length = 50)
    @Builder.Default
    private String status = "DRAFT";

    @OneToMany(mappedBy = "blueprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BlueprintLessonSelection> lessonSelections = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
