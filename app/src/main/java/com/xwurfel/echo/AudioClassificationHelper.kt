package com.xwurfel.echo

import android.content.Context
import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import org.tensorflow.lite.task.core.BaseOptions
import java.lang.ref.WeakReference
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface AudioClassificationListener {
    fun onError(error: String)
    fun onResult(results: List<Category>, inferenceTime: Long)
}

class AudioClassificationHelper(
    contextRef: Context,
    private val listener: AudioClassificationListener,
    private var currentModel: String = YAMNET_MODEL,
    private var classificationThreshold: Float = DISPLAY_THRESHOLD,
    private var overlap: Float = DEFAULT_OVERLAP_VALUE,
    private var numOfResults: Int = DEFAULT_NUM_OF_RESULTS,
    private var currentDelegate: Int = 0,
    private var numThreads: Int = 2
) {
    private lateinit var classifier: AudioClassifier
    private lateinit var tensorAudio: TensorAudio
    private lateinit var recorder: AudioRecord
    private lateinit var executor: ScheduledThreadPoolExecutor

    private val context = WeakReference(contextRef)

    private val classifyRunnable = Runnable {
        classifyAudio()
    }

    init {
        initClassifier()
    }

    fun initClassifier() {
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
            }

            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setScoreThreshold(classificationThreshold).setMaxResults(numOfResults)
            .setBaseOptions(baseOptionsBuilder.build()).build()

        try {
            context.get()?.let {
                classifier = AudioClassifier.createFromFileAndOptions(it, currentModel, options)
                tensorAudio = classifier.createInputTensorAudio()
                recorder = classifier.createAudioRecord()
                startAudioClassification()
            } ?: throw IllegalStateException("Context is null")
        } catch (e: IllegalStateException) {
            listener.onError(
                "Audio Classifier failed to initialize. See error logs for details"
            )
            Log.e("AudioClassification", "TFLite failed to load with error: " + e.message)
        }
    }

    fun startAudioClassification() {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        recorder.startRecording()
        executor = ScheduledThreadPoolExecutor(1)

        val lengthInMilliSeconds =
            ((classifier.requiredInputBufferSize * 1.0f) / classifier.requiredTensorAudioFormat.sampleRate) * 1000

        val interval = (lengthInMilliSeconds * (1 - overlap)).toLong()

        executor.scheduleWithFixedDelay(
            classifyRunnable, 0, interval, TimeUnit.MILLISECONDS
        )
    }

    private fun classifyAudio() {
        tensorAudio.load(recorder)
        var inferenceTime = SystemClock.uptimeMillis()
        val output = classifier.classify(tensorAudio)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        listener.onResult(output[0].categories, inferenceTime)
    }

    fun stopAudioClassification() {
        recorder.stop()
        executor.shutdownNow()
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_NNAPI = 1
        const val DISPLAY_THRESHOLD = 0.3f
        const val DEFAULT_NUM_OF_RESULTS = 2
        const val DEFAULT_OVERLAP_VALUE = 0.5f
        const val YAMNET_MODEL = "yamnet.tflite"
        const val SPEECH_COMMAND_MODEL = "speech.tflite"
    }
}
