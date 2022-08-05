package mindustry.ai;

import arc.*;
import arc.func.*;
import arc.struct.*;
import mindustry.ai.types.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/** Defines a pattern of behavior that an RTS-controlled unit should follow. Shows up in the command UI. */
public class UnitCommand{
    /** List of all commands by ID. */
    public static final Seq<UnitCommand> all = new Seq<>();

    public static final UnitCommand

    //TODO they do not use the command "interface" or designation at all
    moveCommand = new UnitCommand("move", "right", u -> null),
    repairCommand = new UnitCommand("repair", "modeSurvival", u -> new RepairAI()),
    rebuildCommand = new UnitCommand("rebuild", "hammer", u -> new BuilderAI()),
    assistCommand = new UnitCommand("assist", "players", u -> {
        var ai = new BuilderAI();
        ai.onlyAssist = true;
        return ai;
    }),
    mineCommand = new UnitCommand("mine", "production", u -> new MinerAI());

    /** Default set of specified commands. */
    public static final UnitCommand[] defaultCommands = {moveCommand};

    /** Unique ID number. */
    public final int id;
    /** Named used for tooltip/description. */
    public final String name;
    /** Name of UI icon (from Icon class). */
    public final String icon;
    /** Controller that this unit will use when this command is used. Return null for "default" behavior. */
    public final Func<Unit, AIController> controller;

    public UnitCommand(String name, String icon, Func<Unit, AIController> controller){
        this.name = name;
        this.icon = icon;
        this.controller = controller;

        id = all.size;
        all.add(this);
    }

    public String localized(){
        return Core.bundle.get("command." + name);
    }
}
