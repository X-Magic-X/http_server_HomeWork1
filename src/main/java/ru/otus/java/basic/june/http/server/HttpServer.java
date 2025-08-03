package ru.otus.java.basic.june.http.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private final int port;
    private final Dispatcher dispatcher;
    private final int THREADSCOUNT = 5;

    public HttpServer(int port) {
        this.port = port;
        this.dispatcher = new Dispatcher();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port + ". Ожидаем подключения");
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
                byte[] buffer = new byte[8192];
                int bytesRead = inputStream.read(buffer);
                if (bytesRead < 1) {
                    return;
                }
                String rawRequest = new String(buffer, 0, bytesRead);
                HttpRequest request = new HttpRequest(rawRequest);
                request.info(true);
                dispatcher.execute(request, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(!socket.isClosed()){
                    try{
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
