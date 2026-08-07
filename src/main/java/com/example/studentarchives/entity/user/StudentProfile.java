package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "student_profiles")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class StudentProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "political_status", length = 50)
    private String politicalStatus;

    /** 学生状态：current=在校生 fresh_graduate=应届毕业生 graduated=已毕业 */
    @Column(name = "student_status", length = 30)
    private String studentStatus;

    /** 学历层次：associate=专科 undergraduate=本科 master=研究生/硕士 doctor=博士 postdoctor=博士后 */
    @Column(name = "degree_type", length = 30)
    private String degreeType;

    @Column(name = "volunteer_hours", precision = 8, scale = 2)
    private BigDecimal volunteerHours;

    @Lob
    @Column(name = "self_evaluation", columnDefinition = "TEXT")
    private String selfEvaluation;
}
