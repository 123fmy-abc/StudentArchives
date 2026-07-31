package com.example.studentarchives;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(
        exclude = {RedisRepositoriesAutoConfiguration.class}
)
public class StudentArchivesApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentArchivesApplication.class, args);
    }
}
