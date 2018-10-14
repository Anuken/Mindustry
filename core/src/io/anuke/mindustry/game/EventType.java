package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.function.Event;

public class EventType{

    public static class SectorCompleteEvent implements Event{

    }

    public static class GameLoadEvent implements Event{

    }

    public static class PlayEvent implements Event{

    }

    public static class ResetEvent implements Event{

    }

    public static class WaveEvent implements Event{

    }

    public static class GameOverEvent implements Event{
        public final Team winner;

        public GameOverEvent(Team winner){
            this.winner = winner;
        }
    }

    /**
     * This event is called from the logic thread.
     * DO NOT INITIALIZE GRAPHICS HERE.
     */
    public static class WorldLoadEvent implements Event{

    }

    /**
     * Called after the WorldLoadEvent is, and all logic has been loaded.
     * It is safe to intialize graphics here.
     */
    public static class WorldLoadGraphicsEvent implements Event{

    }

    /**Called from the logic thread. Do not access graphics here!*/
    public static class TileChangeEvent implements Event{
        public final Tile tile;

        public TileChangeEvent(Tile tile){
            this.tile = tile;
        }
    }

    public static class StateChangeEvent implements Event{
        public final State from, to;

        public StateChangeEvent(State from, State to){
            this.from = from;
            this.to = to;
        }
    }

    public static class UnlockEvent implements Event{
        public final Content content;

        public UnlockEvent(Content content){
            this.content = content;
        }
    }

    public static class BlockBuildEvent implements Event{
        public final Tile tile;
        public final Team team;

        public BlockBuildEvent(Tile tile, Team team){
            this.tile = tile;
            this.team = team;
        }
    }

    public static class ResizeEvent implements Event{

    }
}

