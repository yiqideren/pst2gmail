package com.atsid.outlook.app;

import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Spring boot main class.
 */
public class Main {
    /**
     * Main method of spring boot application used to startup application
     *
     * @param args
     */
    public static void main(String[] args) {
        new SpringApplicationBuilder(ExtractApplication.class).headless(false).run(args);
    }
}