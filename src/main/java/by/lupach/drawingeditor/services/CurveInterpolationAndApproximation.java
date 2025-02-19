package by.lupach.drawingeditor.services;

import by.lupach.drawingeditor.models.Pixel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CurveInterpolationAndApproximation {
    /**
     * Генерация кривой Эрмита.
     * Для каждой группы из 2 точек с касательными вычисляются промежуточные точки.
     * P(t) = h1(t)*p0 + h2(t)*p1+h3(t)*r0+h4(t)*r1
     */
    public List<Pixel> generateHermiteCurve(List<Pixel> points) {
        List<Pixel> pixels = new ArrayList<>();

        if (points == null || points.size() != 4) {
            return pixels;
        }


        Pixel p0 = points.get(0); // Первая точка
        Pixel p1 = points.get(2); // Вторая точка
        Pixel r0 = points.get(1); // Касательная 1
        Pixel r1 = points.get(3); // Касательная 2

        int pointsNum = Pixel.getDistance(p0, p1) + Pixel.getDistance(r0, p0) + Pixel.getDistance(r1, p1);


        for (int i = 0; i <= pointsNum; i++) {
            double t = (double) i / pointsNum;
            double[] hermiteBasis = getHermiteBasis(t);

            double x = hermiteBasis[0] * p0.getX() + hermiteBasis[1] * p1.getX() +
                    hermiteBasis[2] * r0.getX() + hermiteBasis[3] * r1.getX();

            double y = hermiteBasis[0] * p0.getY() + hermiteBasis[1] * p1.getY() +
                    hermiteBasis[2] * r0.getY() + hermiteBasis[3] * r1.getY();

            pixels.add(new Pixel((int) x, (int) y,  "rgba(0, 0, 0, 255)"));
        }

        return pixels;
    }

    private double[] getHermiteBasis(double t) {
        double h1 = 2 * Math.pow(t, 3) - 3 * Math.pow(t, 2) + 1;
        double h2 = -2 * Math.pow(t, 3) + 3 * Math.pow(t, 2);
        double h3 = Math.pow(t, 3) - 2 * Math.pow(t, 2) + t;
        double h4 = Math.pow(t, 3) - Math.pow(t, 2);
        return new double[]{h1, h2, h3, h4};
    }

    /**
     * Генерация кривой Безье для 4 контрольных точек.
     * P(t) = (1-t)^3*p0 + 3t(1-t)^2*p1+3t^2(1-t)*p2+t3*p3
     */
    public List<Pixel> generateBezierCurve(List<Pixel> points) {
        List<Pixel> pixels = new ArrayList<>();

        if (points == null || points.size() != 4) {
            return pixels;
        }

        Pixel p0 = points.get(0);
        Pixel p1 = points.get(1);
        Pixel p2 = points.get(2);
        Pixel p3 = points.get(3);

        int pointsNum = Pixel.getDistance(p0, p2) + Pixel.getDistance(p0, p1) + Pixel.getDistance(p2, p3);

        for (int i = 0; i <= pointsNum; i++) {
            double t = (double) i / pointsNum;
            double x = Math.pow(1 - t, 3) * p0.getX() +
                    3 * t * Math.pow(1 - t, 2) * p1.getX() +
                    3 * Math.pow(t, 2) * (1 - t) * p2.getX() +
                    Math.pow(t, 3) * p3.getX();

            double y = Math.pow(1 - t, 3) * p0.getY() +
                    3 * t * Math.pow(1 - t, 2) * p1.getY() +
                    3 * Math.pow(t, 2) * (1 - t) * p2.getY() +
                    Math.pow(t, 3) * p3.getY();

            pixels.add(new Pixel((int) x, (int) y,  "rgba(0, 0, 0, 255)"));

        }

        return pixels;
    }

    /**
     * Генерация B‑сплайна.

     * Основная формула B‑сплайна:
          C(t) = ∑[i=0..n] Pᵢ · Nᵢ,ₖ(t)

     * где:
          Pᵢ — контрольные точки,
          Nᵢ,ₖ(t) — базисные функции B‑сплайна степени k, вычисляемые рекурсивно.
     **/

    public List<Pixel> generateBSplineCurve(List<Pixel> points) {
        List<Pixel> pixels = new ArrayList<>();

        if (points == null || points.size() < 4) {
            return pixels;
        }

        int degree = 3;                     // Степень сплайна (кубический сплайн)
        int n = points.size() - 1;          // Индекс последней контрольной точки
        int m = n + degree + 1;             // m = n + degree + 1

        double[] knots = bSplineGenerateKnots(m, degree);
        double tStart = knots[degree];
        double tEnd = knots[m - degree];

        int pointsNum =0;
        for (int i=0; i<points.size()-2; i++){
            pointsNum+=Pixel.getDistance(points.get(i), points.get(i+1));
        }

        for (int i = 0; i < pointsNum; i++) {
            double t = tStart + (tEnd - tStart) * i / (pointsNum - 1);
            double x = 0.0;
            double y = 0.0;
            for (int j = 0; j <= n; j++) {
                double b = bSplineBasisFunction(j, degree, t, knots);
                x += points.get(j).getX() * b;
                y += points.get(j).getY() * b;
            }
            pixels.add(new Pixel((int) Math.round(x), (int) Math.round(y), "rgba(0, 0, 0, 255)"));
        }

        return pixels;
    }

    private double[] bSplineGenerateKnots(int m, int degree){
        double[] knots = new double[m + 1];

        double knotMax = m - 2 * degree;
        for (int i = m - degree; i <= m; i++) {
            knots[i] = knotMax;
        }
        for (int i = degree + 1; i < m - degree; i++) {
            knots[i] = i - degree;
        }

        return knots;
    }

    /**
     * Рекурсивное определение базисных функций (алгоритм Кокса–де Бура):
     Nᵢ,₀(t) = { 1, если tᵢ ≤ t < tᵢ₊₁
     0, иначе }

     Nᵢ,ₖ(t) = (t - tᵢ) / (tᵢ₊ₖ - tᵢ) · Nᵢ,ₖ₋₁(t)
     + (tᵢ₊ₖ₊₁ - t) / (tᵢ₊ₖ₊₁ - tᵢ₊₁) · Nᵢ₊₁,ₖ₋₁(t)

     * tᵢ — узловой вектор (knots), который, как правило, определяется равномерно
     или по схеме open-uniform (например, для кубического сплайна:
     первые k+1 узлов равны 0, последние k+1 узлов равны knotMax,
     а внутренние узлы равномерно возрастают).
     */
    private double bSplineBasisFunction(int i, int k, double t, double[] knots) {
        if (k == 0) {
            // Особый случай: чтобы последняя точка была включена в кривую
            if ((knots[i] <= t && t < knots[i + 1]) ||
                    (t == knots[knots.length - 1] && i == knots.length - 2)) {
                return 1.0;
            }
            return 0.0;
        } else {
            double c1 = 0.0;
            double denominator1 = knots[i + k] - knots[i];
            if (denominator1 != 0) {
                c1 = (t - knots[i]) / denominator1 * bSplineBasisFunction(i, k - 1, t, knots);
            }
            double c2 = 0.0;
            double denominator2 = knots[i + k + 1] - knots[i + 1];
            if (denominator2 != 0) {
                c2 = (knots[i + k + 1] - t) / denominator2 * bSplineBasisFunction(i + 1, k - 1, t, knots);
            }
            return c1 + c2;
        }
    }
}