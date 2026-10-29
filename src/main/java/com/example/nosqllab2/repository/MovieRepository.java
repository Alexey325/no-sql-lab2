package com.example.nosqllab2.repository;

import com.example.nosqllab2.entity.Movie;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Override
    @EntityGraph(attributePaths = "category")
    List<Movie> findAll();

    @EntityGraph(attributePaths = "category")
    Optional<Movie> findWithCategoryById(Long id);

    // показы одного дня, по ним проверяется пересечение времени в единственном зале
    List<Movie> findAllByShowDateBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);
}
