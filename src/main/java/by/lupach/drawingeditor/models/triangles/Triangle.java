package by.lupach.drawingeditor.models.triangles;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public  class Triangle {
    private Pixel a, b, c;
    private Pixel circumcenter;
    private double circumradius;

    public Triangle(Pixel a, Pixel b, Pixel c) {
        this.a = a;
        this.b = b;
        this.c = c;
        computeCircumcircle();
    }

    // Вычисление центра и радиуса описанной окружности
    private void computeCircumcircle() {
        double ax = a.getX(), ay = a.getY();
        double bx = b.getX(), by = b.getY();
        double cx = c.getX(), cy = c.getY();

        double d = 2 * (ax * (by - cy) + bx * (cy - ay) + cx * (ay - by));
        if (Math.abs(d) < 1e-6) {
            // Если точки коллинеарны, задаём центр как среднее
            circumcenter = new Pixel((int) ((ax + bx + cx) / 3), (int) ((ay + by + cy) / 3));
            circumradius = Double.MAX_VALUE;
            return;
        }
        double ax2ay2 = ax * ax + ay * ay;
        double bx2by2 = bx * bx + by * by;
        double cx2cy2 = cx * cx + cy * cy;

        double ux = ((ax2ay2) * (by - cy) + (bx2by2) * (cy - ay) + (cx2cy2) * (ay - by)) / d;
        double uy = ((ax2ay2) * (cx - bx) + (bx2by2) * (ax - cx) + (cx2cy2) * (bx - ax)) / d;

        circumcenter = new Pixel((int) ux, (int) uy);
        circumradius = Math.sqrt((ux - ax) * (ux - ax) + (uy - ay) * (uy - ay));
    }

    // Проверка: находится ли точка p внутри описанной окружности треугольника
    public boolean containsInCircumcircle(Pixel p) {
        double dx = circumcenter.getX() - p.getX();
        double dy = circumcenter.getY() - p.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        return dist < circumradius;
    }

    // Проверка: является ли точка вершиной треугольника
    public boolean hasVertex(Pixel p) {
        return a.equals(p) || b.equals(p) || c.equals(p);
    }

    // Возвращает список ребер треугольника
    public List<Edge> getEdges() {
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(a, b));
        edges.add(new Edge(b, c));
        edges.add(new Edge(c, a));
        return edges;
    }
}
