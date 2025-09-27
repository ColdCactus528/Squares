package squares;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * REST по их Swagger:
 *  - GET  /health                       -> 200 OK + "OK"
 *  - POST /api/{rules}/nextMove         -> 200 {"x","y","color"} | 204 No Content | 400 Bad Request
 * Использует общий движок из Task 1 (Ai / SquareDetector / Rules).
 */
public final class ApiServer {

    /* ===== DTO из их схемы ===== */
    static final class BoardDto { int size; String data; String nextPlayerColor; }
    static final class SimpleMoveDto { int x, y; String color; SimpleMoveDto(int x,int y,String c){ this.x=x; this.y=y; this.color=c; } }

    public static void main(String[] args) throws IOException {
        final int port = 3000;

        // Привязываемся к IPv4 loopback (чтобы не уезжать на ::1)
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        Gson gson = new Gson();

        // ---- /health --------------------------------------------------------
        server.createContext("/health", ex -> {
            System.out.println("[/health] " + ex.getRequestMethod() + " " + ex.getRequestURI());
            try {
                addCors(ex.getResponseHeaders());
                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
                byte[] ok = "OK".getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, ok.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(ok); }
            } catch (Exception e) {
                e.printStackTrace();
                ex.sendResponseHeaders(500, -1);
            } finally {
                ex.close();
            }
        });

        // ---- /api/{rules}/nextMove -----------------------------------------
        server.createContext("/api", ex -> {
            System.out.println("[/api] " + ex.getRequestMethod() + " " + ex.getRequestURI());
            try {
                addCors(ex.getResponseHeaders());
                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }

                String path = ex.getRequestURI().getPath(); // /api/<rules>/nextMove
                String[] seg = path.split("/");
                boolean okPath = seg.length == 4 && "api".equals(seg[1]) && "nextMove".equals(seg[3]);
                if (!okPath) { ex.sendResponseHeaders(404, -1); return; }
                if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

                // читаем «как есть», логируем сырое тело
                String body = readBody(ex);
                System.out.println("DBG raw body len=" + body.length() + " body='" + body + "'");

                BoardDto req;
                try {
                    req = gson.fromJson(body, BoardDto.class);
                } catch (Exception ge) {
                    ge.printStackTrace();
                    send400(ex, "malformed json: " + ge.getClass().getSimpleName());
                    return;
                }

                // говорящая валидация
                if (req == null) { send400(ex, "bad json"); return; }
                if (req.size <= 2) { send400(ex, "size must be > 2"); return; }
                if (req.data == null) { send400(ex, "data is null"); return; }
                int expected = req.size * req.size;
                int actual = req.data.length();
                if (actual != expected) {
                    System.out.println("DBG invalid length: expected=" + expected + " actual=" + actual + " data='" + req.data + "'");
                    send400(ex, "data length mismatch: expected " + expected + ", got " + actual);
                    return;
                }

                char turn = parseColor(req.nextPlayerColor);
                GameState state = fromBoardDto(req, turn); // распаковали строку в нашу модель

                // если уже конец — хода нет
                if (state.finished) { ex.sendResponseHeaders(204, -1); return; }

                int[] mv = Ai.bestMove(state, turn);
                if (mv == null) { ex.sendResponseHeaders(204, -1); return; }

                byte[] out = gson.toJson(new SimpleMoveDto(mv[0], mv[1], String.valueOf(Character.toLowerCase(turn))))
                                 .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                ex.sendResponseHeaders(200, out.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(out); }
            } catch (Exception e) {
                e.printStackTrace();
                ex.sendResponseHeaders(400, -1);
            } finally {
                ex.close();
            }
        });

        server.setExecutor(null);
        System.out.println("API listening on http://localhost:" + port);
        server.start();
    }

    /* ===== Утилиты ===== */

    private static void addCors(Headers h) {
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Access-Control-Allow-Headers", "Content-Type");
        h.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    }

    private static void send400(com.sun.net.httpserver.HttpExchange ex, String msg) throws IOException {
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(400, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private static String readBody(com.sun.net.httpserver.HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) bos.write(buf, 0, r);
            return bos.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static char parseColor(String s) {
        if (s == null || s.trim().isEmpty()) return 'W';
        char c = Character.toUpperCase(s.trim().charAt(0));
        return c == 'B' ? 'B' : 'W';
    }

    /** Конвертация их BoardDto (строка N*N) в наш GameState, с вычислением finished/winner. */
    private static GameState fromBoardDto(BoardDto dto, char turn) {
        int n = dto.size;
        Character[][] board = new Character[n][n];
        String data = dto.data;

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                char ch = data.charAt(y * n + x);
                if (ch == 'w' || ch == 'W') board[y][x] = 'W';
                else if (ch == 'b' || ch == 'B') board[y][x] = 'B';
                else board[y][x] = null; // любой другой символ = пусто
            }
        }

        Character winner = null;
        boolean wWin = SquareDetector.hasSquare(board, 'W');
        boolean bWin = SquareDetector.hasSquare(board, 'B');
        if (wWin && !bWin) winner = 'W';
        else if (bWin && !wWin) winner = 'B';

        boolean full = true;
        outer: for (int yy = 0; yy < n; yy++) for (int xx = 0; xx < n; xx++) if (board[yy][xx] == null) { full = false; break outer; }

        boolean finished = (winner != null) || full;
        return new GameState(n, board, turn, finished, winner);
    }
}
