package com.example.skeleton.repository;

import com.example.skeleton.domain.model.Project;
import com.example.skeleton.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByNameContainingIgnoreCase(String name);

    List<Project> findByCreatedBy(User createdBy);

    long countByCreatedById(Long userId);
}