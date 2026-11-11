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

    /** MIME 类型 → 规范扩展名（存储对象 key 后缀以服务端判定格式为准，不沿用客户端文件名） */
    private static final Map<String, String> MIME_EXTENSION = Map.ofEntries(
            Map.entry("image/jpeg", "jpg"),
            Map.entry("image/png", "png"),
            Map.entry("image/gif", "gif"),
            Map.entry("image/webp", "webp"),
            Map.entry("image/bmp", "bmp"),
            Map.entry("application/pdf", "pdf"),
            Map.entry("application/msword", "doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
            Map.entry("application/vnd.ms-excel", "xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
            Map.entry("application/vnd.ms-powerpoint", "ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"),
            Map.entry("text/plain", "txt"),
            Map.entry("text/csv", "csv"),
            Map.entry("application/zip", "zip"));

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
        return checkSignature(ext, readHeader(file), allowed);
    }

    /**
     * 按文件名扩展名返回服务端判定的 MIME 类型，未知类型回退 application/octet-stream。
     */
    public static String mimeTypeForFilename(String filename) {
        String ext = extractExtension(filename);
        return ext != null ? EXTENSION_MIME.getOrDefault(ext, "application/octet-stream")
                : "application/octet-stream";
    }

    /**
     * 按服务端判定的 MIME 类型返回规范扩展名（不含点号），未知类型返回 null。
     * <p>
     * 用于生成存储对象 key：扩展名必须与真实内容一致，不能沿用客户端文件名后缀。
     */
    public static String extensionForMime(String mimeType) {
        return mimeType != null ? MIME_EXTENSION.get(mimeType) : null;
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
     * 按声明扩展名校验文件头魔数；不匹配时按魔数反向检测真实格式，
     * 真实格式属于本次上传允许的白名单时放行并按真实格式返回 MIME，
     * 否则抛出明确错误。
     */
    private static String checkSignature(String ext, byte[] header, Set<String> allowed) {
        if (header.length == 0) {
            throw formatError(ext);
        }
        boolean matched = switch (ext) {
            case "jpg", "jpeg" -> hasMagic(header, 0xFF, 0xD8, 0xFF);
            case "png" -> hasMagic(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a");
            case "webp" -> header.length >= 12 && startsWithAscii(header, "RIFF")
                    && startsWithAsciiAt(header, 8, "WEBP");
            case "bmp" -> hasMagic(header, 0x42, 0x4D);
            case "pdf" -> hasMagic(header, 0x25, 0x50, 0x44, 0x46, 0x2D);
            case "zip", "docx", "xlsx", "pptx" -> isZipHeader(header);
            case "doc", "xls", "ppt" -> hasMagic(header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "txt", "csv" -> isPlainText(header);
            default -> false;
        };
        if (matched) {
            return EXTENSION_MIME.get(ext);
        }
        // 声明扩展名与内容不符：按魔数检测真实格式，属于白名单则按真实格式放行
        String real = detectSignature(header);
        if (real != null && isAllowedFormat(real, allowed)) {
            return EXTENSION_MIME.get(real);
        }
        if (real != null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                    "文件实际格式为 " + real + "，与扩展名 ." + ext + " 不符，且该格式不在允许范围内");
        }
        throw formatError(ext);
    }

    /** jpg 与 jpeg 视为同一格式，其余按扩展名精确匹配 */
    private static boolean isAllowedFormat(String ext, Set<String> allowed) {
        if (allowed.contains(ext)) {
            return true;
        }
        return "jpg".equals(ext) && (allowed.contains("jpeg") || allowed.contains("jpg"))
                || "jpeg".equals(ext) && allowed.contains("jpg");
    }

    /** 按魔数识别真实格式，无法唯一识别（zip 族、纯文本、未知）返回 null */
    private static String detectSignature(byte[] header) {
        if (hasMagic(header, 0xFF, 0xD8, 0xFF)) {
            return "jpg";
        }
        if (hasMagic(header, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "png";
        }
        if (startsWithAscii(header, "GIF87a") || startsWithAscii(header, "GIF89a")) {
            return "gif";
        }
        if (header.length >= 12 && startsWithAscii(header, "RIFF")
                && startsWithAsciiAt(header, 8, "WEBP")) {
            return "webp";
        }
        if (hasMagic(header, 0x42, 0x4D)) {
            return "bmp";
        }
        if (hasMagic(header, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return "pdf";
        }
        // OLE 复合文档：doc/xls/ppt 共用同一魔数，无法唯一区分，统一按 doc 处理
        if (hasMagic(header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            return "doc";
        }
        // ISOBMFF 容器：iPhone HEIC 照片常被改名为 .jpeg 上传，给出明确提示
        if (header.length >= 12 && startsWithAsciiAt(header, 4, "ftyp")) {
            return isHeicBrand(header) ? "heic" : "mov";
        }
        return null;
    }

    /** 偏移 8 起为品牌码，heic/heif/mif1/msf1/hevc 均为 iPhone HEIC 照片家族 */
    private static boolean isHeicBrand(byte[] header) {
        String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return switch (brand) {
            case "heic", "heif", "mif1", "msf1", "hevc" -> true;
            default -> false;
        };
    }

    private static boolean hasMagic(byte[] header, int... expected) {
        if (header.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((header[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isZipHeader(byte[] header) {
        if (header.length < 4 || header[0] != 'P' || header[1] != 'K') {
            return false;
        }
        int low = header[2] & 0xFF;
        int high = header[3] & 0xFF;
        return (low == 0x03 && high == 0x04)
                || (low == 0x05 && high == 0x06)
                || (low == 0x07 && high == 0x08);
    }

    private static boolean isPlainText(byte[] header) {
        for (byte b : header) {
            if (b == 0x00) {
                return false;
            }
        }
        return true;
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
