package arc.util;

import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.Timer.*;

public class Time{
    /** Conversion factors for ticks to other unit values. */
    public static final float toSeconds = 60f, toMinutes = 60f * 60f, toHours = 60f * 60f * 60f;

    /** Global delta value. Do not change. */
    public static float delta = 1f;
    /** Global time value. Do not change. */
    public static float globalTime;
    private static double globalTimeRaw;

    public static final long nanosPerMilli = 1000000;

    private static LongSeq marks = new LongSeq();
    private static Floatp deltaimpl = () -> Math.min(Core.graphics.getDeltaTime() * 60f, 3f);

    /** Runs a task with a delay of several ticks. Unless the application is closed, this task will always complete. */
    public static Task runTask(float delay, Runnable r){
        return Timer.schedule(r, delay / 60f);
    }

    public static void mark(){
        marks.add(nanos());
    }

    /** A value of -1 means mark() wasn't called beforehand. */
    public static float elapsed(){
        if(marks.size == 0){
            return -1;
        }else{
            return timeSinceNanos(marks.pop()) / 1000000f;
        }
    }

    public static void updateGlobal(){
        globalTimeRaw += Core.graphics.getDeltaTime()*60f;
        delta = deltaimpl.get();
        globalTime = (float)globalTimeRaw;
    }

    public static void setDeltaProvider(Floatp impl){
        deltaimpl = impl;
        delta = impl.get();
    }

    /** @return The current value of the system timer, in nanoseconds. */
    public static long nanos(){
        return System.nanoTime();
    }

    /** @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC. */
    public static long millis(){
        return System.currentTimeMillis();
    }

    /**
     * Convert nanoseconds time to milliseconds
     * @param nanos must be nanoseconds
     * @return time value in milliseconds
     */
    public static long nanosToMillis(long nanos){
        return nanos / nanosPerMilli;
    }

    /**
     * Convert milliseconds time to nanoseconds
     * @param millis must be milliseconds
     * @return time value in nanoseconds
     */
    public static long millisToNanos(long millis){
        return millis * nanosPerMilli;
    }

    /**
     * Get the time in nanos passed since a previous time
     * @param prevTime - must be nanoseconds
     * @return - time passed since prevTime in nanoseconds
     */
    public static long timeSinceNanos(long prevTime){
        return nanos() - prevTime;
    }

    /**
     * Get the time in millis passed since a previous time
     * @param prevTime - must be milliseconds
     * @return - time passed since prevTime in milliseconds
     */
    public static long timeSinceMillis(long prevTime){
        return millis() - prevTime;
    }

}
