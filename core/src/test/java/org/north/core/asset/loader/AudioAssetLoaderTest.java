package org.north.core.asset.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.north.core.audio.Audio;
import org.north.core.audio.AudioClip;
import org.north.core.io.ResourceLoader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;

class AudioAssetLoaderTest {

    @Test
    void decodesWavIntoPlayableClip() throws Exception {
        byte[] wav = createWav();

        Audio audio = new Audio();
        try {
            AudioAssetLoader loader = new AudioAssetLoader(new InMemoryResourceLoader(wav), audio);
            AudioClip clip = loader.get("test.wav", null);

            Assertions.assertNotNull(clip);
            Assertions.assertEquals(0.1f, clip.getDuration(), 0.001f);
            Assertions.assertEquals(audio.isAvailable(), clip.isPlayable());

            loader.close();
        } finally {
            audio.close();
        }
    }

    private byte[] createWav() throws Exception {
        int sampleRate = 44100;
        int sampleCount = 4410;
        int bytesPerSample = 2;
        byte[] samples = new byte[sampleCount * bytesPerSample];

        for (int i = 0; i < sampleCount; i++) {
            short value = (short) (Math.sin(2.0 * Math.PI * 220.0 * i / sampleRate) * Short.MAX_VALUE * 0.25);
            samples[i * 2] = (byte) (value & 0xFF);
            samples[i * 2 + 1] = (byte) ((value >>> 8) & 0xFF);
        }

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (AudioInputStream stream = new AudioInputStream(
                new ByteArrayInputStream(samples), format, sampleCount)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, output);
        }
        return output.toByteArray();
    }

    private static final class InMemoryResourceLoader implements ResourceLoader {
        private final byte[] content;

        private InMemoryResourceLoader(byte[] content) {
            this.content = content;
        }

        @Override
        public InputStream loadAsStream(String path) {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String loadAsString(String path) {
            return new String(content, StandardCharsets.UTF_8);
        }

        @Override
        public URL getResourceUrl(String path) {
            return null;
        }

        @Override
        public Iterator<URL> getAllResourceUrl(String path) {
            return Collections.emptyIterator();
        }
    }
}
