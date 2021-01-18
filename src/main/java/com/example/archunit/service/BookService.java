package com.example.archunit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookService {
    private final CatService catService;

    private final DogService dogService;

    @Autowired
    private AutowiredService autowiredService;

    public BookService(CatService catService, DogService dogService) {
        this.catService = catService;
        this.dogService = dogService;
    }
}
