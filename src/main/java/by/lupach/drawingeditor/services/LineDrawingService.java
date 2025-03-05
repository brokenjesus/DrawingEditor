package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LineDrawingService {
    public List<Pixel> generateDDALine(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        float xIncrement = dx / (float) steps;
        float yIncrement = dy / (float) steps;
        float x = x1, y = y1;

        for (int i = 0; i <= steps; i++) {
            pixels.add(new Pixel(Math.round(x), Math.round(y),"rgba(0, 0, 0, 255)"));
            x += xIncrement;
            y += yIncrement;
        }

        return pixels;
    }

    public List<Pixel> generateBresenhamLine(int x1, int y1, int x2, int y2) {
        return generateBresenhamLineWithColor(x1, y1, x2, y2, "rgba(0, 0, 0, 255)");
    }

    public List<Pixel> generateBresenhamLineWithColor(int x1, int y1, int x2, int y2, String color) {
        List<Pixel> pixels = new ArrayList<>();
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            pixels.add(new Pixel(x1, y1, color));
            if (x1 == x2 && y1 == y2) {
                break;
            }
            int e2 = err * 2;
            if (e2 > -dy) {
                err -= dy; x1 += sx;
            }
            if (e2 < dx) {
                err += dx; y1 += sy;
            }
        }

        return pixels;
    }

    public List<Pixel> generateWuLine(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();

        boolean isSteep = Math.abs(y2 - y1) > Math.abs(x2 - x1);
        if (isSteep) {
            // Swap x and y coordinates for steep line
            int temp = x1;
            x1 = y1;
            y1 = temp;
            temp = x2;
            x2 = y2;
            y2 = temp;
        }

        if (x1 > x2) {
            // Swap start and end points to ensure x1 <= x2
            int temp = x1;
            x1 = x2;
            x2 = temp;
            temp = y1;
            y1 = y2;
            y2 = temp;
        }

        int dx = x2 - x1;
        int dy = y2 - y1;
        float gradient = dx == 0 ? 1 : (float) dy / dx;

        // Start drawing from the rounded start point
        float roundedStartX = Math.round(x1);
        float initialY = y1 + gradient * (roundedStartX - x1);
        int xPxl1 = (int) roundedStartX;
        int yPxl1 = (int) Math.floor(initialY);

        wuHelperAddPixel(pixels, xPxl1, yPxl1, initialY, isSteep);

        float yPosition = initialY + gradient;

        // Draw intermediate points
        for (int x = xPxl1 + 1; x <= x2; x++) {
            int yPxl = (int) Math.floor(yPosition);
            wuHelperAddPixel(pixels, x, yPxl, yPosition, isSteep);
            yPosition += gradient;
        }

        // Draw the final point
        float roundedEndX = Math.round(x2);
        int endXPixel = (int) roundedEndX;
        int endYPixel = (int) Math.floor(yPosition);
        wuHelperAddPixel(pixels, endXPixel, endYPixel, yPosition, isSteep);

        return pixels;
    }

    private void wuHelperAddPixel(List<Pixel> pixels, int x, int y, float position, boolean isSteep) {
        if (isSteep) {
            pixels.add(new Pixel(y, x, "rgba(0, 0, 0," + rfPart(position) + ")"));
            pixels.add(new Pixel(y + 1, x, "rgba(0, 0, 0," + fPart(position) + ")"));
        } else {
            pixels.add(new Pixel(x, y, "rgba(0, 0, 0," + rfPart(position) + ")"));
            pixels.add(new Pixel(x, y + 1, "rgba(0, 0, 0," + fPart(position) + ")"));
        }
    }

    private float fPart(float x) {
        return x - (int) x;
    }

    private float rfPart(float x) {
        return 1 - fPart(x);
    }

}
