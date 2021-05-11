package com.chattycheese.voicetrainer

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.main_layout.*
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(),TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var buttonSpeak: Button? = null
    private var buttonRecord: Button? = null
    private var buttonPlay: Button? = null
    private var buttonDelete: Button? = null
    private var buttonSubmit: Button? = null
    private var textView: TextView? = null
    private var filename: String? = null
    private var retrofit: Retrofit? = null
    private var service: TrainingDataService? = null

    private var startButton: Boolean? = null
    private var playButton: Boolean? = null


    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        ActivityCompat.requestPermissions(
            this,
            permissions,
            REQUEST_RECORD_AUDIO_PERMISSION
        )

        buttonSpeak = this.button_speak
        buttonRecord = this.button_record
        buttonPlay = this.button_play
        buttonDelete = this.button_delete
        buttonSubmit = this.button_submit
        //textView = this.textView
        textView = findViewById(R.id.text_view_id)
        startButton = true
        playButton = true

        buttonSpeak!!.isEnabled = false
        tts = TextToSpeech(this, this)

        retrofit = Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                //TODO: vv
            .baseUrl("<<INSERT DESTINATION ADDRESS FOR DATA SERVICE HERE>>")
            .build()
        service = retrofit!!.create<TrainingDataService>(TrainingDataService::class.java)

        buttonSpeak!!.setOnClickListener { speakOut() }
        buttonRecord!!.setOnClickListener {
            if (startButton == true) {
                MediaHandler.startRecording(applicationContext);
                buttonRecord!!.setText("stop")
                startButton = false
            } else {
                MediaHandler.stopRecording();
                buttonRecord!!.setText("record")
                filename = MediaHandler.saveRecording(applicationContext)
                startButton = true
                //play/delete button visibility
                this.button_delete.visibility = View.VISIBLE
                this.button_play.visibility = View.VISIBLE
                this.button_record.visibility = View.GONE
                this.button_play.performClick()

            }
        }
        buttonPlay!!.setOnClickListener{
            if (playButton == true) {
                MediaHandler.startPlaying(applicationContext, filename);
                this.button_submit.visibility = View.VISIBLE
                buttonPlay!!.setText("stop")
                playButton = false
            } else {
                MediaHandler.stopPlaying();
                buttonPlay!!.setText("play")
                playButton = true
            }

        }
        buttonDelete!!.setOnClickListener{
            MediaHandler.deleteSavedRecording(applicationContext, filename)
            playButton = true
            this.button_delete.visibility = View.GONE
            this.button_play.visibility = View.GONE
            this.button_record.visibility = View.VISIBLE
        }
        buttonSubmit!!.setOnClickListener{
            val file = File(applicationContext.getFilesDir().getAbsolutePath() + "/" + filename + ".mp4")

            val filePart: RequestBody = RequestBody.create(
                MediaType.parse("*/*"),
                file
            )
            val audioTxt: RequestBody = RequestBody.create(
                MediaType.parse("text/*"),
                textView!!.text.toString()
            )

            val call: Call<ResponseBody> = service!!.submitAudio(
                filePart,
                audioTxt
            )

            call.enqueue(object : Callback, retrofit2.Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: retrofit2.Response<ResponseBody>
                ) {
                    try {
                        val text = response.body()?.string()
                        println(text)
                        //textView?.text = text
                        MediaHandler.deleteSavedRecording(applicationContext, filename)
                        playButton = true
                        buttonDelete!!.visibility = View.GONE
                        buttonPlay!!.visibility = View.GONE
                        buttonSubmit!!.visibility = View.GONE
                        buttonRecord!!.visibility = View.VISIBLE
                        getTrainingText()

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    t.printStackTrace()
                }

                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    try {
                        val text = response.body()?.string()
                        println(text)
                        //textView?.text = text
                        MediaHandler.deleteSavedRecording(applicationContext, filename)
                        playButton = true
                        buttonDelete!!.visibility = View.GONE
                        buttonPlay!!.visibility = View.GONE
                        buttonSubmit!!.visibility = View.GONE
                        buttonRecord!!.visibility = View.VISIBLE
                        getTrainingText()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    override fun onInit(status: Int) {
        getTrainingText()
        //textView?.setText(getTrainingText())
        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                buttonSpeak!!.isEnabled = true
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    private fun getTrainingText() {

        val training = service!!.trainingText
        training.enqueue(object : Callback, retrofit2.Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: retrofit2.Response<ResponseBody>
            ) {
                try {
                    val text = response.body()?.string()
                    println(text)
                    textView?.text = text
                    speakOut()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                try {
                    val text = response.body()?.string()
                    println(text)
                    textView?.text = text
                    speakOut()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun speakOut() {
        val text = textView!!.text.toString()
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

}
