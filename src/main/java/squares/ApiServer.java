package squares;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class ApiServer {

  /* ===== DTO из их Swagger ===== */
  static final class BoardDto {
    int size;
    String data;            // строка N*N: 'w','b',' ' (или '.')
    String nextPlayerColor; // "w" | "b"
  }
  static final class SimpleMoveDto {
    int x;
    int y;
    String color;           // "w" | "b"
    SimpleMoveDto(int x,int y,String c){ this.x=x; this.y=y; this.color=c; }
  }

  public static void main(String[] args) throws IOException {
    int port = 3000;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    Gson gson = new Gson();

    // CORS (для задания 3)
    HttpHandler cors = ex -> {
      Headers h = ex.getResponseHeaders();
      h.add("Access-Control-Allow-Origin", "*");
      h.add("Access-Control-Allow-Headers", "Content-Type");
      h.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
      ex.getHttpContext().getHandler().handle(ex);
    };

    // /health
    server.createContext("/health", with(ex -> {
      byte[] ok = "OK".getBytes(StandardCharsets.UTF_8);
      ex.sendResponseHeaders(200, ok.length);
      try(OutputStream os = ex.getResponseBody()){ os.write(ok); }
    }, cors));

    server.createContext("/api", with(ex -> {
      if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(204,-1); return; }

      String path = ex.getRequestURI().getPath(); // /api/<rules>/nextMove
      // ожидаем ровно 4 сегмента: ["","api","{rules}","nextMove"]
      String[] seg = path.split("/");
      boolean okPath = seg.length == 4 && "api".equals(seg[1]) && "nextMove".equals(seg[3]);
      if (!okPath) { ex.sendResponseHeaders(404, -1); return; }
      if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { ex.sendResponseHeaders(405, -1); return; }

      try (Reader r = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
        BoardDto req = gson.fromJson(r, BoardDto.class);
        if (req == null || req.size <= 2 || req.data == null || req.data.length() != req.size*req.size) {
          ex.sendResponseHeaders(400, -1); return;
        }
        char turn = parseColor(req.nextPlayerColor);

        GameState state = fromBoardDto(req, turn);        // распаковали строку в нашу модель

        // если уже конец — хода нет → 204 (нет контента)
        if (state.finished) { ex.sendResponseHeaders(204, -1); return; }

        int[] mv = Ai.bestMove(state, turn);
        if (mv == null) { ex.sendResponseHeaders(204, -1); return; }

        SimpleMoveDto out = new SimpleMoveDto(mv[0], mv[1], String.valueOf(Character.toLowerCase(turn)));
        byte[] json = gson.toJson(out).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, json.length);
        try(OutputStream os = ex.getResponseBody()){ os.write(json); }
      } catch (Exception e) {
        ex.sendResponseHeaders(400, -1);
      }
    }, cors));

    server.setExecutor(null);
    System.out.println("API listening on http://localhost:" + port);
    server.start();
  }

  /* ===== Утилиты ===== */

  private static HttpHandler with(HttpHandler core, HttpHandler cors) {
    return ex -> { ex.getHttpContext().setHandler(core); cors.handle(ex); };
  }

  private static char parseColor(String s) {
    if (s == null) return 'W';
    char c = Character.toUpperCase(s.trim().isEmpty() ? 'W' : s.trim().charAt(0));
    return (c == 'B') ? 'B' : 'W';
  }

  /** Конвертируем их BoardDto в наш GameState, заодно вычисляем finished/winner. */
  private static GameState fromBoardDto(BoardDto dto, char turn) {
    int n = dto.size;
    Character[][] board = new Character[n][n];
    String data = dto.data;

    for (int y=0; y<n; y++) for (int x=0; x<n; x++) {
      int i = y*n + x;
      char ch = data.charAt(i);
      if (ch=='w' || ch=='W') board[y][x]='W';
      else if (ch=='b' || ch=='B') board[y][x]='B';
      else board[y][x]=null; 
    }

    // Вычисляем окончание партии
    Character winner = null;
    boolean wWin = SquareDetector.hasSquare(board, 'W');
    boolean bWin = SquareDetector.hasSquare(board, 'B');
    if (wWin && !bWin) winner = 'W';
    else if (bWin && !wWin) winner = 'B';

    boolean full = true;
    outer: for (int yy=0; yy<n; yy++) for (int xx=0; xx<n; xx++) if (board[yy][xx]==null) { full = false; break outer; }

    boolean finished = (winner != null) || full;

    return new GameState(n, board, turn, finished, winner);
  }
}
