package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import ru.practicum.moviehub.store.MoviesStore;

public class MoviesServer {
    private final HttpServer server;

    public MoviesServer(MoviesStore moviesStore) {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        // запустите сервер
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        // остановите сервер
        server.stop(0);
        System.out.println("Сервер остановлен");
    }
}

