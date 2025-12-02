# gRPC Bidirectional Streaming — Quick Revision

This document summarizes important concepts, patterns and practical notes for implementing gRPC bidirectional (bi\-di) streaming in a Java Spring Boot project.

## Key Concepts
- gRPC supports four RPC types: unary, server\-streaming, client\-streaming, and bidirectional streaming (both sides stream independently).
- In bidirectional streaming each side sends a stream of messages over a single long\-lived HTTP/2 connection. Streams are independent: server can send before/after client messages.
- Order is preserved per direction, not across directions.
- Flow control/backpressure is important: avoid unbounded buffering; use gRPC flow control APIs to request messages.
- Streams are stateful connections; properly close with `onCompleted()` or `onError()`.

## Protobuf (example)
Put proto in `protos/stock.proto` (generate Java + gRPC code with configured `protobuf-maven-plugin`).
    syntax = "proto3";
    package stock;
    
    message TradeRequest {
        string client_id = 1;
        string symbol = 2;
        int32 qty = 3;
        double price = 4;
    }
    
    message TradeResponse {
        string status = 1;
        string info = 2;
    }
    
    service StockTrading {
        rpc Trade(stream TradeRequest) returns (stream TradeResponse);
    }

## Java server (core ideas)
- Implement the generated `StockTradingGrpc.StockTradingImplBase`.
- Return a `StreamObserver<TradeRequest>` and use the provided `StreamObserver<TradeResponse>` to send messages.
- Handle concurrency and avoid blocking Netty event loop.

Example pattern (conceptual, not full file; run heavy work on separate executor):
    @Override
    public StreamObserver<TradeRequest> trade(StreamObserver<TradeResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(TradeRequest req) {
                // process and optionally reply
                responseObserver.onNext(TradeResponse.newBuilder().setStatus("OK").build());
            }
            @Override
            public void onError(Throwable t) {
                // cleanup
            }
            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

## Java client (core ideas)
- Use async stub: `StockTradingGrpc.StockTradingStub stub = StockTradingGrpc.newStub(channel);`
- Provide a `StreamObserver<TradeResponse>` to receive server messages.
- Use `StreamObserver<TradeRequest> requestObserver = stub.trade(responseObserver);`
- Send with `requestObserver.onNext(...)` and close with `requestObserver.onCompleted()`.

Advanced: use `ClientResponseObserver<Req,Resp>` to access `ClientCallStreamObserver` and manage flow control:
    // inside ClientResponseObserver.beforeStart(ClientCallStreamObserver<Req> clientCallStreamObserver) {...}
    clientCallStreamObserver.disableAutoInboundFlowControl();
    clientCallStreamObserver.request(1); // request messages one-by-one

## Flow control & backpressure
- Server can call `ServerCallStreamObserver` methods to check `isReady()` and react to demand.
- Client can call `ClientCallStreamObserver.request(n)` to consume messages gradually.
- Use `isReady()` callbacks to avoid sending when the transport buffer is full.

## Error handling & statuses
- Return gRPC statuses using `io.grpc.Status`:
    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("...").asRuntimeException());
- Handle client/server cancellations via `Context.current().isCancelled()`.

## Timeouts & deadlines
- Use `stub.withDeadlineAfter(5, TimeUnit.SECONDS)` on client calls.
- Server should check deadlines/cancellations and stop work promptly.

## Spring Boot integration (project specifics)
- The project uses `org.springframework.grpc:spring-grpc-spring-boot-starter`. Expose your gRPC service as a Spring bean (check your starter's annotation e.g. `@GrpcService` or register bean implementing generated base class).
- `protobuf-maven-plugin` in `pom.xml` generates classes to `target/generated-sources/protobuf/...`. Ensure IDE picks them up (Maven -> Reimport).
- `grpc-netty-shaded` + `grpc-protobuf` + `grpc-stub` are present in `pom.xml`.

## Testing
- Use `spring-grpc-test` or gRPC in\-process server / `InProcessChannel` for fast unit tests.
- Use `GrpcCleanupRule` (grpc-testing) or automatic Spring test support to clean resources.
- Test message ordering, error propagation, and cancellation behavior.

## Performance & threading
- Avoid blocking Netty event loop threads — offload blocking work to dedicated executors.
- Pool resources if many concurrent streams expected.
- Monitor memory and transport buffers.

## Common pitfalls
- Forgetting to call `onCompleted()` leads to streams that never close.
- Blocking in `onNext()` causing starvation/deadlocks.
- Mismatched proto versions between client/server — regenerate stubs after proto changes.
- Not handling `onError()` or cancellations.

## Best practices
- Keep message sizes small; stream large payloads in chunks.
- Design messages with idempotency in mind (retries).
- Limit stream lifetime or use heartbeats for liveness.
- Use authentication/authorization at transport or interceptor level.
- Use interceptors to centralize logging, auth, and metrics.

## Checklist to revise quickly
- [ ] Protobuf message/service structure
- [ ] StreamObserver lifecycle: onNext, onError, onCompleted
- [ ] Flow control APIs (ClientCallStreamObserver / ServerCallStreamObserver)
- [ ] Server/client threading model and executors
- [ ] Error mapping (Status)
- [ ] Deadlines/cancellation
- [ ] Spring Boot wiring and generated sources
- [ ] Testing with in\-process or Spring test support

## Useful references
- Official gRPC Java docs: https://grpc.io/docs/languages/java/
- gRPC Java flow control examples and `ClientResponseObserver` docs
- Your `pom.xml` plugin settings: protobuf plugin generates Java and gRPC sources automatically.