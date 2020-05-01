package mindustry.async;

public interface AsyncProcess{

    /** Synchronous. Called at the beginning of the main loop. */
    void begin();

    /** Async. Called in a separate thread. */
    void process();

    /** Sync. Called in the end of the main loop. */
    void end();
}
