package mindustry.async;

public interface AsyncProcess{

    /** Sync. Called when the world loads. */
    default void init(){}

    /** Sync. Called when the world resets. */
    default void reset(){}

    /** Sync. Called at the beginning of the main loop. */
    default void begin(){}

    /** Async. Called in a separate thread. */
    default void process(){}

    /** Sync. Called in the end of the main loop. */
    default void end(){}

    default boolean shouldProcess(){
        return true;
    }
}
