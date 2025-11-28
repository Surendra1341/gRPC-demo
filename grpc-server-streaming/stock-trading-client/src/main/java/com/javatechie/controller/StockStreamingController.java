package com.javatechie.controller;


import com.demo.grpc.StockRequest;
import com.demo.grpc.StockResponse;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;



import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/stocks")
public class StockStreamingController {

    @GrpcClient("stockService")
    private com.demo.grpc.StockTradingServiceGrpc.StockTradingServiceStub stockServiceStub;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping(value = "/subscribe/{symbol}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeStockPrice(@PathVariable String symbol) {
        SseEmitter emitter = new SseEmitter();
        executor.execute(() -> {
            StockRequest request = StockRequest.newBuilder().setStockSymbol(symbol).build();

            stockServiceStub.subscribeStockPrice(request, new StreamObserver<>() {
                @Override
                public void onNext(StockResponse response) {
                    try {
                        String jsonResponse = JsonFormat.printer()
                                .includingDefaultValueFields()
                                .preservingProtoFieldNames()
                                .print(response);
                        emitter.send(jsonResponse);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable t) {
                    emitter.completeWithError(t);
                }

                @Override
                public void onCompleted() {
                    emitter.complete();
                }
            });
        });
        return emitter;
    }
}
