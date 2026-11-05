package org.north.core.scene.component;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The main component of each game object that tells about its position, rotation and scale
 *
 * @author cucumberbatch
 */
public class Transform extends AbstractComponent {
    public Transform parentTransform;

    private Vector3f position = new Vector3f(0, 0, 0);
    private Vector3f rotation = new Vector3f(0, 0, 0);
    private Vector3f scale    = new Vector3f(1, 1, 1);

    private Vector3f globalPosition = new Vector3f(0, 0, 0);
    private Vector3f globalRotation = new Vector3f(0, 0, 0);
    private Vector3f globalScale    = new Vector3f(1, 1, 1);

    private boolean globalDirty = true;

    public Transform() {
        this(null);
    }

    public Transform(Transform parentTransform) {
        this.parentTransform = parentTransform;
        super.setState(ComponentState.READY_TO_OPERATE_STATE);
    }

    public void setParent(Transform parentTransform) {
        this.parentTransform = parentTransform;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }

    public void moveTo(Vector3f position) {
        this.moveTo(position.x, position.y, position.z);
    }

    public void moveRel(Vector3f position) {
        this.moveRel(position.x, position.y, position.z);
    }

    public void rescaleTo(Vector3f scale) {
        this.rescaleTo(scale.x, scale.y, scale.z);
    }

    public void rescaleRel(Vector3f scale) {
        this.rescaleRel(scale.x, scale.y, scale.z);
    }

    public void moveTo(float x, float y, float z) {
        this.position.set(x, y, z);
        markGlobalDirty();
    }

    public void moveRel(float x, float y, float z) {
        this.position.add(x, y, z);
        markGlobalDirty();
    }

    public void rescaleTo(float x, float y, float z) {
        this.scale.set(x, y, z);
        markGlobalDirty();
    }

    public void rescaleRel(float x, float y, float z) {
        this.scale.add(x, y, z);
        markGlobalDirty();
    }

    private void markGlobalDirty() {
        this.globalDirty = true;
    }

    /**
     * Returns a local model matrix of this transform
     *
     * @apiNote memory consumption! matrix object creation on each call
     * @deprecated use method with matrix argument as temporary destination object
     * @return a local model matrix of this transform
     */
    @Deprecated(forRemoval = true)
    public Matrix4f getLocalModelMatrix() {
        return getLocalModelMatrix(this, new Matrix4f().identity());
    }

    private Matrix4f getLocalModelMatrix(Transform transform, Matrix4f destination) {
        return destination
                .translate(transform.position)
                .rotateX((float) Math.toRadians(transform.rotation.x))
                .rotateY((float) Math.toRadians(transform.rotation.y))
                .rotateZ((float) Math.toRadians(transform.rotation.z))
                .scale(transform.scale);
    }

    public Matrix4f getGlobalModelMatrix(Matrix4f destination) {
        destination = destination.identity();

        int depth = 0;
        Transform root = this;
        while (root.parentTransform != null) {
            depth++;
            root = root.parentTransform;
        }

        Transform[] chain = new Transform[depth + 1];
        Transform current = this;
        while (current.parentTransform != null) {
            chain[depth--] = current;
            current = current.parentTransform;
        }
        chain[0] = root;
        
        for (Transform transform : chain) {
            destination = getLocalModelMatrix(transform, destination);
        }
        
        return destination;
    }

    public Vector3f getGlobalPosition(Vector3f destination) {
        globalPosition.set(getGlobalModelMatrix(new Matrix4f()).getTranslation(destination));
        return destination;
    }

    public Vector3f getGlobalRotation(Vector3f destination) {
        destination = destination.zero();
        for (Transform it = this; it != null; it = it.parentTransform)
            destination.rotateX(it.rotation.x).rotateY(it.rotation.y).rotateZ(it.rotation.z);
        globalRotation.set(destination);
        return destination;
    }

    public Vector3f getGlobalScale(Vector3f destination) {
        globalScale.set(getGlobalModelMatrix(new Matrix4f()).getScale(destination));
        return destination;
    }

    @Deprecated
    public Transform getGlobalTransform() {
        Transform gTransform = new Transform();
        for (Transform it = this; it != null; it = it.parentTransform) {
            gTransform.position.add(it.position);
            gTransform.rotation.add(it.rotation);
            gTransform.scale.mul(it.scale);
        }
        return gTransform;
    }

    @Override
    public String toString() {
        return "Transform{" +
                "localPos=" + position +
                ", localRot=" + rotation +
                ", localScl=" + scale +
                ", globalPos=" + globalPosition +
                ", globalRot=" + globalRotation +
                ", globalScl=" + globalScale +
                '}';
    }

}
