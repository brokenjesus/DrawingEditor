package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CurveDrawingService {
    public List<Pixel> generateCircle(int x0, int y0, double r) {
        List<Pixel> pixels = new ArrayList<>();
        int x = (int) r;
        int y = 0;
        int err = 0;

        while (x >= y) {
            pixels.add(new Pixel(x0 + x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + y, y0 + x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - y, y0 + x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 + y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 - y,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - y, y0 - x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + y, y0 - x,  "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 - y,  "rgba(0, 0, 0, 255)"));

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
        int err = (int) (b2 - a2 * b + (double) a2 / 4);

        while (a2 * y > b2 * x) {
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

        x = (int) a;
        y = 0;
        err = (int) (a2 - b2 * a + (double) b2 / 4);

        while (b2 * x > a2 * y) {
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
        double preErr = 0.5 - a;
        double postErr = 1 - a * Math.ceil(div) - 0.25 * a;

        while (x <= xLimit) {
            pixels.add(new Pixel(x + x0, y + y0, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(-x + x0, y + y0, "rgba(0, 0, 0, 255)"));

            if (x < div) {
                double tmp = -2 * a * x - 3 * a;
                x++;
                if (preErr < 0) {
                    y++;
                    preErr += tmp + 1;
                } else {
                    preErr += tmp;
                }
            } else {
                double tmp = -2 * a * x - 2 * a + 1;
                y++;
                if (postErr >= 0) {
                    x++;
                    postErr += tmp;
                } else {
                    postErr += 1;
                }
            }
        }
        return pixels;
    }


    public List<Pixel> generateHyperbola(int x0, int y0, double a, double b) {
        List<Pixel> pixels = new ArrayList<>();

        a = Math.pow(a, 2);
        b = Math.pow(b, 2);

        int x = (int) Math.abs(Math.sqrt(a));
        int y = 0;

        int xLimit = (int) (a * Math.sqrt(1 + Math.pow(720 / b, 2))); // Adjust based on screen height (720px)

        int err = (int) (b * (2 * x + 1) - a);

        while (x <= xLimit) {
            boolean f1 = (err <= 0) || (2 * err - b * (2 * x + 1) <= 0);
            boolean f2 = (err <= 0) || (2 * err - a * (2 * y + 1) > 0);

            pixels.add(new Pixel(x0 - x, y0 - y, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 + y, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 + x, y0 - y, "rgba(0, 0, 0, 255)"));
            pixels.add(new Pixel(x0 - x, y0 + y, "rgba(0, 0, 0, 255)"));

            if (f1) {
                x++;
            }
            if (f2) {
                y++;
            }

            if (f1) {
                err += (int) (b * (2 * x + 1));
            }
            if (f2) {
                err -= (int) (a * (2 * y - 1));
            }
        }

        return pixels;
    }
}
