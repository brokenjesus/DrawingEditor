class WebSocketConnector {
    constructor(url) {
        this.url = url;
        this.socket = null;
        this.stompClient = null;
    }

    connect(subscriptions = []) {
        this.socket = new SockJS(this.url);
        this.stompClient = Stomp.over(this.socket);
        const self = this;
        this.stompClient.connect({}, function (frame) {
            console.log('Connected: ' + frame);
            subscriptions.forEach(sub => {
                self.stompClient.subscribe(sub.topic, sub.callback);
            });
        }, function (error) {
            console.error('WebSocket ошибка:', error);
        });
    }

    send(destination, message) {
        if (this.stompClient) {
            this.stompClient.send(destination, {}, JSON.stringify(message));
        } else {
            console.error("STOMP клиент не подключен.");
        }
    }
}

const drawingEditor = {
    pixels: null,
    canvas: null,
    context: null,

    socketConnection: null,
    debug: false,
    activeTab: 'lines', center: null, linePoints: [], curvePoints: [], polygonPoints: [], voronoiPoints: [],
    current3DObject: null,
    transformationMatrix: null,

    init() {
        this.canvas = document.getElementById('canvas');
        this.context = this.canvas.getContext('2d');
        this.initWebSocket();
        this.initEventListeners();
    },

    initWebSocket() {
        const subscriptions = [{
            topic: '/topic/drawings', callback: (message) => {
                const data = JSON.parse(message.body);
                console.log('Получены данные с сервера:', data);
                this.drawPixels(data);
            }
        }, {
            topic: '/topic/drawings3d', callback: (message) => {
                const response = JSON.parse(message.body);
                console.log('Получены данные с сервера (3D):', response);
                this.clearCanvas();
                this.drawPixels(response.pixels);
                console.log('Текущая матрица преобразования:', response.matrix);
                this.transformationMatrix = response.matrix;
            }
        }];
        this.socketConnection = new WebSocketConnector('/ws');
        this.socketConnection.connect(subscriptions);
    },

    initEventListeners() {
        const self = this;
        document.getElementById('debug_checkbox').addEventListener('click', function() {
            self.toggleDebug();
        });
        document.getElementById('curveType').addEventListener('change', function() {
            self.updateInputs();
        });
        this.canvas.addEventListener('click', function(e) {
            self.handleCanvasClick(e);
        });
        const transTypeElement = document.getElementById('transformationType');
        if (transTypeElement) {
            transTypeElement.addEventListener('change', function() {
                self.updateTransformInputs();
            });
        }
    },

    toggleDebug() {
        this.debug = document.getElementById("debug_checkbox").checked;
    },

    toggleActiveTab() {
        const buttons = document.querySelectorAll('.text-center button');
        buttons.forEach(button => {
            button.classList.remove('btn-primary');
            button.classList.add('btn-outline-primary');
        });
        const activeButton = Array.from(buttons).find(button => {
            const onclick = button.getAttribute('onclick');
            return onclick && onclick.includes(this.activeTab);
        });
        if (activeButton) {
            activeButton.classList.remove('btn-outline-primary');
            activeButton.classList.add('btn-primary');
        }
    },

    showTab(tabName) {
        this.activeTab = tabName;
        this.toggleActiveTab();
        document.getElementById('lines').style.display = tabName === 'lines' ? 'block' : 'none';
        document.getElementById('curves').style.display = tabName === 'curves' ? 'block' : 'none';
        document.getElementById('threeD').style.display = tabName === '3d' ? 'block' : 'none';
        document.getElementById('polygon').style.display = tabName === 'polygon' ? 'block' : 'none';
        document.getElementById('voronoi').style.display = tabName === 'voronoi' ? 'block' : 'none';
        document.getElementById('instruction').innerText = 'Кликните, чтобы выбрать точку.';
        if (tabName === '3d') {
            this.updateTransformInputs();
        }
    },

    clearCanvas() {
        this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
        // this.clearPoints();
    },

    clear(){
        this.clearCanvas();
        this.clearPoints();
    },

    clearPoints() {
        this.linePoints = [];
        this.curvePoints = [];
        this.polygonPoints = [];
        this.voronoiPoints = [];
    },

    drawPixels(pixels) {
        if (Array.isArray(pixels)) {
            this.pixels = pixels;
            pixels.forEach(pixel => {
                this.context.fillStyle = pixel.color || 'black';
                this.context.fillRect(pixel.x, pixel.y, 1, 1);
            });
        } else if (pixels.vertices && pixels.edges) {
            pixels.edges.forEach(edge => {
                const startVertex = pixels.vertices[edge.start];
                const endVertex = pixels.vertices[edge.end];
                this.context.beginPath();
                this.context.moveTo(startVertex.x, startVertex.y);
                this.context.lineTo(endVertex.x, endVertex.y);
                this.context.stroke();
            });
        }
    },

    handleCanvasClick(e) {
        const rect = this.canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        if (this.activeTab === 'lines') {
            if (this.linePoints.length === 0 || ('x2' in this.linePoints[this.linePoints.length - 1])) {
                this.linePoints.push({x1: x, y1: y});
                document.getElementById('instruction').innerText = 'Выберите конечную точку.';
            } else {
                const currentLine = this.linePoints[this.linePoints.length - 1];
                currentLine.x2 = x;
                currentLine.y2 = y;
                this.drawLine(currentLine.x1, currentLine.y1, currentLine.x2, currentLine.y2);
                document.getElementById('instruction').innerText = 'Кликните, чтобы выбрать начальную точку.';
            }
        } else if (this.activeTab === 'curves') {
            const curveType = document.getElementById("curveType").value;
            if (["circle", "ellipse", "parabola", "hyperbola"].includes(curveType)) {
                this.center = { x, y };
                document.getElementById('instruction').innerText = `Начальная точка: (${x}, ${y}). Введите параметры.`;
            } else if (["hermite", "bezier", "bspline"].includes(curveType)) {
                this.curvePoints.push({ x, y });
                this.context.fillStyle = "rgba(0, 0, 255, 0.2)";
                this.context.beginPath();
                this.context.arc(x, y, 3, 0, 2 * Math.PI);
                this.context.fill();
                document.getElementById('instruction').innerText = `Добавлено точек: ${this.curvePoints.length}`;
            }
        } else if (this.activeTab === 'polygon') {
            this.polygonPoints.push({x, y});
            this.context.fillStyle = "rgba(255, 0, 0, 0.2)";
            this.context.beginPath();
            this.context.arc(x, y, 3, 0, 2 * Math.PI);
            this.context.fill();
            document.getElementById('instruction').innerText = `Добавлено точек: ${this.polygonPoints.length}`;
        } else if (this.activeTab === 'voronoi') {
            this.voronoiPoints.push({x, y});
            this.context.fillStyle = "rgba(0, 255, 0, 0.2)";
            this.context.beginPath();
            this.context.arc(x, y, 3, 0, 2 * Math.PI);
            this.context.fill();
            document.getElementById('instruction').innerText = `Добавлено точек: ${this.voronoiPoints.length}`;
        }
    },

    drawLine(x1, y1, x2, y2) {
        const algorithm = document.getElementById("algorithm").value;
        console.log("Отправка линии:", x1, y1, x2, y2, algorithm);
        this.socketConnection.send("/app/draw", {x1, y1, x2, y2, algorithm});
    },

    drawCurve() {
        const curveType = document.getElementById("curveType").value;
        if (["circle", "ellipse", "parabola", "hyperbola"].includes(curveType)) {
            if (!this.center) {
                alert("Сначала кликните по canvas для выбора начальной точки!");
                return;
            }
            const param1 = parseFloat(document.getElementById("param1").value);
            const param2 = document.getElementById("param2") ? parseFloat(document.getElementById("param2").value) : null;
            this.socketConnection.send("/app/draw", {curveType, center: this.center, param1, param2});
        } else if (["hermite", "bezier", "bspline"].includes(curveType)) {
            if (this.curvePoints.length === 0) {
                alert("Добавьте хотя бы одну точку!");
                return;
            }
            this.socketConnection.send("/app/draw", {curveType, points: this.curvePoints});
            this.curvePoints = [];
        }
    },

    checkConvex() {
        if (this.polygonPoints.length < 3) {
            alert("Добавьте хотя бы 3 точки для полигона!");
            return;
        }
        fetch('/draw/checkConvex', {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(this.polygonPoints)
        })
            .then(response => response.json())
            .then(data => {
                alert(data ? 'Полигон выпуклый' : 'Полигон не выпуклый');
            });
    },

    convexHullGraham() {
        if (this.polygonPoints.length < 3) {
            alert("Добавьте хотя бы 3 точки!");
            return;
        }
        fetch('/draw/convexHullGraham', {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(this.polygonPoints)
        })
            .then(response => response.json())
            .then(data => {
                this.polygonPoints = data;
                this.clearCanvas();
                for (let i=-1; i<data.length-1; i++){
                    this.drawLine(data.at(i).x, data.at(i).y, data.at(i+1).x, data.at(i+1).y)
                }
                // this.drawPixels(data);
            });
    },

    generateVoronoiDiagram() {
        if (this.voronoiPoints.length < 1) {
            alert("Добавьте хотя бы 1 точку!");
            return;
        }

        this.socketConnection.send("/app/voronoiDiagram", this.voronoiPoints);
    },

    convexHullJarvis() {
        if (this.polygonPoints.length < 3) {
            alert("Добавьте хотя бы 3 точки!");
            return;
        }
        fetch('/draw/convexHullJarvis', {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(this.polygonPoints)
        })
            .then(response => response.json())
            .then(data => {
                this.polygonPoints = data;
                this.clearCanvas();
                for (let i=-1; i<data.length-1; i++){
                    this.drawLine(data.at(i).x, data.at(i).y, data.at(i+1).x, data.at(i+1).y)
                }
                // this.drawPixels(data);
            });
    },

    checkPointInside() {
        if (!this.polygonPoints) {
            alert("Сначала кликните по canvas для выбора точки!");
            return;
        }
        const pointToCheck = this.polygonPoints[this.polygonPoints.length - 1];
        this.polygonPoints.splice(this.polygonPoints.indexOf(pointToCheck), 1);
        fetch('/draw/isPointInsidePolygon', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({point: pointToCheck, polygon: this.polygonPoints})
        })
            .then(response => response.json())
            .then(data => {
                alert(data ? 'Точка внутри полигона' : 'Точка снаружи полигона');
            });
    },

    checkSegmentIntersects() {
        if (this.polygonPoints.length < 3) {
            alert("Добавьте хотя бы 3 точки для полигона!");
            return;
        }
        if (!this.linePoints.length) {
            alert("Сначала нарисуйте отрезок!");
            return;
        }

        let lastLine = this.linePoints.at(-1);

        fetch('/draw/segmentIntersectsPolygon', {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({
                a: {x: lastLine.x1, y: lastLine.y1}, b: {x: lastLine.x2, y: lastLine.y2}, polygon: this.polygonPoints
            })
        })
            .then(response => response.json())
            .then(data => {
                alert(data ? 'Отрезок пересекает полигон' : 'Отрезок не пересекает полигон');
            });
    },

    fillPolygon() {
        if (this.polygonPoints.length < 3) {
            alert("Добавьте хотя бы 3 точки для полигона!");
            return;
        }

        const algorithm = document.getElementById("fillAlgorithm").value;
        const seed = this.polygonPoints[0]; // Затравка (начальная точка)
        const fillColor = "rgba(255, 20, 147, 0.2)"; // Цвет заполнения
        const boundaryColor = "rgba(0, 0, 0, 1)"; // Цвет границы

        const request = {
            polygon: this.polygonPoints,
            algorithm: algorithm,
            seed: seed,
            fillColor: fillColor,
            boundaryColor: boundaryColor
        };

        this.socketConnection.send("/app/fillPolygon", request);
    },
    load3DObject() {
        const fileInput = document.getElementById('objFileInput');
        if (fileInput.files.length === 0) {
            alert("Выберите файл для загрузки!");
            return;
        }
        const file = fileInput.files[0];
        const reader = new FileReader();
        reader.onload = (event) => {
            const fileContent = event.target.result;
            const objectData = this.parseOBJFile(fileContent);
            this.current3DObject = objectData;
            alert(`Загружено вершин: ${objectData.vertices.length}`);
            const request = {
                transformationType: "none",
                vertices: this.transformVerticesForServer(objectData.vertices),
                edges: this.transformEdgesForServer(objectData.edges)
            };
            this.socketConnection.send("/app/transform3D", request);
        };
        reader.readAsText(file);
    },

    parseOBJFile(content) {
        const vertices = [];
        const edges = [];
        const lines = content.split("\n");
        lines.forEach(line => {
            if (line.startsWith("v ")) {
                const parts = line.split(" ").filter(p => p.trim() !== "");
                const x = parseFloat(parts[1]);
                const y = parseFloat(parts[2]);
                const z = parseFloat(parts[3]);
                vertices.push([x, y, z, 1]);
            } else if (line.startsWith("f ")) {
                const parts = line.split(" ").filter(p => p.trim() !== "");
                const faceIndices = parts.slice(1).map(part => parseInt(part.split('/')[0], 10) - 1);
                for (let i = 0; i < faceIndices.length; i++) {
                    const startIndex = faceIndices[i];
                    const endIndex = faceIndices[(i + 1) % faceIndices.length];
                    edges.push([startIndex, endIndex]);
                }
            }
        });
        return { vertices, edges };
    },

    transform3DControl(type, params) {
        if (!this.current3DObject) {
            alert("Сначала загрузите 3D объект!");
            return;
        }
        const request = {
            transformationType: type,
            vertices: this.current3DObject.vertices,
            edges: this.current3DObject.edges,
            matrix: this.transformationMatrix
        };
        Object.assign(request, params);
        this.socketConnection.send("/app/transform3D", request);
    },

    updateInputs() {
        const curveType = document.getElementById("curveType").value;
        const paramsDiv = document.getElementById("params");
        if (curveType === "circle") {
            paramsDiv.innerHTML = `<label>Радиус (r):</label>
                <input id="param1" type="number" class="form-control w-auto d-inline-block" />`;
            this.center = null;
        } else if (curveType === "ellipse" || curveType === "hyperbola") {
            paramsDiv.innerHTML = `
                <label>a:</label> <input id="param1" type="number" class="form-control w-auto d-inline-block" />
                <label>b:</label> <input id="param2" type="number" class="form-control w-auto d-inline-block" />`;
            this.center = null;
        } else if (curveType === "parabola") {
            paramsDiv.innerHTML = `<label>Фокус (a):</label>
                <input id="param1" type="number" class="form-control w-auto d-inline-block" />`;
            this.center = null;
        } else if (["hermite", "bezier", "bspline"].includes(curveType)) {
            paramsDiv.innerHTML = `<p>Кликните по canvas для добавления точек.<br>
                Для Hermite и Bezier: 4 точки.<br>
                Для BSpline: минимум 4 точки.</p>`;
            this.curvePoints = [];
        }
    },
};

