package by.lupach.drawingeditor.controllers;

import by.lupach.drawingeditor.models.DrawingRequest;
import by.lupach.drawingeditor.models.Pixel;
import by.lupach.drawingeditor.services.CurveDrawingService;
import by.lupach.drawingeditor.services.CurveInterpolationAndApproximation;
import by.lupach.drawingeditor.services.LineDrawingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/draw")
public class DrawingController {

    private final LineDrawingService lineDrawingService;
    private final CurveDrawingService curveDrawingService;
    private final CurveInterpolationAndApproximation interpolationService;
    private final SimpMessagingTemplate messagingTemplate; // For sending messages to WebSocket

    public DrawingController(LineDrawingService lineDrawingService,
                             CurveDrawingService curveDrawingService,
                             CurveInterpolationAndApproximation interpolationService,
                             SimpMessagingTemplate messagingTemplate) {
        this.lineDrawingService = lineDrawingService;
        this.curveDrawingService = curveDrawingService;
        this.interpolationService = interpolationService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/draw") // Слушаем сообщения от WebSocket клиентов
    public void draw(@RequestBody DrawingRequest drawingRequest) {
        List<Pixel> pixels = null;

        // Обработка рисования линий (алгоритмы DDA, Bresenham, Wu)
        if (drawingRequest.getAlgorithm() != null) {
            pixels = switch (drawingRequest.getAlgorithm()) {
                case "dda" -> lineDrawingService.generateDDALine(
                        drawingRequest.getX1(), drawingRequest.getY1(),
                        drawingRequest.getX2(), drawingRequest.getY2());
                case "bresenham" -> lineDrawingService.generateBresenhamLine(
                        drawingRequest.getX1(), drawingRequest.getY1(),
                        drawingRequest.getX2(), drawingRequest.getY2());
                case "wu" -> lineDrawingService.generateWuLine(
                        drawingRequest.getX1(), drawingRequest.getY1(),
                        drawingRequest.getX2(), drawingRequest.getY2());
                default -> throw new IllegalArgumentException("Unknown line algorithm: " + drawingRequest.getAlgorithm());
            };
        }
        // Обработка рисования кривых
        else if (drawingRequest.getCurveType() != null) {
            switch (drawingRequest.getCurveType()) {
                case "circle" ->
                        pixels = curveDrawingService.generateCircle(
                                (int) drawingRequest.getCenter().getX(),
                                (int) drawingRequest.getCenter().getY(),
                                drawingRequest.getParam1());
                case "ellipse" ->
                        pixels = curveDrawingService.generateEllipse(
                                (int) drawingRequest.getCenter().getX(),
                                (int) drawingRequest.getCenter().getY(),
                                drawingRequest.getParam1(),
                                drawingRequest.getParam2());
                case "parabola" ->
                        pixels = curveDrawingService.generateParabola(
                                (int) drawingRequest.getCenter().getX(),
                                (int) drawingRequest.getCenter().getY(),
                                drawingRequest.getParam1());
                case "hyperbola" ->
                        pixels = curveDrawingService.generateHyperbola(
                                (int) drawingRequest.getCenter().getX(),
                                (int) drawingRequest.getCenter().getY(),
                                drawingRequest.getParam1(),
                                drawingRequest.getParam2());
                // Новые алгоритмы интерполяции/аппроксимации
                case "hermite" ->
                        pixels = interpolationService.generateHermiteCurve(drawingRequest.getPoints());
                case "bezier" ->
                        pixels = interpolationService.generateBezierCurve(drawingRequest.getPoints());
                case "bspline" ->
                        pixels = interpolationService.generateBSplineCurve(drawingRequest.getPoints());
                default ->
                        throw new IllegalArgumentException("Unknown curve type: " + drawingRequest.getCurveType());
            }
        }

        messagingTemplate.convertAndSend("/topic/drawings", pixels); // Отправляем пиксели на топик
    }


    @GetMapping("/line/dda")
    public List<Pixel> drawDDALine(@RequestParam int x1, @RequestParam int y1, @RequestParam int x2, @RequestParam int y2) {
        return lineDrawingService.generateDDALine(x1, y1, x2, y2);
    }

    @GetMapping("/line/bresenham")
    public List<Pixel> drawBresenhamLine(@RequestParam int x1, @RequestParam int y1, @RequestParam int x2, @RequestParam int y2) {
        return lineDrawingService.generateBresenhamLine(x1, y1, x2, y2);
    }

    @GetMapping("/line/wu")
    public List<Pixel> drawWuLine(@RequestParam int x1, @RequestParam int y1, @RequestParam int x2, @RequestParam int y2) {
        return lineDrawingService.generateWuLine(x1, y1, x2, y2);
    }

    @GetMapping("/curve/circle")
    public List<Pixel> drawCircle(@RequestParam int x, @RequestParam int y, @RequestParam double r) {
        return curveDrawingService.generateCircle(x, y, r);
    }

    @GetMapping("/curve/ellipse")
    public List<Pixel> drawEllipse(@RequestParam int x, @RequestParam int y, @RequestParam double a, @RequestParam double b) {
        return curveDrawingService.generateEllipse(x, y, a, b);
    }

    @GetMapping("/curve/parabola")
    public List<Pixel> drawParabola(@RequestParam int x, @RequestParam int y, @RequestParam double a) {
        return curveDrawingService.generateParabola(x, y, a);
    }

    @GetMapping("/curve/hyperbola")
    public List<Pixel> drawHyperbola(@RequestParam int x, @RequestParam int y, @RequestParam double a, @RequestParam double b) {
        return curveDrawingService.generateHyperbola(x, y, a, b);
    }
}
