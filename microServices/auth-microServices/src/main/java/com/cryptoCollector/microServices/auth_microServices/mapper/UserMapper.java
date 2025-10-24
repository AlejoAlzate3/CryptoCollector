package com.cryptoCollector.microServices.auth_microServices.mapper;

import com.cryptoCollector.microServices.auth_microServices.dto.RegisterRequest;
import com.cryptoCollector.microServices.auth_microServices.dto.UserResponse;
import com.cryptoCollector.microServices.auth_microServices.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
