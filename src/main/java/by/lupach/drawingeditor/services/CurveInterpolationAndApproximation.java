package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class CurveInterpolationAndApproximation {

    /**
     * Генерация кривой методом Эрмита.
     * Для каждой пары последовательных точек вычисляются касательные (по X и Y)
     * и затем по шагу t от 0 до 1 вычисляются промежуточные точки.
     */
    public List<Pixel> generateHermiteCurve(List<Pixel> points) {
        List<Pixel> pixels = new ArrayList<>();
        if (points == null || points.size() < 2) return pixels;
        double step = 0.001;
        for (int i = 0; i < points.size() - 1; i++) {
            Pixel p0 = points.get(i);
            Pixel p1 = points.get(i + 1);
            double t0x, t0y, t1x, t1y;
            if (i > 0) {
                Pixel pPrev = points.get(i - 1);
                t0x = (p1.getX() - pPrev.getX()) / 2.0;
                t0y = (p1.getY() - pPrev.getY()) / 2.0;
            } else {
                t0x = 0;
                t0y = 0;
            }
            if (i < points.size() - 2) {
                Pixel pNext = points.get(i + 2);
                t1x = (pNext.getX() - p0.getX()) / 2.0;
                t1y = (pNext.getY() - p0.getY()) / 2.0;
            } else {
                t1x = 0;
                t1y = 0;
            }
            for (double t = 0; t <= 1; t += step) {
                double h1 = 2 * Math.pow(t, 3) - 3 * Math.pow(t, 2) + 1;
                double h2 = -2 * Math.pow(t, 3) + 3 * Math.pow(t, 2);
                double h3 = Math.pow(t, 3) - 2 * Math.pow(t, 2) + t;
                double h4 = Math.pow(t, 3) - Math.pow(t, 2);

                double x = h1 * p0.getX() + h2 * p1.getX() + h3 * t0x + h4 * t1x;
                double y = h1 * p0.getY() + h2 * p1.getY() + h3 * t0y + h4 * t1y;
                pixels.add(new Pixel((int) Math.round(x), (int) Math.round(y), "rgba(0, 0, 0, 255)"));
            }
        }
        return pixels;
    }

    /**
     * Генерация кривой Безье.
     * Для каждой группы из 4 точек вычисляются промежуточные точки.
     */
    public List<Pixel> generateBezierCurve(List<Pixel> points) {
        List<Pixel> pixels = new ArrayList<>();
        if (points == null || points.size() < 4 || ((points.size() - 1) % 3) != 0) {
            return pixels;
        }
        int iterations = 1000; // можно регулировать число итераций
        for (int i = 0; i < points.size() - 1; i += 3) {
            Pixel p0 = points.get(i);
            Pixel p1 = points.get(i + 1);
            Pixel p2 = points.get(i + 2);
            Pixel p3 = points.get(i + 3);
            for (int j = 0; j <= iterations; j++) {
                double t = (double) j / iterations;
                double oneMinusT = 1 - t;
                double x = Math.pow(oneMinusT, 3) * p0.getX() +
                        3 * Math.pow(oneMinusT, 2) * t * p1.getX() +
                        3 * oneMinusT * Math.pow(t, 2) * p2.getX() +
                        Math.pow(t, 3) * p3.getX();
                double y = Math.pow(oneMinusT, 3) * p0.getY() +
                        3 * Math.pow(oneMinusT, 2) * t * p1.getY() +
                        3 * oneMinusT * Math.pow(t, 2) * p2.getY() +
                        Math.pow(t, 3) * p3.getY();
                pixels.add(new Pixel((int) Math.round(x), (int) Math.round(y), "rgba(0, 0, 0, 255)"));
            }
        }
        return pixels;
    }

    /**
     * Генерация кривой BSpline (равномерный кубический B‑сплайн).
     * Для каждой группы из 4 точек вычисляются точки кривой по базисным функциям.
     */
    public List<Pixel> generateBSplineCurve(List<Pixel> points) {
        List<Pixel> pixels = new ArrayList<>();
        if (points == null || points.size() < 4) return pixels;
        double step = 0.001;
        // Для i от 0 до (n - 4)
        for (int i = 0; i <= points.size() - 4; i++) {
            Pixel p0 = points.get(i);
            Pixel p1 = points.get(i + 1);
            Pixel p2 = points.get(i + 2);
            Pixel p3 = points.get(i + 3);
            for (double t = 0; t <= 1; t += step) {
                double B0 = Math.pow(1 - t, 3) / 6.0;
                double B1 = (3 * Math.pow(t, 3) - 6 * Math.pow(t, 2) + 4) / 6.0;
                double B2 = (-3 * Math.pow(t, 3) + 3 * Math.pow(t, 2) + 3 * t + 1) / 6.0;
                double B3 = Math.pow(t, 3) / 6.0;

                double x = B0 * p0.getX() + B1 * p1.getX() + B2 * p2.getX() + B3 * p3.getX();
                double y = B0 * p0.getY() + B1 * p1.getY() + B2 * p2.getY() + B3 * p3.getY();
                pixels.add(new Pixel((int) Math.round(x), (int) Math.round(y), "rgba(0, 0, 0, 255)"));
            }
        }
        return pixels;
    }
}
