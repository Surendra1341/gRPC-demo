package com.demo.unaryclientdemo.service;


import com.demo.StockRequest;
import com.demo.StockResponse;
import com.demo.StockTradingServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class StockClientService {

    // use blocking stubs-> for unary(request and response model)
    @GrpcClient("stockService")
    private StockTradingServiceGrpc.StockTradingServiceBlockingStub stub;

    //StockResponse getStockPrice(StockRequest)


    // here for testing we can do anything like create normal endpoints
    // for now use go to mainApplication
    public StockResponse getStockPrice(String symbol){
        StockRequest re = StockRequest.newBuilder().setStockSymbol(symbol).build();
        return stub.getStockPrice(re);
    }


}
