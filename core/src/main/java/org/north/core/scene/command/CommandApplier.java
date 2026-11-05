package org.north.core.scene.command;

import org.north.core.architecture.Inject;
import org.north.core.system.SystemManager;

import java.util.LinkedList;
import java.util.Queue;

public class CommandApplier {
    private final SystemManager systemManager;
    private final Queue<DeferredCommand> deferredCommands;

    @Inject
    public CommandApplier(SystemManager systemManager) {
        this.systemManager = systemManager;
        this.deferredCommands = new LinkedList<>();
    }

    /**
     * Adds a deferred command to a command queue
     * @param command a deferred command to execute
     */
    public void addDeferredCommand(DeferredCommand command) {
        deferredCommands.add(command);
    }

    /**
     * Executes all deferred commands in a deferred command queue and clears it
     */
    public void applyDeferredCommands() {
        if (deferredCommands.isEmpty()) return;
        for (DeferredCommand command : deferredCommands) {
            command.execute(systemManager);
//            log.info("executed deferred command: {}", command);
        }
        deferredCommands.clear();
    }

}
