package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CurveDrawingService {
    @Autowired
    LineDrawingService lineDrawingService;

    public List<Pixel> generateCircle(int x0, int y0, double r) {
        List<Pixel> pixels = new ArrayList<>();
        int x = (int) r;
        int y = 0;
        int err = 0;

        while (x >= y) {
            // Отображение симметричных точек
            pixels.add(new Pixel(x0 + x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + y, y0 + x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - y, y0 + x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 - y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - y, y0 - x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + y, y0 - x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 - y,  "rgba(0, 0, 0, 255)"));

            // Корректировка ошибки
            y++;
            err += 2 * y + 1;
            if (2 * err + 1 > 2 * x) {
                x--;
                err -= 2 * x + 1;
            }
        }

        return pixels;
    }

    public List<Pixel> generateEllipse(int x0, int y0, double a, double b) {
        List<Pixel> pixels = new ArrayList<>();
        int x = 0;
        int y = (int) b;
        int a2 = (int) (a * a);
        int b2 = (int) (b * b);
        int err = (int) (b2 - a2 * b + a2 / 4);

        while (a2 * y > b2 * x) {
            // Отображение симметричных точек
            pixels.add(new Pixel(x0 + x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 - y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 - y,  "rgba(0, 0, 0, 255)"));

            if (err <= 0) {
                err += 2 * b2 * x + 3 * b2;
            } else {
                err += 2 * (b2 * x - a2 * y) + 2 * a2 + 3 * b2;
                y--;
            }
            x++;
        }

        // Первая часть эллипса завершена, теперь рисуем симметричные точки для второй части
        x = (int) a;
        y = 0;
        err = (int) (a2 - b2 * a + b2 / 4);

        while (b2 * x > a2 * y) {
            // Отображение симметричных точек
            pixels.add(new Pixel(x0 + x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 - y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 - y,  "rgba(0, 0, 0, 255)"));

            if (err <= 0) {
                err += 2 * a2 * y + 3 * a2;
            } else {
                err += 2 * (a2 * y - b2 * x) + 2 * b2 + 3 * a2;
                x--;
            }
            y++;
        }

        return pixels;
    }


    public List<Pixel> generateParabola(int x0, int y0, double a) {
        List<Pixel> pixels = new ArrayList<>();

        int xLimit = (int) Math.sqrt(720 / Math.abs(a)); // Оценка предела x по высоте 720

        double div = 0.5 / a;
        int x = 0, y = 0;
        double dPre = 0.5 - a;
        double dPost = 1 - a * Math.ceil(div) - 0.25 * a;

        while (x <= xLimit) {
            pixels.add(new Pixel(x + x0, y + y0, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(-x + x0, y + y0, "rgba(0, 0, 0, 255)"));

            if (x < div) {
                double tmp = -2 * a * x - 3 * a;
                x++;
                if (dPre < 0) {
                    y++;
                    dPre += tmp + 1;
                } else {
                    dPre += tmp;
                }
            } else {
                double tmp = -2 * a * x - 2 * a + 1;
                y++;
                if (dPost >= 0) {
                    x++;
                    dPost += tmp;
                } else {
                    dPost += 1;
                }
            }
        }
        return pixels;
    }


    public List<Pixel> generateHyperbola(int x0, int y0, double a, double b) {
        List<Pixel> pixels = new ArrayList<>();

        // Squared values of a and b
        a = Math.pow(a, 2);
        b = Math.pow(b, 2);

        // Starting point
        int x = (int) Math.abs(Math.sqrt(a));
        int y = 0;

        // Calculating the maximum x value
        int xLimit = (int) (a * Math.sqrt(1 + Math.pow(720 / b, 2))); // Adjust based on screen height (720px)

        // Initial decision variable
        int d = (int) (b * (2 * x + 1) - a);

        while (x <= xLimit) {
            // Determine which region to move to
            boolean f1 = (d <= 0) || (2 * d - b * (2 * x + 1) <= 0);
            boolean f2 = (d <= 0) || (2 * d - a * (2 * y + 1) > 0);

            // Add points for the four quadrants of the hyperbola
            pixels.add(new Pixel(x0 - x, y0 - y, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 + y, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 - y, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 + y, "rgba(0, 0, 0, 255)"));

            // Update x and y values depending on the region
            if (f1) {
                x++;
            }
            if (f2) {
                y++;
            }

            // Update the decision variable
            if (f1) {
                d += b * (2 * x + 1);
            }
            if (f2) {
                d -= a * (2 * y - 1);
            }
        }

        return pixels;
    }
}