document.addEventListener('DOMContentLoaded', function() {
    drawingEditor.init();
});

document.addEventListener('keydown', function(event) {
    const angle = 5;
    const scaleFactor = 1.1;
    const moveStep = 0.1;
    const perspectiveStep = 10;

    switch (event.code) {
        case 'ArrowUp':
        case 'ArrowDown':
        case 'ArrowLeft':
        case 'ArrowRight':
            event.preventDefault();
            break;
    }

    switch (event.code) {
        case 'NumpadAdd':
        case 'Equal':
            drawingEditor.transform3DControl('scaling', { sx: scaleFactor, sy: scaleFactor, sz: scaleFactor });
            break;
        case 'NumpadSubtract':
        case 'Minus':
            drawingEditor.transform3DControl('scaling', { sx: 1 / scaleFactor, sy: 1 / scaleFactor, sz: 1 / scaleFactor });
            break;
        case 'ArrowUp':
            drawingEditor.transform3DControl('rotationX', { angle: angle });
            break;
        case 'ArrowDown':
            drawingEditor.transform3DControl('rotationX', { angle: -angle });
            break;
        case 'ArrowLeft':
            drawingEditor.transform3DControl('rotationY', { angle: angle });
            break;
        case 'ArrowRight':
            drawingEditor.transform3DControl('rotationY', { angle: -angle });
            break;
        case 'KeyW':
            drawingEditor.transform3DControl('translation', { x: 0, y: moveStep, z: 0 });
            break;
        case 'KeyS':
            drawingEditor.transform3DControl('translation', { x: 0, y: -moveStep, z: 0 });
            break;
        case 'KeyA':
            drawingEditor.transform3DControl('translation', { x: -moveStep, y: 0, z: 0 });
            break;
        case 'KeyD':
            drawingEditor.transform3DControl('translation', { x: moveStep, y: 0, z: 0 });
            break;
        case 'KeyQ':
            drawingEditor.transform3DControl('translation', { x: 0, y: 0, z: moveStep });
            break;
        case 'KeyE':
            drawingEditor.transform3DControl('translation', { x: 0, y: 0, z: -moveStep });
            break;
        case 'KeyP':
            drawingEditor.transform3DControl('perspective', { d: perspectiveStep });
            break;
        case 'KeyO':
            drawingEditor.transform3DControl('perspective', { d: -perspectiveStep });
            break;
    }
});