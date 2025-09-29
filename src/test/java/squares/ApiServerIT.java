package squares;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ApiServerIT {

    private HttpServer server;
    private int port;

    @Before
    public void setUp() throws Exception {
        // 0 = ephemeral port; узнаём фактический порт через getAddress().getPort()
        server = ApiServer.start(0);
        port = ((InetSocketAddress) server.getAddress()).getPort();
        assertTrue(port > 0);
    }

    @After
    public void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    public void healthReturnsOK() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:"+port+"/health").openConnection();
        c.setRequestMethod("GET");
        assertEquals(200, c.getResponseCode());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            assertEquals("OK", br.readLine());
        }
    }

    @Test
    public void nextMoveReturns200WithMove() throws Exception {
        String body = "{ \"size\":5, \"data\":\".b.w.....................\", \"nextPlayerColor\":\"b\" }";
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:"+port+"/api/squares/nextMove").openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        assertEquals(200, c.getResponseCode());
        String resp;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            resp = br.readLine();
        }
        assertNotNull(resp);
        // примитивная проверка формата
        assertTrue(resp.contains("\"x\":"));
        assertTrue(resp.contains("\"y\":"));
        assertTrue(resp.contains("\"color\":\"b\""));
    }

    @Test
    public void nextMoveReturns204WhenNoMove() throws Exception {
        // Поле 3x3, полностью заполнено без квадратов — ожидаем 204
        String data = "WBW" +
                      "BWB" +
                      "WBW"; 
        String body = "{ \"size\":3, \"data\":\""+data+"\", \"nextPlayerColor\":\"b\" }";
        HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:"+port+"/api/squares/nextMove").openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = c.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        assertEquals(204, c.getResponseCode());
    }
}
