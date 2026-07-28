package com.example.studentarchives.entity.user;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_contact_infos")
public class UserContactInfo extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "emergency_name", length = 50)
    private String emergencyName;

    @Column(name = "emergency_relation", length = 30)
    private String emergencyRelation;

    @Column(name = "emergency_phone", length = 20)
    private String emergencyPhone;

    @Column(name = "updated_by")
    private Long updatedBy;
}
