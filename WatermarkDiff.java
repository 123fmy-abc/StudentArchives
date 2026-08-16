import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** 一次性验证：对比带水印/不带水印 PNG，统计差异强度（不属于正式源码） */
public class WatermarkDiff {
    public static void main(String[] args) throws Exception {
        BufferedImage with = ImageIO.read(new File("watermark_with.png"));
        BufferedImage without = ImageIO.read(new File("watermark_without.png"));
        System.out.println("with size=" + with.getWidth() + "x" + with.getHeight()
                + ", without size=" + without.getWidth() + "x" + without.getHeight());

        int w = Math.min(with.getWidth(), without.getWidth());
        int h = Math.min(with.getHeight(), without.getHeight());
        long sumAbs = 0;
        long sumSq = 0;
        long maxAbs = 0;
        int count = 0;
        int diffCount = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = with.getRGB(x, y);
                int b = without.getRGB(x, y);
                int dr = Math.abs(((a >> 16) & 255) - ((b >> 16) & 255));
                int dg = Math.abs(((a >> 8) & 255) - ((b >> 8) & 255));
                int db = Math.abs((a & 255) - (b & 255));
                int dmax = Math.max(dr, Math.max(dg, db));
                sumAbs += dmax;
                sumSq += (long) dmax * dmax;
                maxAbs = Math.max(maxAbs, dmax);
                count++;
                if (dmax > 8) diffCount++;
            }
        }
        System.out.println("像素总数=" + count
                + ", 平均通道差=" + (sumAbs / (double) count)
                + ", RMSE=" + Math.sqrt(sumSq / (double) count)
                + ", 最大通道差=" + maxAbs
                + ", 差异>8的像素占比=" + (100.0 * diffCount / count) + "%");

        // 中央放大区域（水印应横跨整页，中间最明显）
        int cx = w / 2 - 120, cy = h / 2 - 60, cw = 240, ch = 120;
        BufferedImage crop = with.getSubimage(Math.max(0, cx), Math.max(0, cy),
                Math.min(cw, w - Math.max(0, cx)), Math.min(ch, h - Math.max(0, cy)));
        ImageIO.write(crop, "png", new File("watermark_crop_center.png"));
        System.out.println("central crop written.");
    }
}
