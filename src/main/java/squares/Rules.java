package squares;

public final class Rules {

    /** Проверка границ. */
    public static boolean inBounds(GameState s, int x, int y) {
        return x >= 0 && y >= 0 && x < s.n && y < s.n;
    }

    /**
     * Применяет ход: ставит фишку текущего игрока в (x,y), проверяет контекст,
     * меняет очередь. Пока без проверки победы/ничьей — добавим позже.
     *
     * @throws IllegalArgumentException при нарушении правил/контекста
     */
    public static GameState applyMove(GameState s, int x, int y, char color) {
        if (s == null) throw new IllegalArgumentException("No game");
        if (s.finished) throw new IllegalArgumentException("Game finished");
        if (color != s.turn) throw new IllegalArgumentException("Not this color turn");
        if (!inBounds(s, x, y)) throw new IllegalArgumentException("Out of bounds");
        if (s.board[y][x] != null) throw new IllegalArgumentException("Cell busy");

        Character[][] nb = new Character[s.n][s.n];
        for (int yy = 0; yy < s.n; yy++) {
            System.arraycopy(s.board[yy], 0, nb[yy], 0, s.n);
        }
        nb[y][x] = color;

        char nextTurn = (s.turn == 'W') ? 'B' : 'W';
        return new GameState(s.n, nb, nextTurn, false, null);
    }
}
