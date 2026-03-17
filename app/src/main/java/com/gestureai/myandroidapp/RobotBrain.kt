package com.gestureai.myandroidapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.*

/**
 * RobotBrain — Imitation Learning AI
 *
 * Works exactly like how Pokémon GO player interactions trained Google's AI:
 *   1. A human "plays" — swiping to navigate the robot through the grid.
 *   2. Every move is stored as a (state → action) demonstration pair.
 *   3. The AI learns by finding the most similar past states and copying
 *      what the human did then. (K-Nearest Neighbors behavioral cloning)
 *   4. Flip "AI Mode" and the robot drives itself using that learned policy.
 *
 * State vector (12 features):
 *   [0..3]  blocked N/E/S/W   (1.0 = wall/obstacle)
 *   [4..7]  blocked NE/SE/SW/NW diagonals
 *   [8]     Δrow  to goal, normalized −1..1
 *   [9]     Δcol  to goal, normalized −1..1
 *   [10]    Manhattan distance to goal, normalized 0..1
 *   [11]    steps taken this episode, normalized 0..1
 *
 * Actions: 0=Up  1=Right  2=Down  3=Left
 */
class RobotBrain(private val context: Context) {

    companion object {
        const val ACTION_UP    = 0
        const val ACTION_RIGHT = 1
        const val ACTION_DOWN  = 2
        const val ACTION_LEFT  = 3
        private const val STATE_DIM = 12
        private const val K = 5           // neighbours for KNN vote
        private const val MIN_DEMOS = 20  // minimum demos before AI can act
    }

    data class Demo(
        val state: List<Double>,
        val action: Int
    )

    /** All human demonstrations collected so far */
    private val demos: MutableList<Demo> = loadDemos()
    private val prefs: SharedPreferences = context.getSharedPreferences("robot_brain", Context.MODE_PRIVATE)
    private val gson = Gson()

    val demoCount: Int get() = demos.size
    val isReadyToAct: Boolean get() = demos.size >= MIN_DEMOS

    // ── Training ──────────────────────────────────────────────────────────────

    /** Record one human demonstration. */
    fun recordDemo(state: DoubleArray, action: Int) {
        demos.add(Demo(state.toList(), action))
        saveDemos()
    }

    fun clearDemos() {
        demos.clear()
        saveDemos()
    }

    // ── Inference (Behavioral Cloning via KNN) ────────────────────────────────

    /**
     * Given the current world state, return the action the AI thinks is best.
     * Uses KNN: find the K most similar past human states, vote on their actions.
     *
     * Returns null if not enough demonstrations yet.
     */
    fun chooseAction(state: DoubleArray): ActionResult? {
        if (!isReadyToAct) return null

        val neighbours = demos
            .map { demo -> Pair(euclidean(state, demo.state.toDoubleArray()), demo.action) }
            .sortedBy { it.first }
            .take(K)

        val votes = IntArray(4)
        neighbours.forEach { (_, action) -> votes[action]++ }

        val bestAction = votes.indices.maxByOrNull { votes[it] }!!
        val confidence = votes[bestAction].toDouble() / K

        return ActionResult(bestAction, confidence, votes.toList())
    }

    data class ActionResult(
        val action: Int,
        val confidence: Double,
        val votes: List<Int>              // vote counts [up, right, down, left]
    )

    // ── State extraction ──────────────────────────────────────────────────────

    /**
     * Build the 12-dim state vector from the current grid situation.
     *
     * @param grid       2D boolean grid — true means obstacle.
     * @param robotRow   Current robot row.
     * @param robotCol   Current robot col.
     * @param goalRow    Goal row.
     * @param goalCol    Goal col.
     * @param stepsTaken Steps taken this episode.
     * @param maxSteps   Max steps per episode (for normalization).
     */
    fun buildState(
        grid: Array<BooleanArray>,
        robotRow: Int, robotCol: Int,
        goalRow: Int, goalCol: Int,
        stepsTaken: Int, maxSteps: Int
    ): DoubleArray {
        val rows = grid.size
        val cols = grid[0].size
        val s = DoubleArray(STATE_DIM)

        // Cardinal blocking
        s[0] = if (robotRow == 0      || grid[robotRow - 1][robotCol]) 1.0 else 0.0  // N
        s[1] = if (robotCol == cols-1 || grid[robotRow][robotCol + 1]) 1.0 else 0.0  // E
        s[2] = if (robotRow == rows-1 || grid[robotRow + 1][robotCol]) 1.0 else 0.0  // S
        s[3] = if (robotCol == 0      || grid[robotRow][robotCol - 1]) 1.0 else 0.0  // W

        // Diagonal blocking
        s[4] = if (robotRow == 0      || robotCol == cols-1 || grid[robotRow-1][robotCol+1]) 1.0 else 0.0 // NE
        s[5] = if (robotRow == rows-1 || robotCol == cols-1 || grid[robotRow+1][robotCol+1]) 1.0 else 0.0 // SE
        s[6] = if (robotRow == rows-1 || robotCol == 0      || grid[robotRow+1][robotCol-1]) 1.0 else 0.0 // SW
        s[7] = if (robotRow == 0      || robotCol == 0      || grid[robotRow-1][robotCol-1]) 1.0 else 0.0 // NW

        // Direction to goal (normalized)
        val maxDist = sqrt((rows * rows + cols * cols).toDouble())
        s[8]  = (goalRow - robotRow).toDouble() / rows   // Δrow (-1..1)
        s[9]  = (goalCol - robotCol).toDouble() / cols   // Δcol (-1..1)
        s[10] = (abs(goalRow - robotRow) + abs(goalCol - robotCol)).toDouble() / (rows + cols) // manhattan
        s[11] = stepsTaken.toDouble() / maxSteps.coerceAtLeast(1)

        return s
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    /** Return per-action demo distribution for display. */
    fun actionDistribution(): Map<String, Int> = mapOf(
        "Up"    to demos.count { it.action == ACTION_UP },
        "Right" to demos.count { it.action == ACTION_RIGHT },
        "Down"  to demos.count { it.action == ACTION_DOWN },
        "Left"  to demos.count { it.action == ACTION_LEFT }
    )

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun saveDemos() {
        prefs.edit().putString("demos", gson.toJson(demos)).apply()
    }

    private fun loadDemos(): MutableList<Demo> {
        val json = prefs.getString("demos", null) ?: return mutableListOf()
        return try {
            gson.fromJson(json, object : TypeToken<MutableList<Demo>>() {}.type)
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    // ── Math util ─────────────────────────────────────────────────────────────

    private fun euclidean(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        for (i in a.indices) { val d = a[i] - b[i]; sum += d * d }
        return sqrt(sum)
    }
}
