package com.examforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "exam_paper_questions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_paper_id", "question_order"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamPaperQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_paper_id", nullable = false)
    private ExamPaper examPaper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBank question;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal marks = BigDecimal.valueOf(1.0);
}
