package io.anuke.mindustry.game;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.traits.BuilderTrait;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.Tile;

public class EventType{

    /** Called when a zone's requirements are met. */
    public static class ZoneRequireCompleteEvent{
        public final Zone zone, required;

        public ZoneRequireCompleteEvent(Zone zone, Zone required){
            this.zone = zone;
            this.required = required;
        }
    }

    /** Called when a zone's requirements are met. */
    public static class ZoneConfigureCompleteEvent{
        public final Zone zone;

        public ZoneConfigureCompleteEvent(Zone zone){
            this.zone = zone;
        }
    }

    /** Called when the game is first loaded. */
    public static class GameLoadEvent{

    }

    public static class PlayEvent{

    }

    public static class ResetEvent{

    }

    public static class WaveEvent{

    }

    public static class GameOverEvent{
        public final Team winner;

        public GameOverEvent(Team winner){
            this.winner = winner;
        }
    }

    /** Called when a game begins and the world is loaded. */
    public static class WorldLoadEvent{

    }

    /** Called from the logic thread. Do not access graphics here! */
    public static class TileChangeEvent{
        public final Tile tile;

        public TileChangeEvent(Tile tile){
            this.tile = tile;
        }
    }

    public static class StateChangeEvent{
        public final State from, to;

        public StateChangeEvent(State from, State to){
            this.from = from;
            this.to = to;
        }
    }

    public static class UnlockEvent{
        public final UnlockableContent content;

        public UnlockEvent(UnlockableContent content){
            this.content = content;
        }
    }

    /**
     * Called when block building begins by placing down the BuildBlock.
     * The tile's block will nearly always be a BuildBlock.
     */
    public static class BlockBuildBeginEvent{
        public final Tile tile;
        public final Team team;
        public final boolean breaking;

        public BlockBuildBeginEvent(Tile tile, Team team, boolean breaking){
            this.tile = tile;
            this.team = team;
            this.breaking = breaking;
        }
    }

    public static class BlockBuildEndEvent{
        public final Tile tile;
        public final Team team;
        public final boolean breaking;

        public BlockBuildEndEvent(Tile tile, Team team, boolean breaking){
            this.tile = tile;
            this.team = team;
            this.breaking = breaking;
        }
    }

    /**
     * Called when a player or drone begins building something.
     * This does not necessarily happen when a new BuildBlock is created.
     */
    public static class BuildSelectEvent{
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

    public static class BlockDestroyEvent{
        public final Tile tile;

        public BlockDestroyEvent(Tile tile){
            this.tile = tile;
        }
    }

    public static class UnitDestroyEvent{
        public final Unit unit;

        public UnitDestroyEvent(Unit unit){
            this.unit = unit;
        }
    }

    public static class ResizeEvent{

    }
}

