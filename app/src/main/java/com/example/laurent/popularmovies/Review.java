package com.example.laurent.popularmovies;

public class Review {
    public String body;
    public String author;

    public Review() {
        super();
    }

    public Review(String author, String body) {
        this.author = author;
        this.body = body;
    }
}
