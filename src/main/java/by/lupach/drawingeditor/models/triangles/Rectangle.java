package by.lupach.drawingeditor.models.triangles;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

@Data
public class Rectangle {
    private int x, y, width, height;
    public Rectangle(int x, int y, int width, int height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }
    public boolean contains(Pixel p) {
        return p.getX() >= x && p.getX() <= x + width && p.getY() >= y && p.getY() <= y + height;
    }
}
