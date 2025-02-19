const drawingEditor = {

    // Основные свойства редактора
    canvas: null,
    context: null,
    socket: null,
    stompClient: null,
    debug: false,
    activeTab: 'lines',
    start: null,       // Для линий – начальная точка
    center: null,      // Для кривых с центром
    curvePoints: [],   // Для кривых-интерполяций
    // Для 3D‑объекта – объект вида { vertices: [...], edges: [...] }
    current3DObject: null,
    transformationMatrix: null,

    // Инициализация редактора
    init() {
        this.canvas = document.getElementById('canvas');
        this.context = this.canvas.getContext('2d');
        this.initWebSocket();
        this.initEventListeners();
    },

    // Настройка подключения через SockJS/STOMP
    initWebSocket() {
        this.socket = new SockJS('/ws');
        this.stompClient = Stomp.over(this.socket);
        const self = this;
        this.stompClient.connect({}, function(frame) {
            console.log('Connected: ' + frame);
            self.stompClient.subscribe('/topic/drawings', function(message) {
                const data = JSON.parse(message.body);
                console.log('Получены данные с сервера:', data);
                self.drawPixels(data);
            });
            self.stompClient.subscribe('/topic/drawings3d', function(message) {
                const response = JSON.parse(message.body);
                console.log('Получены данные с сервера (3D):', response);
                self.drawPixels(response.pixels);
                console.log('Текущая матрица преобразования:', response.matrix);
                self.transformationMatrix = response.matrix; // Обновляем матрицу
            });
        }, function(error) {
            console.error('WebSocket ошибка:', error);
        });
    },


    // Привязка обработчиков событий
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

    // Переключение debug-режима
    toggleDebug() {
        this.debug = document.getElementById("debug_checkbox").checked;
    },

    // Обновление внешнего вида кнопок переключения вкладок
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

    // Переключение вкладок (линии, кривые, 3D)
    showTab(tabName) {
        this.activeTab = tabName;
        this.toggleActiveTab();
        document.getElementById('lines').style.display = tabName === 'lines' ? 'block' : 'none';
        document.getElementById('curves').style.display = tabName === 'curves' ? 'block' : 'none';
        document.getElementById('threeD').style.display = tabName === '3d' ? 'block' : 'none';
        // Сброс состояния для всех вкладок
        this.start = null;
        this.center = null;
        this.curvePoints = [];
        this.clearCanvas();
        document.getElementById('instruction').innerText = 'Кликните, чтобы выбрать точку.';
        if (tabName === '3d') {
            this.updateTransformInputs();
        }
    },

    // Очистка холста
    clearCanvas() {
        this.context.clearRect(0, 0, this.canvas.width, this.canvas.height);
    },

    // Отрисовка пикселей (предполагается, что сервер возвращает массив объектов {x, y, color})
    drawPixels(pixels) {
        this.clearCanvas();
        // Если получен массив пикселей – рисуем их как точки (1x1)
        if (Array.isArray(pixels)) {
            pixels.forEach(pixel => {
                this.context.fillStyle = pixel.color;
                this.context.fillRect(pixel.x, pixel.y, 1, 1);
            });
        } else if (pixels.vertices && pixels.edges) {
            // Альтернативный вариант: если вернулись вершины и ребра (например, для 2D‑объекта)
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

    // Обработка кликов по canvas для линий или кривых
    handleCanvasClick(e) {
        const rect = this.canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        if (this.activeTab === 'lines') {
            if (!this.start) {
                this.start = { x, y };
                document.getElementById('instruction').innerText = 'Выберите конечную точку.';
            } else {
                this.drawLine(this.start.x, this.start.y, x, y);
                this.start = null;
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
        }
    },

    // Отправка данных для отрисовки линии (для 2D‑линий)
    drawLine(x1, y1, x2, y2) {
        const algorithm = document.getElementById("algorithm").value;
        console.log("Отправка линии:", x1, y1, x2, y2, algorithm);
        this.stompClient.send("/app/draw", {}, JSON.stringify({ x1, y1, x2, y2, algorithm }));
    },

    // Отправка данных для отрисовки кривой
    drawCurve() {
        const curveType = document.getElementById("curveType").value;
        if (["circle", "ellipse", "parabola", "hyperbola"].includes(curveType)) {
            if (!this.center) {
                alert("Сначала кликните по canvas для выбора начальной точки!");
                return;
            }
            const param1 = parseFloat(document.getElementById("param1").value);
            const param2 = document.getElementById("param2") ? parseFloat(document.getElementById("param2").value) : null;
            this.stompClient.send("/app/draw", {},
                JSON.stringify({ curveType, center: this.center, param1, param2 }));
        } else if (["hermite", "bezier", "bspline"].includes(curveType)) {
            if (this.curvePoints.length === 0) {
                alert("Добавьте хотя бы одну точку!");
                return;
            }
            this.stompClient.send("/app/draw", {},
                JSON.stringify({ curveType, points: this.curvePoints }));
            this.curvePoints = [];
        }
    },

    // Функция для загрузки 3D‑объекта (данные из файла передаются с клиента)
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
            // Разбор OBJ-файла: извлекаем вершины и рёбра
            const objectData = this.parseOBJFile(fileContent);
            this.current3DObject = objectData; // Объект с { vertices, edges }
            alert(`Загружено вершин: ${objectData.vertices.length}`);
            // Отправляем объект на сервер для первичной отрисовки (без трансформации)
            const request = {
                transformationType: "none",
                vertices: this.transformVerticesForServer(objectData.vertices),
                edges: this.transformEdgesForServer(objectData.edges)
            };
            this.stompClient.send("/app/transform3D", {}, JSON.stringify(request));
        };
        reader.readAsText(file);
    },

    // Функция парсинга OBJ‑файла: возвращает объект { vertices, edges }
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
                // Приводим к формату [x, y, z, 1]
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
            vertices: this.transformVerticesForServer(this.current3DObject.vertices),
            edges: this.transformEdgesForServer(this.current3DObject.edges),
            matrix: this.transformationMatrix,  // Включаем текущую матрицу в запрос
        };
        Object.assign(request, params);
        this.stompClient.send("/app/transform3D", {}, JSON.stringify(request));
    },

    // Преобразование массива вершин для передачи на сервер
    transformVerticesForServer(vertices) {
        return vertices;
    },

    // Преобразование ребер для передачи на сервер
    transformEdgesForServer(edges) {
        return edges;
    },

    // Обновление матрицы после трансформации (можно вызывать после получения ответа от сервера)
    updateTransformationMatrix(newMatrix) {
        this.transformationMatrix = newMatrix;
    },
    // Обновление полей ввода для параметров кривых
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

    // Обновление полей для трансформации (если требуется)
    updateTransformInputs() {
        // Здесь можно дополнять блок управления 3D-параметрами, если хотите динамически менять поля ввода.
        // Сейчас для управления используются кнопки, вызывающие transform3DControl с фиксированными параметрами.
    }
};

