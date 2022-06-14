package com.example.coloredlines

/**
 * Toto je hlavná aktivita
 * @author Ruslan Hlazkov
 * @version 13.06.2022
 */

import android.animation.TimeAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.core.view.size
import com.example.coloredlines.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var selectedBallIndex_: Int = -1
    private var score_: Int = 0
    private var topScore_ : Int = 0
    private val maxBallsCount_ : Int = 81
    private var timePerOneMove_ = 70F
    private var timeAfterLastMove_ = 0F
    private var isGameOver_ = false
    private var isMusicTurnedOff_ = false
    private var isSfxTurnedOff_ = false
    private var movingBall_ : Ball? = null
    private lateinit var graph_ : Graph
    private lateinit var sharedPref_ : SharedPreferences
    private lateinit var restartButton_ : Button
    private lateinit var settingsButton_: Button
    private lateinit var exitButton_ : Button
    private lateinit var clearButton_: Button
    private lateinit var gridLayout_ : TableLayout
    private lateinit var playerScoreTextView_ : TextView
    private lateinit var playerTopScoreTextView_ : TextView
    private lateinit var binding_: ActivityMainBinding
    private lateinit var grid_ :  Array<IntArray>
    private lateinit var balls_ : MutableList<Ball>
    private lateinit var deadBalls_ : MutableList<Ball>
    private lateinit var unoccupiedCells_ : MutableList<IntArray>
    private lateinit var mediaPlayers_ : HashMap<String, MediaPlayer>

    /**
     * Metoda onCreate() sa volá  pri prvom vytvorení aktivity.
     * Ide o metóde, v ktorej program vykoná všetky bežné statické nastavenia.
     * @param savedInstanceState balík obsahujúci predtým zmrazený stav aktivity, ak nejaký existoval.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        graph_ = Graph()
        grid_ = Array(9) { IntArray(9) }
        balls_ = mutableListOf()
        deadBalls_  = mutableListOf()
        unoccupiedCells_ = mutableListOf()
        mediaPlayers_ = HashMap()
        val backgroundMusicPlayer = MediaPlayer.create(this, R.raw.background_music)
        backgroundMusicPlayer.setVolume(0.2f, 0.2f)
        backgroundMusicPlayer.isLooping = true
        val ballClickSoundPlayer = MediaPlayer.create(this, R.raw.ball_click_sound)
        ballClickSoundPlayer.setVolume(1f, 1f)
        val destroyingBallsSoundPlayer = MediaPlayer.create(this, R.raw.destroying_balls_sound)
        destroyingBallsSoundPlayer.setVolume(1f, 1f)
        val movingBallSoundPlayer = MediaPlayer.create(this, R.raw.moving_ball_sound)
        movingBallSoundPlayer.setVolume(0.8f, 0.8f)
        val buttonClickSoundPlayer = MediaPlayer.create(this, R.raw.button_click_sound)
        buttonClickSoundPlayer.setVolume(1f, 1f)
        mediaPlayers_["backgroundMusicPlayer"] = backgroundMusicPlayer
        mediaPlayers_["ballClickSoundPlayer"] = ballClickSoundPlayer
        mediaPlayers_["destroyingBallsSoundPlayer"] = destroyingBallsSoundPlayer
        mediaPlayers_["movingBallSoundPlayer"] = movingBallSoundPlayer
        mediaPlayers_["buttonClickSoundPlayer"] = buttonClickSoundPlayer
        sharedPref_ = getPreferences(Context.MODE_PRIVATE)
        val bundle : Bundle? = intent.extras
        if (bundle != null) {
            timePerOneMove_ = when (bundle.getInt("gameSpeed")) {
                1 -> 120F
                3 -> 40F
                else -> 70F
            }
            isMusicTurnedOff_ = bundle.getBoolean("isMusicTurnedOff")
            isSfxTurnedOff_ = bundle.getBoolean("isSfxTurnedOff")
            mediaPlayers_["backgroundMusicPlayer"]!!.seekTo(bundle.getInt("backgroundMusicPlayerPosition"))
        }
        setLayoutConfigurations()
        createGrid()
        for (i in 1..5)
            generateBall()

        val timeAnimator = TimeAnimator()
        var isCellAdded = false
        timeAnimator.start()
        timeAnimator.setTimeListener { animation, totalTime, deltaTime ->
            if (!isGameOver_) {
                timeAfterLastMove_ += deltaTime.toFloat() / timePerOneMove_
                if (timeAfterLastMove_ >= 1F) {
                    if (deadBalls_.size > 0) {
                        deleteDeadBalls()
                    }
                    else if (maxBallsCount_ - balls_.size == 0)
                        lose()
                    if (movingBall_ != null) {
                        if (!isCellAdded) {
                            isCellAdded = true
                            val coordinates = intArrayOf(movingBall_!!.cellY_, movingBall_!!.cellX_)
                            unoccupiedCells_.add(coordinates)
                            grid_[movingBall_!!.cellY_][movingBall_!!.cellX_] = 0
                        }
                        moveBall(movingBall_!!)
                    } else if (isCellAdded)
                        isCellAdded = false
                    timeAfterLastMove_ = 0F
                }
            }
        }
    }

    /**
     * Metóda setLayoutConfigurations() vykoná všetky bežné nastavenia rozloženia.
     */

    private fun setLayoutConfigurations() {
        //setContentView(R.layout.activity_main)
        binding_ = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding_.root)
        playerScoreTextView_ = binding_.tvPlayerScore
        playerTopScoreTextView_ = binding_.tvPlayerTopScore
        playerScoreTextView_.text = score_.toString()
        topScore_ = sharedPref_.getInt(getString(R.string.top_score_key), 0)
        playerTopScoreTextView_.text = topScore_.toString()
        restartButton_ = binding_.bRestart
        clearButton_ = binding_.bClear
        exitButton_ = binding_.bExit
        settingsButton_ = binding_.bSettings
        if (!this::gridLayout_.isInitialized) {
            gridLayout_ = binding_.tlGrid
        }
        else {
            val newGridLayout = binding_.tlGrid
            makeCopyOfGrid(newGridLayout)
            gridLayout_ = newGridLayout
        }
        exitButton_.setOnClickListener {
            playButtonClickSound()
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.exit))
            builder.setMessage(getString(R.string.warning_before_leaving))
            builder.setPositiveButton(getString(R.string.yes)) { dialog, which ->
                playButtonClickSound()
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                onDestroy()
            }
            builder.setNegativeButton(getString(R.string.no)) { dialog, which ->
                playButtonClickSound()
            }
            builder.show()
        }
        restartButton_.setOnClickListener {
            playButtonClickSound()
            restart()
        }
        settingsButton_.setOnClickListener {
            playButtonClickSound()
            intent = Intent(this, SettingsActivity::class.java)
            when (timePerOneMove_) {
                40f -> intent.putExtra("gameSpeed", 3)
                70f -> intent.putExtra("gameSpeed", 2)
                120f -> intent.putExtra("gameSpeed", 1)
            }
            intent.putExtra("isMusicTurnedOff", isMusicTurnedOff_)
            intent.putExtra("isSfxTurnedOff", isSfxTurnedOff_)
            intent.putExtra("backgroundMusicPlayerPosition", mediaPlayers_["backgroundMusicPlayer"]!!.currentPosition)
            startActivity(intent)
            for ((mediaPlayer, name) in mediaPlayers_.values.zip( mediaPlayers_.keys)) {
                if (name != "buttonClickSoundPlayer")
                    mediaPlayer.release()
                else
                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.release()
                    }
            }
            mediaPlayers_.clear()
        }
        clearButton_.setOnClickListener {
            playButtonClickSound()
            if (topScore_ != 0) {
                with (sharedPref_.edit()) {
                    putInt(getString(R.string.top_score_key), 0)
                    apply()
                }
                topScore_ = 0
                playerTopScoreTextView_.text = topScore_.toString()
            }
        }
    }

    /**
     * Metóda generateBall() vytvorí jednu guľu na mriežke,
     * ktorá bude mať náhodnú farbu a bude umiestnená na náhodnom voľnom mieste.
     */

    private fun generateBall() {
        if (balls_.size < maxBallsCount_) {
            val choice = Random.nextInt(0, unoccupiedCells_.size)
            val coordinates = unoccupiedCells_[choice]
            val cellY = coordinates[0]
            val cellX = coordinates[1]
            val row = gridLayout_.getChildAt(cellY) as LinearLayout
            val col = row.getChildAt(cellX) as FrameLayout
            val ballImageView = ImageView(this)
            ballImageView.adjustViewBounds = true
            ballImageView.setPadding(4)
            val color = Random.nextInt(0, 7) + 1
            when (color) {
                1 -> ballImageView.setImageResource(R.drawable.black_ball)
                2 -> ballImageView.setImageResource(R.drawable.blue_ball)
                3 -> ballImageView.setImageResource(R.drawable.brown_ball)
                4 -> ballImageView.setImageResource(R.drawable.green_ball)
                5 -> ballImageView.setImageResource(R.drawable.pink_ball)
                6 -> ballImageView.setImageResource(R.drawable.red_ball)
                7 -> ballImageView.setImageResource(R.drawable.yellow_ball)
            }
            ballImageView.id = View.generateViewId()
            col.addView(ballImageView)
            grid_[cellY][cellX] = color
            val ball = Ball(cellX, cellY, color, col.id)
            balls_.add(ball)
            unoccupiedCells_.removeAt(choice)
            checkLine(ball)
        }
    }

    /**
     * Metóda deleteDeadBalls() odstráni guličky,
     * ktoré boli umiestnené v rade.
     */

    private fun deleteDeadBalls() {
        for (deadBall in deadBalls_) {
            val col : FrameLayout = gridLayout_.findViewById(deadBall.frameLayoutId_)
            col.removeViewAt(1)
            col.removeViewAt(1)
            balls_.removeAll { x -> x == deadBall }
            val coordinates = intArrayOf(deadBall.cellY_, deadBall.cellX_)
            unoccupiedCells_.add(coordinates)
            grid_[deadBall.cellY_][deadBall.cellX_] = 0
        }
        deadBalls_.clear()
    }

    /**
     * Metóda checkLine() po pohybe guličky kontroluje, či je ona v rade s ostatnými.
     * Ak áno, pridá hráčovi body a pridá guličky z radu do zoznamu guličiek,
     * ktoré sa majú odstrániť.
     * @param ball posledná pohyblivá guľa
     * @return true ak gulička je v rade s 5 guliek
     */

    private fun checkLine(ball: Ball): Boolean {
        var result = false
        var left = 0
        var right = 0
        var up = 0
        var down = 0
        var k = 1
        while (ball.cellY_ >= k && grid_[ball.cellY_ - k][ball.cellX_] == ball.color_) {
            up++
            k++
        }
        k = 1
        while (ball.cellY_ < grid_.size - k && grid_[ball.cellY_ + k][ball.cellX_] == ball.color_) {
            down++
            k++
        }
        if (up + down < 4) {
            k = 1
            while (ball.cellX_ >= k && grid_[ball.cellY_][ball.cellX_ - k] == ball.color_) {
                left++
                k++
            }
            k = 1
            while (ball.cellX_ < grid_[0].size - k && grid_[ball.cellY_][ball.cellX_ + k] == ball.color_) {
                right++
                k++
            }
        }
        if (left + right >= 4) {
            score_ += 50
            for (i in 1..left + right - 4) {
                score_ += 10 + 10 * i
            }
            deadBalls_.add(ball)
            playerScoreTextView_.text = score_.toString()
            playDestroyingBallsSound()
            if (left > 0) {
                for (i in 1..left) {
                    for (ball2 in balls_) {
                        if (ball2.cellY_ == ball.cellY_ && ball2.cellX_ == ball.cellX_ - i) {
                            deadBalls_.add(ball2)
                            break
                        }
                    }
                }
            }
            if (right > 0) {
                for (i in 1..right) {
                    for (ball2 in balls_) {
                        if (ball2.cellY_ == ball.cellY_ && ball2.cellX_ == ball.cellX_ + i) {
                            deadBalls_.add(ball2)
                            break
                        }
                    }
                }
            }
            result = true
        } else if (up + down >= 4) {
            score_ += 50
            for (i in 1..up + down - 4) {
                score_ += 10 + 10 * i
            }
            deadBalls_.add(ball)
            playerScoreTextView_.text = score_.toString()
            playDestroyingBallsSound()
            if (up > 0) {
                for (i in 1..up) {
                    for (ball2 in balls_) {
                        if (ball2.cellY_ == ball.cellY_ - i && ball2.cellX_ == ball.cellX_) {
                            deadBalls_.add(ball2)
                            break
                        }
                    }
                }
            }
            if (down > 0) {
                for (i in 1..down) {
                    for (ball2 in balls_) {
                        if (ball2.cellY_ == ball.cellY_ + i && ball2.cellX_ == ball.cellX_) {
                            deadBalls_.add(ball2)
                            break
                        }
                    }
                }
            }
            result = true
        }
        if (result)
            addRedFrame()
        return result
    }

    /**
     * Metóda addRedFrame() pridá červené rámy guličkam, ktoré budú zničené
     */

    private fun addRedFrame() {
        for (deadBall in deadBalls_) {
            val col : FrameLayout = gridLayout_.findViewById(deadBall.frameLayoutId_)
            val frameImageView = ImageView(this)
            frameImageView.setImageResource(R.drawable.frame2)
            frameImageView.adjustViewBounds = true
            col.addView(frameImageView, 1)
        }
    }

    /**
     * Metóda ballIsSelected(), keď hráč klikne na guličku,
     * pridá ju do rámca a zapamätá si ju (ak ešte nebola vybratá),
     * alebo odstráni rámec (ak už bola táto gulička vybraná)
     * @param col pole s guličkou, na ktore hráč klikol
     * @return true ak pole obsahovalo guličku
     */

    private fun ballIsSelected(col : FrameLayout) : Boolean {
        var result = false
        if (col.size == 2) {
            for ((i, ball) in balls_.withIndex()) {
                if (ball.frameLayoutId_ == col.id) {
                    ball.isSelected_ = true
                    val frameImageView = ImageView(this)
                    frameImageView.setImageResource(R.drawable.frame)
                    frameImageView.adjustViewBounds = true
                    selectedBallIndex_ = i
                    result = true
                    col.addView(frameImageView, 1)
                    playBallClickSound()
                }
                else if (ball.isSelected_) {
                    ball.isSelected_ = false
                    val frameLayoutId = ball.frameLayoutId_
                    gridLayout_.findViewById<FrameLayout>(frameLayoutId).removeViewAt(1)
                }
            }
        }
        else if (col.size == 3) {
            for (ball in balls_) {
                if (ball.frameLayoutId_ == col.id) {
                    if (ball.isSelected_) {
                        ball.isSelected_ = false
                        selectedBallIndex_ = -1
                        col.removeViewAt(1)
                        result = true
                    }
                    playBallClickSound()
                    break
                }
            }
        }
        return result
    }

    /**
     * Metóda cellIsSelected() po kliknutí hráča na voľné pole spôsobí,
     * že sa vybraná guľa (ak existuje) pohne smerom k nemu (ak guľa nie je zakrytá inými).
     * @param cellY súradnice poľa y
     * @param cellX súradnice poľa x
     */

    private fun cellIsSelected(cellY: Int, cellX: Int) {
        if (selectedBallIndex_ != -1) {
            val selectedBall = balls_[selectedBallIndex_]
            if (selectedBall.isSelected_ && !selectedBall.isMoving_ && (selectedBall.cellX_ != cellX || selectedBall.cellY_ != cellY)) {
                if (grid_[cellY][cellX] == 0) {
                    selectedBall.prepareToMove(cellX, cellY, graph_.makeGraph(grid_, selectedBall.cellX_, selectedBall.cellY_))
                    selectedBallIndex_ = -1
                    gridLayout_.findViewById<FrameLayout>(selectedBall.frameLayoutId_).removeViewAt(1)
                    if (selectedBall.isMoving_) {
                        movingBall_ = selectedBall
                        timeAfterLastMove_ = 1F
                    }
                }
            }
        }
    }

    /**
     * Metóda nextStep() vytvorí nové guličky (ak je pre nych voľné miesto v mriežke,
     * inak koniec hry).
     */

    private fun nextStep() {
        val remainingBalls: Int = maxBallsCount_ - balls_.size
        if (remainingBalls > 3)
            for (i in 0..2) {
                generateBall()
            }
        else {
            for (i in 0 until remainingBalls) {
                generateBall()
            }
        }
    }

    /**
     * Metóda lose() zobrazí na obrazovke titulok, ktorý hráča upozorní, že prehral.
     * Ak hráč dosiahol rekordný počet bodov, uloží ich do súboru.
     * Zablokuje tiež všetko okrem tlačidiel na použitie.
     */

    private fun lose() {
        val loseLabel = findViewById<LinearLayout>(R.id.ll_lose_panel)
        loseLabel.visibility = View.VISIBLE
        isGameOver_ = true
        if (score_ > topScore_) {
            topScore_ = score_
            with (sharedPref_.edit()) {
                putInt(getString(R.string.top_score_key), topScore_)
                apply()
            }
        }
    }

    /**
     * Metóda restart() sa volá keď sa stlačí tlačidlo Reštartovať. Začne hru od začiatku.
     * Vymaže staré guličky a vytvory nové.
     */

    private fun restart() {
        for (ball in balls_) {
            unoccupiedCells_.add(intArrayOf(ball.cellY_ , ball.cellX_))
            grid_[ball.cellY_][ball.cellX_] = 0
            val col = gridLayout_.findViewById<FrameLayout>(ball.frameLayoutId_)
            while (col.size > 1)
                col.removeViewAt(1)
        }
        balls_.clear()
        selectedBallIndex_ = -1
        score_ = 0
        playerScoreTextView_.text = score_.toString()
        playerTopScoreTextView_.text = topScore_.toString()
        isGameOver_ = false
        val linearLayout = findViewById<LinearLayout>(R.id.ll_lose_panel)
        linearLayout.visibility = View.GONE
        for (i in 1..5)
            generateBall()
    }

    /**
     * Metóda createGrid() vytvorí hracie pole
     */

    private fun createGrid() {
        for (y in 0..8) {
            val rowLayoutGrid = TableRow(this)
            val tableLayoutParams = TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                0
            )
            tableLayoutParams.weight = 1F
            rowLayoutGrid.layoutParams = tableLayoutParams
            for (x in 0..8) {
                unoccupiedCells_.add(intArrayOf(y , x))
                val frameLayoutGrid = FrameLayout(this)
                val gridImageView = ImageView(this)
                gridImageView.setImageResource(R.drawable.cell)
                val tableRowParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.MATCH_PARENT
                )
                tableRowParams.weight = 1F
                frameLayoutGrid.layoutParams = tableRowParams
                frameLayoutGrid.id = View.generateViewId()
                gridImageView.adjustViewBounds = true
                frameLayoutGrid.addView(gridImageView)
                rowLayoutGrid.addView(frameLayoutGrid)
                grid_[y][x] = 0
                frameLayoutGrid.setOnClickListener {
                    if (!isGameOver_) {
                        if (movingBall_ == null) {
                            if (!ballIsSelected(frameLayoutGrid))
                                cellIsSelected(y, x)
                        }
                    }
                }
            }
            gridLayout_.addView(rowLayoutGrid)
        }
    }

    /**
     * Metóda moveBall() urobí jeden pohyb guličkou. Presunie obrázok na ďalšie políčko.
     * @param movingBall pohyblivá gulička
     */

    private fun moveBall(movingBall : Ball) {
        movingBall.move()
        val ballImageView = gridLayout_.findViewById<FrameLayout>(movingBall.frameLayoutId_).getChildAt(1) as ImageView
        gridLayout_.findViewById<FrameLayout>(movingBall.frameLayoutId_).removeViewAt(1)
        val row = gridLayout_.getChildAt(movingBall.cellY_) as LinearLayout
        val col = row.getChildAt(movingBall.cellX_) as FrameLayout
        col.addView(ballImageView)
        movingBall.frameLayoutId_ = col.id
        playMovingBallSound()
        if (!movingBall.isMoving_) {
            selectedBallIndex_ = -1
            grid_[movingBall.cellY_][movingBall.cellX_] = movingBall.color_
            unoccupiedCells_.removeAll { x -> x[0] == movingBall.cellY_ &&  x[1] == movingBall.cellX_ }
            if (!checkLine(movingBall)) {
                nextStep()
            }
            movingBall_ = null
            return
        }
    }

    /**
     * Metóda playBackgroundMusic() spustí hudbu na pozadí.
     */

    private fun playBackgroundMusic() {
        if (!isMusicTurnedOff_) {
            mediaPlayers_["backgroundMusicPlayer"]?.start()
        }
    }

    /**
     * Metóda pauseBackgroundMusic() zastaví hudbu na pozadí.
     */

    private fun pauseBackgroundMusic() {
        if (mediaPlayers_.size > 0)
            mediaPlayers_["backgroundMusicPlayer"]?.pause()
    }

    /**
     * Metóda playBallClickSound() spustí zvuk kliknutia na guličku.
     */

    private fun playBallClickSound() {
        if (!isSfxTurnedOff_) {
            mediaPlayers_["ballClickSoundPlayer"]?.seekTo(0)
            mediaPlayers_["ballClickSoundPlayer"]?.start()
        }
    }

    /**
     * Metóda playDestroyingBallsSound() spustí zvuk ničenia guliček.
     */

    private fun playDestroyingBallsSound() {
        if (!isSfxTurnedOff_) {
            mediaPlayers_["destroyingBallsSoundPlayer"]?.seekTo(0)
            mediaPlayers_["destroyingBallsSoundPlayer"]?.start()
        }
    }

    /**
     * Metóda playMovingBallSound() spustí zvuk pohybu guličky.
     */

    private fun playMovingBallSound() {
        if (!isSfxTurnedOff_) {
            mediaPlayers_["movingBallSoundPlayer"]?.seekTo(0)
            mediaPlayers_["movingBallSoundPlayer"]?.start()
        }
    }

    /**
     * Metóda playButtonClickSound() spustí zvuk kliknutia na tlačidlo.
     */

    private fun playButtonClickSound() {
        if (!isSfxTurnedOff_) {
            mediaPlayers_["buttonClickSoundPlayer"]?.seekTo(0)
            mediaPlayers_["buttonClickSoundPlayer"]?.start()
        }
    }

    /**
     * Metóda onBackPressed(), keď hráč stlačí tlačidlo Späť na svojom zariadení,
     * sa ho opýta, či chce hru ukončiť.
     */

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.exit))
        builder.setMessage(getString(R.string.warning_before_leaving))
        builder.setPositiveButton(getString(R.string.yes)) { dialog, which ->
            playButtonClickSound()
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            onDestroy()
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, which ->
            playButtonClickSound()
        }
        builder.show()
    }

    /**
     * Metóda onStop() sa volá, keď aktivita už nie je pre používateľa viditeľná.
     * Zastaví hudbu.
     */

    override fun onStop() {
        super.onStop()
        pauseBackgroundMusic()
    }

    /**
     * Metóda onResume() sa volá, keď aktivita začne interagovať s používateľom.
     * Zapne hudbu.
     */

    override fun onResume() {
        super.onResume()
        playBackgroundMusic()
    }

    /**
     * Metóda onDestroy() sa volá pred tym, keď aktivita sa zniči.
     * Zastavi hudbu.
     */

    override fun onDestroy() {
        super.onDestroy()
        for (mediaPlayer in mediaPlayers_.values) {
            mediaPlayer.release()
        }
        mediaPlayers_.clear()
        finish()
    }

    /**
     * Metóda makeCopyOfGrid() vytvára kópiu hracieho poľa.
     * Používa sa na obnovenie hernej mriežky po otočení obrazovky zariadenia.
     * @param newGrid nová tabuľka
     */

    private fun makeCopyOfGrid(newGrid : TableLayout) {
        for (y in 0 until gridLayout_.size) {
            val row = gridLayout_.getChildAt(y) as TableRow
            val newRow = TableRow(this)
            newRow.layoutParams = row.layoutParams
            for (x in 0 until row.size) {
                val col = row.getChildAt(x) as FrameLayout
                val newCol = FrameLayout(this)
                newCol.layoutParams = col.layoutParams
                newCol.id = col.id
                for (i in 0 until col.size) {
                    val imageView = col.getChildAt(i) as ImageView
                    val newImageView = ImageView(this)
                    newImageView.setImageDrawable(imageView.drawable)
                    newImageView.layoutParams = imageView.layoutParams
                    newImageView.adjustViewBounds = true
                    newCol.addView(newImageView)
                }
                newCol.setOnClickListener {
                    if (!isGameOver_) {
                        if (movingBall_ == null) {
                            if (!ballIsSelected(newCol))
                                cellIsSelected(y, x)
                        }
                    }
                }
                newRow.addView(newCol)
            }
            newGrid.addView(newRow)
        }
        gridLayout_.removeAllViews()
    }

    /**
     * Metóda onConfigurationChanged() volá sa keď používateľ otočí obrazovku zariadenia.
     * Nastaví nové rozloženia pre danú orientáciu zariadenia.
     * @param newConfig nová orientácia zariadenia
     */

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setLayoutConfigurations()
        if (isGameOver_){
            val loseLabel = findViewById<LinearLayout>(R.id.ll_lose_panel)
            loseLabel.visibility = View.VISIBLE
        }
    }
}