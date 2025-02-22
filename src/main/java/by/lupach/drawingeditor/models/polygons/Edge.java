package by.lupach.drawingeditor.models.polygons;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

@Data
public class Edge {
    int yMax;
    int yMin;
    double x;
    double dxPerY;

    public Edge(Pixel p1, Pixel p2) {
        if (p1.getY() < p2.getY()) {
            yMin = p1.getY();
            yMax = p2.getY();
            x = p1.getX();
        } else {
            yMin = p2.getY();
            yMax = p1.getY();
            x = p2.getX();
        }
        dxPerY = (double)(p2.getX() - p1.getX()) / (p2.getY() - p1.getY());
    }
}
