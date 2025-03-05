package by.lupach.drawingeditor.models.voronoi;

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
    public Pixel getStart() { return start; }
    public Pixel getEnd() { return end; }
    @Override
    public String toString() {
        return "LineSegment{" + start + " -> " + end + "}";
    }
}