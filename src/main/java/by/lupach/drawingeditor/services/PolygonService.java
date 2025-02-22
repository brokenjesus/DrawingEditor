package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import by.lupach.drawingeditor.models.polygons.Edge;
import org.springframework.stereotype.Service;

import java.util.*;
@Service
public class PolygonService {

    int SCREEN_WIDTH = 1280;
    int SCREEN_HEIGHT = 720;

    // Проверка, является ли полигон выпуклым
    public boolean isConvex(List<Pixel> polygon) {
        int n = polygon.size();
        if (n < 3) return false;

        boolean hasPositive = false, hasNegative = false;

        for (int i = 0; i < n; i++) {
            int dx1 = polygon.get((i + 1) % n).getX() - polygon.get(i).getX();
            int dy1 = polygon.get((i + 1) % n).getY() - polygon.get(i).getY();
            int dx2 = polygon.get((i + 2) % n).getX() - polygon.get(i).getX();
            int dy2 = polygon.get((i + 2) % n).getY() - polygon.get(i).getY();
            int crossProduct = dx1 * dy2 - dy1 * dx2;

            if (crossProduct > 0) hasPositive = true;
            if (crossProduct < 0) hasNegative = true;
            if (hasPositive && hasNegative) return false;
        }

        return true;
    }

    // Алгоритм Грэхема
    public List<Pixel> convexHullGraham(List<Pixel> points) {
        if (points.size() < 3) return points;

        points.sort(Comparator.comparingInt(Pixel::getX));

        Stack<Pixel> hull = new Stack<>();
        for (int i = 0; i < 2; i++) hull.push(points.get(i));

        for (int i = 2; i < points.size(); i++) {
            while (hull.size() >= 2 && crossProduct(hull.get(hull.size() - 2), hull.peek(), points.get(i)) <= 0) {
                hull.pop();
            }
            hull.push(points.get(i));
        }

        int lowerSize = hull.size();
        for (int i = points.size() - 2; i >= 0; i--) {
            while (hull.size() > lowerSize && crossProduct(hull.get(hull.size() - 2), hull.peek(), points.get(i)) <= 0) {
                hull.pop();
            }
            hull.push(points.get(i));
        }

        hull.pop(); // Убираем дублирующую вершину
        List<Pixel> convexHull = new ArrayList<>(hull);

        return convexHull;
//        return fillPolygon(convexHull);
    }

    // Алгоритм Джарвиса
    public List<Pixel> convexHullJarvis(List<Pixel> points) {
        if (points.size() < 3) return points;

        List<Pixel> hull = new ArrayList<>();
        Pixel leftmost = points.stream().min(Comparator.comparingInt(Pixel::getX)).orElse(points.get(0));
        Pixel p = leftmost;

        do {
            hull.add(p);
            Pixel q = points.get(0);
            for (Pixel r : points) {
                if (q == p || crossProduct(p, q, r) < 0) q = r;
            }
            p = q;
        } while (p != leftmost);


        return hull;
        // Заполняем полигон пикселями
//        return fillPolygon(hull);
    }

