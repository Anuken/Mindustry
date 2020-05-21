package mindustry.game;

import arc.util.ArcAnnotate.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class EventType{

    //events that occur very often
    public enum Trigger{
        shock,
        phaseDeflectHit,
        impactPower,
        thoriumReactorOverheat,
        itemLaunch,
        fireExtinguish,
        newGame,
        tutorialComplete,
        flameAmmo,
        turretCool,
        enablePixelation,
        drown,
        exclusionDeath,
        suicideBomb,
        openWiki,
        teamCoreDamage,
        socketConfigChanged,
        update
    }

    public static class TurnEvent{}
    public static class WinEvent{}
    public static class LoseEvent{}
    public static class LaunchEvent{}
    public static class ResizeEvent{}
    public static class MapMakeEvent{}
    public static class MapPublishEvent{}
    public static class SaveLoadEvent{}
    public static class ClientCreateEvent{}
    public static class ServerLoadEvent{}
    public static class ContentReloadEvent{}
    public static class DisposeEvent{}
    public static class PlayEvent{}
    public static class ResetEvent{}
    public static class WaveEvent{}
    /** Called when the player places a line, mobile or desktop.*/
    public static class LineConfirmEvent{}
    /** Called when a turret recieves ammo, but only when the tutorial is active! */
    public static class TurretAmmoDeliverEvent{}
    /** Called when a core recieves ammo, but only when the tutorial is active! */
    public static class CoreItemDeliverEvent{}
    /** Called when the player opens info for a specific block.*/
    public static class BlockInfoEvent{}
    /** Called when the client game is first loaded. */
    public static class ClientLoadEvent{}
    /** Called when a game begins and the world is loaded. */
    public static class WorldLoadEvent{}

    public static class LaunchItemEvent{
        public final ItemStack stack;

        public LaunchItemEvent(ItemStack stack){
            this.stack = stack;
        }
    }


    public static class CommandIssueEvent{
        public final Tilec tile;
        public final UnitCommand command;

        public CommandIssueEvent(Tilec tile, UnitCommand command){
            this.tile = tile;
            this.command = command;
        }
    }

    public static class PlayerChatEvent{
        public final Playerc player;
        public final String message;

        public PlayerChatEvent(Playerc player, String message){
            this.player = player;
            this.message = message;
        }
    }

    /** Called when a zone's requirements are met. */
    public static class ZoneRequireCompleteEvent{
        public final SectorPreset zoneMet, zoneForMet;
        public final Objectives.Objective objective;

        public ZoneRequireCompleteEvent(SectorPreset zoneMet, SectorPreset zoneForMet, Objectives.Objective objective){
            this.zoneMet = zoneMet;
            this.zoneForMet = zoneForMet;
            this.objective = objective;
        }
    }

    /** Called when a zone's requirements are met. */
    public static class ZoneConfigureCompleteEvent{
        public final SectorPreset zone;

        public ZoneConfigureCompleteEvent(SectorPreset zone){
            this.zone = zone;
        }
    }

    /** Called when a sector is conquered, e.g. a boss or base is defeated. */
    public static class SectorCaptureEvent{
        public final Sector sector;

        public SectorCaptureEvent(Sector sector){
            this.sector = sector;
        }
    }

    /** Called when the player withdraws items from a block. */
    public static class WithdrawEvent{
        public final Tilec tile;
        public final Playerc player;
        public final Item item;
        public final int amount;

        public WithdrawEvent(Tilec tile, Playerc player, Item item, int amount){
            this.tile = tile;
            this.player = player;
            this.item = item;
            this.amount = amount;
        }
    }

    /** Called when a player deposits items to a block.*/
    public static class DepositEvent{
        public final Tilec tile;
        public final Playerc player;
        public final Item item;
        public final int amount;

        public DepositEvent(Tilec tile, Playerc player, Item item, int amount){
            this.tile = tile;
            this.player = player;
            this.item = item;
            this.amount = amount;
        }
    }

    /** Called when the player taps a block. */
    public static class TapEvent{
        public final Tilec tile;
        public final Playerc player;

        public TapEvent(Tilec tile, Playerc player){
            this.tile = tile;
            this.player = player;
        }
    }

    /** Called when the player sets a specific block. */
    public static class TapConfigEvent{
        public final Tilec tile;
        public final Playerc player;
        public final Object value;

        public TapConfigEvent(Tilec tile, Playerc player, Object value){
            this.tile = tile;
            this.player = player;
            this.value = value;
        }
    }

    public static class GameOverEvent{
        public final Team winner;

        public GameOverEvent(Team winner){
            this.winner = winner;
        }
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

    public static class ResearchEvent{
        public final UnlockableContent content;

        public ResearchEvent(UnlockableContent content){
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
        public final @Nullable Unitc unit;
        public final boolean breaking;

        public BlockBuildEndEvent(Tile tile, @Nullable Unitc unit, Team team, boolean breaking){
            this.tile = tile;
            this.team = team;
            this.unit = unit;
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
        public final Builderc builder;
        public final boolean breaking;

        public BuildSelectEvent(Tile tile, Team team, Builderc builder, boolean breaking){
            this.tile = tile;
            this.team = team;
            this.builder = builder;
            this.breaking = breaking;
        }
    }

    /** Called right before a block is destroyed.
     * The tile entity of the tile in this event cannot be null when this happens.*/
    public static class BlockDestroyEvent{
        public final Tile tile;

        public BlockDestroyEvent(Tile tile){
            this.tile = tile;
        }
    }

    public static class UnitDestroyEvent{
        public final Unitc unit;

        public UnitDestroyEvent(Unitc unit){
            this.unit = unit;
        }
    }

    public static class UnitCreateEvent{
        public final Unitc unit;

        public UnitCreateEvent(Unitc unit){
            this.unit = unit;
        }
    }

    //TODO rename
    public static class MechChangeEvent{
        public final Playerc player;
        public final UnitType mech;

        public MechChangeEvent(Playerc player, UnitType mech){
            this.player = player;
            this.mech = mech;
        }
    }

    /** Called after connecting; when a player recieves world data and is ready to play.*/
    public static class PlayerJoin{
        public final Playerc player;

        public PlayerJoin(Playerc player){
            this.player = player;
        }
    }

    /** Called when a player connects, but has not joined the game yet.*/
    public static class PlayerConnect{
        public final Playerc player;

        public PlayerConnect(Playerc player){
            this.player = player;
        }
    }

    public static class PlayerLeave{
        public final Playerc player;

        public PlayerLeave(Playerc player){
            this.player = player;
        }
    }
    
    public static class PlayerBanEvent{
        public final Playerc player;

        public PlayerBanEvent(Playerc player){
            this.player = player;
        }
    }
    
    public static class PlayerUnbanEvent{
        public final Playerc player;

        public PlayerUnbanEvent(Playerc player){
            this.player = player;
        }
    }
    
    public static class PlayerIpBanEvent{
        public final String ip;

        public PlayerIpBanEvent(String ip){
            this.ip = ip;
        }
    }
    
    public static class PlayerIpUnbanEvent{
        public final String ip;

        public PlayerIpUnbanEvent(String ip){
            this.ip = ip;
        }
    }
    
}

