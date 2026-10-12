package com.examforge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blueprint_lesson_selections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlueprintLessonSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private ExamBlueprint blueprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "target_percentage", nullable = false)
    private Integer targetPercentage;

    @Column(name = "question_count", nullable = false)
    private Integer questionCount;
}
