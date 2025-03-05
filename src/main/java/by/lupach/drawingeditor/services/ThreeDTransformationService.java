package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.threeD.Matrix4f;
import by.lupach.drawingeditor.models.Pixel;
import by.lupach.drawingeditor.models.threeD.TransformationRequest;
import by.lupach.drawingeditor.models.threeD.TransformationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ThreeDTransformationService {

    private final int canvasWidth = 800;
    private final int canvasHeight = 600;
    private final double scale = 100.0;

    @Autowired
    private LineDrawingService lineDrawingService;

    private double[][] currentMatrix = Matrix4f.identity();

    public TransformationResponse applyTransformation(TransformationRequest request) {
        double[][] newTransform = Matrix4f.identity();
        switch (request.getTransformationType()) {
            case "translation":
                newTransform = Matrix4f.translation(request.getX(), request.getY(), request.getZ());
                break;
            case "rotationX":
                newTransform = Matrix4f.rotationX(request.getAngle());
                break;
            case "rotationY":
                newTransform = Matrix4f.rotationY(request.getAngle());
                break;
            case "scaling":
                newTransform = Matrix4f.scaling(request.getSx(), request.getSy(), request.getSz());
                break;
            case "perspective":
                newTransform = Matrix4f.perspective(request.getD());
                break;
            default:
                // Если тип не определён, можно оставить матрицу без изменений
                break;
        }
        currentMatrix = Matrix4f.multiply(newTransform, currentMatrix);

        double[][] transformedVertices = transform(request.getVertices(), currentMatrix);

        List<Pixel> pixels = new ArrayList<>();
        for (int[] edge : request.getEdges()) {
            double[] v0 = transformedVertices[edge[0]];
            double[] v1 = transformedVertices[edge[1]];

            // Перевод в экранные координаты
            int x0 = (int) (canvasWidth / 2 + v0[0] * scale);
            int y0 = (int) (canvasHeight / 2 - v0[1] * scale);
            int x1 = (int) (canvasWidth / 2 + v1[0] * scale);
            int y1 = (int) (canvasHeight / 2 - v1[1] * scale);

            pixels.addAll(lineDrawingService.generateBresenhamLine(x0, y0, x1, y1));
        }

        return new TransformationResponse(pixels, currentMatrix);
    }

    private double[][] transform(double[][] points, double[][] matrix) {
        double[][] result = new double[points.length][4];
        for (int i = 0; i < points.length; i++) {
            result[i] = Matrix4f.multiply(matrix, points[i]);
                if (result[i][3] != 0) {
                result[i][0] /= result[i][3];
                result[i][1] /= result[i][3];
                result[i][2] /= result[i][3];
            }
        }
        return result;
    }
}
