package com.example.studentarchives.support;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 验证码图片生成器
 * <p>
 * 使用 Java AWT 生成包含干扰线和噪点的 4 位字母数字验证码图片，
 * 排除易混淆字符（0/O/1/l/I），返回 Base64 编码的 PNG 图片。
 */
@Component
public class CaptchaGenerator {

    /** 可用字符集（排除易混淆字符） */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int CODE_LENGTH = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成验证码
     *
     * @return CaptchaResult 包含明文字符串和 Base64 图片数据
     */
    public CaptchaResult generate() {
        String code = generateCode();
        String base64Image = generateBase64Image(code);
        return new CaptchaResult(code, base64Image);
    }

    /** 生成随机验证码字符串 */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 生成 Base64 编码的 PNG 图片 */
    private String generateBase64Image(String code) {
        BufferedImage image = createImage(code);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }

    /** 创建验证码图片 */
    private BufferedImage createImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // 抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 随机背景色（浅色系）
        float hue = RANDOM.nextFloat();
        Color bgColor = Color.getHSBColor(hue, 0.15f, 0.95f);
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制干扰线（3-5 条）
        drawNoiseLines(g2d);

        // 绘制干扰点（20-40 个）
        drawNoiseDots(g2d);

        // 绘制验证码字符
        drawCode(g2d, code);

        // 轻微边框
        g2d.setColor(new Color(180, 180, 180));
        g2d.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        g2d.dispose();
        return image;
    }

    /** 绘制干扰线 */
    private void drawNoiseLines(Graphics2D g2d) {
        int lineCount = 3 + RANDOM.nextInt(3);
        for (int i = 0; i < lineCount; i++) {
            Color lineColor = new Color(
                    100 + RANDOM.nextInt(100),
                    100 + RANDOM.nextInt(100),
                    100 + RANDOM.nextInt(100),
                    100 + RANDOM.nextInt(80));
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(1.0f + RANDOM.nextFloat()));

            int x1 = RANDOM.nextInt(WIDTH);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = RANDOM.nextInt(WIDTH);
            int y2 = RANDOM.nextInt(HEIGHT);
            g2d.drawLine(x1, y1, x2, y2);
        }
    }

    /** 绘制干扰点 */
    private void drawNoiseDots(Graphics2D g2d) {
        int dotCount = 20 + RANDOM.nextInt(20);
        for (int i = 0; i < dotCount; i++) {
            Color dotColor = new Color(
                    RANDOM.nextInt(150),
                    RANDOM.nextInt(150),
                    RANDOM.nextInt(150),
                    80 + RANDOM.nextInt(80));
            g2d.setColor(dotColor);
            int x = RANDOM.nextInt(WIDTH);
            int y = RANDOM.nextInt(HEIGHT);
            int size = 1 + RANDOM.nextInt(3);
            g2d.fillOval(x, y, size, size);
        }
    }

    /** 绘制验证码字符（带随机旋转和颜色，确保与背景色有足够对比度） */
    private void drawCode(Graphics2D g2d, String code) {
        int charWidth = WIDTH / CODE_LENGTH;
        int fontSize = 26 + RANDOM.nextInt(8);

        Font font = new Font("Arial", Font.BOLD | Font.ITALIC, fontSize);
        g2d.setFont(font);

        FontMetrics metrics = g2d.getFontMetrics();
        int baselineY = (HEIGHT + metrics.getAscent() / 2) / 2;

        for (int i = 0; i < CODE_LENGTH; i++) {
            // 随机深色（R/G/B 各 30-120 之间，确保与浅色背景有足够对比度）
            Color charColor = new Color(
                    30 + RANDOM.nextInt(90),
                    30 + RANDOM.nextInt(90),
                    30 + RANDOM.nextInt(90));
            g2d.setColor(charColor);

            // 随机旋转
            double angle = (RANDOM.nextDouble() - 0.5) * 0.6;
            AffineTransform orig = g2d.getTransform();

            int x = 5 + i * charWidth + RANDOM.nextInt(5);
            g2d.translate(x + 10, baselineY);
            g2d.rotate(angle);
            g2d.drawString(String.valueOf(code.charAt(i)), 0, 0);

            g2d.setTransform(orig);
        }
    }

    @Data
    @AllArgsConstructor
    public static class CaptchaResult {
        /** 验证码明文字符串（用于哈希后存储） */
        private String code;
        /** Base64 编码的 PNG 图片数据（含 data:image/png;base64, 前缀） */
        private String base64Image;
    }
}
