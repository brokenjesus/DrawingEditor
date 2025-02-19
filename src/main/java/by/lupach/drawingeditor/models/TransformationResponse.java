package by.lupach.drawingeditor.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransformationResponse {
    private List<Pixel> pixels;
    private double[][] matrix;
}
