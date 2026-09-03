package com.interview.grpc.server;

import com.interview.grpc.product.EmptyRequest;
import com.interview.grpc.product.ProductGrpcServiceGrpc;
import com.interview.grpc.product.ProductListResponse;
import com.interview.grpc.product.ProductRequest;
import com.interview.grpc.product.ProductResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ProductGrpcServiceImpl extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    @Override
    public void listProducts(EmptyRequest request, StreamObserver<ProductListResponse> responseObserver) {
        ProductListResponse response = ProductListResponse.newBuilder()
                .addProducts(product("P-1", "Laptop", 75000))
                .addProducts(product("P-2", "Mouse", 500))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        responseObserver.onNext(product(request.getId(), "Laptop", 75000));
        responseObserver.onCompleted();
    }

    private static ProductResponse product(String id, String name, int price) {
        return ProductResponse.newBuilder()
                .setId(id)
                .setName(name)
                .setPrice(price)
                .build();
    }
}
