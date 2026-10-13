package com.biddly.category;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    List<CategoryResponse> list() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    record CategoryResponse(Long id, String name, String slug) {
    }
}
