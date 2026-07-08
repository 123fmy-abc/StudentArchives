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

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Lob
    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "setting_group", nullable = false, length = 50)
    private String settingGroup;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_editable", nullable = false)
    private byte isEditable;
}
