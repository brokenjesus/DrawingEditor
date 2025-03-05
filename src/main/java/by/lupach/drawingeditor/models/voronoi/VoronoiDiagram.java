package by.lupach.drawingeditor.models.voronoi;


import by.lupach.drawingeditor.models.Pixel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;

public class VoronoiDiagram {
    /*
     * Строит рёбра диаграммы Вороного на основе триангуляции Делоне:
     * 1. Создаёт карту рёбер и связанных с ними треугольников
     * 2. Для каждого ребра:
     *    - При наличии двух соседних треугольников создаёт отрезок между их центрами окружностей
     *    - Для граничных рёбер строит перпендикулярный луч из центра окружности до границы ограничивающего прямоугольника
     */
    public List<LineSegment> getVoronoiEdges(List<Triangle> triangles, Rectangle boundingBox) {
        // Построение карты: ребро -> список треугольников, имеющих это ребро
        Map<Edge, List<Triangle>> edgeTriangleMap = new HashMap<>();
        for (Triangle t : triangles) {
            for (Edge edge : t.getEdges()) {
                edgeTriangleMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(t);
            }
        }

        List<LineSegment> voronoiEdges = new ArrayList<>();

        // Для каждого ребра триангуляции определяем дуальное ребро Вороного
        for (Map.Entry<Edge, List<Triangle>> entry : edgeTriangleMap.entrySet()) {
            List<Triangle> adjacentTriangles = entry.getValue();
            if (adjacentTriangles.size() == 2) {
                // Если ребро разделяет два треугольника,
                // дуальная линия – отрезок, соединяющий центры описанных окружностей этих треугольников.
                Pixel cc1 = adjacentTriangles.get(0).getCircumcenter();
                Pixel cc2 = adjacentTriangles.get(1).getCircumcenter();
                voronoiEdges.add(new LineSegment(cc1, cc2));
            } else if (adjacentTriangles.size() == 1) {
                // Ребро на границе триангуляции:
                // строим луч, исходящий из центра описанной окружности единственного треугольника
                // в направлении, перпендикулярном ребру и удалённом от треугольника.
                Triangle t = adjacentTriangles.get(0);
                Pixel cc = t.getCircumcenter();

                // Вычисляем середину ребра
                Pixel p1 = entry.getKey().getA();
                Pixel p2 = entry.getKey().getB();
                double midX = (p1.getX() + p2.getX()) / 2.0;
                double midY = (p1.getY() + p2.getY()) / 2.0;

                // Вычисляем вектор, задающий ребро
                double ex = p2.getX() - p1.getX();
                double ey = p2.getY() - p1.getY();

                // Два кандидата для перпендикулярного направления
                double cand1X = -ey;
                double cand1Y = ex;
                double cand2X = ey;
                double cand2Y = -ex;

                // Выбираем направление, которое уводит от треугольника.
                // Для этого получаем третью вершину треугольника (не принадлежащую ребру)
                Pixel p3 = t.getThirdVertex(entry.getKey());
                double dot1 = cand1X * (p3.getX() - cc.getX()) + cand1Y * (p3.getY() - cc.getY());
                double dot2 = cand2X * (p3.getX() - cc.getX()) + cand2Y * (p3.getY() - cc.getY());
                double chosenDx, chosenDy;
                if (dot1 < dot2) {
                    chosenDx = cand1X;
                    chosenDy = cand1Y;
                } else {
                    chosenDx = cand2X;
                    chosenDy = cand2Y;
                }
                // Нормализуем вектор
                double len = sqrt(chosenDx * chosenDx + chosenDy * chosenDy);
                if (len != 0) {
                    chosenDx /= len;
                    chosenDy /= len;
                }
                // Расширяем луч от центра до границ ограничивающего прямоугольника
                Pixel ccExtended = intersectRayWithRectangle(cc, chosenDx, chosenDy, boundingBox);
                voronoiEdges.add(new LineSegment(cc, ccExtended));
            }
        }
        return voronoiEdges;
    }

    /**
     *
     * Вычисляет пересечение луча с прямоугольной областью:
     * 1. Определяет потенциальные точки пересечения с вертикальными и горизонтальными границами
     * 2. Выбирает ближайшую валидную точку пересечения в направлении луча
     * 3. Возвращает исходную точку при отсутствии пересечений
     */
    private Pixel intersectRayWithRectangle(Pixel origin, double dx, double dy, Rectangle rect) {
        double xMin = rect.getX();
        double yMin = rect.getY();
        double xMax = rect.getX() + rect.getWidth();
        double yMax = rect.getY() + rect.getHeight();
        double tMin = Double.MAX_VALUE;

        if (dx != 0) {
            double t1 = (xMin - origin.getX()) / dx;
            double t2 = (xMax - origin.getX()) / dx;
            if (t1 > 0) tMin = min(tMin, t1);
            if (t2 > 0) tMin = min(tMin, t2);
        }
        if (dy != 0) {
            double t3 = (yMin - origin.getY()) / dy;
            double t4 = (yMax - origin.getY()) / dy;
            if (t3 > 0) tMin = min(tMin, t3);
            if (t4 > 0) tMin = min(tMin, t4);
        }
        if (tMin == Double.MAX_VALUE) {
            return origin;
        }
        int ix = (int) Math.round(origin.getX() + dx * tMin);
        int iy = (int) Math.round(origin.getY() + dy * tMin);
        return new Pixel(ix, iy);
    }
}