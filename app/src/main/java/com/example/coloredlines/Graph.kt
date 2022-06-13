package com.example.coloredlines

/**
 * Trieda, ktorá vytvára graf vhodný na orientáciu guličiek v mriežke.
 * @author Ruslan Hlazkov
 * @version 13.06.2022
 */

class Graph {

    /**
     * Metoda makeGraph() vytvorí dvojrozmerný zoznam veľkosti hracieho poľa.
     * Ak je v štvorci gulička, pre ktorú sa tento graf vytvara, tak sa stĺpcu priradí hodnota 1.
     * V opačnom prípade, ak je v štvorci ďalšia guľa, tak sa stĺpcu priradí -1, ak je štvorec prázdny, 0
     * @return vytvorený graf
     */

    fun makeGraph(grid: Array<IntArray>, ballCellX: Int, ballCellY: Int): Array<IntArray> {
        val graph = Array(grid.size) { IntArray(grid[0].size) }
        for (y in grid.indices) {
            for (x in grid[0].indices) {
                if (grid[y][x] == 0)
                    graph[y][x] = 0
                else if (y == ballCellY && x == ballCellX)
                    graph[y][x] = 1
                else
                    graph[y][x] = -1
            }
        }
        return graph
    }
}