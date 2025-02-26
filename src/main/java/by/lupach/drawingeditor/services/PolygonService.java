package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import by.lupach.drawingeditor.models.polygons.PolygonEdge;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;

import static by.lupach.drawingeditor.configs.ScreenConstants.SCREEN_HEIGHT;
import static by.lupach.drawingeditor.configs.ScreenConstants.SCREEN_WIDTH;

@Service
public class PolygonService {
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
    /**
     * 1. Выбираем точку с наименьшей X.
     * 2. Сортируем все точки по углу наклона относительно этой опорной точки.
     * 3. Проходим по отсортированным точкам, используя стек:
     *    - Если новая точка образует "правый поворот" относительно предыдущих двух, убираем предыдущую.
     *    - Если "левый поворот" — добавляем в стек.
     * 4. В стеке остаются точки выпуклой оболочки.
     **/
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

        return new ArrayList<>(hull);
    }

    // Алгоритм Джарвиса
    /**
     * 1. Находим самую левую точку (она точно принадлежит оболочке).
     * 2. Выбираем точку, образующую самый левый поворот.
     * 3. Повторяем процесс, пока не вернемся в начальную точку.
     * 4. Итоговый список содержит точки выпуклой оболочки.
     **/
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

    private int crossProduct(Pixel a, Pixel b, Pixel c) {
        return (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY()) * (c.getX() - a.getX());
    }

    private boolean segmentsIntersect(Pixel a, Pixel b, Pixel c, Pixel d) {
        int d1 = crossProduct(c, d, a);
        int d2 = crossProduct(c, d, b);
        int d3 = crossProduct(a, b, c);
        int d4 = crossProduct(a, b, d);

        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0));
    }

    //Алгоритм растровой развертки с упорядоченным списком ребер
    /**
     * Алгоритм:
     * 1. Находим минимальную и максимальную Y-координаты полигона.
     * 2. Для каждой строки (сканирующей линии) от minY до maxY:
     *    - Находим точки пересечения сканирующей линии с ребрами полигона.
     *    - Сортируем точки пересечения по X-координате.
     *    - Заполняем пиксели между каждой парой точек пересечения.
     */
    public List<Pixel> scanlineFill(List<Pixel> polygon) {
        // Find min and max y coordinates
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Pixel point : polygon) {
            minY = Math.min(minY, point.getY());
            maxY = Math.max(maxY, point.getY());
        }

        List<Pixel> filledPixels = new ArrayList<>();

        // Loop through all scanline y values from minY to maxY
        for (int y = minY; y <= maxY; y++) {
            List<Integer> intersections = new ArrayList<>();

            // Check for intersections with each edge of the polygon
            for (int i = 0; i < polygon.size(); i++) {
                Pixel point1 = polygon.get(i);
                Pixel point2 = polygon.get((i + 1) % polygon.size());
                int x1 = point1.getX(), y1 = point1.getY();
                int x2 = point2.getX(), y2 = point2.getY();

                // Skip horizontal edges
                if (y1 == y2) continue;

                // Check if the scanline intersects the edge
                if (Math.min(y1, y2) <= y && y < Math.max(y1, y2)) {
                    int x = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                    intersections.add(x);
                }
            }

            // Sort the intersection points
            Collections.sort(intersections);

            // Fill between each pair of intersections
            for (int i = 0; i < intersections.size() - 1; i += 2) {
                int xStart = Math.round(intersections.get(i));
                int xEnd = Math.round(intersections.get(i + 1));

                for (int x = xStart; x <= xEnd; x++) {
                    filledPixels.add(new Pixel(x, y, "rgba(205,20,217,0.2)"));
                }
            }
        }

        return filledPixels;
    }

    //Алгоритм растровой развертки с упорядоченным списком ребер, использующий список активных ребер
    /**
     Алгоритм:
     * 1. Находим минимальную и максимальную Y-координаты полигона.
     * 2. Создаем список всех ребер полигона, исключая горизонтальные ребра.
     * 3. Для каждой строки (сканирующей линии) от minY до maxY:
     *    - Добавляем в AEL ребра, которые начинаются на текущей строке.
     *    - Удаляем из AEL ребра, которые заканчиваются на текущей строке.
     *    - Сортируем AEL по X-координате пересечения с текущей строкой.
     *    - Заполняем пиксели между каждой парой ребер в AEL.
     */
    public List<Pixel> activeEdgeFill(List<Pixel> points) {
        int minY = points.stream().min(Comparator.comparingInt(Pixel::getY)).get().getY();
        int maxY = points.stream().max(Comparator.comparingInt(Pixel::getY)).get().getY();
        List<Pixel> filledPoints = new ArrayList<>();

        List<PolygonEdge> edges = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            Pixel point1 = points.get(i);
            Pixel point2 = points.get((i + 1) % points.size()); // Wrap around to the first point
            if (point1.getY() != point2.getY()) {
                edges.add(new PolygonEdge(point1, point2));
            }
        }

        edges.sort(Comparator.comparingInt(edge -> Math.min(edge.getPoint1().getY(), edge.getPoint2().getY())));

        List<PolygonEdge> ael = new ArrayList<>();
        int currentY = minY;

        while (currentY <= maxY) {
            // Add edges that start at currentY
            for (PolygonEdge edge : edges) {
                if (Math.min(edge.getPoint1().getY(), edge.getPoint2().getY()) == currentY) {
                    ael.add(edge);
                }
            }

            // Remove edges that have passed the currentY
            int finalCurrentY = currentY;
            ael.removeIf(edge -> Math.max(edge.getPoint1().getY(), edge.getPoint2().getY()) <= finalCurrentY);

            // Sort by x-intercept
            int finalCurrentY1 = currentY;
            ael.sort(Comparator.comparingDouble(edge -> edge.getXIntercept(finalCurrentY1)));

            // Fill in the pixels between pairs of edges
            for (int i = 0; i < ael.size(); i += 2) {
                PolygonEdge edge1 = ael.get(i);
                PolygonEdge edge2 = ael.get(i + 1);

                int xStart = (int) edge1.getXIntercept(currentY);
                int xEnd = (int) edge2.getXIntercept(currentY);

                for (int x = xStart; x <= xEnd; x++) {
                    filledPoints.add(new Pixel(x, currentY, "rgba(255,20,147,0.2)"));
                }
            }

            currentY++;
        }

        return filledPoints;
    }

    //Простой алгоритм заполнения с затравкой
    /**
     * Алгоритм:
     * 1. Проверяем, находится ли пиксель в пределах экрана и не был ли он уже обработан.
     * 2. Если пиксель является границей или его цвет не совпадает с целевым, пропускаем его.
     * 3. Заполняем пиксель и добавляем его соседей в стек для дальнейшей обработки.
     */
    public List<Pixel> floodFill(Pixel seed, List<Pixel> polygon, String boundaryColor) {
        List<Pixel> filledPixels = new ArrayList<>();
        boolean[][] visited = new boolean[SCREEN_WIDTH][SCREEN_HEIGHT];
        Stack<Pixel> stack = new Stack<>();

        // Исходный цвет, который будем заменять
        String targetColor = seed.getColor();
        String fillColor = "rgba(115,255,136,0.2)";
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

    //Построчный алгоритм заполнения с затравкой
    /**
     * Алгоритм:
     * 1. Находим интервал на текущей строке, который нужно заполнить.
     * 2. Заполняем интервал и проверяем соседние строки (выше и ниже) для каждого пикселя заполненного интервала.
     * 3. Добавляем новые интервалы в стек для дальнейшей обработки.
     */
    public List<Pixel> scanlineFloodFill(Pixel seed, List<Pixel> polygon, String boundaryColor) {
        List<Pixel> filledPixels = new ArrayList<>();
        boolean[][] visited = new boolean[SCREEN_WIDTH][SCREEN_HEIGHT];
        Stack<Pixel> stack = new Stack<>();

        // Определяем исходный цвет, который будем заменять
        String targetColor = seed.getColor();
        String fillColor = "rgba(105,120,255,0.2)";
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

}