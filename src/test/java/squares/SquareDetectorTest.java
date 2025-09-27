package squares;

import org.junit.Test;
import static org.junit.Assert.*;

public class SquareDetectorTest {

    @Test
    public void detectsAxisAlignedSquare2x2() {
        GameState s = GameState.empty(3, 'W');
        s.board[0][0]='W'; s.board[0][1]='W';
        s.board[1][0]='W'; s.board[1][1]='W';
        assertTrue(SquareDetector.hasSquare(s.board, 'W'));
    }

    @Test
    public void detectsRotatedSquareRombOn4x4() {
        GameState s = GameState.empty(4, 'W');
        s.board[0][1]='W'; s.board[1][2]='W';
        s.board[2][1]='W'; s.board[1][0]='W';
        assertTrue(SquareDetector.hasSquare(s.board, 'W'));
    }

    @Test
    public void noFalsePositiveOnThreePoints() {
        GameState s = GameState.empty(4, 'W');
        s.board[0][0]='W'; s.board[0][1]='W'; s.board[1][0]='W';
        assertFalse(SquareDetector.hasSquare(s.board, 'W'));
    }
}
