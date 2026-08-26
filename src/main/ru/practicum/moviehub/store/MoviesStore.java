package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MoviesStore {
    private int nextId = 1;
    private final HashMap<Integer, Movie> movies = new HashMap<>();

    public void addMovie(Movie movie) {
        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public void clearMovies() {
        movies.clear();
        nextId = 1;
    }

    public Movie getMovieById(int id) {
        return movies.get(id);
    }

    public Movie deleteMovie(int id) {
        return movies.remove(id);
    }
}