package org.north.core.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

/**
 * A single OpenAL source. Sources are cheap compared with clips and can be reused for one-shot
 * sounds such as footsteps.
 */
public class AudioSource implements AutoCloseable {
    private final Audio audio;
    private final int sourceId;

    AudioSource(Audio audio, int sourceId) {
        this.audio = audio;
        this.sourceId = sourceId;
    }

    public boolean isAvailable() {
        return sourceId != 0;
    }

    public void setClip(AudioClip clip) {
        if (!isAvailable()) return;

        int buffer = clip == null ? 0 : clip.getBufferId();
        AL10.alSourcei(sourceId, AL10.AL_BUFFER, buffer);
    }

    public void setPosition(Vector3f position) {
        if (!isAvailable()) return;
        AL10.alSource3f(sourceId, AL10.AL_POSITION, position.x, position.y, position.z);
    }

    public void setRelative(boolean relative) {
        if (!isAvailable()) return;
        AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void setLooping(boolean looping) {
        if (!isAvailable()) return;
        AL10.alSourcei(sourceId, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void setGain(float gain) {
        if (!isAvailable()) return;
        AL10.alSourcef(sourceId, AL10.AL_GAIN, gain);
    }

    public void setPitch(float pitch) {
        if (!isAvailable()) return;
        AL10.alSourcef(sourceId, AL10.AL_PITCH, pitch);
    }

    public void setRolloffFactor(float rolloffFactor) {
        if (!isAvailable()) return;
        AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, rolloffFactor);
    }

    public void setReferenceDistance(float distance) {
        if (!isAvailable()) return;
        AL10.alSourcef(sourceId, AL10.AL_REFERENCE_DISTANCE, distance);
    }

    public void setMaxDistance(float distance) {
        if (!isAvailable()) return;
        AL10.alSourcef(sourceId, AL10.AL_MAX_DISTANCE, distance);
    }

    public void play() {
        if (!isAvailable()) return;
        AL10.alSourcePlay(sourceId);
    }

    public void stop() {
        if (!isAvailable()) return;
        AL10.alSourceStop(sourceId);
    }

    public boolean isPlaying() {
        return isAvailable()
                && AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
    }

    @Override
    public void close() {
        audio.deleteSource(sourceId);
    }
}
