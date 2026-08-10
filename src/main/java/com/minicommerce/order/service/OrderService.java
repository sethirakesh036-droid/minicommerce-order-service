package com.minicommerce.order.service;

import com.minicommerce.order.client.UserClient;
import com.minicommerce.order.dto.OrderRequest;
import com.minicommerce.order.dto.OrderResponse;
import com.minicommerce.order.dto.UserDto;
import com.minicommerce.order.entity.Order;
import com.minicommerce.order.exception.ResourceNotFoundException;
import com.minicommerce.order.exception.UserServiceUnavailableException;
import com.minicommerce.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient; // OpenFeign client -> User Service

    public OrderResponse createOrder(OrderRequest request) {
        // Service-to-service call: validate the user exists before creating the order
        UserDto user = getUser(request.getUserId());

        Order order = Order.builder()
                .userId(request.getUserId())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();

        Order saved = orderRepository.save(order);
        return toResponse(saved, user);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        UserDto user = getUser(order.getUserId());
        return toResponse(order, user);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(order -> toResponse(order, getUser(order.getUserId())))
                .toList();
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        // Validates the user exists first
        UserDto user = getUser(userId);
        return orderRepository.findByUserId(userId).stream()
                .map(order -> toResponse(order, user))
                .toList();
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }

    /**
     * All calls to User Service go through here, guarded by Resilience4j.
     * - @Retry retries transient failures (e.g. a brief network blip) up to 3 times.
     * - @CircuitBreaker trips open after enough failures in the sliding window, so once
     *   User Service is clearly down, we stop hammering it and fail fast instead of
     *   piling up blocked threads waiting on a service that isn't coming back soon.
     * Note: Retry wraps CircuitBreaker (annotations apply innermost-first, so listing
     * @CircuitBreaker above @Retry here means Retry executes first, then CircuitBreaker).
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "userServiceFallback")
    @Retry(name = "userService")
    public UserDto getUser(Long userId) {
        return userClient.getUserById(userId);
    }

    // Fallback signature must match the guarded method's params + a Throwable
    private UserDto userServiceFallback(Long userId, Throwable t) {
        log.warn("User Service call failed for userId={}, circuit breaker fallback triggered: {}",
                userId, t.getMessage());
        throw new UserServiceUnavailableException(
                "User Service is currently unavailable. Please try again in a moment.");
    }

    private OrderResponse toResponse(Order order, UserDto user) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .totalAmount(order.getPrice() * order.getQuantity())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
