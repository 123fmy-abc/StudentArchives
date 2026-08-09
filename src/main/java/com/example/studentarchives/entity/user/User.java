package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "users")
@SQLRestriction(BaseEntity.DELETED_AT_IS_NULL)
public class User extends BaseEntity {

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @Column(name = "user_no", nullable = false, length = 50)
    private String userNo;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @Column(name = "refresh_token_version", nullable = false)
    private Integer refreshTokenVersion = 0;
}
