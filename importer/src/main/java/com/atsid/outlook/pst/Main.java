package com.atsid.outlook.pst;

import org.springframework.boot.builder.SpringApplicationBuilder;

public class Main {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ExtractApplication.class).headless(false).run(args);
    }
}