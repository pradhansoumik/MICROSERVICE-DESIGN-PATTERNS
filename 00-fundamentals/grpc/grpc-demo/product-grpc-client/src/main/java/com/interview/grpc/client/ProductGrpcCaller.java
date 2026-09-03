package com.interview.grpc.client;

import com.interview.grpc.product.EmptyRequest;
import com.interview.grpc.product.ProductGrpcServiceGrpc;
import com.interview.grpc.product.ProductListResponse;
import com.interview.grpc.product.ProductRequest;
import com.interview.grpc.product.ProductResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductGrpcCaller {

    @GrpcClient("productService")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub stub;

    public Map<String, Object> listProducts() {
        ProductListResponse response = stub.listProducts(EmptyRequest.newBuilder().build());
        List<Map<String, Object>> products = new ArrayList<>();
        for (ProductResponse p : response.getProductsList()) {
            products.add(toMap(p));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calledVia", "gRPC");
        body.put("products", products);
        return body;
    }

    public Map<String, Object> getProduct(String id) {
        ProductResponse p = stub.getProduct(ProductRequest.newBuilder().setId(id).build());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("calledVia", "gRPC");
        body.put("product", toMap(p));
        return body;
    }

    private static Map<String, Object> toMap(ProductResponse p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("price", p.getPrice());
        return m;
    }
}
