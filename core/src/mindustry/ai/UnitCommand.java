package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.input.*;

/** Defines a pattern of behavior that an RTS-controlled unit should follow. Shows up in the command UI. */
public class UnitCommand{
    /** List of all commands by ID. */
    public static final Seq<UnitCommand> all = new Seq<>();

    public static final UnitCommand

    moveCommand = new UnitCommand("move", "right", Binding.unit_command_move, null){{
        drawTarget = true;
        resetTarget = false;
    }},
    repairCommand = new UnitCommand("repair", "modeSurvival", Binding.unit_command_repair, u -> new RepairAI()),
    rebuildCommand = new UnitCommand("rebuild", "hammer", Binding.unit_command_rebuild, u -> new BuilderAI()),
    assistCommand = new UnitCommand("assist", "players", Binding.unit_command_assist, u -> {
        var ai = new BuilderAI();
        ai.onlyAssist = true;
        return ai;
    }),
    mineCommand = new UnitCommand("mine", "production", Binding.unit_command_mine, u -> new MinerAI()),
    boostCommand = new UnitCommand("boost", "up", Binding.unit_command_boost, u -> new BoostAI()){{
        switchToMove = false;
        drawTarget = true;
        resetTarget = false;
    }},
    loadUnitsCommand = new UnitCommand("loadUnits", "download", Binding.unit_command_load_units, null){{
        switchToMove = false;
        drawTarget = true;
        resetTarget = false;
    }},
    loadBlocksCommand = new UnitCommand("loadBlocks", "down", Binding.unit_command_load_blocks, null){{
        switchToMove = false;
        drawTarget = true;
        resetTarget = false;
    }},
    unloadPayloadCommand = new UnitCommand("unloadPayload", "upload", Binding.unit_command_unload_payload, null){{
        switchToMove = false;
        drawTarget = true;
        resetTarget = false;
    }};

    /** Unique ID number. */
    public final int id;
    /** Named used for tooltip/description. */
    public final String name;
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
    /** Key to press for this command. */
    public @Nullable Binding keybind = null;

    public UnitCommand(String name, String icon, Func<Unit, AIController> controller){
        this.name = name;
        this.icon = icon;
        this.controller = controller == null ? u -> null : controller;

        id = all.size;
        all.add(this);
    }

    public UnitCommand(String name, String icon, Binding keybind, Func<Unit, AIController> controller){
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
        return (char) Iconc.codes.get(icon, Iconc.cancel);
    }

    @Override
    public String toString(){
        return "UnitCommand:" + name;
    }
}
