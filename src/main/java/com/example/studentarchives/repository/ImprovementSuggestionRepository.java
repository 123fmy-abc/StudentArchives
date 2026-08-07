package com.example.studentarchives.repository;

import com.example.studentarchives.entity.weakness.ImprovementSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AI/教师改进建议 Repository（对应表 improvement_suggestions）
 */
@Repository
public interface ImprovementSuggestionRepository extends JpaRepository<ImprovementSuggestion, Long> {
}
