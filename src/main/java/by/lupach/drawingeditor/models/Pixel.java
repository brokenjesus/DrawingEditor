package by.lupach.drawingeditor.models;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.awt.*;

@Data
@SuperBuilder
@Builder
@Getter
@Setter
@AllArgsConstructor
public class Pixel {
    private int x;
    private int y;
    private String color;
}
