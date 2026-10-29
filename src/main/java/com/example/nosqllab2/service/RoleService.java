package com.example.nosqllab2.service;

import com.example.nosqllab2.dto.request.create.CreateRoleDto;
import com.example.nosqllab2.dto.response.RoleResponseDto;
import com.example.nosqllab2.entity.Role;
import com.example.nosqllab2.exception.custom.EntityAlreadyExistsException;
import com.example.nosqllab2.exception.custom.EntityNotFoundException;
import com.example.nosqllab2.mapper.RoleMapper;
import com.example.nosqllab2.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    public RoleResponseDto createRole(CreateRoleDto dto) {
        if (roleRepository.existsByValue(dto.value())) {
            throw new EntityAlreadyExistsException("Роль с таким названием уже существует");
        }

        Role role = new Role();
        role.setValue(dto.value());
        role.setDescription(dto.description());

        return roleMapper.toDto(roleRepository.save(role));
    }

    public List<RoleResponseDto> findAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDto)
                .toList();
    }

    public Role getEntityByValue(String value) {
        Optional<Role> optionalRole = roleRepository.findByValue(value);

        if (optionalRole.isEmpty()) {
            throw new EntityNotFoundException("Роль не найдена");
        }

        return optionalRole.get();
    }
}
