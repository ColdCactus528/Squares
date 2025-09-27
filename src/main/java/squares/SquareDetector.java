package squares;

import java.util.ArrayList;
import java.util.List;

/** Обнаружение квадрата произвольной ориентации: берём пару точек как сторону,
 *  поворачиваем вектор на 90° и проверяем оставшиеся углы. */
public final class SquareDetector {

    static final class P { final int x,y; P(int x,int y){this.x=x; this.y=y;} }

    public static boolean hasSquare(Character[][] board, char color) {
        final int n = board.length;
        // собрать все клетки данного цвета
        List<P> pts = new ArrayList<>();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (board[y][x] != null && board[y][x] == color) {
                    pts.add(new P(x, y));
                }
            }
        }
        if (pts.size() < 4) return false;

        for (int i = 0; i < pts.size(); i++) {
            P a = pts.get(i);
            for (int j = i + 1; j < pts.size(); j++) {
                P b = pts.get(j);
                int vx = b.x - a.x, vy = b.y - a.y;
                if (vx == 0 && vy == 0) continue;

                // поворот вектора AB на 90°
                int rx = -vy, ry = vx;

                // вариант +r
                P p3 = new P(a.x + rx, a.y + ry);
                P p4 = new P(b.x + rx, b.y + ry);
                if (exists(board, color, p3) && exists(board, color, p4)) return true;

                // вариант -r
                P p3b = new P(a.x - rx, a.y - ry);
                P p4b = new P(b.x - rx, b.y - ry);
                if (exists(board, color, p3b) && exists(board, color, p4b)) return true;
            }
        }
        return false;
    }

    private static boolean exists(Character[][] b, char color, P p) {
        int n = b.length;
        return p.x >= 0 && p.y >= 0 && p.x < n && p.y < n
                && b[p.y][p.x] != null && b[p.y][p.x] == color;
    }
}
