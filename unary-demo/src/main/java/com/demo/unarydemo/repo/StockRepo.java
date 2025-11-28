package com.demo.unarydemo.repo;

import com.demo.unarydemo.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepo extends JpaRepository<Stock, Long> {
    Stock findByStockSymbol(String stockSymbol);
}
