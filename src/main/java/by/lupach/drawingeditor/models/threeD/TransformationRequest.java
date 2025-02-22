package by.lupach.drawingeditor.models.threeD;

import lombok.Data;

@Data
public class TransformationRequest {
    // Параметры трансформации
    private String transformationType;
    private double x, y, z, sx, sy, sz, angle, d;

    // Данные объекта: вершины и рёбра (индексы вершин)
    // Вершины – двумерный массив [N][4] (однородные координаты)
    private double[][] vertices;
    // Рёбра – двумерный массив [M][2]
    private int[][] edges;
}
