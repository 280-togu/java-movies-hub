package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Year;

import java.io.IOException;
import java.util.List;

import com.google.gson.Gson;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        int currentYear = Year.now().getValue();
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        if (method.equalsIgnoreCase("GET")) {

            String path = ex.getRequestURI().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length == 2) {

                String query = ex.getRequestURI().getQuery();

                if (query == null) {
                    sendJson(ex, 200, gson.toJson(moviesStore.getMovies()));
                    return;
                }
                String[] queryParts = query.split("=");
                if (queryParts.length != 2 || !queryParts[0].equals("year")) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("неподдерживаемый параметр запроса"));
                    sendJson(ex, 400, gson.toJson(errorResponse));
                    return;
                }
                try {
                    int year = Integer.parseInt(queryParts[1]);

                    List<Movie> movies = moviesStore.getMovies().stream().filter(movie -> movie.getYear() == year).toList();
                    sendJson(ex, 200, gson.toJson(movies));
                    return;
                } catch (NumberFormatException e) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("некорректный параметр year"));
                    sendJson(ex, 400, gson.toJson(errorResponse));
                    return;
                }

            }
            if (pathParts.length == 3) {
                try {
                    int movieId = Integer.parseInt(pathParts[2]);
                    Movie movie = moviesStore.getMovieById(movieId);
                    if (movie == null) {
                        ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("фильм не найден"));
                        sendJson(ex, 404, gson.toJson(errorResponse));
                        return;
                    } else {
                        sendJson(ex, 200, gson.toJson(movie));
                    }
                } catch (NumberFormatException e) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("некорректный ID фильма"));
                    sendJson(ex, 400, gson.toJson(errorResponse));
                    return;
                }
            }

        }
        if (method.equalsIgnoreCase("POST")) {
            if (!"application/json".equalsIgnoreCase(contentType)) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("неподдерживаемый Content-Type"));
                sendJson(ex, 415, gson.toJson(errorResponse));
                return;
            }
            try {
                Movie movie = gson.fromJson(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8), Movie.class);
                if (movie.getTitle().trim().isEmpty()) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("название не должно быть пустым"));
                    sendJson(ex, 422, gson.toJson(errorResponse));
                    return;
                }
                if (movie.getTitle().length() > 100) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("название не должно превышать 100 символов"));
                    sendJson(ex, 422, gson.toJson(errorResponse));
                    return;
                }
                if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("год должен быть между 1888 и " + (currentYear + 1)));
                    sendJson(ex, 422, gson.toJson(errorResponse));
                    return;
                }
                moviesStore.addMovie(movie);
                sendJson(ex, 201, gson.toJson(movie));
            } catch (JsonSyntaxException e) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("некорректный JSON"));
                sendJson(ex, 422, gson.toJson(errorResponse));
            }
        }
        if (method.equalsIgnoreCase("DELETE")) {
            String path = ex.getRequestURI().getPath();
            String[] pathParts = path.split("/");
            try {
                int movieId = Integer.parseInt(pathParts[2]);
                Movie deletedMovie = moviesStore.deleteMovie(movieId);
                if (deletedMovie == null) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("фильм не найден"));
                    sendJson(ex, 404, gson.toJson(errorResponse));
                    return;
                } else {
                    ex.sendResponseHeaders(204, -1);
                    ex.close();
                }
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("некорректный ID фильма"));
                sendJson(ex, 400, gson.toJson(errorResponse));
                return;
            }
        }
        if (!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("POST") && !method.equalsIgnoreCase("DELETE")) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", List.of("неподдерживаемый метод"));
            sendJson(ex, 405, gson.toJson(errorResponse));
            return;
        }
    }
}