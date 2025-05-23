package com.kj.dom.repository

import com.kj.dom.model.entity.GameTurn
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GameTurnRepository: JpaRepository<GameTurn, Long>