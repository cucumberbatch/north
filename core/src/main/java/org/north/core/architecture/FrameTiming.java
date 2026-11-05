package org.north.core.architecture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public class FrameTiming {
    private static final int  NANO_TIME_STEP = 100;
    private static final long NANO_SECONDS_IN_SECOND = 1_000_000_000L;
    private static final int DEFAULT_FRAME_RATE = 60;
    private static final Logger log = LoggerFactory.getLogger(FrameTiming.class);

    private long previousNanoTime;
    private long loopStartTimeInNano;
    private long elapsedTimeInNano;
    private long loopSlotInNano;

    private int  targetFrameRate;
    private int  actualFrameRate;

    public FrameTiming() {
        this(DEFAULT_FRAME_RATE);
    }

    public FrameTiming(int targetFrameRate) {
        setTargetFrameRate(targetFrameRate);
        previousNanoTime = getCurrentNanoTime();
    }

    public void updateTiming() {
        loopStartTimeInNano = getCurrentNanoTime();
        elapsedTimeInNano   = loopStartTimeInNano - previousNanoTime;
        previousNanoTime    = loopStartTimeInNano;
        actualFrameRate     = (int) (NANO_SECONDS_IN_SECOND / elapsedTimeInNano);
    }

    public long getCurrentNanoTime() {
        return System.nanoTime();
    }

    public void sync() {
        long endTimeInNano = loopStartTimeInNano + loopSlotInNano - NANO_TIME_STEP;
        long sleepTime = endTimeInNano - getCurrentNanoTime();
        if (sleepTime > 0) {
            LockSupport.parkNanos(sleepTime);
        }
    }

    public float getElapsedTime() {
        return (float) this.elapsedTimeInNano / NANO_SECONDS_IN_SECOND;
    }

    public void setTargetFrameRate(int targetFrameRate) {
        if (targetFrameRate < 1) return;
        this.targetFrameRate = targetFrameRate;
        this.loopSlotInNano = NANO_SECONDS_IN_SECOND / targetFrameRate;
    }

    public int getActualFrameRate() {
        return this.actualFrameRate;
    }
}
