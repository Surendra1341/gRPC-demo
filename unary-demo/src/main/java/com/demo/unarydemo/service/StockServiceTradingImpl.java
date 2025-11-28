// java
package com.demo.unarydemo.service;

import com.demo.StockRequest;
import com.demo.StockResponse;
import com.demo.StockTradingServiceGrpc;
import com.demo.unarydemo.entity.Stock;
import com.demo.unarydemo.repo.StockRepo;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@GrpcService
public class StockServiceTradingImpl extends StockTradingServiceGrpc.StockTradingServiceImplBase {



    private final StockRepo stockRepo;

    public StockServiceTradingImpl(StockRepo stockRepo) {
        this.stockRepo = stockRepo;
    }


    @Override
    public void getStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver) {// here it returns void here -> so StreamObserver is one who play role here// the StreamObserver has three methods
//        void onNext(V var1);  -> when send response
//        void onError(Throwable var1);  -> if u find any error
//        void onCompleted();  -> signal the client that done!



        //  business logic = stockName-> Db-> map response -> return
        String stockSymbol = request.getStockSymbol();
        Stock stockEntity = stockRepo.findByStockSymbol(stockSymbol);

        StockResponse stockResponse = StockResponse.newBuilder()
                .setStockSymbol(stockEntity.getStockSymbol())
                .setPrice(stockEntity.getPrice())
                .setTimestamp(stockEntity.getLastUpdated().toString())
                .build();

        responseObserver.onNext(stockResponse);  // send response
        responseObserver.onCompleted(); // send client work done
    }
}
