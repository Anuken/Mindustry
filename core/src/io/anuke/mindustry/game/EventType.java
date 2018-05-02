package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.Tile;
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

    public interface GameOverEvent extends Event{
        void handle();
    }

    public interface WorldLoadEvent extends Event{
        void handle();
    }

    public interface TileChangeEvent extends Event{
        void handle(Tile tile);
    }

    public interface TileRemoveEvent extends Event{
        void handle(Tile tile, Team oldTeam);
    }

    public interface StateChangeEvent extends Event{
        void handle(State from, State to);
    }
}
