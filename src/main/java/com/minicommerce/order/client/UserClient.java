package com.minicommerce.order.client;

import com.minicommerce.order.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// "USER-SERVICE" must match spring.application.name of the User Service (Eureka resolves it)
@FeignClient(name = "USER-SERVICE")
public interface UserClient {

    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    // simple health endpoint call to the User Service (returns the actuator health map)
    @GetMapping("/actuator/health")
    java.util.Map<String, Object> health();
}
