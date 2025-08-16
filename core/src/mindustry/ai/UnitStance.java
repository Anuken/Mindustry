package mindustry.ai;

import arc.*;
import arc.input.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.type.*;

public class UnitStance extends MappableContent{
    public static UnitStance stop, holdFire, pursueTarget, patrol, ram, mineAuto;

    /** Name of UI icon (from Icon class). */
    public String icon;
    /** Key to press for this stance. */
    public @Nullable KeyBind keybind;
    /** Stances that are mutually exclusive to this stance. This is used for convenience, for writing only! */
    public Seq<UnitStance> incompatibleStances = new Seq<>();
    /** Incompatible stances as a bitset for easier operations. This is where incompatibility is actually stored. */
    public Bits incompatibleBits = new Bits(32);
    /** If true, this stance can be toggled on or off. */
    public boolean toggle = true;

    public UnitStance(String name, String icon, KeyBind keybind, boolean toggle){
        super(name);
        this.icon = icon;
        this.keybind = keybind;
        this.toggle = toggle;
    }

    public UnitStance(String name, String icon, KeyBind keybind){
        this(name, icon, keybind, true);
    }

    @Override
    public void init(){
        super.init();

        for(var stance : incompatibleStances){
            if(stance == this) continue;
            incompatibleBits.set(stance.id);
            stance.incompatibleBits.set(id);
        }
    }

    public String localized(){
        return Core.bundle.get("stance." + name);
    }

    public TextureRegionDrawable getIcon(){
        return Icon.icons.get(icon, Icon.cancel);
    }

    public char getEmoji() {
        return (char)Iconc.codes.get(icon, Iconc.cancel);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unitStance;
    }

    @Override
    public String toString(){
        return "UnitStance:" + name;
    }

    public static void loadAll(){
        stop = new UnitStance("stop", "cancel", Binding.cancelOrders, false);
        holdFire = new UnitStance("holdfire", "none", Binding.unitStanceHoldFire);
        pursueTarget = new UnitStance("pursuetarget", "right", Binding.unitStancePursueTarget);
        patrol = new UnitStance("patrol", "refresh", Binding.unitStancePatrol);
        ram = new UnitStance("ram", "rightOpen", Binding.unitStanceRam);
        mineAuto = new UnitStance("mineauto", "settings", null, false);

        //Only vanilla items are supported for now
        for(Item item : Vars.content.items()){
            new ItemUnitStance(item);
        }
    }
}
