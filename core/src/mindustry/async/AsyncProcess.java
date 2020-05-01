package mindustry.async;

public interface AsyncProcess{

    /** Sync. Called when the world loads. */
    void init();

    /** Sync. Called when the world resets. */
    void reset();

    /** Sync. Called at the beginning of the main loop. */
    void begin();

    /** Async. Called in a separate thread. */
    void process();

    /** Sync. Called in the end of the main loop. */
    void end();
}
