package by.lupach.drawingeditor.models.linesAndCurves;

import by.lupach.drawingeditor.models.Pixel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.List;

@Data
@Setter
@Getter
public class DrawingRequest {
    private Integer x1, y1, x2, y2;
    private String algorithm; // For line algorithms
    private String curveType; // For curve types
    private Point center;
    private Double param1, param2;

    private List<Pixel> points; //For interpolation/approximation
}