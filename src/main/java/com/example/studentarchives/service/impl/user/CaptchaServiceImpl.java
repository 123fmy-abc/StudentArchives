package com.example.studentarchives.service.impl.user;

import com.example.studentarchives.dto.response.user.CaptchaResponse;
import com.example.studentarchives.service.user.CaptchaService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现
 * <p>
 * 使用内存存储验证码（ConcurrentHashMap），
 * 每 60 秒清理一次过期验证码。
 * 适用于单实例部署，多实例场景需改用 Redis。
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 验证码过期时间（毫秒） */
    private static final long CAPTCHA_TTL_MS = 5 * 60 * 1000L;

    /** 验证码字符集（排除易混淆字符） */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** 图片宽度 */
    private static final int WIDTH = 130;

    /** 图片高度 */
    private static final int HEIGHT = 48;

    /** 验证码字符数 */
    private static final int CODE_LENGTH = 4;

    private final Random random = new SecureRandom();

    /** 验证码存储：key -> {code, expireAt} */
    private final Map<String, CaptchaData> store = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 每 60 秒清理过期验证码
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "captcha-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public CaptchaResponse generateCaptcha() {
        // 1. 清除所有旧验证码（新生成的会使之前的失效）
        store.clear();

        // 2. 生成随机验证码
        String code = generateCode();

        // 2. 生成图片
        String base64Image = drawCaptcha(code);

        // 3. 生成唯一 key
        String key = java.util.UUID.randomUUID().toString();

        // 4. 存入内存
        store.put(key, new CaptchaData(code, System.currentTimeMillis() + CAPTCHA_TTL_MS));

        log.debug("验证码已生成: key={}, code={}", key, code);

        return CaptchaResponse.builder()
                .key(key)
                .image("data:image/png;base64," + base64Image)
                .devCode(code) // 明文验证码，方便调试
                .build();
    }

    @Override
    public boolean validateCaptcha(String key, String code) {
        if (key == null || code == null) {
            return false;
        }

        CaptchaData data = store.remove(key);
        if (data == null) {
            return false; // key 不存在或已使用
        }

        if (System.currentTimeMillis() > data.expireAt) {
            return false; // 已过期
        }

        return data.code.equalsIgnoreCase(code.trim());
    }

    /** 生成随机验证码字符串 */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 绘制验证码图片，返回 Base64 编码 */
    private String drawCaptcha(String code) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // 抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 6; i++) {
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g.drawLine(x1, y1, x2, y2);
            }

            // 噪点
            for (int i = 0; i < 80; i++) {
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                g.drawRect(x, y, 1, 1);
            }

            // 绘制验证码字符
            Font font = new Font("Arial", Font.BOLD | Font.ITALIC, 28);
            g.setFont(font);

            char[] chars = code.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                // 每个字符随机颜色
                g.setColor(new Color(
                        50 + random.nextInt(180),
                        50 + random.nextInt(180),
                        50 + random.nextInt(180)
                ));
                // 轻微旋转和偏移
                double angle = (random.nextDouble() - 0.5) * 0.4;
                int x = 10 + i * 28 + random.nextInt(6);
                int y = 35 + random.nextInt(6);

                g.rotate(angle, x, y);
                g.drawString(String.valueOf(chars[i]), x, y);
                g.rotate(-angle, x, y);
            }

            g.dispose();

            // 输出为 PNG Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            // 回退：返回纯文本验证码（仅开发环境）
            return Base64.getEncoder().encodeToString(code.getBytes());
        }
    }

    /** 清理过期验证码 */
    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(entry -> now > entry.getValue().expireAt);
        log.debug("验证码已清理，当前存储数: {}", store.size());
    }

    /** 验证码内存数据 */
    private static class CaptchaData {
        final String code;
        final long expireAt;

        CaptchaData(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
