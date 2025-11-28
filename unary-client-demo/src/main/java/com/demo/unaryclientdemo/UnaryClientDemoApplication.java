package com.demo.unaryclientdemo;

import com.demo.unaryclientdemo.service.StockClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UnaryClientDemoApplication implements CommandLineRunner {

    private final StockClientService stockClientService;

    public UnaryClientDemoApplication(StockClientService stockClientService) {
        this.stockClientService = stockClientService;
    }

    public static void main(String[] args) {
        SpringApplication.run(UnaryClientDemoApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Grpc Client Response"+
                stockClientService.getStockPrice("GOOGL"));
    }
}
