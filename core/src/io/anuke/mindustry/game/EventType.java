package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events.Event;

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

    /**Called when block building begins by placing down the BuildBlock.
     * The tile's block will nearly always be a BuildBlock.*/
    public static class BlockBuildBeginEvent implements Event{
        public final Tile tile;
        public final Team team;
        public final boolean breaking;

        public BlockBuildBeginEvent(Tile tile, Team team, boolean breaking){
            this.tile = tile;
            this.team = team;
            this.breaking = breaking;
        }
    }

    /**Called when a player or drone begins building something.
     * This does not necessarily happen when a new BuildBlock is created.*/
    public static class BuildSelectEvent implements Event{
        public final Tile tile;
        public final Team team;
        public final BuilderTrait builder;
        public final boolean breaking;

        public BuildSelectEvent(Tile tile, Team team, BuilderTrait builder, boolean breaking){
            this.tile = tile;
            this.team = team;
            this.builder = builder;
            this.breaking = breaking;
        }
    }

    public static class ResizeEvent implements Event{

    }
}

