package squares;

import java.util.Arrays;

public final class GameState {
    public final int n;
    public final Character[][] board; // null | 'W' | 'B'
    public final char turn;           // 'W' or 'B'
    public final boolean finished;
    public final Character winner;    // null | 'W' | 'B'

    public GameState(int n, Character[][] board, char turn, boolean finished, Character winner) {
        this.n = n;
        this.board = board;
        this.turn = turn;
        this.finished = finished;
        this.winner = winner;
    }

    public static GameState empty(int n, char start) {
        Character[][] b = new Character[n][n];
        for (int y = 0; y < n; y++) Arrays.fill(b[y], null);
        return new GameState(n, b, start, false, null);
    }
}
