package com.example.studentarchives.entity.foundation;

import com.example.studentarchives.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key")
    private String settingKey;

    @Lob
    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_group")
    private String settingGroup;

    @Column(name = "value_type")
    private String valueType;

    private String description;

    @Column(name = "is_editable")
    private Integer isEditable;
}
