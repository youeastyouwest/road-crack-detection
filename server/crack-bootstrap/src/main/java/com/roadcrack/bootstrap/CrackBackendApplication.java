package com.roadcrack.bootstrap;

import com.roadcrack.service.config.AlgorithmClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.roadcrack", exclude = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(AlgorithmClientProperties.class)
public class CrackBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrackBackendApplication.class, args);
    }
}