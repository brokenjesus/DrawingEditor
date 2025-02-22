package by.lupach.drawingeditor.models.threeD;

public class Matrix4f {

    public static double[][] identity() {
        return new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] translation(double x, double y, double z) {
        double[][] m = identity();
        m[0][3] = x;
        m[1][3] = y;
        m[2][3] = z;
        return m;
    }

    public static double[][] scaling(double sx, double sy, double sz) {
        return new double[][]{
                {sx, 0, 0, 0},
                {0, sy, 0, 0},
                {0, 0, sz, 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] rotationX(double angle) {
        double rad = Math.toRadians(angle);
        return new double[][]{
                {1, 0, 0, 0},
                {0, Math.cos(rad), -Math.sin(rad), 0},
                {0, Math.sin(rad), Math.cos(rad), 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] rotationY(double angle) {
        double rad = Math.toRadians(angle);
        return new double[][]{
                {Math.cos(rad), 0, Math.sin(rad), 0},
                {0, 1, 0, 0},
                {-Math.sin(rad), 0, Math.cos(rad), 0},
                {0, 0, 0, 1}
        };
    }

    public static double[][] perspective(double d) {
        return new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, -1 / d, 1}
        };
    }


    // Метод умножения матрицы на вектор
    public static double[] multiply(double[][] m, double[] v) {
        double[] r = new double[4];
        for (int i = 0; i < 4; i++) {
            r[i] = 0;
            for (int j = 0; j < 4; j++) {
                r[i] += m[i][j] * v[j];
            }
        }
        return r;
    }

    // Метод умножения двух матриц
    public static double[][] multiply(double[][] a, double[][] b) {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i][j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }
}
