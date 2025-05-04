package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.scene.style.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.input.*;

/** Defines a pattern of behavior that an RTS-controlled unit should follow. Shows up in the command UI. */
public class UnitCommand extends MappableContent{
    public static UnitCommand moveCommand, repairCommand, rebuildCommand, assistCommand, mineCommand, boostCommand, enterPayloadCommand, loadUnitsCommand, loadBlocksCommand, unloadPayloadCommand, loopPayloadCommand;

    /** Name of UI icon (from Icon class). */
    public final String icon;
    /** Controller that this unit will use when this command is used. Return null for "default" behavior. */
    public final Func<Unit, AIController> controller;
    /** If true, this unit will automatically switch away to the move command when given a position. */
    public boolean switchToMove = true;
    /** Whether to draw the movement/attack target. */
    public boolean drawTarget = false;
    /** Whether to reset targets when switching to or from this command. */
    public boolean resetTarget = true;
    /** Whether to snap the command destination to ally buildings. */
    public boolean snapToBuilding = false;
    /** */
    public boolean exactArrival = false;
    /** If true, this command refreshes the list of stances when selected TODO: do not use, this will likely be removed later!*/
    public boolean refreshOnSelect = false;
    /** Key to press for this command. */
    public @Nullable KeyBind keybind = null;

    public UnitCommand(String name, String icon, Func<Unit, AIController> controller){
        super(name);

        this.icon = icon;
        this.controller = controller == null ? u -> null : controller;
    }

    public UnitCommand(String name, String icon, KeyBind keybind, Func<Unit, AIController> controller){
        this(name, icon, controller);
        this.keybind = keybind;
    }

    public String localized(){
        return Core.bundle.get("command." + name);
    }

    public TextureRegionDrawable getIcon(){
        return Icon.icons.get(icon, Icon.cancel);
    }

    public char getEmoji() {
        return (char)Iconc.codes.get(icon, Iconc.cancel);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unitCommand;
    }

    @Override
    public String toString(){
        return "UnitCommand:" + name;
    }

    public static void loadAll(){

        moveCommand = new UnitCommand("move", "right", Binding.unitCommandMove, null){{
            drawTarget = true;
            resetTarget = false;
        }};
        repairCommand = new UnitCommand("repair", "modeSurvival", Binding.unitCommandRepair, u -> new RepairAI());
        rebuildCommand = new UnitCommand("rebuild", "hammer", Binding.unitCommandRebuild, u -> new BuilderAI());
        assistCommand = new UnitCommand("assist", "players", Binding.unitCommandAssist, u -> {
            var ai = new BuilderAI();
            ai.onlyAssist = true;
            return ai;
        });
        mineCommand = new UnitCommand("mine", "production", Binding.unitCommandMine, u -> new MinerAI()){{
            refreshOnSelect = true;
        }};
        boostCommand = new UnitCommand("boost", "up", Binding.unitCommandBoost, u -> new BoostAI()){{
            switchToMove = false;
            drawTarget = true;
            resetTarget = false;
        }};
        enterPayloadCommand = new UnitCommand("enterPayload", "downOpen", Binding.unitCommandEnterPayload, null){{
            switchToMove = false;
            drawTarget = true;
            resetTarget = false;
            snapToBuilding = true;
        }};
        loadUnitsCommand = new UnitCommand("loadUnits", "upload", Binding.unitCommandLoadUnits, null){{
            switchToMove = false;
            drawTarget = true;
            resetTarget = false;
        }};
        loadBlocksCommand = new UnitCommand("loadBlocks", "up", Binding.unitCommandLoadBlocks, null){{
            switchToMove = false;
            drawTarget = true;
            resetTarget = false;
            exactArrival = true;
        }};
        unloadPayloadCommand = new UnitCommand("unloadPayload", "download", Binding.unitCommandUnloadPayload, null){{
            switchToMove = false;
            drawTarget = true;
            resetTarget = false;
        }};
        loopPayloadCommand = new UnitCommand("loopPayload", "resize", Binding.unitCommandLoopPayload, null){{
            switchToMove = false;
            drawTarget = true;
            resetTarget = false;
        }};
    }
}
