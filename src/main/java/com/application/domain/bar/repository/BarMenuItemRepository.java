package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BarMenuItemRepository extends JpaRepository<BarMenuItem, Long> {

    List<BarMenuItem> findByBarIdOrderByPriorityAscIdAsc(Long barId);

    List<BarMenuItem> findByBarIdAndCocktailIdIsNotNullOrderByPriorityAscIdAsc(Long barId);
}
