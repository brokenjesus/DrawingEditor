package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.stereotype.Service;

import java.util.*;
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

        // Заполняем полигон пикселями
        return fillPolygon(convexHull);
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

        // Заполняем полигон пикселями
        return fillPolygon(hull);
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
}