package by.lupach.drawingeditor.models.voronoi;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.round;

@Data
public class Triangle {
    private Pixel a;
    private Pixel b;
    private Pixel c;
    private Pixel circumcenter;
    private double circumradiusSquared;

    public Triangle(Pixel a, Pixel b, Pixel c) {
        this.a = a;
        this.b = b;
        this.c = c;
        calculateCircumcircle();
    }

    /**
     * Возвращает список трёх рёбер треугольника.
     */
    public List<Edge> getEdges() {
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(a, b));
        edges.add(new Edge(b, c));
        edges.add(new Edge(c, a));
        return edges;
    }

    /**
     * Вычисляет центр и радиус описанной окружности треугольника.
     */
    private void calculateCircumcircle() {
        double x1 = a.getX(), y1 = a.getY();
        double x2 = b.getX(), y2 = b.getY();
        double x3 = c.getX(), y3 = c.getY();
        double d = 2 * (x1*(y2 - y3) + x2*(y3 - y1) + x3*(y1 - y2));
        if (d == 0) {
            // На случай вырожденного треугольника
            circumcenter = new Pixel((int) x1, (int) y1);
            circumradiusSquared = 0;
            return;
        }
        double x1Sq = x1*x1 + y1*y1;
        double x2Sq = x2*x2 + y2*y2;
        double x3Sq = x3*x3 + y3*y3;
        double ux = (x1Sq*(y2 - y3) + x2Sq*(y3 - y1) + x3Sq*(y1 - y2)) / d;
        double uy = (x1Sq*(x3 - x2) + x2Sq*(x1 - x3) + x3Sq*(x2 - x1)) / d;
        circumcenter = new Pixel((int) round(ux), (int) round(uy));
        double dx = circumcenter.getX() - x1;
        double dy = circumcenter.getY() - y1;
        circumradiusSquared = dx*dx + dy*dy;
    }

    public boolean hasVertex(Pixel p) {
        return a.equals(p) || b.equals(p) || c.equals(p);
    }

    /**
     * Проверяет, находится ли точка p внутри описанной окружности треугольника.
     */
    public boolean containsInCircumcircle(Pixel p) {
        double dx = p.getX() - circumcenter.getX();
        double dy = p.getY() - circumcenter.getY();
        double distSquared = dx*dx + dy*dy;
        return distSquared <= circumradiusSquared;
    }

    /**
     * Для заданного ребра возвращает ту вершину треугольника,
     * которая не принадлежит данному ребру.
     */
    public Pixel getThirdVertex(Edge edge) {
        if (!a.equals(edge.getA()) && !a.equals(edge.getB())) return a;
        if (!b.equals(edge.getA()) && !b.equals(edge.getB())) return b;
        return c;
    }

    @Override
    public String toString() {
        return "Triangle{" + a + ", " + b + ", " + c + "}";
    }
}