package com.kj.dom.model.entity

import com.kj.dom.model.AdMessage
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne

@Entity
data class GameTurn(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val gameId: String,
    val turn: Int,
    val turnScore: Int,
    val gold: Int,
    val lives: Int,

    @ElementCollection
    @CollectionTable(name = "game_turn_ads", joinColumns = [JoinColumn(name = "game_turn_id")])
    val ads: List<AdMessage>,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "suggested_move_id", referencedColumnName = "id")
    val turnAction: SuggestedMove,
)