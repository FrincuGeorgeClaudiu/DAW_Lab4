package com.library.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    private final String libraryName;

    public GreetingService(@Value("${library.name}") String libraryName) {
        this.libraryName = libraryName;
    }

    public String greet(String name) {
        return "Welcome to " + libraryName + ", " + name;
    }
}