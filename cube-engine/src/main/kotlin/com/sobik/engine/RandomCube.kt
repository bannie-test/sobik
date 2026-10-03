package com.sobik.engine

import kotlin.random.Random

/** Uniformly random reachable states (useful for tests, demos and scramble generation). */
object RandomCube {
    fun cubie(random: Random = Random.Default): CubieCube {
        val cp = (0 until 8).shuffled(random).toIntArray()
        val ep = (0 until 12).shuffled(random).toIntArray()
        if (CubieCube.permutationParity(cp) != CubieCube.permutationParity(ep)) {
            val t = ep[0]; ep[0] = ep[1]; ep[1] = t
        }
        val co = IntArray(8) { if (it < 7) random.nextInt(3) else 0 }
        co[7] = (3 - co.sum() % 3) % 3
        val eo = IntArray(12) { if (it < 11) random.nextInt(2) else 0 }
        eo[11] = eo.sum() % 2
        return CubieCube(cp, co, ep, eo)
    }

    /** Random 2x2 corner state with DBL solved (every 2x2 state up to whole-cube rotation). */
    fun corners(random: Random = Random.Default): CornerCube {
        val others = listOf(0, 1, 2, 3, 4, 5, 7).shuffled(random)
        val cp = IntArray(8)
        var k = 0
        for (i in 0 until 8) cp[i] = if (i == CubieCube.DBL) CubieCube.DBL else others[k++]
        val co = IntArray(8)
        val free = (0 until 8).filter { it != CubieCube.DBL }
        for (i in free.dropLast(1)) co[i] = random.nextInt(3)
        co[free.last()] = (3 - co.sum() % 3) % 3
        return CornerCube(cp, co)
    }
}
