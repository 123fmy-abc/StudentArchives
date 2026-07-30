package com.example.studentarchives.annotation;

import com.example.studentarchives.enums.SensitiveType;
import com.example.studentarchives.util.SensitiveSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * 标记敏感字段，返回时自动脱敏
 * <p>
 * 使用方式：
 * <pre>{@code
 * @Sensitive(SensitiveType.PHONE)
 * private String phone;
 *
 * @Sensitive(SensitiveType.EMAIL)
 * private String email;
 * }</pre>
 * <p>
 * 通过 {@link SensitiveSerializer} 在 Jackson 序列化时拦截处理。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
@Documented
public @interface Sensitive {

    /** 脱敏类型 */
    SensitiveType value();
}