// Инициализация редактора после загрузки DOM
document.addEventListener('DOMContentLoaded', function() {
    drawingEditor.init();
});


document.addEventListener('keydown', function(event) {
    const angle = 5;
    const scaleFactor = 1.1;
    const moveStep = 0.1;
    const perspectiveStep = 10;

    switch (event.code) {
        // Блокируем прокручивание
        case 'ArrowUp':
        case 'ArrowDown':
        case 'ArrowLeft':
        case 'ArrowRight':
            event.preventDefault();
            break;
    }

    switch (event.code) {
        // Масштабирование
        case 'NumpadAdd':
        case 'Equal':
            drawingEditor.transform3DControl('scaling', { sx: scaleFactor, sy: scaleFactor, sz: scaleFactor });
            break;
        case 'NumpadSubtract':
        case 'Minus':
            drawingEditor.transform3DControl('scaling', { sx: 1 / scaleFactor, sy: 1 / scaleFactor, sz: 1 / scaleFactor });
            break;

        // Вращение
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

        // Перемещение
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

        // Перспектива
        case 'KeyP':
            drawingEditor.transform3DControl('perspective', { d: perspectiveStep });
            break;
        case 'KeyO':
            drawingEditor.transform3DControl('perspective', { d: -perspectiveStep });
            break;
    }
});