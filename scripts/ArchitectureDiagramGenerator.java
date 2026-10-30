import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public class ArchitectureDiagramGenerator {

    private static final int WIDTH = 2400;
    private static final int HEIGHT = 1700;
    private static final int NODE_HEIGHT = 66;
    private static final int NODE_PADDING_X = 26;
    private static final Pattern MODULE_DEPENDENCY = Pattern.compile("\"([a-zA-Z0-9_]+)::[^\"]+\"");

    private record Module(String name, Set<String> dependencies) {}

    public static void main(String[] args) throws IOException {
        Path sourceRoot = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/java/kwh/cofshop");
        Path outputPath = args.length > 1 ? Path.of(args[1]) : Path.of("docs/architecture/cofshop-architecture.png");

        Map<String, Module> modules = parseModules(sourceRoot);
        if (modules.isEmpty()) {
            throw new IllegalStateException("No Spring Modulith modules found under " + sourceRoot);
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawBackground(g);
            drawTitle(g, modules.size());

            Map<String, RoundRectangle2D.Float> nodeShapes = layoutModules(g, modules.keySet());
            drawEdges(g, modules, nodeShapes);
            drawNodes(g, nodeShapes);
            drawFooter(g);
        } finally {
            g.dispose();
        }

        Files.createDirectories(outputPath.getParent());
        ImageIO.write(image, "png", outputPath.toFile());

        int edgeCount = modules.values().stream().mapToInt(m -> m.dependencies.size()).sum();
        System.out.println("Generated: " + outputPath);
        System.out.println("Modules: " + modules.size() + ", Dependencies: " + edgeCount);
    }

    private static Map<String, Module> parseModules(Path sourceRoot) throws IOException {
        Map<String, Module> modules = new LinkedHashMap<>();
        if (!Files.exists(sourceRoot) || !Files.isDirectory(sourceRoot)) {
            return modules;
        }

        try (var stream = Files.list(sourceRoot)) {
            List<Path> moduleDirs = stream.filter(Files::isDirectory).sorted().toList();
            for (Path dir : moduleDirs) {
                Path packageInfo = dir.resolve("package-info.java");
                if (!Files.exists(packageInfo)) {
                    continue;
                }

                String text = Files.readString(packageInfo);
                if (!text.contains("@org.springframework.modulith.ApplicationModule")) {
                    continue;
                }

                String moduleName = dir.getFileName().toString();
                Set<String> dependencies = new LinkedHashSet<>();
                Matcher matcher = MODULE_DEPENDENCY.matcher(text);
                while (matcher.find()) {
                    dependencies.add(matcher.group(1));
                }

                modules.put(moduleName, new Module(moduleName, dependencies));
            }
        }

        Set<String> knownNames = modules.keySet();
        Map<String, Module> normalized = new LinkedHashMap<>();
        for (Module module : modules.values()) {
            Set<String> filtered = module.dependencies.stream()
                    .filter(knownNames::contains)
                    .filter(dep -> !dep.equals(module.name))
                    .collect(LinkedHashSet::new, Set::add, Set::addAll);
            normalized.put(module.name, new Module(module.name, filtered));
        }
        return normalized;
    }

    private static void drawBackground(Graphics2D g) {
        GradientPaint gradient = new GradientPaint(
                0,
                0,
                new Color(245, 249, 255),
                WIDTH,
                HEIGHT,
                new Color(233, 240, 248));
        g.setPaint(gradient);
        g.fillRect(0, 0, WIDTH, HEIGHT);
    }

    private static void drawTitle(Graphics2D g, int moduleCount) {
        g.setColor(new Color(18, 37, 56));
        g.setFont(new Font("SansSerif", Font.BOLD, 52));
        g.drawString("COFSHOP Modulith Architecture", 90, 110);

        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        g.setColor(new Color(61, 82, 102));
        g.drawString("Top-level module dependencies inferred from package-info.java", 92, 154);
        g.drawString("Modules: " + moduleCount, 92, 194);
    }

    private static Map<String, RoundRectangle2D.Float> layoutModules(Graphics2D g, Set<String> moduleNames) {
        List<String> names = new ArrayList<>(moduleNames);
        names.sort(Comparator.naturalOrder());

        Map<String, RoundRectangle2D.Float> result = new HashMap<>();
        String centerModule = names.contains("global") ? "global" : null;
        List<String> orbit = names.stream().filter(name -> !name.equals(centerModule)).toList();

        float centerX = WIDTH / 2f;
        float centerY = HEIGHT / 2f + 70;
        float radiusX = 820f;
        float radiusY = 520f;

        if (centerModule != null) {
            RoundRectangle2D.Float centerShape = createNodeShape(g, centerModule, centerX, centerY);
            result.put(centerModule, centerShape);
        }

        int count = orbit.size();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i / count) - (Math.PI / 2);
            float x = (float) (centerX + (Math.cos(angle) * radiusX));
            float y = (float) (centerY + (Math.sin(angle) * radiusY));
            result.put(orbit.get(i), createNodeShape(g, orbit.get(i), x, y));
        }
        return result;
    }

    private static RoundRectangle2D.Float createNodeShape(Graphics2D g, String moduleName, float centerX, float centerY) {
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        int textWidth = g.getFontMetrics().stringWidth(moduleName);
        float width = textWidth + NODE_PADDING_X * 2f;
        float x = centerX - width / 2f;
        float y = centerY - NODE_HEIGHT / 2f;
        return new RoundRectangle2D.Float(x, y, width, NODE_HEIGHT, 24, 24);
    }

    private static void drawEdges(
            Graphics2D g,
            Map<String, Module> modules,
            Map<String, RoundRectangle2D.Float> nodeShapes) {
        Set<String> seen = new HashSet<>();
        g.setStroke(new BasicStroke(2.2f));

        for (Module source : modules.values()) {
            RoundRectangle2D.Float sourceShape = nodeShapes.get(source.name);
            if (sourceShape == null) {
                continue;
            }
            Point2D.Float sourceCenter = centerOf(sourceShape);

            for (String targetName : source.dependencies) {
                String edgeId = source.name + "->" + targetName;
                if (!seen.add(edgeId)) {
                    continue;
                }

                RoundRectangle2D.Float targetShape = nodeShapes.get(targetName);
                if (targetShape == null) {
                    continue;
                }

                Point2D.Float targetCenter = centerOf(targetShape);
                Point2D.Float start = edgePoint(sourceShape, targetCenter);
                Point2D.Float end = edgePoint(targetShape, sourceCenter);
                drawArrow(g, start, end, new Color(53, 85, 115, 145));
            }
        }
    }

    private static void drawArrow(Graphics2D g, Point2D.Float from, Point2D.Float to, Color color) {
        g.setColor(color);
        g.drawLine(Math.round(from.x), Math.round(from.y), Math.round(to.x), Math.round(to.y));

        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double angle = Math.atan2(dy, dx);
        int arrowLength = 14;
        int arrowWidth = 8;

        int x1 = (int) (to.x - arrowLength * Math.cos(angle - Math.PI / 7));
        int y1 = (int) (to.y - arrowLength * Math.sin(angle - Math.PI / 7));
        int x2 = (int) (to.x - arrowLength * Math.cos(angle + Math.PI / 7));
        int y2 = (int) (to.y - arrowLength * Math.sin(angle + Math.PI / 7));

        int[] xPoints = {(int) to.x, x1, x2};
        int[] yPoints = {(int) to.y, y1, y2};
        g.fillPolygon(xPoints, yPoints, 3);
    }

    private static Point2D.Float centerOf(RoundRectangle2D.Float rect) {
        return new Point2D.Float(
                (float) (rect.getX() + rect.getWidth() / 2d),
                (float) (rect.getY() + rect.getHeight() / 2d));
    }

    private static Point2D.Float edgePoint(RoundRectangle2D.Float rect, Point2D.Float towards) {
        Point2D.Float center = centerOf(rect);
        double dx = towards.x - center.x;
        double dy = towards.y - center.y;

        double halfW = rect.getWidth() / 2d;
        double halfH = rect.getHeight() / 2d;
        double scale = Math.max(Math.abs(dx) / halfW, Math.abs(dy) / halfH);
        if (scale < 1e-6) {
            return center;
        }

        return new Point2D.Float((float) (center.x + dx / scale), (float) (center.y + dy / scale));
    }

    private static void drawNodes(Graphics2D g, Map<String, RoundRectangle2D.Float> nodeShapes) {
        for (Map.Entry<String, RoundRectangle2D.Float> entry : nodeShapes.entrySet()) {
            String module = entry.getKey();
            RoundRectangle2D.Float shape = entry.getValue();

            Color fill = moduleColor(module);
            g.setColor(fill);
            g.fill(shape);

            g.setColor(new Color(20, 42, 60));
            g.setStroke(new BasicStroke(2.2f));
            g.draw(shape);

            g.setColor(new Color(15, 33, 48));
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            int textWidth = g.getFontMetrics().stringWidth(module);
            int tx = (int) (shape.x + (shape.width - textWidth) / 2f);
            int ty = (int) (shape.y + (shape.height + g.getFontMetrics().getAscent()) / 2f - 6);
            g.drawString(module, tx, ty);
        }
    }

    private static Color moduleColor(String module) {
        return switch (module) {
            case "global" -> new Color(220, 226, 234);
            case "member" -> new Color(255, 229, 214);
            case "item" -> new Color(222, 240, 220);
            case "order" -> new Color(255, 244, 198);
            case "payment" -> new Color(221, 233, 255);
            case "coupon" -> new Color(241, 224, 255);
            case "security" -> new Color(252, 222, 222);
            case "cart" -> new Color(214, 244, 246);
            case "chat" -> new Color(236, 238, 255);
            case "statistics" -> new Color(232, 244, 225);
            case "file" -> new Color(255, 235, 209);
            default -> new Color(229, 235, 243);
        };
    }

    private static void drawFooter(Graphics2D g) {
        g.setColor(new Color(66, 87, 106));
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.drawString("Generated from Spring Modulith package metadata on " + LocalDate.now(), 90, HEIGHT - 70);
    }
}
