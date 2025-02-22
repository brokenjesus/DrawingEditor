package by.lupach.drawingeditor.models.polygons;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;
import java.util.List;

@Data
public class SegmentIntersectsRequest {
    private Pixel a;
    private Pixel b;
    private List<Pixel> polygon;
}