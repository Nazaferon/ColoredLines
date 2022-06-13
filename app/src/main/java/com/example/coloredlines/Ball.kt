package com.example.coloredlines

/**
 * Trieda, ktorá vytvára guličku so svojimi vlastnými atribútmi a metódami pohybu v hre.
 * @author Ruslan Hlazkov
 * @version 13.06.2022
 */

class Ball(cellX: Int, cellY: Int, color : Int, frameLayoutId : Int) {
    var cellX_ : Int = cellX
    var cellY_ = cellY
    val color_ = color
    var frameLayoutId_ = frameLayoutId
    var targetCellX_ = 0
    var targetCellY_ = 0
    var path_: ArrayList<ArrayList<Int>>? = null
    var isSelected_: Boolean = false
    var isMoving_: Boolean = false
    var isPathFound_: Boolean = false

    /**
     * Metoda prepareToMove() pripravuje guličku na pohyb.
     * @param cellX súradnica X cieľového poľa
     * @param cellY súradnica Y cieľového poľa
     * @param graph graf hracieho poľa
     */

    fun prepareToMove(cellX: Int, cellY: Int, graph: Array<IntArray>) {
        targetCellX_ = cellX
        targetCellY_ = cellY
        if (!isPathFound_) {
            path_ = findPath(graph)
            isSelected_ = false
            if (!isMoving_ && isPathFound_)
                isMoving_ = true
        }
    }

    /**
     * Metoda findPath() hľadá najkratšiu cestu v grafe (ak existuje) pomocou Dijkstreeho algoritmu.
     * Gulička sa bude pohybovať po tejto ceste.
     * @param graph graf hracieho poľa
     * @return vráti novú cestu v grafe
     */

    private fun findPath(graph: Array<IntArray>): ArrayList<ArrayList<Int>>? {
        var queue = ArrayList<ArrayList<Int>>()
        var tempo = ArrayList<Int>()
        tempo.add(cellX_)
        tempo.add(cellY_)
        queue.add(tempo)
        var queue2 = ArrayList<ArrayList<Int>>()
        var stopParam: Int
        while (graph[targetCellY_][targetCellX_] == 0) {
            stopParam = 0
            for (i in queue.indices) {
                if (queue[i][0] > 0) {
                    if (graph[queue[i][1]][queue[i][0] - 1] == 0) {
                        graph[queue[i][1]][queue[i][0] - 1] = graph[queue[i][1]][queue[i][0]] + 1
                        tempo = ArrayList()
                        tempo.add(queue[i][0] - 1)
                        tempo.add(queue[i][1])
                        queue2.add(tempo)
                        stopParam++
                    }
                }
                if (queue[i][0] < graph[0].size - 1) {
                    if (graph[queue[i][1]][queue[i][0] + 1] == 0) {
                        graph[queue[i][1]][queue[i][0] + 1] = graph[queue[i][1]][queue[i][0]] + 1
                        tempo = ArrayList()
                        tempo.add(queue[i][0] + 1)
                        tempo.add(queue[i][1])
                        queue2.add(tempo)
                        stopParam++
                    }
                }
                if (queue[i][1] > 0) {
                    if (graph[queue[i][1] - 1][queue[i][0]] == 0) {
                        graph[queue[i][1] - 1][queue[i][0]] = graph[queue[i][1]][queue[i][0]] + 1
                        tempo = ArrayList()
                        tempo.add(queue[i][0])
                        tempo.add(queue[i][1] - 1)
                        queue2.add(tempo)
                        stopParam++
                    }
                }
                if (queue[i][1] < graph.size - 1) {
                    if (graph[queue[i][1] + 1][queue[i][0]] == 0) {
                        graph[queue[i][1] + 1][queue[i][0]] = graph[queue[i][1]][queue[i][0]] + 1
                        tempo = ArrayList()
                        tempo.add(queue[i][0])
                        tempo.add(queue[i][1] + 1)
                        queue2.add(tempo)
                        stopParam++
                    }
                }
            }
            if (stopParam == 0) {
                isPathFound_ = false
                return null
            }
            queue = queue2
            queue2 = ArrayList()
        }
        var x = targetCellX_
        var y = targetCellY_
        tempo = ArrayList()
        tempo.add(x)
        tempo.add(y)
        val newPath = ArrayList<ArrayList<Int>>()
        newPath.add(tempo)
        while (x != cellX_ || y != cellY_) {
            if (y < graph.size - 1) {
                if (graph[y + 1][x] < graph[y][x] && graph[y + 1][x] > 0) {
                    tempo = ArrayList()
                    tempo.add(x)
                    tempo.add(y + 1)
                    newPath.add(tempo)
                    y++
                }
            }
            if (x < graph[0].size - 1) {
                if (graph[y][x + 1] < graph[y][x] && graph[y][x + 1] > 0) {
                    tempo = ArrayList()
                    tempo.add(x + 1)
                    tempo.add(y)
                    newPath.add(tempo)
                    x++
                }
            }
            if (y > 0) {
                if (graph[y - 1][x] < graph[y][x] && graph[y - 1][x] > 0) {
                    tempo = ArrayList()
                    tempo.add(x)
                    tempo.add(y - 1)
                    newPath.add(tempo)
                    y--
                }
            }
            if (x > 0) {
                if (graph[y][x - 1] < graph[y][x] && graph[y][x - 1] > 0) {
                    tempo = ArrayList()
                    tempo.add(x - 1)
                    tempo.add(y)
                    newPath.add(tempo)
                    x--
                }
            }
        }
        isPathFound_ = true
        return newPath
    }

    /**
     * Metoda move() priradí guličke nové súradnice ďalšieho štvorca cesty
     */

    fun move() {
        if (isMoving_ && isPathFound_) {
            if (cellX_ != targetCellX_ || cellY_ != targetCellY_) {
                if (cellX_ == path_!![path_!!.size - 1][0] && cellY_ == path_!![path_!!.size - 1][1])
                    path_!!.removeAt(path_!!.size - 1)
                if (cellY_ < path_!![path_!!.size - 1][1])
                    cellY_++
                else if (cellY_ > path_!![path_!!.size - 1][1])
                    cellY_--
                else if (cellX_ < path_!![path_!!.size - 1][0])
                    cellX_++
                else if (cellX_ > path_!![path_!!.size - 1][0])
                    cellX_--
            }
            if (cellX_ == targetCellX_ && cellY_ == targetCellY_) {
                isMoving_ = false
                isPathFound_ = false
            }
        }
    }
}