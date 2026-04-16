package cn.lacknb.blog.llm.stream;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class CallbackServer implements AutoCloseable {
    private static final String RESPONSE_HTML = "<html><body><h3>登录完成</h3><p>现在可以返回 IDE 继续使用。</p></body></html>";

    private final HttpServer server;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<Map<String, String>> queryParams = new AtomicReference<>();

    public CallbackServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/callback", this::handleCallback);
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public Map<String, String> awaitCallback(long timeout, TimeUnit unit) throws InterruptedException {
        if (!latch.await(timeout, unit)) {
            return null;
        }
        return queryParams.get();
    }

    public String getRedirectUri() {
        return "http://127.0.0.1:" + getPort() + "/callback";
    }

    private void handleCallback(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI());
        queryParams.set(params);
        byte[] body = RESPONSE_HTML.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        } finally {
            latch.countDown();
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
