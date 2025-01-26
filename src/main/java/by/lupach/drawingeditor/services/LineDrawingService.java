package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.stereotype.Service;

import java.awt.*;
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
            pixels.add(new Pixel(Math.round(x), Math.round(y),"rgba(0, 0, 0, 255)")); // Full opacity for primary pixel
            x += xIncrement;
            y += yIncrement;
        }

        return pixels;
    }

    public List<Pixel> generateBresenhamLine(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            pixels.add(new Pixel(x1, y1, "rgba(0, 0, 0, 255)")); // Full opacity for primary pixel
            if (x1 == x2 && y1 == y2) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }

        return pixels;
    }

    public List<Pixel> generateWuLine(int x1, int y1, int x2, int y2) {
        List<Pixel> pixels = new ArrayList<>();

        // Handle steep lines (swap x and y)
        boolean steep = Math.abs(y2 - y1) > Math.abs(x2 - x1);
        if (steep) {
            int temp = x1;
            x1 = y1;
            y1 = temp;
            temp = x2;
            x2 = y2;
            y2 = temp;
        }

        if (x1 > x2) {
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

        float xend = Math.round(x1);
        float yend = y1 + gradient * (xend - x1);
        int xpxl1 = (int) xend;
        int ypxl1 = (int) Math.floor(yend);

        if (steep) {
            pixels.add(new Pixel(ypxl1, xpxl1, "rgba(0, 0, 0,"+ rfpart(yend)+")"));
            pixels.add(new Pixel(ypxl1 + 1, xpxl1, "rgba(0, 0, 0,"+ fpart(yend)+")"));
        } else {
            pixels.add(new Pixel(xpxl1, ypxl1, "rgba(0, 0, 0,"+ rfpart(yend)+")"));
            pixels.add(new Pixel(xpxl1, ypxl1 + 1, "rgba(0, 0, 0,"+ fpart(yend)+")"));
        }

        float intery = yend + gradient;

        for (int x = xpxl1 + 1; x <= x2; x++) {
            int ypxl = (int) Math.floor(intery);
            if (steep) {
                pixels.add(new Pixel(ypxl, x, "rgba(0, 0, 0,"+ rfpart(intery)+")"));
                pixels.add(new Pixel(ypxl + 1, x, "rgba(0, 0, 0,"+ fpart(intery)+")"));
            } else {
                pixels.add(new Pixel(x, ypxl, "rgba(0, 0, 0,"+ rfpart(intery)+")"));
                pixels.add(new Pixel(x, ypxl + 1, "rgba(0, 0, 0,"+ fpart(intery)+")"));
            }
            intery += gradient;
        }

        float xend2 = Math.round(x2);
        int xpxl2 = (int) xend2;
        int ypxl2 = (int) Math.floor(intery);

        if (steep) {
            pixels.add(new Pixel(ypxl2, xpxl2, "rgba(0, 0, 0,"+ rfpart(intery)+")"));
            pixels.add(new Pixel(ypxl2 + 1, xpxl2, "rgba(0, 0, 0,"+ fpart(intery)+")"));
        } else {
            pixels.add(new Pixel(xpxl2, ypxl2, "rgba(0, 0, 0,"+ rfpart(intery)+")"));
            pixels.add(new Pixel(xpxl2, ypxl2 + 1, "rgba(0, 0, 0,"+ fpart(intery)+")"));
        }

        return pixels;
    }

    // Helper method to calculate the fractional part
    private float fpart(float x) {
        return x - (int) x;
    }

    // Helper method to calculate the reverse fractional part
    private float rfpart(float x) {
        return 1 - fpart(x);
    }
}
