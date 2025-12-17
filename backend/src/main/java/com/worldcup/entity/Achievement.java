package com.worldcup.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // Unique code like "FIRST_PREDICTION", "PERFECT_WEEK"

    @Column(nullable = false, length = 100)
    private String name; // Display name like "First Prediction"

    @Column(length = 500)
    private String description; // Description of how to earn it

    @Column(length = 50)
    private String icon; // Emoji or icon identifier

    @Column(length = 50)
    private String category; // Category like "MILESTONE", "STREAK", "PERFORMANCE", "SPECIAL"

    @Column(nullable = false)
    private Integer rarity; // 1=Common, 2=Uncommon, 3=Rare, 4=Epic, 5=Legendary

    @Column(nullable = false)
    private Boolean active = true; // Whether this achievement is currently active
}

