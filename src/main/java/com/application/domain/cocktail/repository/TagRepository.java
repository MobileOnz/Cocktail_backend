package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.Tag;
import com.application.domain.cocktail.enums.TagType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByType(TagType tagType);
}
