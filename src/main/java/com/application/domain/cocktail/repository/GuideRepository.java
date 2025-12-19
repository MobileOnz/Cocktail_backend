package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.guide.Guide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideRepository extends JpaRepository<Guide, Long> {

    Optional<Guide> findByPart(Integer part);
}
