package by.lupach.drawingeditor.controllers;

import by.lupach.drawingeditor.models.*;
import by.lupach.drawingeditor.models.linesAndCurves.DrawingRequest;
import by.lupach.drawingeditor.models.polygons.PointInsideRequest;
import by.lupach.drawingeditor.models.polygons.PolygonFillRequest;
import by.lupach.drawingeditor.models.polygons.SegmentIntersectsRequest;
import by.lupach.drawingeditor.models.threeD.TransformationRequest;
import by.lupach.drawingeditor.models.threeD.TransformationResponse;
import by.lupach.drawingeditor.services.*;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/draw")
public class DrawingController {

    private final LineDrawingService lineDrawingService;
    private final CurveDrawingService curveDrawingService;
    private final TransformationService transformationService;
    private final CurveInterpolationAndApproximation interpolationService;
    private final PolygonService polygonService;
    private final VoronoiDiagramService voronoiDiagramService;
    private final SimpMessagingTemplate messagingTemplate;

    public DrawingController(LineDrawingService lineDrawingService,
                             CurveDrawingService curveDrawingService, TransformationService transformationService,
                             CurveInterpolationAndApproximation interpolationService,
                             PolygonService polygonService, VoronoiDiagramService voronoiDiagramService, // Добавлен сервис для работы с полигонами
                             SimpMessagingTemplate messagingTemplate) {
        this.lineDrawingService = lineDrawingService;
        this.curveDrawingService = curveDrawingService;
        this.transformationService = transformationService;
        this.interpolationService = interpolationService;
        this.polygonService = polygonService; // Инициализация сервиса для работы с полигонами
        this.voronoiDiagramService = voronoiDiagramService;
        this.messagingTemplate = messagingTemplate;
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


    @PostMapping("/checkConvex")
    public boolean checkConvex(@RequestBody List<Pixel> polygon) {
        return polygonService.isConvex(polygon);
    }

    @PostMapping("/convexHullGraham")
    public List<Pixel> convexHullGraham(@RequestBody List<Pixel> points) {
        return polygonService.convexHullGraham(points);
    }

    @PostMapping("/convexHullJarvis")
    public List<Pixel> convexHullJarvis(@RequestBody List<Pixel> points) {
        return polygonService.convexHullJarvis(points);
    }

    @PostMapping("/isPointInsidePolygon")
    public boolean isPointInsidePolygon(@RequestBody PointInsideRequest request) {
        return polygonService.isPointInsidePolygon(request.getPoint(), request.getPolygon());
    }

    @PostMapping("/segmentIntersectsPolygon")
    public boolean segmentIntersectsPolygon(@RequestBody SegmentIntersectsRequest request) {
        return polygonService.segmentIntersectsPolygon(request.getA(), request.getB(), request.getPolygon());
    }

    @MessageMapping("/fillPolygon")
    public void fillPolygon(@RequestBody PolygonFillRequest request) {
        List<Pixel> filledPixels = null;

        switch (request.getAlgorithm()) {
            case "scanline" ->
                    filledPixels = polygonService.scanlineFill(request.getPolygon());
            case "aet" ->
                    filledPixels = polygonService.aetFill(request.getPolygon());
            case "floodFill" ->
                    filledPixels = polygonService.floodFill(request.getSeed(), request.getPolygon(), request.getFillColor(), request.getBoundaryColor());
            case "scanlineFloodFill" ->
                    filledPixels = polygonService.scanlineFloodFill(request.getSeed(), request.getPolygon(), request.getFillColor(), request.getBoundaryColor());
            default ->
                    throw new IllegalArgumentException("Unknown fill algorithm: " + request.getAlgorithm());
        }

        messagingTemplate.convertAndSend("/topic/drawings", filledPixels);
    }

    @MessageMapping("/voronoiDiagram")
    public void delaunayTriangulator(@RequestBody List<Pixel> request) {
        List<Pixel> response = VoronoiDiagramService.buildVoronoiDiagram(request, lineDrawingService);

        messagingTemplate.convertAndSend("/topic/drawings", response);
    }
}