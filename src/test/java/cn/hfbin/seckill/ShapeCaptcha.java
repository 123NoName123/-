package cn.hfbin.seckill;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.Random;

public class ShapeCaptcha {

    // 生成带有图形的验证码图片
    public BufferedImage generateCaptchaImage() {
        // 设置图像大小
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景填充为白色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 200);

        // 随机生成不同形状的图形
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            int x = rand.nextInt(300);
            int y = rand.nextInt(150);
            int size = rand.nextInt(50) + 20;
            int shapeType = rand.nextInt(3);  // 随机选择图形类型（0=三角形，1=圆形，2=矩形）

            switch (shapeType) {
                case 0:  // 三角形
                    g.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                    int[] xPoints = {x, x + size, x + size / 2};
                    int[] yPoints = {y, y, y - size};
                    g.fillPolygon(xPoints, yPoints, 3);
                    break;
                case 1:  // 圆形
                    g.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                    g.fillOval(x, y, size, size);
                    break;
                case 2:  // 矩形
                    g.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
                    g.fillRect(x, y, size, size);
                    break;
            }
        }

        g.dispose();  // 释放资源
        return image; // 返回生成的图像
    }

    public static void main(String[] args) {
        ShapeCaptcha captcha = new ShapeCaptcha();
        BufferedImage image = captcha.generateCaptchaImage();

        // 打印图像到控制台（实际应用中，通常是保存为文件或返回给前端）
        // 注意：这是为演示方便，真实应用中会将图像返回给前端，供用户选择
        try {
            javax.imageio.ImageIO.write(image, "PNG", new java.io.File("captcha.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
