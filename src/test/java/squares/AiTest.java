package squares;

import org.junit.Test;
import static org.junit.Assert.*;

public class AiTest {

    @Test
    public void aiTakesWinningMove() {
        GameState s = GameState.empty(3, 'W');
        s.board[0][0]='W'; s.board[1][0]='W'; s.board[0][1]='W'; 
        int[] mv = Ai.bestMove(s, 'W');
        assertNotNull(mv);
        assertArrayEquals(new int[]{1,1}, mv);
    }

    @Test
    public void aiBlocksOpponentsImmediateWin() {
        GameState s = GameState.empty(3, 'B');
        s.board[0][0]='W'; s.board[1][0]='W'; s.board[0][1]='W';
        int[] mv = Ai.bestMove(s, 'B');
        assertNotNull(mv);
        assertArrayEquals(new int[]{1,1}, mv);
    }
}
