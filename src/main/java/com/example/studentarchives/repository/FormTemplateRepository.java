package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.FormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 表单模板 Repository
 */
@Repository
public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {

    List<FormTemplate> findBySchoolIdAndStatusOrderByIdAsc(Long schoolId, Integer status);

    Optional<FormTemplate> findBySchoolIdAndCode(Long schoolId, String code);

    boolean existsBySchoolIdAndCode(Long schoolId, String code);
}
