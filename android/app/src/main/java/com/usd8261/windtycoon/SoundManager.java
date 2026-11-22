package com.usd8261.windtycoon;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    private static SoundManager instance;
    private final Context context;
    private MediaPlayer bgmPlayer;
    private Vibrator vibrator;
    private final ExecutorService soundExecutor = Executors.newSingleThreadExecutor();
    private boolean soundEnabled = true;

    private SoundManager(Context context) {
        this.context = context.getApplicationContext();
        try {
            vibrator = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {}
        initBgm();
    }

    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    private void initBgm() {
        try {
            int rawId = context.getResources().getIdentifier("bg_music", "raw", context.getPackageName());
            if (rawId != 0) {
                bgmPlayer = MediaPlayer.create(context, rawId);
                if (bgmPlayer != null) {
                    bgmPlayer.setLooping(true);
                    bgmPlayer.setVolume(0.45f, 0.45f);
                }
            }
        } catch (Exception ignored) {}
    }

    public void startMusic() {
        if (bgmPlayer != null && !bgmPlayer.isPlaying() && soundEnabled) {
            try {
                bgmPlayer.start();
            } catch (Exception ignored) {}
        }
    }

    public void pauseMusic() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            try {
                bgmPlayer.pause();
            } catch (Exception ignored) {}
        }
    }

    public void toggleSound() {
        soundEnabled = !soundEnabled;
        if (soundEnabled) {
            startMusic();
        } else {
            pauseMusic();
        }
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void vibrate(long millis) {
        if (!soundEnabled || vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(millis);
            }
        } catch (Exception ignored) {}
    }

    public void playSound(String soundType) {
        if (!soundEnabled) return;
        soundExecutor.execute(() -> {
            try {
                switch (soundType) {
                    case "click":
                        vibrate(15);
                        playTone(700, 35, 0.4f);
                        break;
                    case "coin":
                        vibrate(25);
                        playTone(880, 50, 0.5f);
                        playTone(1174, 80, 0.5f);
                        break;
                    case "crit":
                        vibrate(50);
                        playTone(523, 40, 0.7f);
                        playTone(659, 40, 0.8f);
                        playTone(784, 40, 0.9f);
                        playTone(1046, 120, 1.0f);
                        break;
                    case "build":
                        vibrate(35);
                        playTone(392, 60, 0.6f);
                        playTone(523, 70, 0.7f);
                        playTone(659, 100, 0.8f);
                        break;
                    case "upgrade":
                        vibrate(40);
                        playTone(587, 60, 0.6f);
                        playTone(880, 100, 0.8f);
                        break;
                    case "evolve":
                        vibrate(70);
                        playTone(440, 60, 0.7f);
                        playTone(554, 60, 0.75f);
                        playTone(659, 60, 0.85f);
                        playTone(880, 160, 1.0f);
                        break;
                    case "repair":
                        vibrate(20);
                        playTone(330, 80, 0.4f);
                        playTone(440, 80, 0.5f);
                        break;
                    case "rebirth":
                        vibrate(100);
                        int[] fan = {440, 554, 659, 880, 1108, 1318};
                        for (int f : fan) {
                            playTone(f, 70, 0.8f);
                        }
                        break;
                }
            } catch (Exception ignored) {}
        });
    }

    private void playTone(int freqHz, int durationMs, float volume) {
        try {
            int sampleRate = 22050;
            int numSamples = (sampleRate * durationMs) / 1000;
            short[] buffer = new short[numSamples];
            double angleIncrement = 2.0 * Math.PI * freqHz / sampleRate;

            for (int i = 0; i < numSamples; i++) {
                double envelope = 1.0 - ((double) i / numSamples); // smooth decay
                double sample = Math.sin(i * angleIncrement) * envelope * volume;
                buffer[i] = (short) (sample * Short.MAX_VALUE);
            }

            AudioTrack track;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(buffer.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
            } else {
                track = new AudioTrack(android.media.AudioManager.STREAM_MUSIC,
                        sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, buffer.length * 2,
                        AudioTrack.MODE_STATIC);
            }

            track.write(buffer, 0, buffer.length);
            track.play();
            Thread.sleep(durationMs + 10);
            track.release();
        } catch (Exception ignored) {}
    }

    public void release() {
        if (bgmPlayer != null) {
            try {
                bgmPlayer.stop();
                bgmPlayer.release();
                bgmPlayer = null;
            } catch (Exception ignored) {}
        }
        soundExecutor.shutdown();
    }
}
