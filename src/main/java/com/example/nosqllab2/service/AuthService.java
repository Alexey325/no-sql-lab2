package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.request.auth.LoginDto;
import com.example.nosqllab2.dto.request.auth.RegisterDto;
import com.example.nosqllab2.dto.response.AuthTokenDto;
import com.example.nosqllab2.dto.response.UserResponseDto;
import com.example.nosqllab2.entity.Role;
import com.example.nosqllab2.entity.User;
import com.example.nosqllab2.exception.custom.EntityAlreadyExistsException;
import com.example.nosqllab2.exception.custom.InvalidCredentialsException;
import com.example.nosqllab2.mapper.UserMapper;
import com.example.nosqllab2.repository.UserRepository;
import com.example.nosqllab2.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(
            UserRepository userRepository,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    public AuthTokenDto register(RegisterDto dto) {
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new EntityAlreadyExistsException("Пользователь с таким username уже существует");
        }

        User user = new User();
        user.setUsername(dto.username());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));

        // каждый зарегистрировавшийся получает роль ADMIN
        Role role = roleService.getEntityByValue(DEFAULT_ROLE);
        user.setRoles(Set.of(role));

        return new AuthTokenDto(jwtService.generateToken(userRepository.save(user)));
    }

    public AuthTokenDto login(LoginDto dto) {
        Optional<User> optionalUser = userRepository.findByUsername(dto.username());

        if (optionalUser.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthTokenDto(jwtService.generateToken(user));
    }

    public UserResponseDto getCurrentUser(Long userId) {
        Optional<User> optionalUser = userRepository.findWithRolesById(userId);

        if (optionalUser.isEmpty()) {
            throw new InvalidCredentialsException("Пользователь не найден");
        }

        return userMapper.toDto(optionalUser.get());
    }
}
