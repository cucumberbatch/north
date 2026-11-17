package io.github.cucumberbatch.north.system;

/**
 * Marker for update systems which keep running even when the update flow is stopped.
 *
 * <p>Editor services such as camera controls use it, so the pipeline does not have to know
 * about concrete system types.</p>
 */
public interface AlwaysUpdates {
}
