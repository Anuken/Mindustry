package mindustry.ai;

import arc.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.*;

public class UnitStance{
    /** List of all stances by ID. */
    public static final Seq<UnitStance> all = new Seq<>();

    public static final UnitStance

    stop = new UnitStance("stop", "cancel", Binding.cancel_orders), //not a real stance, cannot be selected, just cancels ordewrs
    shoot = new UnitStance("shoot", "commandAttack", Binding.unit_stance_shoot),
    holdFire = new UnitStance("holdfire", "none", Binding.unit_stance_hold_fire),
    pursueTarget = new UnitStance("pursuetarget", "right", Binding.unit_stance_pursue_target),
    patrol = new UnitStance("patrol", "refresh", Binding.unit_stance_patrol),
    ram = new UnitStance("ram", "rightOpen", Binding.unit_stance_ram);

    /** Unique ID number. */
    public final int id;
    /** Named used for tooltip/description. */
    public final String name;
    /** Name of UI icon (from Icon class). */
    public final String icon;
    /** Key to press for this stance. */
    public @Nullable Binding keybind = null;

    public UnitStance(String name, String icon, Binding keybind){
        this.name = name;
        this.icon = icon;
        this.keybind = keybind;

        id = all.size;
        all.add(this);
    }

    public String localized(){
        return Core.bundle.get("stance." + name);
    }

    public TextureRegionDrawable getIcon(){
        return Icon.icons.get(icon, Icon.cancel);
    }

    public char getEmoji() {
        return (char) Iconc.codes.get(icon, Iconc.cancel);
    }

    @Override
    public String toString(){
        return "UnitStance:" + name;
    }
}
