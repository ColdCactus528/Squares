package squares;

import java.util.ArrayList;
import java.util.List;

/** Простой ИИ: победный ход → блок оппонента → эвристика (центр + соседство). */
public final class Ai {

    public static int[] bestMove(GameState s, char color) {
        List<int[]> free = freeCells(s);
        if (free.isEmpty()) return null;

        // 1) выигрывающий ход
        for (int[] m : free) if (winsAfter(s, m[0], m[1], color)) return m;

        // 2) блок оппонента
        char opp = (color == 'W') ? 'B' : 'W';
        for (int[] m : free) if (winsAfter(s, m[0], m[1], opp)) return m;

        // 3) эвристика
        int bestIdx = 0, bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < free.size(); i++) {
            int[] m = free.get(i);
            int sc = heuristic(s, m[0], m[1], color);
            if (sc > bestScore) { bestScore = sc; bestIdx = i; }
        }
        return free.get(bestIdx);
    }

    private static List<int[]> freeCells(GameState s) {
        ArrayList<int[]> out = new ArrayList<>();
        for (int y = 0; y < s.n; y++) for (int x = 0; x < s.n; x++)
            if (s.board[y][x] == null) out.add(new int[]{x, y});
        return out;
    }

    private static boolean winsAfter(GameState s, int x, int y, char c) {
        if (s.board[y][x] != null) return false;
        Character[][] nb = new Character[s.n][s.n];
        for (int yy = 0; yy < s.n; yy++) System.arraycopy(s.board[yy], 0, nb[yy], 0, s.n);
        nb[y][x] = c;
        return SquareDetector.hasSquare(nb, c);
    }

    private static int heuristic(GameState s, int x, int y, char color) {
        double cx = (s.n - 1) / 2.0, cy = (s.n - 1) / 2.0;
        int centerBias = (int) ( - ((x - cx)*(x - cx) + (y - cy)*(y - cy)) );
        int near = 0;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int xx = x + dx, yy = y + dy;
            if (xx>=0 && yy>=0 && xx<s.n && yy<s.n) {
                Character c = s.board[yy][xx];
                if (c != null && c == color) near++;
            }
        }
        return centerBias + near;
    }
}
