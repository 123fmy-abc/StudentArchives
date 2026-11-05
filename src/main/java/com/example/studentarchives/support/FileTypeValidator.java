package com.example.studentarchives.support;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 上传文件类型校验器
 * <p>
 * 扩展名白名单 → 扩展名-MIME 映射 → 文件头（魔数）三重校验，
 * 不信任客户端声明的 Content-Type，阻断 HTML/SVG/脚本/双扩展名/伪造 MIME。
 * <p>
 * 图片类以文件头检测的真实格式为准：扩展名与内容不一致时（如 JPG 内容命名 .png），
 * 只要真实格式仍在图片白名单内即放行，并按真实格式返回 MIME；非图片格式仍严格匹配魔数。
 */
public final class FileTypeValidator {

    /** 默认允许的扩展名（attachment_limits 未配置时兜底） */
    public static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "csv", "zip");

    /** 允许的图片扩展名 */
    public static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 扩展名 → 服务端判定的 MIME 类型 */
    private static final Map<String, String> EXTENSION_MIME = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("zip", "application/zip"));

    /** 读取文件头字节数 */
    private static final int HEADER_LENGTH = 512;

    private FileTypeValidator() {
    }

    /**
     * 校验文件类型并返回服务端判定的 MIME 类型。
     *
     * @param file              上传文件
     * @param allowedExtensions 允许的扩展名（空集合时使用默认白名单）
     * @return 服务端判定的 MIME 类型
     */
    public static String validate(MultipartFile file, Collection<String> allowedExtensions) {
        String ext = extractExtension(file.getOriginalFilename());
        if (ext == null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "文件缺少扩展名");
        }
        Set<String> allowed = normalize(allowedExtensions);
        if (allowed.isEmpty()) {
            allowed = DEFAULT_ALLOWED_EXTENSIONS;
        }
        if (!allowed.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                    "不支持的文件格式，允许: " + String.join(", ", allowed));
        }
        String mimeType = EXTENSION_MIME.get(ext);
        if (mimeType == null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "不支持的文件格式: " + ext);
        }
        byte[] header = readHeader(file);
        // 图片类以内容为准：扩展名与魔数不一致时（如 JPG 内容存成 .png 名），
        // 只要检测出的真实格式仍是允许的图片格式就放行，并按真实格式返回 MIME
        if (IMAGE_EXTENSIONS.contains(ext)) {
            String detected = detectImageFormat(header);
            if (detected == null) {
                throw formatError(ext);
            }
            return detected;
        }
        checkSignature(ext, header);
        return mimeType;
    }

    /**
     * 按文件名扩展名返回服务端判定的 MIME 类型，未知类型回退 application/octet-stream。
     */
    public static String mimeTypeForFilename(String filename) {
        String ext = extractExtension(filename);
        return ext != null ? EXTENSION_MIME.getOrDefault(ext, "application/octet-stream")
                : "application/octet-stream";
    }

    /** 提取小写扩展名（不含点号） */
    public static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT)
                : null;
    }

    private static Set<String> normalize(Collection<String> extensions) {
        Set<String> result = new HashSet<>();
        if (extensions == null) {
            return result;
        }
        for (String extension : extensions) {
            if (extension != null && !extension.isBlank()) {
                result.add(extension.trim().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return inputStream.readNBytes(HEADER_LENGTH);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "文件读取失败");
        }
    }

    /**
     * 按魔数检测图片真实格式，返回对应 MIME；无法识别时返回 null。
     */
    private static String detectImageFormat(byte[] header) {
        if (header.length >= 3 && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (header.length >= 8 && startsWithAscii(header, "\u0089PNG\r\n\u001A\n")) {
            return "image/png";
        }
        if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) {
            return "image/gif";
        }
        if (header.length >= 12 && startsWithAscii(header, "RIFF")
                && startsWithAsciiAt(header, 8, "WEBP")) {
            return "image/webp";
        }
        if (header.length >= 2 && header[0] == 0x42 && header[1] == 0x4D) {
            return "image/bmp";
        }
        return null;
    }

    private static void checkSignature(String ext, byte[] header) {
        if (header.length == 0) {
            throw formatError(ext);
        }
        switch (ext) {
            case "pdf" -> requireMagic(header, ext, 0x25, 0x50, 0x44, 0x46, 0x2D);
            case "zip", "docx", "xlsx", "pptx" -> requireZip(header, ext);
            case "doc", "xls", "ppt" -> requireMagic(header, ext,
                    0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "txt", "csv" -> requirePlainText(header, ext);
            default -> throw formatError(ext);
        }
    }

    private static void requireMagic(byte[] header, String ext, int... expected) {
        if (header.length < expected.length) {
            throw formatError(ext);
        }
        for (int i = 0; i < expected.length; i++) {
            if ((header[i] & 0xFF) != expected[i]) {
                throw formatError(ext);
            }
        }
    }

    private static void requireZip(byte[] header, String ext) {
        if (header.length < 4 || header[0] != 'P' || header[1] != 'K') {
            throw formatError(ext);
        }
        int low = header[2] & 0xFF;
        int high = header[3] & 0xFF;
        boolean valid = (low == 0x03 && high == 0x04)
                || (low == 0x05 && high == 0x06)
                || (low == 0x07 && high == 0x08);
        if (!valid) {
            throw formatError(ext);
        }
    }

    private static void requirePlainText(byte[] header, String ext) {
        for (byte b : header) {
            if (b == 0x00) {
                throw formatError(ext);
            }
        }
    }

    private static boolean startsWithAscii(byte[] header, String ascii) {
        return startsWithAsciiAt(header, 0, ascii);
    }

    private static boolean startsWithAsciiAt(byte[] header, int offset, String ascii) {
        if (header.length < offset + ascii.length()) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if (header[offset + i] != (byte) ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static BusinessException formatError(String ext) {
        return new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                "文件内容与扩展名不匹配: " + ext);
    }
}
