package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();

        server = new MoviesServer(store);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
     void beforeEach() {
        store.clearMovies();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, response.statusCode());

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentType
        );

        assertEquals("[]", response.body().trim());
    }

    @Test
    void getMovies_whenMoviesExist_returnsMovies() throws Exception {
        Movie movie = new Movie(
                "Интерстеллар",
                "Кристофер Нолан",
                2014
        );

        store.addMovie(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("Интерстеллар"));
        assertTrue(body.contains("Кристофер Нолан"));
        assertTrue(body.contains("2014"));
    }
    @Test
    void postMovies_whenValidMovie_returnsCreatedMovie() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"director\":\"Кристофер Нолан\",\"year\":2010}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(201, response.statusCode());

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");

        assertEquals(
                "application/json; charset=UTF-8",
                contentType
        );

        String body = response.body();

        assertTrue(body.contains("\"title\":\"Начало\""));
        assertTrue(body.contains("\"director\":\"Кристофер Нолан\""));
        assertTrue(body.contains("\"year\":2010"));
        assertTrue(body.contains("\"id\":"));
    }
    @Test
    void postMovies_whenTitleIsEmpty_returns422() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"\",\"director\":\"Кристофер Нолан\",\"year\":2010}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void postMovies_whenTitleTooLong_returns422() throws Exception {
        String longTitle = "А".repeat(101);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"" + longTitle
                                + "\",\"director\":\"Кристофер Нолан\",\"year\":2010}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void postMovies_whenYearTooEarly_returns422() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"director\":\"Кристофер Нолан\",\"year\":1887}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void postMovies_whenYearTooLate_returns422() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"director\":\"Кристофер Нолан\",\"year\":2028}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());
    }

    @Test
    void postMovies_whenContentTypeIsWrong_returns415() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"director\":\"Кристофер Нолан\",\"year\":2010}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(415, response.statusCode());
    }

    @Test
    void postMovies_whenJsonIsInvalid_returns422() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"year\":"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());
    }
    @Test
    void postMovies_whenTitleTooLong_returnsValidationError() throws Exception {
        StringBuilder titleBuilder = new StringBuilder();

        for (int i = 0; i < 101; i++) {
            titleBuilder.append("А");
        }

        String title = titleBuilder.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"" + title + "\",\"director\":\"Кристофер Нолан\",\"year\":2010}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("Ошибка валидации"));
        assertTrue(body.contains("название не должно превышать 100 символов"));
    }
    @Test
    void postMovies_whenYearTooEarly_returnsValidationError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"director\":\"Кристофер Нолан\",\"year\":1887}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("Ошибка валидации"));
        assertTrue(body.contains("год должен быть между 1888 и 2027"));
    }
    @Test
    void postMovies_whenWrongContentType_returnsUnsupportedMediaType() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"director\":\"Кристофер Нолан\",\"year\":2010}"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(415, response.statusCode());
    }
    @Test
    void postMovies_whenInvalidJson_returnsValidationError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"title\":\"Начало\",\"year\":"
                ))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(422, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("Ошибка валидации"));
    }
    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        Movie movie = new Movie(
                "Начало",
                "Кристофер Нолан",
                2010
        );

        store.addMovie(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, response.statusCode());

        String body = response.body().trim();

        assertTrue(body.startsWith("{"));
        assertTrue(body.endsWith("}"));

        assertTrue(body.contains("\"id\":1"));
        assertTrue(body.contains("\"title\":\"Начало\""));
        assertTrue(body.contains("\"director\":\"Кристофер Нолан\""));
        assertTrue(body.contains("\"year\":2010"));
    }
    @Test
    void deleteMovie_whenMovieExists_returns204() throws Exception {
        Movie movie = new Movie(
                "Начало",
                "Кристофер Нолан",
                2010
        );

        store.addMovie(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(204, response.statusCode());
        assertNull(store.getMovieById(movie.getId()));
    }


    @Test
    void deleteMovie_whenMovieNotFound_returns404() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(404, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("фильм не найден"));
    }


    @Test
    void deleteMovie_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(400, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("некорректный ID фильма"));
    }
    @Test
    void getMoviesByYear_whenMoviesExist_returnsMoviesOfGivenYear() throws Exception {
        store.addMovie(new Movie("Начало", "Кристофер Нолан", 2010));
        store.addMovie(new Movie("Интерстеллар", "Кристофер Нолан", 2014));
        store.addMovie(new Movie("Дюна", "Дени Вильнёв", 2021));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, response.statusCode());

        String body = response.body();

        assertTrue(body.startsWith("["));
        assertTrue(body.endsWith("]"));
        assertTrue(body.contains("\"title\":\"Начало\""));
        assertFalse(body.contains("\"title\":\"Интерстеллар\""));
        assertFalse(body.contains("\"title\":\"Дюна\""));
    }

    @Test
    void getMoviesByYear_whenNoMoviesOfGivenYear_returnsEmptyList() throws Exception {
        store.addMovie(new Movie("Начало", "Кристофер Нолан", 2010));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body().trim());
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returnsBadRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(400, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("\"error\""));
        assertTrue(body.contains("\"details\""));
        assertTrue(body.contains("некорректный параметр year"));
    }
    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        assertEquals(405, response.statusCode());

        String body = response.body();

        assertTrue(body.contains("\"error\""));
    }
}