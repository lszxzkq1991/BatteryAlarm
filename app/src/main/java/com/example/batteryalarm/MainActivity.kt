package com.example.batteryalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.IllegalArgumentException
import kotlin.concurrent.thread
import kotlin.coroutines.coroutineContext

class BatteryReceiver : BroadcastReceiver() {

    var audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    var mSoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(audioAttributes)
        .build()

    var soundId = 0
    var streamId = 0
    var threshold = 0
    var triggered = false
    var soundReady = false

    override fun onReceive(context: Context, intent: Intent) {
        val cur = intent.getIntExtra("level", 0)
        val total = intent.getIntExtra("scale", 1)
        val percent = cur * 1.0 / total

        if ((percent * 100) <= threshold) {
            Log.d("Battery Receiver", "value: $percent * 100, $threshold")
            if (!triggered) {
                thread {
                    alarm()
                }
                triggered = true
            }
        }
    }

    private fun alarm()
    {
        while (!soundReady)
        {
            sleep(500)
        }

        streamId = mSoundPool.play(soundId, 1F, 1F, 0, -1, 1F)
        Log.d("Battery Receiver", "Sound Played")
    }

    fun setAlarmThreshold(percent: Int)
    {
        threshold = percent
        Log.d("Battery Receiver", "Set Threshold to $percent")
    }

    fun loadSound(fd: AssetFileDescriptor) {
        soundReady = false
        soundId = mSoundPool.load(fd, 1)
        mSoundPool.setOnLoadCompleteListener { soundPool: SoundPool, soundId: Int, status: Int ->
            if (status == 0) {
                soundReady = true
                Log.d("Battery Receiver", "Sound Ready")
            }
        }
        Log.d("Battery Receiver", "Loaded Sound ID $soundId")
    }

    fun stop() {
        mSoundPool.stop(streamId)
        triggered = false
    }
}

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val receiver = BatteryReceiver()

        batteryStart.setOnClickListener {
            receiver.stop()

            val fd = assets.openFd("test.mp3")
            receiver.loadSound(fd)

            val percent = batteryPercent.text.toString().toInt()
            receiver.setAlarmThreshold(percent)

            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            registerReceiver(receiver, filter)

            Toast.makeText(this, "Battery Alarm Registered!", Toast.LENGTH_SHORT).show()
        }

        batteryStop.setOnClickListener {
            try {
                unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.d("Main Activity", "Battery Receiver Not Registered")
            }
            receiver.stop()
        }
    }

}