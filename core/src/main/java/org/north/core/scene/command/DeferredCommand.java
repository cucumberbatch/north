package org.north.core.scene.command;

import org.north.core.system.SystemManager;

@FunctionalInterface
public interface DeferredCommand {
    void execute(SystemManager systemManager);
}