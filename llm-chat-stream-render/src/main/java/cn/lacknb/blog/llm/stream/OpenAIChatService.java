package cn.lacknb.blog.llm.stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OpenAIChatService {
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public OpenAIChatService(String baseUrl, String model, String apiKey) {
        this.baseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model;
        this.apiKey = apiKey == null || apiKey.isBlank() ? getApiKey() : apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public static String getApiKey() {
        return System.getenv("OPENAI_API_KEY");
    }

    public StreamSession streamChatCompletion(List<ChatMessage> messages, StreamHandler handler) {
        if (apiKey == null || apiKey.isBlank()) {
            handler.onError(new IllegalStateException(
                    "Missing API key. Set OPENAI_API_KEY or create .llm-chat-stream-render.json (or set LLM_CONFIG_PATH)."
            ));
            return StreamSession.noop();
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        payload.addProperty("stream", true);
        payload.addProperty("temperature", 0.2);

        JsonArray jsonMessages = new JsonArray();
        for (ChatMessage message : messages) {
            JsonObject msg = new JsonObject();
            msg.addProperty("role", message.getRole());
            msg.addProperty("content", message.getContent());
            jsonMessages.add(msg);
        }
        payload.add("messages", jsonMessages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(Duration.ofMinutes(2))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<java.util.stream.Stream<String>> streamRef = new AtomicReference<>();
        CompletableFuture<HttpResponse<java.util.stream.Stream<String>>> future = httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofLines()
        );

        new Thread(() -> {
            try {
                HttpResponse<java.util.stream.Stream<String>> response = future.join();
                if (cancelled.get()) {
                    return;
                }

                if (response.statusCode() != 200) {
                    if (!cancelled.get()) {
                        handler.onError(new IOException("OpenAI API error: HTTP " + response.statusCode()));
                    }
                    return;
                }

                StringBuilder full = new StringBuilder();
                java.util.stream.Stream<String> bodyStream = response.body();
                streamRef.set(bodyStream);
                Iterator<String> iterator = bodyStream.iterator();
                while (iterator.hasNext()) {
                    if (cancelled.get()) {
                        return;
                    }
                    String line = iterator.next();
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) {
                        continue;
                    }
                    if ("[DONE]".equals(data)) {
                        if (!cancelled.get()) {
                            handler.onComplete(full.toString());
                        }
                        return;
                    }

                    JsonElement parsed = JsonParser.parseString(data);
                    JsonObject root = parsed.getAsJsonObject();
                    JsonArray choices = root.getAsJsonArray("choices");
                    if (choices == null || choices.size() == 0) {
                        continue;
                    }
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject delta = choice.getAsJsonObject("delta");
                    if (delta == null || !delta.has("content")) {
                        continue;
                    }
                    String chunk = delta.get("content").getAsString();
                    if (!chunk.isEmpty()) {
                        full.append(chunk);
                        if (!cancelled.get()) {
                            handler.onDelta(chunk);
                        }
                    }
                }
                if (!cancelled.get()) {
                    handler.onComplete(full.toString());
                }
            } catch (CancellationException e) {
                if (!cancelled.get()) {
                    handler.onError(e);
                }
            } catch (Exception e) {
                if (!cancelled.get()) {
                    handler.onError(e);
                }
            }
        }, "openai-stream-thread").start();

        return new StreamSession(cancelled, future, streamRef);
    }

    public interface StreamHandler {
        void onDelta(String text);

        void onComplete(String fullText);

        void onError(Throwable error);
    }

    public static final class StreamSession {
        private final AtomicBoolean cancelled;
        private final CompletableFuture<HttpResponse<java.util.stream.Stream<String>>> future;
        private final AtomicReference<java.util.stream.Stream<String>> streamRef;

        private StreamSession(AtomicBoolean cancelled,
                              CompletableFuture<HttpResponse<java.util.stream.Stream<String>>> future,
                              AtomicReference<java.util.stream.Stream<String>> streamRef) {
            this.cancelled = cancelled;
            this.future = future;
            this.streamRef = streamRef;
        }

        public static StreamSession noop() {
            return new StreamSession(new AtomicBoolean(true), null, new AtomicReference<>());
        }

        public void cancel() {
            if (cancelled.getAndSet(true)) {
                return;
            }
            if (future != null) {
                future.cancel(true);
            }
            java.util.stream.Stream<String> stream = streamRef.getAndSet(null);
            if (stream != null) {
                stream.close();
            }
        }
    }
}
