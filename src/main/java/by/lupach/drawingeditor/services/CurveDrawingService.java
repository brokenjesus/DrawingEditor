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


    public List<Pixel> generateParabola(int x, int y, double a) {
        List<Pixel> pixels = new ArrayList<>();
        List<Pixel> parabolaPoints = new ArrayList<>();

        // Вычисление максимального значения x, чтобы парабола не выходила за пределы высоты 720 пикселей
        int maxX = (int) Math.sqrt(720 / Math.abs(a));  // Максимальное значение x для параболы, чтобы y не превышало 720

        // Генерация точек параболы для всего диапазона
        for (int i = -maxX; i < maxX+1; i++) {
            int yPos = (int) (a * i * i);
            parabolaPoints.add(new Pixel(x + i, y + yPos, "rgba(0, 0, 0, 255)"));
        }

        // Генерация линий между точками параболы
        for (int i = 0; i < parabolaPoints.size() - 1; i++) {
            int distance = (int) Math.sqrt(Math.pow(parabolaPoints.get(i + 1).getX() - parabolaPoints.get(i).getX(), 2) + Math.pow(parabolaPoints.get(i + 1).getY() - parabolaPoints.get(i).getY(), 2));
            if (distance > 1) {
                pixels.addAll(lineDrawingService.generateDDALine(parabolaPoints.get(i + 1).getX(), parabolaPoints.get(i + 1).getY(), parabolaPoints.get(i).getX(), parabolaPoints.get(i).getY()));
            } else {
                pixels.add(parabolaPoints.get(i));
            }
        }

        return pixels;
    }


    public List<Pixel> generateHyperbola(int x, int y, double a, double b) {
        List<Pixel> pixels = new ArrayList<>();
        List<Pixel> positiveXBranch = new ArrayList<>();
        List<Pixel> negativeXBranch = new ArrayList<>();


        // Вычисление максимального значения x, чтобы гипербола не выходила за пределы экрана
        int maxX = (int) (a * Math.sqrt(1 + Math.pow(720 / b, 2))); // Максимальное значение x для гиперболы, чтобы y не превышало 720

        for (float i = 0; i < maxX; i+=0.2) {
            if (Math.abs(i) > a) {
                double yPos1 = b * Math.sqrt((i * i) / (a * a) - 1); // positiveY(top) sub-branch
                double yPos2 = -yPos1; //  negativeY(bottom) sub-branch
                //positiveX(right) branch
                positiveXBranch.add(new Pixel((int) (x + i), y + (int) yPos1, "rgba(0, 0, 0, 255)"));
                positiveXBranch.add(new Pixel((int) (x + i), y + (int) yPos2, "rgba(0, 0, 0, 255)"));

                //negativeX(left) branch
                negativeXBranch.add(new Pixel((int) (x - i), y + (int) yPos1, "rgba(0, 0, 0, 255)"));
                negativeXBranch.add(new Pixel((int) (x - i), y + (int) yPos2, "rgba(0, 0, 0, 255)"));
            }
        }

        for (int i = 0; i < positiveXBranch.size() - 1; i++) {
            int distance = (int) Math.sqrt(Math.pow(positiveXBranch.get(i + 1).getX() - positiveXBranch.get(i).getX(), 2) + Math.pow(positiveXBranch.get(i + 1).getY() - positiveXBranch.get(i).getY(), 2));
            if (distance > 1) {
//                pixels.addAll(lineDrawingService.generateDDALine(positiveXBranch.get(i + 1).getX(), positiveXBranch.get(i + 1).getY(), positiveXBranch.get(i).getX(), positiveXBranch.get(i).getY()));
//                pixels.addAll(lineDrawingService.generateDDALine(negativeXBranch.get(i + 1).getX(), negativeXBranch.get(i + 1).getY(), negativeXBranch.get(i).getX(), negativeXBranch.get(i).getY()));
                pixels.add(positiveXBranch.get(i));
                pixels.add(negativeXBranch.get(i));
            } else {
                pixels.add(positiveXBranch.get(i));
                pixels.add(negativeXBranch.get(i));
            }
        }

//        // Генерация точек гиперболы для отрицательных x
//        for (int i = 0; i < maxX; i++) { // Только для отрицательных x
//            if (Math.abs(i) > a) {
//                double yPos1 = b * Math.sqrt((i * i) / (a * a) - 1); // Положительная ветвь
//                double yPos2 = -yPos1; // Отрицательная ветвь
//                pixels.add(new Pixel(x - i, y + (int) yPos1, "rgba(0, 0, 0, 255)"));
//                pixels.add(new Pixel(x - i, y + (int) yPos2, "rgba(0, 0, 0, 255)"));
//            }
//        }

        return pixels;
    }

}
