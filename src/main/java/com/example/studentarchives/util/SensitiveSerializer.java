package com.example.studentarchives.util;

import com.example.studentarchives.annotation.Sensitive;
import com.example.studentarchives.enums.SensitiveType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Jackson 敏感字段脱敏序列化器
 * <p>
 * 配合 {@link Sensitive} 注解使用，在序列化时自动对手机号、邮箱等
 * 敏感字段进行脱敏处理。
 * <p>
 * 示例：
 * <ul>
 *   <li>手机号：13812345678 → 138****5678</li>
 *   <li>邮箱：   zhangsan@example.com → zha***@example.com</li>
 * </ul>
 */
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final SensitiveType type;

    /** 无参构造（Jackson 反射调用） */
    public SensitiveSerializer() {
        this.type = null;
    }

    public SensitiveSerializer(SensitiveType type) {
        this.type = type;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(mask(value));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
            throws JsonMappingException {
        if (property != null) {
            Sensitive annotation = property.getAnnotation(Sensitive.class);
            if (annotation != null) {
                return new SensitiveSerializer(annotation.value());
            }
        }
        return this;
    }

    // ==================== 脱敏逻辑 ====================

    private String mask(String value) {
        if (value.isEmpty()) return value;
        return switch (type) {
            case PHONE -> maskPhone(value);
            case EMAIL -> maskEmail(value);
        };
    }

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间 4 位用 **** 代替
     * <pre>13812345678 → 138****5678</pre>
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 邮箱脱敏：根据 @ 前字符数动态保留前缀
     * <ul>
     *   <li>1 字符（a@qq.com）       → a***@qq.com</li>
     *   <li>2 字符（ja@163.com）     → ja***@163.com</li>
     *   <li>3+ 字符（zhangsan@...）  → zha***@outlook.com</li>
     * </ul>
     */
    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // 异常格式：@qq.com → ***@qq.com
            return "***" + email;
        }
        int keepCount = Math.min(atIndex, 3);
        return email.substring(0, keepCount) + "***" + email.substring(atIndex);
    }
}
