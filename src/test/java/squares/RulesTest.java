package squares;

import org.junit.Test;
import static org.junit.Assert.*;

public class RulesTest {

    @Test
    public void moveSwitchesTurnAndBlocksBusyCell() {
        GameState s = GameState.empty(3, 'W');
        s = Rules.applyMove(s,0,0,'W'); // ok
        assertEquals('B', s.turn);

        try {
            Rules.applyMove(s,0,0,'B'); // занято
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void winForWOn2x2Square() {
        GameState s = GameState.empty(3, 'W');
        s = Rules.applyMove(s,0,0,'W');
        s = Rules.applyMove(s,2,0,'B');
        s = Rules.applyMove(s,1,0,'W');
        s = Rules.applyMove(s,2,1,'B');
        s = Rules.applyMove(s,0,1,'W');
        s = Rules.applyMove(s,2,2,'B');
        s = Rules.applyMove(s,1,1,'W'); 
        assertTrue(s.finished);
        assertEquals(Character.valueOf('W'), s.winner);
    }

    @Test
    public void drawWhenBoardIsFullWithoutSquares() {
        GameState s = GameState.empty(3, 'W');
        s = Rules.applyMove(s,0,0,'W'); 
        s = Rules.applyMove(s,1,0,'B'); 
        s = Rules.applyMove(s,2,0,'W'); 
        s = Rules.applyMove(s,0,1,'B'); 
        s = Rules.applyMove(s,2,1,'W'); 
        s = Rules.applyMove(s,0,2,'B'); 
        s = Rules.applyMove(s,1,2,'W'); 
        s = Rules.applyMove(s,2,2,'B'); 
        s = Rules.applyMove(s,1,1,'W'); 
        assertTrue(s.finished);
        assertNull(s.winner);
    }

    @Test
    public void rejectsOutOfBoundsAndWrongTurn() {
        GameState s = GameState.empty(3, 'W');
        try { Rules.applyMove(s,-1,0,'W'); fail(); } catch (IllegalArgumentException expected) {}
        try { Rules.applyMove(s,0,3,'W');  fail(); } catch (IllegalArgumentException expected) {}
        s = Rules.applyMove(s,0,0,'W');
        try { Rules.applyMove(s,1,0,'W');  fail(); } catch (IllegalArgumentException expected) {}
    }
}
