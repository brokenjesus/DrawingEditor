package by.lupach.drawingeditor.models.polygons;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;

import java.util.List;

@Data
public class PolygonFillRequest {
    private List<Pixel> polygon;
    private Pixel seed;
    private String boundaryColor;
    private String algorithm;
}