package org.example;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();

        server.addHandler("GET", "/messages", (request, out) -> {
            String responseBody = "{\"messages\": [\"Hello\", \"World\"]}";
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            out.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n\r\n").getBytes());
            out.write(body);
            out.flush();
        });

        server.addHandler("POST", "/messages", (request, out) -> {
            // Прочитать тело POST-запроса
            byte[] buf = request.getBody().readAllBytes();
            String incoming = new String(buf, StandardCharsets.UTF_8);
            System.out.println("Received POST body: " + incoming);

            String responseBody = "{\"status\": \"ok\"}";
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            out.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n\r\n").getBytes());
            out.write(body);
            out.flush();
        });

        server.listen(9999);
    }
}
