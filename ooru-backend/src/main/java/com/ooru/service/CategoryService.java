package com.ooru.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooru.dto.CategoryDtos.*;
import com.ooru.model.ServiceCategory;
import com.ooru.repository.ServiceCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final ServiceCategoryRepository categoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryService(ServiceCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<ServiceCategory> allActive() {
        return categoryRepository.findByActiveTrue();
    }

    public List<ServiceCategory> all() {
        return categoryRepository.findAll();
    }

    public ServiceCategory create(CreateCategoryRequest req) {
        if (categoryRepository.findByCode(req.code).isPresent()) {
            throw new IllegalStateException("A category with this code already exists");
        }
        ServiceCategory category = new ServiceCategory();
        category.setCode(req.code);
        category.setDisplayName(req.displayName);
        category.setIcon(req.icon);
        category.setFieldsJson(toJson(req.fields));
        category.setActive(true);
        return categoryRepository.save(category);
    }

    public ServiceCategory update(Long id, UpdateCategoryRequest req) {
        ServiceCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (req.displayName != null) category.setDisplayName(req.displayName);
        if (req.icon != null) category.setIcon(req.icon);
        if (req.fields != null) category.setFieldsJson(toJson(req.fields));
        if (req.active != null) category.setActive(req.active);
        return categoryRepository.save(category);
    }

    public CategoryResponse toResponse(ServiceCategory category) {
        CategoryResponse res = new CategoryResponse();
        res.id = category.getId();
        res.code = category.getCode();
        res.displayName = category.getDisplayName();
        res.icon = category.getIcon();
        res.active = category.isActive();
        res.fields = category.getFieldsJson() != null ? parseFields(category.getFieldsJson()) : List.of();
        return res;
    }

    private List<FieldDefDto> parseFields(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(List<FieldDefDto> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not serialize field definitions", e);
        }
    }
}
