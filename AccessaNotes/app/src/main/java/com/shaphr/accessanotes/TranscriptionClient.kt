package com.shaphr.accessanotes

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val transcription: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private val filePath: String by lazy {
        context.getExternalFilesDir(null)?.absolutePath + "/androidMIC.mp3"
    }

    fun startRecording() {
        println("Starting recording...")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(filePath)
            prepare()
            start()
        }
    }

    suspend fun stopRecording() {
        println("Stopping recording...")
        mediaRecorder?.apply {
            stop()
            delay(1000)
            release()
        }
        callWhisper()
    }

    private suspend fun callWhisper() {
        println("Calling Whisper API...")
        val audioFile = File(filePath)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "recording.mp3", RequestBody.create("audio/mpeg".toMediaTypeOrNull(), audioFile))
            .addFormDataPart("model", "whisper-1")
            .addFormDataPart("prompt", "The transcript is about OpenAI which makes technology like DALL·E, GPT-3, and ChatGPT with the hope of one day building an AGI system that benefits all of humanity.")
            .build()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/audio/transcriptions")
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        val response = client.newCall(request).await()
        if (response.isSuccessful) {
            val resultText = response.body?.string() ?: ""
            if (resultText == "") {
                println("Error with calling Whisper: empty response body")
            }

            val json = JSONObject(resultText)
            val text = json.optString("text", "")
            transcription.emit(text)
            Log.d("TEST", resultText)
        } else {
            // Handle error response
            println("Error with calling Whisper: status not 200")
        }

    }

    private fun playRecording() {
        println("Playing recording...")
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////

// FOR STREAMING REAL-TIME AUDIO AND LIVE TRANSCRIPTION (PENDING MEETING WITH DEEPGRAM DEVS)

//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import android.media.AudioFormat
//import android.media.AudioRecord
//import android.media.MediaRecorder
//import android.widget.Toast
//import androidx.core.app.ActivityCompat
//import com.neovisionaries.ws.client.WebSocket
//import com.neovisionaries.ws.client.WebSocketAdapter
//import com.neovisionaries.ws.client.WebSocketFactory
//import com.neovisionaries.ws.client.WebSocketFrame
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//
//class TranscriptionClient(private val context: Context) {
//    private var audioRecord: AudioRecord? = null
//    private val bufferSize: Int
//    private val buffer: ShortArray
//    private var webSocket: WebSocket? = null
//
//    init {
//        val sampleRateInHz = 44100
//        val channelConfig = AudioFormat.CHANNEL_IN_MONO
//        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
//
//        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
//        buffer = ShortArray(bufferSize)
//
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(context, "Permission denied, can't record audio", Toast.LENGTH_SHORT).show()
//        }
//        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSize)
//    }
//
//    fun startRecording() {
//        println("Starting recording...")
//        audioRecord?.startRecording()
//
//        Thread {
//            try {
//                webSocket = WebSocketFactory().createSocket("wss://api.deepgram.com/v1/listen?model=nova&version=latest&punctuate=true&numerals=true&smart_format=true&interim_results=false&token=97b0346515cef2ffbc1f77ade14bf26a18c5c632")
//                webSocket?.addListener(object : WebSocketAdapter() {
//                    override fun onConnected(websocket: WebSocket?, headers: Map<String, List<String>>?) {
//                        println("Connected to Deepgram WebSocket")
//                        val byteBuffer = ByteBuffer.allocate(bufferSize * 2)
//                        while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
//                            println("Sending audio data...")
//                            val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
//                            if (read > 0) {
//                                byteBuffer.asShortBuffer().put(buffer, 0, read)
//                                websocket?.sendBinary(byteBuffer.array())
//                                byteBuffer.clear()
//                            }
//                        }
//                    }
//
//                    override fun onTextMessage(websocket: WebSocket?, text: String?) {
//                        // Handle the received transcript
//                        println("Received transcript: $text")
//                        val transcript = parseTranscript(text)
//                        // do something with the transcript...
//                    }
//
//                    override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
//                        // Handle WebSocket disconnection...
//                    }
//                })
//
//                webSocket?.connect()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }.start()
//    }
//
//    fun stopRecording() {
//        println("Stopping recording...")
//        audioRecord?.stop()
//        audioRecord?.release()
//        audioRecord = null
//
//        // Close WebSocket when you're finished
//        webSocket?.disconnect()
//        webSocket = null
//    }
//
//    private fun parseTranscript(text: String?): String {
//        // Parse the transcript from the text
//        return text ?: ""
//    }
//
//    private fun ShortArray.toByteArray(): ByteArray {
//        val byteBuffer = ByteBuffer.allocate(this.size * 2)
//        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
//        byteBuffer.asShortBuffer().put(this)
//        return byteBuffer.array()
//    }
//}
