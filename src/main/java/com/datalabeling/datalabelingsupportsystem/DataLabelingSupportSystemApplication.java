package com.datalabeling.datalabelingsupportsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Thêm annotation này
public class DataLabelingSupportSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataLabelingSupportSystemApplication.class, args);
    }
}
