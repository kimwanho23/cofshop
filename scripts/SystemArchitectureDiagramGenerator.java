import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.imageio.ImageIO;

public class SystemArchitectureDiagramGenerator {

    private static final int WIDTH = 2400;
    private static final int HEIGHT = 1600;

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0
                ? Path.of(args[0])
                : Path.of("docs/architecture/cofshop-system-architecture.png");

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            drawBackground(g);
            drawTitle(g);
            drawDeploymentArea(g);
            drawUserFlow(g);
            drawCiCdFlow(g);
            drawFooter(g);
        } finally {
            g.dispose();
        }

        Files.createDirectories(output.getParent());
        ImageIO.write(image, "png", output.toFile());
        System.out.println("Generated: " + output);
    }

    private static void drawBackground(Graphics2D g) {
        GradientPaint paint = new GradientPaint(0, 0, new Color(246, 248, 251), WIDTH, HEIGHT, new Color(234, 239, 246));
        g.setPaint(paint);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private static void drawTitle(Graphics2D g) {
        g.setColor(new Color(20, 35, 52));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        g.drawString("COFSHOP System Architecture", 70, 90);
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.setColor(new Color(63, 80, 98));
        g.drawString("Reference: 정보/구성도.png (deployment + CI/CD flow)", 72, 126);
    }

    private static void drawDeploymentArea(Graphics2D g) {
        RoundRectangle2D.Float ec2 = rect(430, 170, 1860, 760, 36);
        fillStroke(g, ec2, new Color(244, 246, 250, 210), new Color(109, 126, 145), 2.6f);
        drawLabel(g, "Amazon EC2", 470, 220, 33, true, new Color(39, 58, 79));

        RoundRectangle2D.Float docker = rect(560, 270, 1600, 600, 28);
        fillStroke(g, docker, new Color(236, 242, 248, 230), new Color(119, 136, 155), 2.0f);
        drawLabel(g, "Docker Host", 600, 320, 30, true, new Color(40, 67, 96));

        RoundRectangle2D.Float app = rect(680, 380, 540, 230, 24);
        fillStroke(g, app, new Color(221, 245, 222), new Color(88, 153, 95), 2.0f);
        drawLabel(g, "Spring Boot API", 730, 450, 38, true, new Color(28, 78, 33));
        drawLabel(g, "REST + WebSocket(STOMP) + JWT", 730, 492, 24, false, new Color(28, 78, 33));

        RoundRectangle2D.Float redis = rect(1290, 380, 390, 230, 24);
        fillStroke(g, redis, new Color(255, 233, 222), new Color(176, 112, 84), 2.0f);
        drawLabel(g, "Redis", 1410, 452, 38, true, new Color(109, 57, 31));
        drawLabel(g, "Cache + Redis Stream", 1364, 494, 24, false, new Color(109, 57, 31));

        RoundRectangle2D.Float mysql = rect(680, 660, 1000, 150, 24);
        fillStroke(g, mysql, new Color(225, 237, 255), new Color(87, 122, 170), 2.0f);
        drawLabel(g, "MySQL", 1100, 722, 36, true, new Color(33, 74, 125));
        drawLabel(g, "(local-db profile or external managed DB)", 900, 760, 24, false, new Color(33, 74, 125));

        RoundRectangle2D.Float portOne = rect(1950, 420, 330, 140, 20);
        fillStroke(g, portOne, new Color(240, 226, 255), new Color(139, 110, 182), 2.0f);
        drawLabel(g, "PortOne API", 2018, 490, 32, true, new Color(90, 63, 131));

        arrow(g, point(1220, 495), point(1290, 495), "cache/stream", new Color(77, 105, 133));
        arrow(g, point(950, 610), point(950, 660), "JPA", new Color(77, 105, 133));
        arrow(g, point(1220, 440), point(1950, 490), "payment verify/refund", new Color(77, 105, 133));
    }

    private static void drawUserFlow(Graphics2D g) {
        int ux = 150;
        int uy = 455;
        g.setColor(new Color(28, 35, 43));
        g.fillOval(ux, uy - 85, 72, 72);
        g.fillRoundRect(ux - 12, uy - 8, 96, 62, 20, 20);
        drawLabel(g, "User", ux - 2, uy + 95, 34, true, new Color(28, 35, 43));

        arrow(g, point(250, 480), point(680, 480), "HTTPS / WS", new Color(50, 70, 95));
    }

    private static void drawCiCdFlow(Graphics2D g) {
        drawLabel(g, "CI/CD", 95, 1040, 34, true, new Color(39, 58, 79));

        RoundRectangle2D.Float ide = rect(90, 1090, 390, 145, 22);
        fillStroke(g, ide, new Color(228, 236, 253), new Color(106, 128, 169), 1.8f);
        drawLabel(g, "Developer / IntelliJ", 132, 1174, 30, true, new Color(50, 66, 99));

        RoundRectangle2D.Float github = rect(570, 1090, 350, 145, 22);
        fillStroke(g, github, new Color(234, 234, 238), new Color(104, 108, 117), 1.8f);
        drawLabel(g, "GitHub Repo", 660, 1174, 30, true, new Color(42, 46, 54));

        RoundRectangle2D.Float actions = rect(1000, 1090, 420, 145, 22);
        fillStroke(g, actions, new Color(217, 241, 255), new Color(77, 129, 165), 1.8f);
        drawLabel(g, "GitHub Actions", 1090, 1168, 33, true, new Color(29, 83, 125));
        drawLabel(g, "test + build + scan + publish", 1053, 1203, 22, false, new Color(29, 83, 125));

        RoundRectangle2D.Float hub = rect(1500, 1090, 360, 145, 22);
        fillStroke(g, hub, new Color(221, 239, 255), new Color(89, 136, 190), 1.8f);
        drawLabel(g, "Docker Hub", 1598, 1174, 31, true, new Color(41, 93, 151));

        arrow(g, point(480, 1162), point(570, 1162), "push", new Color(60, 80, 102));
        arrow(g, point(920, 1162), point(1000, 1162), "trigger", new Color(60, 80, 102));
        arrow(g, point(1420, 1162), point(1500, 1162), "image push", new Color(60, 80, 102));
        arrow(g, point(1680, 1090), point(1680, 870), "pull", new Color(60, 80, 102));
    }

    private static void drawFooter(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g.setColor(new Color(70, 88, 108));
        g.drawString("Generated on " + LocalDate.now() + " from project metadata (README, compose.yml)", 70, 1540);
    }

    private static RoundRectangle2D.Float rect(float x, float y, float w, float h, float arc) {
        return new RoundRectangle2D.Float(x, y, w, h, arc, arc);
    }

    private static void fillStroke(Graphics2D g, RoundRectangle2D.Float shape, Color fill, Color stroke, float strokeWidth) {
        g.setColor(fill);
        g.fill(shape);
        g.setColor(stroke);
        g.setStroke(new BasicStroke(strokeWidth));
        g.draw(shape);
    }

    private static void drawLabel(Graphics2D g, String text, int x, int y, int size, boolean bold, Color color) {
        g.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static Point2D.Float point(float x, float y) {
        return new Point2D.Float(x, y);
    }

    private static void arrow(Graphics2D g, Point2D.Float from, Point2D.Float to, String label, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(2.2f));
        g.drawLine(Math.round(from.x), Math.round(from.y), Math.round(to.x), Math.round(to.y));

        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowLength = 16;
        int arrowHalfWidth = 7;

        int x1 = (int) (to.x - arrowLength * Math.cos(angle) + arrowHalfWidth * Math.sin(angle));
        int y1 = (int) (to.y - arrowLength * Math.sin(angle) - arrowHalfWidth * Math.cos(angle));
        int x2 = (int) (to.x - arrowLength * Math.cos(angle) - arrowHalfWidth * Math.sin(angle));
        int y2 = (int) (to.y - arrowLength * Math.sin(angle) + arrowHalfWidth * Math.cos(angle));

        Polygon head = new Polygon();
        head.addPoint((int) to.x, (int) to.y);
        head.addPoint(x1, y1);
        head.addPoint(x2, y2);
        g.fillPolygon(head);

        float lx = (from.x + to.x) / 2f + (Math.abs(from.x - to.x) < 10 ? 15 : 0);
        float ly = (from.y + to.y) / 2f - 8;
        drawLabel(g, label, Math.round(lx), Math.round(ly), 20, false, new Color(53, 70, 90));
    }
}
