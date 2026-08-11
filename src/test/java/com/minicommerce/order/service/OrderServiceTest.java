package com.minicommerce.order.service;

import com.minicommerce.order.client.UserClient;
import com.minicommerce.order.dto.OrderRequest;
import com.minicommerce.order.dto.OrderResponse;
import com.minicommerce.order.dto.UserDto;
import com.minicommerce.order.entity.Order;
import com.minicommerce.order.exception.ResourceNotFoundException;
import com.minicommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderValidatesUserAndReturnsCalculatedTotal() {
        OrderRequest request = new OrderRequest();
        request.setUserId(7L);
        request.setProductName("Keyboard");
        request.setQuantity(2);
        request.setPrice(49.99);
        UserDto user = new UserDto(7L, "Ada", "ada@example.com", "555-0100");
        Order saved = Order.builder()
                .id(12L)
                .userId(7L)
                .productName("Keyboard")
                .quantity(2)
                .price(49.99)
                .build();

        when(userClient.getUserById(7L)).thenReturn(user);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getId()).isEqualTo(12L);
        assertThat(response.getUserName()).isEqualTo("Ada");
        assertThat(response.getTotalAmount()).isEqualTo(99.98);
        verify(userClient).getUserById(7L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getOrderByIdThrowsWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 99");
    }

    @Test
    void deleteOrderDeletesExistingOrder() {
        when(orderRepository.existsById(12L)).thenReturn(true);

        orderService.deleteOrder(12L);

        verify(orderRepository).deleteById(12L);
    }
}