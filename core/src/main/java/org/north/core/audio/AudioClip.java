package org.north.core.audio;

import org.north.core.asset.Asset;

/**
 * A decoded audio clip stored in an OpenAL buffer.
 */
public class AudioClip implements Asset {
    private final int bufferId;
    private final float duration;

    public AudioClip(int bufferId, float duration) {
        this.bufferId = bufferId;
        this.duration = duration;
    }

    @Override
    public int id() {
        return bufferId;
    }

    public int getBufferId() {
        return bufferId;
    }

    public float getDuration() {
        return duration;
    }

    public boolean isPlayable() {
        return bufferId != 0;
    }
}
