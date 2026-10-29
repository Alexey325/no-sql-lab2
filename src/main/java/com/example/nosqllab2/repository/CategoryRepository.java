package com.example.nosqllab2.repository;

import com.example.nosqllab2.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    // GET /categories/:id отдаёт категорию вместе с её фильмами, поэтому коллекция фетчится сразу
    @EntityGraph(attributePaths = "movies")
    Optional<Category> findWithMoviesById(Long id);
}
