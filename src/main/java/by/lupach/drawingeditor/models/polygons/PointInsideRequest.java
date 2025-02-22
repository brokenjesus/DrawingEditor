package by.lupach.drawingeditor.models.polygons;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

import java.util.List;

@Data
public class PointInsideRequest {
    private Pixel point;
    private List<Pixel> polygon;
}