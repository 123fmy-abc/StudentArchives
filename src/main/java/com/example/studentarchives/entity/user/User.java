package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import com.example.studentarchives.entity.foundation.School;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "school_id")
    private Long schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", insertable = false, updatable = false)
    private School school;

    @Column(name = "user_no", length = 50)
    private String userNo;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "remember_token", length = 100)
    private String rememberToken;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "status")
    private Integer status;
}
