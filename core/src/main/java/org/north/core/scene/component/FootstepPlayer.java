package org.north.core.scene.component;

import org.joml.Vector3f;
import org.north.core.audio.AudioClip;
import org.north.core.scene.Entity;

/**
 * Simple third-person test player that moves on the XZ plane and emits footsteps while walking.
 */
public class FootstepPlayer extends AbstractComponent {
    public Entity cameraEntity;
    public AudioClip[] footstepClips;

    public float moveSpeed = 3.5f;
    public float stepInterval = 0.55f;
    public float stepVolume = 0.85f;
    public int sourceCount = 4;

    public final Vector3f cameraOffset = new Vector3f(0f, 2.6f, 5.0f);
    public final Vector3f lookAtOffset = new Vector3f(0f, 0.9f, 0f);
}
