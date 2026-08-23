package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarMenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BarMenuCategoryRepository extends JpaRepository<BarMenuCategory, Long> {

    List<BarMenuCategory> findByBarIdOrderByPriorityAscIdAsc(Long barId);
}
