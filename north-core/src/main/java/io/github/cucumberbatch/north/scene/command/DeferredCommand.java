package io.github.cucumberbatch.north.scene.command;

import io.github.cucumberbatch.north.system.SystemManager;

@FunctionalInterface
public interface DeferredCommand {
    void execute(SystemManager systemManager);
}