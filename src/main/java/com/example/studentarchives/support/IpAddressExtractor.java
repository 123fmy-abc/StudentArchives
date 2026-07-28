package com.example.studentarchives.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 客户端 IP 地址提取器
 * <p>
 * 仅在请求来自可信代理时信任 X-Forwarded-For / X-Real-IP 等转发头，
 * 防止客户端伪造 IP。
 * 支持 IPv4 和 IPv6 的内网地址判断。
 */
@Component
public class IpAddressExtractor {

    /** IPv6 链路本地地址前缀（fe80::/10） */
    private static final String IPV6_LINK_LOCAL_PREFIX = "fe8";

    /** IPv6 唯一本地地址前缀（fc00::/7） */
    private static final String IPV6_UNIQUE_LOCAL_PREFIX = "fc";

    /** IPv6 唯一本地地址备用前缀（fd00::/8 实际使用段） */
    private static final String IPV6_UNIQUE_LOCAL_ALT_PREFIX = "fd";

    /**
     * 从请求中提取客户端真实 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    public String extract(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = remoteAddr;
        }
        // X-Forwarded-For 可能包含逗号分隔的多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 判断请求来源是否为可信内网代理（支持 IPv4 和 IPv6）。
     */
    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        // IPv4 内网地址
        if (remoteAddr.equals("127.0.0.1")
                || remoteAddr.startsWith("192.168.")
                || remoteAddr.startsWith("10.")
                || isPrivate172(remoteAddr)) {
            return true;
        }
        // IPv6 内网地址
        return isIPv6Private(remoteAddr);
    }

    /**
     * 判断是否为 172.16.0.0/12 范围
     */
    private boolean isPrivate172(String remoteAddr) {
        if (!remoteAddr.startsWith("172.")) {
            return false;
        }
        try {
            int second = Integer.parseInt(remoteAddr.substring(
                    remoteAddr.indexOf('.') + 1,
                    remoteAddr.indexOf('.', remoteAddr.indexOf('.') + 1)));
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为 IPv6 内网/本地地址
     */
    private boolean isIPv6Private(String remoteAddr) {
        String lower = remoteAddr.toLowerCase();
        // ::1 — IPv6 环回地址
        if ("0:0:0:0:0:0:0:1".equals(lower) || "::1".equals(lower)) {
            return true;
        }
        // 链路本地地址 fe80::/10（fe80 ~ febf）
        if (lower.startsWith(IPV6_LINK_LOCAL_PREFIX)) {
            return true;
        }
        // 唯一本地地址 fc00::/7（fc00 ~ fdff）
        if (lower.startsWith(IPV6_UNIQUE_LOCAL_PREFIX) || lower.startsWith(IPV6_UNIQUE_LOCAL_ALT_PREFIX)) {
            return true;
        }
        // IPv4 映射地址（::ffff:127.0.0.1 等）
        if (lower.contains("::ffff:") && lower.endsWith(":127.0.0.1")) {
            return true;
        }
        return false;
    }
}
