package com.kj.dom.model.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class SuggestedMove(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val move: String,
    val itemId: String? = null,
    val adIds: List<String> = emptyList(),
    val gameOverProbability: Int? = null,
)
