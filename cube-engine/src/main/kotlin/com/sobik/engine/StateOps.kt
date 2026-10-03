package com.sobik.engine

import com.sobik.model.CubeState
import com.sobik.model.Move
import com.sobik.model.Notation

/** Sticker-level move application on [CubeState]; supports every move kind and every cube size. */
fun CubeState.applyMoves(moves: List<Move>): CubeState =
    if (moves.isEmpty()) this
    else CubeState.fromBytes(type, CubeGeometry.of(size).apply(toByteArray(), moves))

fun CubeState.applyMoves(notation: String): CubeState = applyMoves(Notation.parse(notation))
