package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import by.lupach.drawingeditor.models.voronoi.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static by.lupach.drawingeditor.configs.ScreenConstants.SCREEN_HEIGHT;
import static by.lupach.drawingeditor.configs.ScreenConstants.SCREEN_WIDTH;

@Service
public class VoronoiDiagramService {
    @Autowired
    LineDrawingService lineService;

    public List<Pixel> buildVoronoiDiagram(List<Pixel> points) {
        // Определяем ограничивающий прямоугольник с небольшим отступом
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (Pixel a : points) {
            if (a.getX() < minX) minX = a.getX();
            if (a.getY() < minY) minY = a.getY();
            if (a.getX() > maxX) maxX = a.getX();
            if (a.getY() > maxY) maxY = a.getY();
        }
        Rectangle boundingBox = new Rectangle(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        List<Pixel> resultPixels = new ArrayList<>();
        // 1. Строим триангуляцию Делоне методом Bowyer–Watson
        Triangulation triangulation = new Triangulation(points);
        List<Triangle> triangles = triangulation.getTriangles();

        for (Triangle triangle : triangles) {
            Pixel a = triangle.getA();
            Pixel b = triangle.getB();
            Pixel c = triangle.getC();

            // Отрисовываем ребра треугольника
            List<Pixel> edgeAB = lineService.generateBresenhamLineWithColor(a.getX(), a.getY(), b.getX(), b.getY(), "rgba(0, 255, 0, 255)");
            List<Pixel> edgeBC = lineService.generateBresenhamLineWithColor(b.getX(), b.getY(), c.getX(), c.getY(), "rgba(0, 255, 0, 255)");
            List<Pixel> edgeCA = lineService.generateBresenhamLineWithColor(c.getX(), c.getY(), a.getX(), a.getY(), "rgba(0, 255, 0, 255)");

            // Добавляем пиксели ребер в результат
            resultPixels.addAll(edgeAB);
            resultPixels.addAll(edgeBC);
            resultPixels.addAll(edgeCA);
        }

        // 2. Из триангуляции вычисляем ребра диаграммы Вороного
        VoronoiDiagram vd = new VoronoiDiagram();
        List<LineSegment> voronoiEdges = vd.getVoronoiEdges(triangles, boundingBox);

        // 3. Для каждого отрезка отрисовываем линию (например, алгоритмом Брезенхэма)
        for (LineSegment seg : voronoiEdges) {
            List<Pixel> linePixels = lineService.generateBresenhamLine(
                    seg.getStart().getX(), seg.getStart().getY(), seg.getEnd().getX(), seg.getEnd().getY());
            resultPixels.addAll(linePixels);
        }
        return resultPixels;
    }
}
