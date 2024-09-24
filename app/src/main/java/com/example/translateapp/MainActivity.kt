package com.example.translateapp

import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.translateapp.ui.theme.TranslateAppTheme

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TranslateAppTheme {
                SpeechToTextApp()
            }
        }
    }
}

@Composable
fun SpeechToTextApp() {
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }
    val textToSpeech = remember { mutableStateOf<TextToSpeech?>(null) }
    val speechRecognizer = remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, initialize SpeechRecognizer
                speechRecognizer.value = SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Check and request permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            speechRecognizer.value = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    // Initialize TextToSpeech
    LaunchedEffect(Unit) {
        textToSpeech.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.value!!.setLanguage(Locale.getDefault())

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(context, "Language not supported", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Initialization failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    startSpeechToText(context, speechRecognizer.value) { text ->
                        recognizedText = text
                    }
                }
            ) {
                Text("Start Speech-to-Text")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (recognizedText.isNotEmpty()) {
                        speakText(context, textToSpeech.value, recognizedText)
                        isSpeaking = true
                    } else {
                        Toast.makeText(context, "Please say something first", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                enabled = recognizedText.isNotEmpty() && !isSpeaking
            ) {
                Text("Start Text-to-Speech")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Recognized Text: $recognizedText")
        }
    }
}

fun startSpeechToText(context: Context, speechRecognizer: SpeechRecognizer?, onResult: (String) -> Unit) {
    if (speechRecognizer == null) {
        Toast.makeText(context, "SpeechRecognizer is not initialized", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak...")
    }

    val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Toast.makeText(context, "Listening...", Toast.LENGTH_SHORT).show()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Toast.makeText(context, "Processing...", Toast.LENGTH_SHORT).show()
        }

        override fun onError(error: Int) {
            Toast.makeText(context, "Error recognizing speech", Toast.LENGTH_SHORT).show()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0]) // Pass recognized text back via callback
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    // Assign listener and start listening
    speechRecognizer.setRecognitionListener(recognitionListener)
    speechRecognizer.startListening(intent)
}

fun speakText(context: Context, textToSpeech: TextToSpeech?, text: String) {
    if (textToSpeech == null) {
        Toast.makeText(context, "TextToSpeech is not initialized", Toast.LENGTH_SHORT).show()
        return
    }
    if (textToSpeech.isSpeaking) {
        textToSpeech.stop()  // Stop any ongoing speech
    }

    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TranslateAppTheme {
        SpeechToTextApp()
    }
}