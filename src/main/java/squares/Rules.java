package squares;

public final class Rules {

    public static boolean inBounds(GameState s, int x, int y) {
        return x >= 0 && y >= 0 && x < s.n && y < s.n;
    }

    /** Применяет ход: валидации, смена хода, проверка победы/ничьей. */
    public static GameState applyMove(GameState s, int x, int y, char color) {
        if (s == null) throw new IllegalArgumentException("No game");
        if (s.finished) throw new IllegalArgumentException("Game finished");
        if (color != s.turn) throw new IllegalArgumentException("Not this color turn");
        if (!inBounds(s, x, y)) throw new IllegalArgumentException("Out of bounds");
        if (s.board[y][x] != null) throw new IllegalArgumentException("Cell busy");

        // копия доски + установка фишки
        Character[][] nb = new Character[s.n][s.n];
        for (int yy = 0; yy < s.n; yy++) System.arraycopy(s.board[yy], 0, nb[yy], 0, s.n);
        nb[y][x] = color;

        // проверка победы текущего игрока (после его хода)
        if (SquareDetector.hasSquare(nb, color)) {
            return new GameState(s.n, nb, s.turn, true, color);
        }

        // проверка ничьей (поле заполнено)
        if (boardFull(nb)) {
            return new GameState(s.n, nb, s.turn, true, null);
        }

        // иначе — продолжаем игру, меняем очередь
        char nextTurn = (s.turn == 'W') ? 'B' : 'W';
        return new GameState(s.n, nb, nextTurn, false, null);
    }

    private static boolean boardFull(Character[][] b) {
        int n = b.length;
        for (int yy = 0; yy < n; yy++) for (int xx = 0; xx < n; xx++)
            if (b[yy][xx] == null) return false;
        return true;
    }
}
