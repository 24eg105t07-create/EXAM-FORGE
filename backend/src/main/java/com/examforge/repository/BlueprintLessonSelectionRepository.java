package com.examforge.repository;

import com.examforge.entity.BlueprintLessonSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlueprintLessonSelectionRepository extends JpaRepository<BlueprintLessonSelection, Long> {
    List<BlueprintLessonSelection> findByBlueprintId(Long blueprintId);
}
