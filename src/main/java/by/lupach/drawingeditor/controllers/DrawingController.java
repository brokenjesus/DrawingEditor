package by.lupach.drawingeditor.controllers;

import by.lupach.drawingeditor.models.*;
import by.lupach.drawingeditor.services.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/draw")
public class DrawingController {

    private final LineDrawingService lineDrawingService;
    private final CurveDrawingService curveDrawingService;
    private final TransformationService transformationService;
    private final CurveInterpolationAndApproximation interpolationService;
    private final SimpMessagingTemplate messagingTemplate; // Для отправки сообщений через WebSocket

    public DrawingController(LineDrawingService lineDrawingService,
                             CurveDrawingService curveDrawingService, TransformationService transformationService,
                             CurveInterpolationAndApproximation interpolationService,
                             SimpMessagingTemplate messagingTemplate) {
        this.lineDrawingService = lineDrawingService;
        this.curveDrawingService = curveDrawingService;
        this.transformationService = transformationService;
        this.interpolationService = interpolationService;
        this.messagingTemplate = messagingTemplate; // Инициализация SimpMessagingTemplate
    }

    @MessageMapping("/draw") // Обработка сообщений от WebSocket клиентов
    public void draw(@RequestBody DrawingRequest drawingRequest) {
        List<Pixel> pixels = null;

        // Обработка рисования линий
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

        messagingTemplate.convertAndSend("/topic/drawings", pixels);
    }

    @MessageMapping("/transform3D")
    public void handleTransformation(TransformationRequest request) {
        TransformationResponse response = transformationService.applyTransformation(request);
        messagingTemplate.convertAndSend("/topic/drawings3d", response);
    }
}