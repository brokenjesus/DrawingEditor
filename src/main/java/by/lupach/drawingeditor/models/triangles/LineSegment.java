package by.lupach.drawingeditor.models.triangles;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

@Data
public class LineSegment {
    private Pixel start;
    private Pixel end;
    public LineSegment(Pixel start, Pixel end) {
        this.start = start;
        this.end = end;
    }
}