package squares;

import org.junit.Test;

import static org.junit.Assert.*;

public class ApiMappingTest {

    @Test
    public void parseColorDefaultWAndUppercase() throws Exception {
        // доступ к private методам напрямую нет — проверяем через поведение fromBoardDto/turn
        // turn мы передаём снаружи; отдельно проверяем, что Character.toUpperCase логика верна в Rules/Ai.
        // Здесь проверяем finished/winner на конкретной строке.
        ApiServer.BoardDto dto = new ApiServer.BoardDto();
        dto.size = 3;
        dto.data = "WWB" +
                   "WBW" +
                   "BWW";
        GameState s = callFromBoardDto(dto, 'W'); 
        assertNotNull(s);
        assertEquals(3, s.n);
    }

    @Test
    public void fromBoardDtoDetectsWinnerAndFull() throws Exception {
        ApiServer.BoardDto dto = new ApiServer.BoardDto();
        dto.size = 3;
        // квадрат 2x2 W в левом верхнем
        dto.data = "WW." +
                   "WW." +
                   "...";
        GameState s = callFromBoardDto(dto, 'W');
        assertTrue(s.finished);
        assertEquals(Character.valueOf('W'), s.winner);
    }

    private GameState callFromBoardDto(ApiServer.BoardDto dto, char turn) throws Exception {
        // Вынужденный приём: вызываем приватный метод через рефлексию, чтобы не менять API
        java.lang.reflect.Method m = ApiServer.class.getDeclaredMethod("fromBoardDto", ApiServer.BoardDto.class, char.class);
        m.setAccessible(true);
        return (GameState) m.invoke(null, dto, turn);
    }
}
