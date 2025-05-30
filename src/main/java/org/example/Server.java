package org.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public static final int MAX_CONNECTIONS = 64;

    public Server() {
        this.threadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS);
    }

    /**
     * Регистрирует обработчик для данного HTTP-метода и пути.
     */
    public void addHandler(String method, String path, Handler handler) {
        handlers
                .computeIfAbsent(method.toUpperCase(), m -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

    /**
     * Запускает сервер на указанном порту и начинает принимать соединения.
     */
    public void listen(int port) {
        System.out.println("Server is starting on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.execute(() -> handleConnection(socket));
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            // Читаем стартовую строку
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                return;
            }
            String method = parts[0].toUpperCase();
            String fullPath = parts[1];

            // есть ли пользовательский хендлер?
            Handler handler = null;
            Map<String, Handler> methodMap = handlers.get(method);

            Request request = null;
            if (methodMap != null) {
                // Временно создаем Request для получения пути без query string
                Map<String, String> tempHeaders = new ConcurrentHashMap<>();
                request = new Request(method, fullPath, tempHeaders, InputStream.nullInputStream());
                String path = request.getPath();
                handler = methodMap.get(path);
            }

            boolean serveStatic = false;
            Path filePath = null;
            if (handler == null && "GET".equals(method)) {
                if (request == null) {
                    Map<String, String> tempHeaders = new ConcurrentHashMap<>();
                    request = new Request(method, fullPath, tempHeaders, InputStream.nullInputStream());
                }
                String path = request.getPath();
                filePath = Path.of("public", path);
                serveStatic = Files.exists(filePath) && !Files.isDirectory(filePath);
            }

            // Ничего не найдено — 404 и возвращаемся
            if (handler == null && !serveStatic) {
                send404(out);
                return;
            }

            // Читаем заголовки
            Map<String, String> headers = new ConcurrentHashMap<>();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                int sep = line.indexOf(':');
                if (sep > 0) {
                    String name = line.substring(0, sep).trim();
                    String value = line.substring(sep + 1).trim();
                    headers.put(name, value);
                }
            }

            // Подготавливаем тело при наличии Content-Length
            InputStream bodyStream = InputStream.nullInputStream();
            if (headers.containsKey("Content-Length")) {
                int length = Integer.parseInt(headers.get("Content-Length"));
                bodyStream = new LimitedInputStream(socket.getInputStream(), length);
            }

            request = new Request(method, fullPath, headers, bodyStream);

            if (handler != null) {
                handler.handle(request, out);
            } else {
                String mimeType = Files.probeContentType(filePath);
                sendFile(out, filePath, mimeType);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private void send404(BufferedOutputStream out) throws IOException {
        out.write((
                          "HTTP/1.1 404 Not Found\r\n" +
                                  "Content-Length: 0\r\n" +
                                  "Connection: close\r\n\r\n"
                  ).getBytes());
        out.flush();
    }

    private void sendFile(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        long length = Files.size(filePath);
        out.write((
                          "HTTP/1.1 200 OK\r\n" +
                                  "Content-Type: " + mimeType + "\r\n" +
                                  "Content-Length: " + length + "\r\n" +
                                  "Connection: close\r\n\r\n"
                  ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }
}
