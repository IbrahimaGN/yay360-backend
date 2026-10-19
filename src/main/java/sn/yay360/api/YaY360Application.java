package sn.yay360.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YaY360Application {

    public static void main(String[] args) {
        SpringApplication.run(YaY360Application.class, args);
    }

}
