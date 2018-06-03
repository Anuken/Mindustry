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

    /**This event is called from the logic thread.
     * DO NOT INITIALIZE GRAPHICS HERE.*/
    public interface WorldLoadEvent extends Event{
        void handle();
    }

    /**Called after the WorldLoadEvent is, and all logic has been loaded.
     * It is safe to intialize graphics here.*/
    public interface WorldLoadGraphicsEvent extends Event{
        void handle();
    }

    /**Called from the logic thread. Do not call graphics here!*/
    public interface TileChangeEvent extends Event{
        void handle(Tile tile);
    }

    public interface TileRemoveEvent extends Event{
        void handle(Tile tile, Team oldTeam);
    }

    public interface StateChangeEvent extends Event{
        void handle(State from, State to);
    }

    public interface UnlockEvent extends Event{
        void handle(Content content);
    }

    public interface BlockBuildEvent extends Event{
        void handle(Team team, Tile tile);
    }
}

