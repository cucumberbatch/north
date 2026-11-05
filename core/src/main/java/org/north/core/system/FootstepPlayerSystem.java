package org.north.core.system;

import java.util.Random;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.audio.Audio;
import org.north.core.audio.AudioSource;
import org.north.core.graphics.Graphics;
import org.north.core.scene.component.Camera;
import org.north.core.scene.component.FootstepPlayer;
import org.north.core.scene.component.Transform;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.InputHandlerProcess;
import org.north.core.system.process.UpdateProcess;

/**
 * Moves the test player, keeps the follow camera behind it, and triggers one-shot footstep sounds.
 */
@ComponentHandler(FootstepPlayer.class)
public class FootstepPlayerSystem extends AbstractSystem<FootstepPlayer>
        implements InitProcess<FootstepPlayer>, InputHandlerProcess<FootstepPlayer>, UpdateProcess<FootstepPlayer> {

    private final Audio audio;
    private final Graphics graphics;

    private final Random random;

    private final Vector3f playerPosition = new Vector3f();
    private final Vector3f cameraPosition = new Vector3f();
    private final Vector3f lookAt = new Vector3f();
    private final Vector3f forward = new Vector3f();
    private final Vector3f up = new Vector3f(0f, 1f, 0f);

    private AudioSource[] footstepSources;
    private boolean forwardHeld;
    private boolean backwardHeld;
    private boolean leftHeld;
    private boolean rightHeld;
    private float stepTimer;
    private int stepIndex;
    
    @Inject
    public FootstepPlayerSystem(ApplicationContext context) {
        super(FootstepPlayer.class, context);
        audio = context.getDependency(Audio.class);
        graphics = context.getDependency(Graphics.class);
        random = new Random();
    }

    @Override
    public void init(FootstepPlayer player) {
        int sourceCount = Math.max(1, player.sourceCount);
        footstepSources = new AudioSource[sourceCount];

        for (int i = 0; i < sourceCount; i++) {
            AudioSource source = audio.createSource();
            source.setClip(player.footstepClips[3]);
            source.setRelative(false);
            source.setLooping(false);
            source.setReferenceDistance(10f);
            source.setRolloffFactor(0.5f);
            source.setMaxDistance(50f);
            footstepSources[i] = source;
        }
    }

    @Override
    public void handleInput(FootstepPlayer player, Input input) {
        forwardHeld = input.isHeld(GLFW.GLFW_KEY_W) || input.isHeld(GLFW.GLFW_KEY_UP);
        backwardHeld = input.isHeld(GLFW.GLFW_KEY_S) || input.isHeld(GLFW.GLFW_KEY_DOWN);
        leftHeld = input.isHeld(GLFW.GLFW_KEY_A) || input.isHeld(GLFW.GLFW_KEY_LEFT);
        rightHeld = input.isHeld(GLFW.GLFW_KEY_D) || input.isHeld(GLFW.GLFW_KEY_RIGHT);
    }

    @Override
    public void update(FootstepPlayer player, float deltaTime) {
        Transform playerTransform = cm.get(player.getEntity(), Transform.class);
        playerTransform.getGlobalPosition(playerPosition);

        float moveX = axis(rightHeld, leftHeld);
        float moveZ = axis(backwardHeld, forwardHeld);
        boolean moving = moveX != 0f || moveZ != 0f;

        if (moving) {
            float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
            playerTransform.moveRel(
                    moveX / length * player.moveSpeed * deltaTime,
                    0f,
                    moveZ / length * player.moveSpeed * deltaTime
            );
            playerTransform.getGlobalPosition(playerPosition);
        }

        updateCamera(player, playerPosition);
        updateFootsteps(player, moving, deltaTime);
    }

    private void updateCamera(FootstepPlayer player, Vector3f playerPosition) {
        if (player.cameraEntity == null) return;

        Transform cameraTransform = cm.get(player.cameraEntity, Transform.class);
        Camera camera = cm.get(player.cameraEntity, Camera.class);
        if (cameraTransform == null || camera == null) return;

        cameraPosition.set(playerPosition).add(player.cameraOffset);
        lookAt.set(playerPosition).add(player.lookAtOffset);
        cameraTransform.moveTo(cameraPosition);

        camera.eye.set(cameraPosition);
        camera.at.set(lookAt);
        camera.viewMatrix.identity().lookAt(camera.eye, camera.at, up);

        graphics.view = camera.viewMatrix;
        graphics.projection = camera.projectionMatrix;

        audio.setListenerPosition(cameraPosition);
        forward.set(lookAt).sub(cameraPosition).normalize();
        audio.setListenerOrientation(forward, up);
    }

    private void updateFootsteps(FootstepPlayer player, boolean moving, float deltaTime) {
        if (!moving) {
            stepTimer = 0f;
            return;
        }

        stepTimer -= deltaTime;
        if (stepTimer > 0f) return;

        playFootstep(player, playerPosition);
        stepTimer = player.stepInterval;
    }

    private void playFootstep(FootstepPlayer player, Vector3f position) {
        int footstepsClipsCount = player.footstepClips.length;
        int index = stepIndex % footstepsClipsCount;
        if (player.footstepClips[index] == null || !player.footstepClips[index].isPlayable()) return;

        AudioSource source = findFreeSource();
        if (source == null) return;

        source.setPosition(position);
        source.setGain(player.stepVolume);
        source.setPitch(0.94f + (stepIndex % 3) * 0.035f);
        source.setClip(player.footstepClips[random.nextInt(footstepsClipsCount)]);
        source.play();
        stepIndex++;
    }

    private AudioSource findFreeSource() {
        if (footstepSources == null) return null;

        for (AudioSource source : footstepSources) {
            if (source != null && !source.isPlaying()) {
                return source;
            }
        }
        return null;
    }

    private float axis(boolean positive, boolean negative) {
        if (positive == negative) return 0f;
        return positive ? 1f : -1f;
    }
}
