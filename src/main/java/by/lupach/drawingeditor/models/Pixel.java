package by.lupach.drawingeditor.models;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.awt.*;

@Data
@SuperBuilder
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pixel {
    private int x;
    private int y;
    private String color;

    public static int getDistance(Pixel p0, Pixel p1) {
        return ((int) Math.sqrt(Math.pow(p0.getX()-Math.abs(p0.getX()-p1.getX()),2)+Math.pow(p0.getY()-Math.abs(p0.getY()-p1.getY()),2)));
    }

}
