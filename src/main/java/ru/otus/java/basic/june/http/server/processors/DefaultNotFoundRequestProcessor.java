package ru.otus.java.basic.june.http.server.processors;

import ru.otus.java.basic.june.http.server.HttpRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultNotFoundRequestProcessor implements RequestProcessor {
    @Override
    public void execute(HttpRequest request, OutputStream output) throws IOException {
        Path filePath = Paths.get("static/", "404error.gif");
        byte[] gifData = Files.readAllBytes(filePath);
        String headers = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: image/gif\r\n" +
                "Content-Length: " + gifData.length + "\r\n" +
                "\r\n";
        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(gifData);
    }
}
