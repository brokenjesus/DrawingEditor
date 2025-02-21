package by.lupach.drawingeditor.models;

import lombok.Data;

import java.util.List;

@Data
public class PointInsideRequest {
    private Pixel point;
    private List<Pixel> polygon;
}