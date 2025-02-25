package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import by.lupach.drawingeditor.models.triangles.Edge;
import by.lupach.drawingeditor.models.triangles.LineSegment;
import by.lupach.drawingeditor.models.triangles.Rectangle;
import by.lupach.drawingeditor.models.triangles.Triangle;
import org.springframework.stereotype.Service;

import java.util.*;

import static by.lupach.drawingeditor.configs.ScreenConstants.*;

@Service
public class VoronoiDiagramService {

    /**
     * Построение диаграммы Вороного по заданному набору точек.
     * Алгоритм:
     * 1. Вычисляем ограничивающий прямоугольник (bounding box).
     * 2. Строим триангуляцию Делоне методом Bowyer–Watson.
     * 3. По триангуляции находим дуальные ребра (центры описанных окружностей соседних треугольников).
     * 4. Для ребер на границе расширяем до границ прямоугольника.
     * 5. Для каждого отрезка вызываем сервис отрисовки линии, получая список пикселей.
     *
     * @param sites       набор точек (сайтов)
     * @param lineService сервис отрисовки линий
     * @return список пикселей, составляющих отрисованные линии диаграммы Вороного
     */
    public static List<Pixel> buildVoronoiDiagram(List<Pixel> sites, LineDrawingService lineService) {
        // Определяем ограничивающий прямоугольник с небольшим отступом
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Pixel p : sites) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() > maxY) maxY = p.getY();
        }
        Rectangle boundingBox = new Rectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // 1. Строим триангуляцию Делоне методом Bowyer–Watson
        DelaunayTriangulation dt = new DelaunayTriangulation();
        List<Triangle> triangles = dt.triangulate(sites);

        // 2. Из триангуляции вычисляем ребра диаграммы Вороного
        VoronoiDiagram vd = new VoronoiDiagram();
        List<LineSegment> voronoiEdges = vd.getVoronoiEdges(triangles, boundingBox);

        // 3. Для каждого отрезка отрисовываем линию (например, алгоритмом Брезенхэма)
        List<Pixel> resultPixels = new ArrayList<>();
        for (LineSegment seg : voronoiEdges) {
            List<Pixel> linePixels = lineService.generateBresenhamLine(
                    seg.getStart().getX(), seg.getStart().getY(), seg.getEnd().getX(), seg.getEnd().getY());
            resultPixels.addAll(linePixels);
        }
        return resultPixels;
    }



    // Класс для построения триангуляции Делоне методом Bowyer–Watson
    public static class DelaunayTriangulation {
        public List<Triangle> triangulate(List<Pixel> points) {
            List<Triangle> triangles = new ArrayList<>();
            // Создаём "супертреугольник", охватывающий все точки
            Triangle superTriangle = createSuperTriangle(points);
            triangles.add(superTriangle);

            for (Pixel p : points) {
                List<Triangle> badTriangles = new ArrayList<>();
                // Находим все треугольники, у которых точка p лежит в описанной окружности
                for (Triangle t : triangles) {
                    if (t.containsInCircumcircle(p)) {
                        badTriangles.add(t);
                    }
                }
                // Формируем полигон (неповторяющиеся ребра) из треугольников, подлежащих удалению
                List<Edge> polygon = new ArrayList<>();
                for (Triangle t : badTriangles) {
                    for (Edge e : t.getEdges()) {
                        boolean shared = false;
                        for (Triangle ot : badTriangles) {
                            if (t == ot) continue;
                            if (ot.getEdges().contains(e)) {
                                shared = true;
                                break;
                            }
                        }
                        if (!shared) {
                            polygon.add(e);
                        }
                    }
                }
                // Удаляем "плохие" треугольники
                triangles.removeAll(badTriangles);
                // Соединяем ребра полигона с точкой p, формируя новые треугольники
                for (Edge e : polygon) {
                    Triangle newTri = new Triangle(e.getP(), e.getQ(), p);
                    triangles.add(newTri);
                }
            }
            // Удаляем треугольники, содержащие вершины супертреугольника
            triangles.removeIf(t -> t.hasVertex(superTriangle.getA()) ||
                    t.hasVertex(superTriangle.getB()) || t.hasVertex(superTriangle.getC()));
            return triangles;
        }

        // Создаёт супертреугольник, охватывающий все точки
        private Triangle createSuperTriangle(List<Pixel> points) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (Pixel p : points) {
                if (p.getX() < minX) minX = p.getX();
                if (p.getY() < minY) minY = p.getY();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getY() > maxY) maxY = p.getY();
            }
            int dx = maxX - minX;
            int dy = maxY - minY;
            int deltaMax = Math.max(dx, dy);
            int midX = (minX + maxX) / 2;
            int midY = (minY + maxY) / 2;

            // Создаём вершины супертреугольника
            Pixel p1 = new Pixel(midX - 2 * deltaMax, midY - deltaMax);
            Pixel p2 = new Pixel(midX, midY + 2 * deltaMax);
            Pixel p3 = new Pixel(midX + 2 * deltaMax, midY - deltaMax);
            return new Triangle(p1, p2, p3);
        }
    }

    // Класс для построения диаграммы Вороного по триангуляции Делоне
    public static class VoronoiDiagram {
        /**
         * Для каждого ребра триангуляции:
         * – Если ребро разделяют два треугольника, то соединяем их описанные окружности.
         * – Если ребро граничное (принадлежит одному треугольнику), то проводим луч от центра окружности в направлении,
         *   перпендикулярном ребру, и обрезаем его по boundingBox.
         */
        public List<LineSegment> getVoronoiEdges(List<Triangle> triangles, Rectangle boundingBox) {
            Map<Edge, List<Triangle>> edgeTriangleMap = new HashMap<>();
            for (Triangle t : triangles) {
                for (Edge e : t.getEdges()) {
                    edgeTriangleMap.computeIfAbsent(e, k -> new ArrayList<>()).add(t);
                }
            }
            List<LineSegment> vorEdges = new ArrayList<>();
            for (Map.Entry<Edge, List<Triangle>> entry : edgeTriangleMap.entrySet()) {
                List<Triangle> tris = entry.getValue();
                if (tris.size() == 2) {
                    // Ребро разделяют два треугольника – соединяем их центры описанных окружностей
                    Pixel cc1 = tris.get(0).getCircumcenter();
                    Pixel cc2 = tris.get(1).getCircumcenter();
                    vorEdges.add(new LineSegment(cc1, cc2));
                } else if (tris.size() == 1) {
                    // Граничное ребро – расширяем луч от центра описанной окружности до границы boundingBox
                    Triangle t = tris.get(0);
                    Pixel cc = t.getCircumcenter();
                    // Вычисляем середину ребра
                    Pixel mid = midpoint(entry.getKey().getP(), entry.getKey().getQ());
                    // Вектор от центра окружности к середине ребра
                    int dx = mid.getX() - cc.getX();
                    int dy = mid.getY() - cc.getY();
                    // Перпендикулярный вектор
                    Pixel dir = new Pixel(dy, -dx);
                    // Нормируем и задаём большое значение (например, 1000 пикселей)
                    double len = Math.sqrt(dir.getX() * dir.getX() + dir.getY() * dir.getY());
                    if (len == 0) continue;
                    int dirX = (int) (dir.getX() / len * 1000);
                    int dirY = (int) (dir.getY() / len * 1000);
                    Pixel farPoint = extendToBoundary(cc, new Pixel(dirX, dirY), boundingBox);
                    vorEdges.add(new LineSegment(cc, farPoint));
                }
            }
            return vorEdges;
        }

        // Середина двух точек
        private Pixel midpoint(Pixel p1, Pixel p2) {
            int mx = (p1.getX() + p2.getX()) / 2;
            int my = (p1.getY() + p2.getY()) / 2;
            return new Pixel(mx, my);
        }

        // Простое расширение линии от точки start в направлении direction с обрезкой по boundingBox
        private Pixel extendToBoundary(Pixel start, Pixel direction, Rectangle rect) {
            int x2 = start.getX() + direction.getX();
            int y2 = start.getY() + direction.getY();
            // Для простоты – просто ограничиваем конечную точку по границам
            int clampedX = Math.max(rect.getX(), Math.min(x2, rect.getX() + rect.getWidth()));
            int clampedY = Math.max(rect.getY(), Math.min(y2, rect.getY() + rect.getHeight()));
            return new Pixel(clampedX, clampedY);
        }
    }
}
