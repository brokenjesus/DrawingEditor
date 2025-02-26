package by.lupach.drawingeditor.models.polygons;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

@Data
public class PolygonEdge {
    private Pixel point1;
    private Pixel point2;

    public PolygonEdge(Pixel point1, Pixel point2) {
        this.point1 = point1;
        this.point2 = point2;
    }

    public double getXIntercept(int y) {
        if (point1.getY() == point2.getY()) {
            return point1.getX(); // Horizontal line, no need for interpolation
        }
        return point1.getX() + (double) (y - point1.getY()) * (point2.getX() - point1.getX()) / (point2.getY() - point1.getY());
    }
}
