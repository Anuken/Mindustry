package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.ucore.function.Event;

public class EventType {

    public interface PlayEvent extends Event{
        void handle();
    }

    public interface ResetEvent extends Event{
        void handle();
    }

    public interface WaveEvent extends Event{
        void handle();
    }

    public interface GameOver extends Event{
        void handle();
    }

    public interface StateChange extends Event{
        void handle(State from, State to);
    }
}
