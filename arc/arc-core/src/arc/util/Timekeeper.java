package arc.util;

/** Keeps track of a time interval. */
public class Timekeeper{
    private final long intervalMs;
    private long lastTime;

    Timekeeper(long ms){
        intervalMs = ms;
    }

    public static Timekeeper ofMillis(long ms){
        return new Timekeeper(ms);
    }

    public static Timekeeper ofTicks(float ticks){
        return ofSeconds(ticks / 60f);
    }

    public static Timekeeper ofSeconds(float seconds){
        return new Timekeeper((long)(seconds * 1000));
    }

    /** @return true if the interval has passed since the last reset(); resets the timer if true */
    public boolean poll(){
        boolean result = get();
        if(result) reset();
        return result;
    }

    /** @return true if the interval has passed since the last reset(). */
    public boolean get(){
        return Time.timeSinceMillis(lastTime) > intervalMs;
    }

    /** resets the timer; the interval will need to pass until get() returns true again. */
    public void reset(){
        lastTime = Time.millis();
    }
}
