package com.minicommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String userName;   // populated via Feign call to User Service
    private String userEmail;  // populated via Feign call to User Service
    private String productName;
    private Integer quantity;
    private Double price;
    private Double totalAmount;
    private LocalDateTime createdAt;
}
