package org.north.core.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Thin OpenAL wrapper used by the engine.
 *
 * <p>If no audio device can be opened, the instance becomes a no-op audio backend. Game code can
 * keep running and the asset loader can still decode clips; only playback is skipped.</p>
 */
public class Audio implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(Audio.class);

    private final Set<Integer> sources = new HashSet<>();

    private long device = NULL;
    private long context = NULL;
    private boolean available;

    public Audio() {
        available = initialize();
    }

    public boolean isAvailable() {
        return available;
    }

    private boolean initialize() {
        try {
            device = ALC10.alcOpenDevice((ByteBuffer) null);
            if (device == NULL) {
                log.warn("OpenAL device could not be opened; audio is disabled");
                return false;
            }

            ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
            context = ALC10.alcCreateContext(device, (IntBuffer) null);
            if (context == NULL) {
                log.warn("OpenAL context could not be created; audio is disabled");
                closeDevice();
                return false;
            }

            ALC10.alcMakeContextCurrent(context);
            AL.createCapabilities(alcCapabilities);

            AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE);
            AL10.alListener3f(AL10.AL_POSITION, 0f, 0f, 0f);
            AL10.alListener3f(AL10.AL_VELOCITY, 0f, 0f, 0f);

            log.info("OpenAL initialized: {}", AL10.alGetString(AL10.AL_VERSION));
            return true;
        } catch (RuntimeException | LinkageError e) {
            log.warn("OpenAL initialization failed; audio is disabled", e);
            closeDevice();
            return false;
        }
    }

    /**
     * Uploads 16-bit little-endian PCM data to an OpenAL buffer.
     *
     * @return the OpenAL buffer id, or {@code 0} when audio is disabled
     */
    public int createBuffer(byte[] pcm16, int channels, int sampleRate) {
        if (!available || pcm16.length == 0 || (channels != 1 && channels != 2)) {
            return 0;
        }

        int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
        ShortBuffer decodedSamples = ByteBuffer.wrap(pcm16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer();

        // OpenAL reads directly from native memory. A heap-backed ShortBuffer is not accepted.
        ShortBuffer nativeSamples = MemoryUtil.memAllocShort(decodedSamples.remaining());
        try {
            nativeSamples.put(decodedSamples).flip();

            int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, format, nativeSamples, sampleRate);
            return buffer;
        } finally {
            MemoryUtil.memFree(nativeSamples);
        }
    }

    public void deleteBuffer(int buffer) {
        if (available && buffer != 0) {
            AL10.alDeleteBuffers(buffer);
        }
    }

    public AudioSource createSource() {
        if (!available) {
            return new AudioSource(this, 0);
        }

        int source = AL10.alGenSources();
        sources.add(source);
        return new AudioSource(this, source);
    }

    void deleteSource(int source) {
        if (!available || source == 0) return;

        AL10.alSourceStop(source);
        AL10.alDeleteSources(source);
        sources.remove(source);
    }

    public void setListenerPosition(Vector3f position) {
        if (!available) return;
        AL10.alListener3f(AL10.AL_POSITION, position.x, position.y, position.z);
    }

    public void setListenerVelocity(Vector3f velocity) {
        if (!available) return;
        AL10.alListener3f(AL10.AL_VELOCITY, velocity.x, velocity.y, velocity.z);
    }

    public void setListenerOrientation(Vector3f forward, Vector3f up) {
        if (!available) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            java.nio.FloatBuffer orientation = stack.mallocFloat(6);
            orientation.put(forward.x).put(forward.y).put(forward.z);
            orientation.put(up.x).put(up.y).put(up.z);
            orientation.flip();
            AL10.alListenerfv(AL10.AL_ORIENTATION, orientation);
        }
    }

    @Override
    public void close() {
        if (!available && device == NULL) return;

        for (Integer source : new ArrayList<>(sources)) {
            deleteSource(source);
        }

        if (context != NULL) {
            ALC10.alcMakeContextCurrent(NULL);
            ALC10.alcDestroyContext(context);
            context = NULL;
        }

        closeDevice();
        available = false;
    }

    private void closeDevice() {
        if (device != NULL) {
            ALC10.alcCloseDevice(device);
            device = NULL;
        }
    }
}
