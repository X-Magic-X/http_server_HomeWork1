package ru.otus.java.basic.june.http.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private int port;
    private Dispatcher dispatcher;
    private static final Logger logger = LogManager.getLogger(HttpServer.class);
    private final int THREADSCOUNT = 5;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_REQUEST_SIZE = 1024 * 1024;

    public HttpServer(int port) {
        this.port = port;
        this.dispatcher = new Dispatcher();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Сервер запущен на порту {}. Ожидаем подключения", port);
            ExecutorService executor = Executors.newFixedThreadPool(THREADSCOUNT);
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(new Threads(socket, dispatcher));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Threads implements Runnable {
        private final Socket socket;
        private final Dispatcher dispatcher;

        public Threads(Socket socket, Dispatcher dispatcher) {
            this.socket = socket;
            this.dispatcher = dispatcher;
        }

        @Override
        public void run() {
            try (InputStream inputStream = socket.getInputStream();
                 OutputStream outputStream = socket.getOutputStream()) {
                ByteArrayOutputStream requestBuffer = new ByteArrayOutputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    requestBuffer.write(buffer, 0, bytesRead);
                    if (requestBuffer.size() > MAX_REQUEST_SIZE) {
                        String response = "HTTP/1.1 413 Payload Too Large\r\n\r\n";
                        outputStream.write(response.getBytes());
                        return;
                    }
                    if (bytesRead < BUFFER_SIZE) {
                        break;
                    }
                }
                String rawRequest = requestBuffer.toString(StandardCharsets.UTF_8);
                HttpRequest request = new HttpRequest(rawRequest);
                request.info();
                dispatcher.execute(request, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (!socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
