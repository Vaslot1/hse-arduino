package com.example.voicehome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {
    private static final int SPEECH_REQUEST = 1;
    private final ExecutorService requests = Executors.newSingleThreadExecutor();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .build();
    private EditText serverAddress;
    private TextView recognized;
    private TextView status;
    private Button talk;
    private SharedPreferences settings;
    private TextToSpeech tts;
    private boolean ttsReady;
    private volatile boolean destroyed;
    private volatile Call activeCall;
    private HttpUrl recognitionServer;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        // Android 15 включает edge-to-edge: защищаем форму от системных панелей.
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        root.requestApplyInsets();
        serverAddress = findViewById(R.id.server_address);
        recognized = findViewById(R.id.recognized);
        status = findViewById(R.id.status);
        talk = findViewById(R.id.talk);
        settings = getSharedPreferences("voicehome", MODE_PRIVATE);
        serverAddress.setText(settings.getString("server", "http://192.168.1.100:8000/"));
        talk.setOnClickListener(view -> startRecognition());

        tts = new TextToSpeech(this, result -> {
            if (destroyed) return;
            if (result == TextToSpeech.SUCCESS) {
                int language = tts.setLanguage(Locale.US);
                ttsReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
                if (ttsReady) {
                    tts.setPitch(1.3f);
                    tts.setSpeechRate(0.7f);
                }
            }
            if (!ttsReady) Toast.makeText(this, R.string.tts_missing, Toast.LENGTH_LONG).show();
        });
        if (state != null) {
            recognized.setText(state.getString("recognized", getString(R.string.recognized_empty)));
            status.setText(state.getString("status", getString(R.string.ready)));
            String pending = state.getString("recognitionServer");
            if (pending != null) recognitionServer = HttpUrl.parse(pending);
        }
    }

    private void startRecognition() {
        String address = serverAddress.getText().toString().trim();
        if (!address.contains("://")) address = "http://" + address;
        HttpUrl base = HttpUrl.parse(address);
        if (base == null || !"http".equals(base.scheme()) || !"/".equals(base.encodedPath())
                || base.query() != null || base.fragment() != null || !base.username().isEmpty() || !base.password().isEmpty()) {
            serverAddress.setError(getString(R.string.invalid_address));
            return;
        }
        recognitionServer = base;
        settings.edit().putString("server", base.toString()).apply();
        if (ttsReady) tts.stop();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.recognition_prompt));
        try {
            startActivityForResult(intent, SPEECH_REQUEST);
        } catch (ActivityNotFoundException error) {
            status.setText(R.string.recognition_missing);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SPEECH_REQUEST) return;
        if (resultCode != RESULT_OK) {
            status.setText(R.string.recognition_cancelled);
            return;
        }
        ArrayList<String> results = data == null ? null : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results == null || results.isEmpty() || results.get(0) == null) {
            status.setText(R.string.recognition_empty);
            return;
        }
        String speech = results.get(0);
        recognized.setText(speech);
        List<String> commands = CommandParser.parse(speech);
        if (commands.isEmpty()) {
            status.setText(R.string.command_unknown);
            speak("I did not understand the command. Please try again.");
            return;
        }
        if (recognitionServer == null) {
            status.setText(R.string.invalid_address);
            return;
        }
        talk.setEnabled(false);
        serverAddress.setEnabled(false);
        status.setText(R.string.loading);
        HttpUrl base = recognitionServer;
        requests.execute(() -> executeCommands(base, commands));
    }

    private void executeCommands(HttpUrl base, List<String> commands) {
        StringBuilder display = new StringBuilder();
        StringBuilder spoken = new StringBuilder();
        for (String command : commands) {
            if (destroyed) return;
            Request request = new Request.Builder().url(base.newBuilder().addPathSegment(command).build()).build();
            Call call = client.newCall(request);
            activeCall = call;
            // onDestroy мог произойти между проверкой destroyed и созданием Call.
            if (destroyed) {
                call.cancel();
                return;
            }
            try (Response response = call.execute()) {
                String body = response.body() == null ? "" : response.body().string().trim();
                if (!response.isSuccessful() || body.isEmpty()) {
                    display.append(getString(R.string.http_error, response.code(), body)).append('\n');
                    spoken.append("The server could not complete the command. ");
                    break;
                }
                String answer = SpeechResponses.forCommand(command, body);
                display.append(answer).append('\n');
                spoken.append(answer).append(' ');
            } catch (IOException error) {
                if (destroyed) return;
                display.append(getString(R.string.network_error));
                spoken.append("Connection failed. Check the Raspberry Pi address, Wi-Fi and server.");
                break;
            } finally {
                activeCall = null;
            }
        }
        runOnUiThread(() -> {
            if (destroyed) return;
            status.setText(display.toString().trim());
            talk.setEnabled(true);
            serverAddress.setEnabled(true);
            // Один текст исключает путаницу и гонки между TTS-ответами разных запросов.
            speak(spoken.toString().trim());
        });
    }

    private void speak(String text) {
        if (ttsReady && !destroyed) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "answer");
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putString("recognized", recognized.getText().toString());
        // При пересоздании Activity текущий запрос отменяется.
        state.putString("status", talk.isEnabled() ? status.getText().toString() : getString(R.string.ready));
        if (recognitionServer != null) state.putString("recognitionServer", recognitionServer.toString());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        Call call = activeCall;
        if (call != null) call.cancel();
        requests.shutdownNow();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        client.connectionPool().evictAll();
        super.onDestroy();
    }
}
