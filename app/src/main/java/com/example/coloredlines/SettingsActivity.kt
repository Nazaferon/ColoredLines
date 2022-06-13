package com.example.coloredlines

/**
 * Toto je aktivita pre nastavenie hry.
 * @author Ruslan Hlazkov
 * @version 13.06.2022
 */

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.coloredlines.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var mediaPlayers_ : HashMap<String, MediaPlayer>
    private lateinit var musicSwitchCompat_ : SwitchCompat
    private lateinit var sfxSwitchCompat_ : SwitchCompat
    private lateinit var gameSpeedSeekBar_ : SeekBar
    private lateinit var backButton_ : Button
    private lateinit var binding_: ActivitySettingsBinding

    /**
     * Metoda onCreate() sa volá  pri prvom vytvorení aktivity.
     * Ide o metóde, v ktorej program vykoná všetky bežné statické nastavenia.
     * @param savedInstanceState balík obsahujúci predtým zmrazený stav aktivity, ak nejaký existoval.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        binding_ = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding_.root)

        mediaPlayers_ = HashMap()
        val buttonClickSoundPlayer = MediaPlayer.create(this, R.raw.button_click_sound)
        buttonClickSoundPlayer.setVolume(1f, 1f)
        val backgroundMusicPlayer = MediaPlayer.create(this, R.raw.background_music)
        backgroundMusicPlayer.setVolume(0.2f, 0.2f)
        backgroundMusicPlayer.isLooping = true
        mediaPlayers_["backgroundMusicPlayer"] = backgroundMusicPlayer
        mediaPlayers_["buttonClickSoundPlayer"] = buttonClickSoundPlayer
        musicSwitchCompat_ = binding_.scMusicSettings
        sfxSwitchCompat_ = binding_.scSfxSettings
        gameSpeedSeekBar_ = binding_.sbGameSpeedSettings
        backButton_ = binding_.bBack
        val bundle :Bundle? = intent.extras
        if (bundle != null) {
            gameSpeedSeekBar_.progress = bundle.getInt("gameSpeed") - 1
            musicSwitchCompat_.isChecked = bundle.getBoolean("isMusicTurnedOff")
            sfxSwitchCompat_.isChecked = bundle.getBoolean("isSfxTurnedOff")
            mediaPlayers_["backgroundMusicPlayer"]!!.seekTo( bundle.getInt("backgroundMusicPlayerPosition"))
        }
        intent = Intent(this, MainActivity::class.java)
        intent.putExtra("gameSpeed", gameSpeedSeekBar_.progress + 1)
        intent.putExtra("isMusicTurnedOff",  musicSwitchCompat_.isChecked)
        intent.putExtra("isSfxTurnedOff", sfxSwitchCompat_.isChecked)
        backButton_.setOnClickListener {
            playButtonClickSound()
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
        musicSwitchCompat_.setOnCheckedChangeListener { buttonView, isChecked ->
            playButtonClickSound()
            if (isChecked) {
                intent.putExtra("isMusicTurnedOff", true)
                pauseBackgroundMusic()
            }
            else {
                intent.putExtra("isMusicTurnedOff", false)
                playBackgroundMusic()
            }
        }
        sfxSwitchCompat_.setOnCheckedChangeListener { buttonView, isChecked ->
            playButtonClickSound()
            if (isChecked) {
                intent.putExtra("isSfxTurnedOff", true)
            }
            else {
                intent.putExtra("isSfxTurnedOff", false)
            }
        }
        gameSpeedSeekBar_.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                playButtonClickSound()

            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                intent.putExtra("gameSpeed", progress + 1)
            }
        })
    }

    /**
     * Metóda playButtonClickSound() spustí zvuk kliknutia na tlačidlo.
     */

    private fun playButtonClickSound() {
        if (!sfxSwitchCompat_.isChecked) {
            mediaPlayers_["buttonClickSoundPlayer"]!!.seekTo(0)
            mediaPlayers_["buttonClickSoundPlayer"]!!.start()
        }
    }

    /**
     * Metóda playBackgroundMusic() spustí hudbu na pozadí.
     */

    private fun playBackgroundMusic() {
        if (!musicSwitchCompat_.isChecked)
            mediaPlayers_["backgroundMusicPlayer"]!!.start()
    }

    /**
     * Metóda pauseBackgroundMusic() zastaví hudbu na pozadí.
     */

    private fun pauseBackgroundMusic() {
        if (mediaPlayers_.size > 0)
            mediaPlayers_["backgroundMusicPlayer"]!!.pause()
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
}