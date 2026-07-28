package com.example.studentarchives.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * <p>
 * 统一管理项目中使用的 DateTimeFormatter 格式，避免重复创建和格式不一致。
 */
@UtilityClass
public class DateUtils {

    /** 完整时间戳：yyyy-MM-dd HH:mm:ss.SSS */
    public static final DateTimeFormatter DTF_FULL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 精确到分钟：yyyy-MM-dd HH:mm */
    public static final DateTimeFormatter DTF_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 日志用时间戳：yyyyMMdd-HHmmss */
    public static final DateTimeFormatter DTF_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** 标准日期时间：yyyy-MM-dd HH:mm:ss */
    public static final DateTimeFormatter DTF_STANDARD = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 生成当前时间的完整时间戳字符串 */
    public static String nowFull() {
        return LocalDateTime.now().format(DTF_FULL);
    }

    /** 生成当前时间的精简时间戳字符串 */
    public static String nowCompact() {
        return LocalDateTime.now().format(DTF_COMPACT);
    }

    /** 生成当前时间的分钟精度字符串 */
    public static String nowMinute() {
        return ZonedDateTime.now(ZoneId.systemDefault()).format(DTF_MINUTE);
    }
}