    // Проверка принадлежности точки полигону
    public boolean isPointInsidePolygon(Pixel p, List<Pixel> polygon) {
        int n = polygon.size();
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Pixel p1 = polygon.get(i);
            Pixel p2 = polygon.get(j);
            if ((p1.getY() > p.getY()) != (p2.getY() > p.getY()) &&
                    (p.getX() < (p2.getX() - p1.getX()) * (p.getY() - p1.getY()) / (p2.getY() - p1.getY()) + p1.getX())) {
                inside = !inside;
            }
        }
        return inside;
    }

    // Проверка пересечения отрезка с полигоном
    public boolean segmentIntersectsPolygon(Pixel a, Pixel b, List<Pixel> polygon) {
        int n = polygon.size();
        for (int i = 0; i < n; i++) {
            Pixel p1 = polygon.get(i);
            Pixel p2 = polygon.get((i + 1) % n);
            if (segmentsIntersect(a, b, p1, p2)) return true;
        }
        return false;
    }

    // Вспомогательные методы
    private int crossProduct(Pixel a, Pixel b, Pixel c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private boolean segmentsIntersect(Pixel a, Pixel b, Pixel c, Pixel d) {
        int d1 = crossProduct(c, d, a);
        int d2 = crossProduct(c, d, b);
        int d3 = crossProduct(a, b, c);
        int d4 = crossProduct(a, b, d);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true;

        return false;
    }

    // Метод для заполнения полигона пикселями
    private List<Pixel> fillPolygon(List<Pixel> polygon) {
        List<Pixel> filledPixels = new ArrayList<>();

        // Находим минимальные и максимальные значения X и Y
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Pixel p : polygon) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();
        }

        // Проходим по всем пикселям внутри ограничивающего прямоугольника
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Pixel p = new Pixel(x, y, "rgba(255, 20, 147, 0.2)");
                if (isPointInsidePolygon(p, polygon)) {
                    filledPixels.add(p);
                }
            }
        }

        return filledPixels;
    }

    public List<Pixel> scanlineFill(List<Pixel> polygon) {
        List<Pixel> filledPixels = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();

        for (int i = 0; i < polygon.size(); i++) {
            Pixel p1 = polygon.get(i);
            Pixel p2 = polygon.get((i+1)%polygon.size());
            if (p1.getY() != p2.getY()) {
                Edge edge = new Edge(p1, p2);
                edges.add(edge);
            }
        }

        edges.sort(Comparator.comparingInt(e -> e.getYMax()));

        int yMin = edges.stream().mapToInt(e -> e.getYMin()).min().orElse(0);
        int yMax = edges.stream().mapToInt(e -> e.getYMax()).max().orElse(0);

        List<Edge> activeEdges = new ArrayList<>();

        for (int y = yMin; y <= yMax-1; y++) {
            Iterator<Edge> it = edges.iterator();
            while (it.hasNext()) {
                Edge edge = it.next();
                if (edge.getYMin() == y) {
                    activeEdges.add(edge);
                    it.remove();
                }
            }

            activeEdges.sort(Comparator.comparingDouble(e -> e.getX()));

            for (int i = 0; i < activeEdges.size()-1; i += 2) {
                int xStart = (int) Math.ceil(activeEdges.get(i).getX());
                int xEnd = (int) Math.floor(activeEdges.get(i+1).getX());
                for (int x = xStart; x <= xEnd; x++) {
                    filledPixels.add(new Pixel(x, y, "rgba(205,20,17,0.2)"));
                }
            }

            int finalY = y;
            activeEdges.removeIf(edge -> edge.getYMax() == finalY);
            for (Edge edge : activeEdges) {
                edge.setX(edge.getX()+edge.getDxPerY());
            }
        }

        return filledPixels;
    }

    public List<Pixel> aetFill(List<Pixel> polygon) {
        List<Pixel> filledPixels = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();

        // Создание и сортировка ребер
        for (int i = 0; i < polygon.size(); i++) {
            Pixel p1 = polygon.get(i);
            Pixel p2 = polygon.get((i+1) % polygon.size());
            if (p1.getY() != p2.getY()) {
                edges.add(new Edge(p1, p2));
            }
        }

        edges.sort(Comparator.comparingInt(e -> e.getYMin()));

        int yMin = edges.get(0).getYMin();
        int yMax = edges.stream().mapToInt(e -> e.getYMax()).max().orElse(yMin);

        List<Edge> activeEdges = new ArrayList<>();

        for (int y = yMin; y <= yMax; y++) {
            while (!edges.isEmpty() && edges.get(0).getYMin() == y) {
                activeEdges.add(edges.remove(0));
            }

            activeEdges.sort(Comparator.comparingDouble(e -> e.getX()));

            for (int i = 0; i < activeEdges.size()-1; i += 2) {
                int xStart = (int) Math.ceil(activeEdges.get(i).getX());
                int xEnd = (int) Math.floor(activeEdges.get(i+1).getX());
                for (int x = xStart; x <= xEnd; x++) {
                    filledPixels.add(new Pixel(x, y, "rgba(255,20,147,0.2)"));
                }
            }

            Iterator<Edge> it = activeEdges.iterator();
            while (it.hasNext()) {
                Edge edge = it.next();
                if (edge.getYMax() == y) {
                    it.remove();
                } else {
                    edge.setX(edge.getX()+edge.getDxPerY());
                }
            }
        }
        return filledPixels;
    }

    public List<Pixel> floodFill(Pixel seed, List<Pixel> polygon, String fillColor, String boundaryColor) {
        List<Pixel> filledPixels = new ArrayList<>();
        boolean[][] visited = new boolean[SCREEN_WIDTH][SCREEN_HEIGHT];
        Stack<Pixel> stack = new Stack<>();

        // Исходный цвет, который будем заменять
        String targetColor = seed.getColor();
        stack.push(seed);

        while (!stack.isEmpty()) {
            Pixel p = stack.pop();
            int x = p.getX();
            int y = p.getY();

            // Проверка границ экрана
            if (x < 0 || x >= SCREEN_WIDTH || y < 0 || y >= SCREEN_HEIGHT) {
                continue;
            }
            // Если пиксель уже обработан, пропускаем его
            if (visited[x][y]) {
                continue;
            }
            visited[x][y] = true;

            // Если точка находится вне полигона, пропускаем её
            if (!isPointInsidePolygon(p, polygon)) {
                continue;
            }

            // Если пиксель является границей, пропускаем его
            if (isBoundary(p, boundaryColor)) {
                continue;
            }
            // Если цвет пикселя не совпадает с целевым, пропускаем его
            if (!p.getColor().equals(targetColor)) {
                continue;
            }

            // Заливаем пиксель (создаём новый объект с заливочным цветом)
            Pixel newFilled = new Pixel(x, y, fillColor);
            filledPixels.add(newFilled);
            // В реальном приложении здесь следует обновить состояние холста

            // Добавляем 4-связанных соседей в стек
            stack.push(new Pixel(x + 1, y, targetColor));
            stack.push(new Pixel(x - 1, y, targetColor));
            stack.push(new Pixel(x, y + 1, targetColor));
            stack.push(new Pixel(x, y - 1, targetColor));
        }
        return filledPixels;
    }

    public List<Pixel> scanlineFloodFill(Pixel seed, List<Pixel> polygon, String fillColor, String boundaryColor) {
        List<Pixel> filledPixels = new ArrayList<>();
        boolean[][] visited = new boolean[SCREEN_WIDTH][SCREEN_HEIGHT];
        Stack<Pixel> stack = new Stack<>();

        // Определяем исходный цвет, который будем заменять
        String targetColor = seed.getColor();
        stack.push(seed);

        while (!stack.isEmpty()) {
            Pixel p = stack.pop();
            int x = p.getX();
            int y = p.getY();

            // Проверка границ экрана
            if (x < 0 || x >= SCREEN_WIDTH || y < 0 || y >= SCREEN_HEIGHT) {
                continue;
            }
            if (visited[x][y]) {
                continue;
            }

            // Проверяем, что пиксель находится внутри полигона
            if (!isPointInsidePolygon(p, polygon)) {
                continue;
            }
            // Если пиксель является граничным или его цвет не совпадает с targetColor, пропускаем
            if (isBoundary(p, boundaryColor) || !p.getColor().equals(targetColor)) {
                continue;
            }

            // Поиск левой границы интервала на текущей строке
            int xLeft = x;
            while (xLeft >= 0) {
                Pixel current = new Pixel(xLeft, y, p.getColor());
                if (visited[xLeft][y] || isBoundary(current, boundaryColor)
                        || !current.getColor().equals(targetColor)
                        || !isPointInsidePolygon(current, polygon)) {
                    break;
                }
                xLeft--;
            }
            xLeft++; // корректный пиксель

            // Поиск правой границы интервала на текущей строке
            int xRight = x;
            while (xRight < SCREEN_WIDTH) {
                Pixel current = new Pixel(xRight, y, p.getColor());
                if (visited[xRight][y] || isBoundary(current, boundaryColor)
                        || !current.getColor().equals(targetColor)
                        || !isPointInsidePolygon(current, polygon)) {
                    break;
                }
                xRight++;
            }
            xRight--; // корректный пиксель

            // Заполнение интервала от xLeft до xRight на строке y
            for (int i = xLeft; i <= xRight; i++) {
                visited[i][y] = true;
                filledPixels.add(new Pixel(i, y, fillColor));
                // При наличии холста здесь следует обновить его состояние
            }

            // Проверка соседних строк (выше и ниже) для каждого пикселя заполненного интервала
            for (int i = xLeft; i <= xRight; i++) {
                // Верхняя строка
                if (y > 0) {
                    Pixel up = new Pixel(i, y - 1, targetColor);
                    if (!visited[i][y - 1] && !isBoundary(up, boundaryColor)
                            && up.getColor().equals(targetColor)
                            && isPointInsidePolygon(up, polygon)) {
                        stack.push(up);
                    }
                }
                // Нижняя строка
                if (y < SCREEN_HEIGHT - 1) {
                    Pixel down = new Pixel(i, y + 1, targetColor);
                    if (!visited[i][y + 1] && !isBoundary(down, boundaryColor)
                            && down.getColor().equals(targetColor)
                            && isPointInsidePolygon(down, polygon)) {
                        stack.push(down);
                    }
                }
            }
        }

        return filledPixels;
    }


    private boolean isBoundary(Pixel p, String boundaryColor) {
        // Получаем цвет пикселя на холсте
        String pixelColor = p.getColor();

        // Сравниваем цвет пикселя с цветом границы
        return pixelColor.equals(boundaryColor);
    }

    private boolean isColored(Pixel p, String color) {
        // Получаем цвет пикселя на холсте
        String pixelColor = p.getColor();

        // Сравниваем цвет пикселя с заданным цветом
        return pixelColor.equals(color);
    }

}