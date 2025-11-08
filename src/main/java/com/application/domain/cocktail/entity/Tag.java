package com.application.domain.cocktail.entity;

import com.application.domain.cocktail.enums.TagType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Comment;


/***
 * Type에 따른 여러 태그들 존재
 * Ex) Type : Flavor, Mood ...
 */
@Getter
@Entity
@Table(name="tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING) // enum 이름 그대로 저장
    @Comment("FLAVOR(맛),MOOD(분위기)")
    @Column(nullable = false, name="type")
    private TagType type;

    @Column(nullable = false, unique = true)
    private String name;

    protected Tag() {}

    public Tag(TagType type, String name) {
        this.type = type;
        this.name = name;
    }

    public void update(TagType type, String name) {
        this.type = type;
        this.name = name;
    }
}
