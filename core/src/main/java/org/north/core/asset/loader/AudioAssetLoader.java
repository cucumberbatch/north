package org.north.core.asset.loader;

import org.north.core.asset.AssetService;
import org.north.core.audio.Audio;
import org.north.core.audio.AudioClip;
import org.north.core.io.ResourceLoader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Loads WAV audio files and uploads them as 16-bit OpenAL buffers.
 */
public class AudioAssetLoader extends AssetLoader<AudioClip> {
    private final ResourceLoader resourceLoader;
    private final Audio audio;

    public AudioAssetLoader(ResourceLoader resourceLoader, Audio audio) {
        this.resourceLoader = resourceLoader;
        this.audio = audio;
    }

    @Override
    protected AudioClip load(String assetPath, AssetService assetService) {
        try (InputStream stream = resourceLoader.loadAsStream(assetPath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Audio resource not found: " + assetPath);
            }

            try (AudioInputStream source = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(stream) //the AudioSystem docs says that there may be problems with mark/reset stream when loading stream from unsupported sources like jar archive
            )) {
                AudioFormat sourceFormat = source.getFormat();
                AudioFormat targetFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        sourceFormat.getSampleRate(),
                        16,
                        sourceFormat.getChannels(),
                        sourceFormat.getChannels() * 2,
                        sourceFormat.getSampleRate(),
                        false
                );

                AudioInputStream pcm = sourceFormat.matches(targetFormat)
                        ? source
                        : AudioSystem.getAudioInputStream(targetFormat, source);

                try (AudioInputStream decoded = pcm) {
                    byte[] samples = decoded.readAllBytes();
                    int channels = targetFormat.getChannels();
                    int sampleRate = Math.round(targetFormat.getSampleRate());

                    if (samples.length == 0) {
                        throw new IllegalArgumentException("Audio file contains no samples: " + assetPath);
                    }

                    int bufferId = audio.createBuffer(samples, channels, sampleRate);
                    float duration = samples.length / (float) (sampleRate * channels * 2);
                    return new AudioClip(bufferId, duration);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load audio '" + assetPath + "'", e);
        }
    }

    @Override
    protected void unload(AudioClip clip) {
        audio.deleteBuffer(clip.getBufferId());
    }
}
