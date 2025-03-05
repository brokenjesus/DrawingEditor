package by.lupach.drawingeditor.models.voronoi;


import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;
import lombok.Getter;

import java.util.*;

@Getter
@Data
public class Triangulation {
    private List<Triangle> triangles;

    public Triangulation(List<Pixel> points) {
        triangles = new ArrayList<>();
        performTriangulation(points);
    }

    private void performTriangulation(List<Pixel> points) {
        // Создаём супер-треугольник, охватывающий все точки
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (Pixel p : points) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        int dx = maxX - minX;
        int dy = maxY - minY;
        int deltaMax = Math.max(dx, dy) * 10;

        Pixel p1 = new Pixel(minX - deltaMax, minY - deltaMax);
        Pixel p2 = new Pixel(minX + deltaMax, minY - deltaMax);
        Pixel p3 = new Pixel(minX, minY + deltaMax * 2);

        Triangle superTriangle = new Triangle(p1, p2, p3);
        triangles.add(superTriangle);

        // Добавляем точки по одной
        for (Pixel p : points) {
            List<Triangle> badTriangles = new ArrayList<>();
            List<Edge> edges = new ArrayList<>();

            for (Triangle t : triangles) {
                if (t.containsInCircumcircle(p)) {
                    badTriangles.add(t);
                    edges.addAll(t.getEdges());
                }
            }

            triangles.removeAll(badTriangles);

            // Убираем дублирующиеся рёбра (оставляем только уникальные)
            edges = removeDuplicateEdges(edges);

            for (Edge edge : edges) {
                triangles.add(new Triangle(edge.getA(), edge.getB(), p));
            }
        }

        // Убираем треугольники, содержащие вершины супер-треугольника
        triangles.removeIf(t -> t.hasVertex(p1) || t.hasVertex(p2) || t.hasVertex(p3));
    }

    private List<Edge> removeDuplicateEdges(List<Edge> edges) {
        Map<Edge, Integer> edgeCount = new HashMap<>();

        for (Edge edge : edges) {
            edgeCount.put(edge, edgeCount.getOrDefault(edge, 0) + 1);
        }

        List<Edge> uniqueEdges = new ArrayList<>();
        for (Map.Entry<Edge, Integer> entry : edgeCount.entrySet()) {
            if (entry.getValue() == 1) {
                uniqueEdges.add(entry.getKey());
            }
        }

        return uniqueEdges;
    }

}
