package com.keybudget.config;

import com.keybudget.category.CategoryServiceImpl;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private final CategoryServiceImpl categoryService;

    public DataSeeder(CategoryServiceImpl categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public void run(ApplicationArguments args) {
        categoryService.seedDefaults();
    }
}
