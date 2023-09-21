package mindustry.ai;

import arc.*;
import arc.scene.style.*;
import arc.struct.*;
import mindustry.gen.*;

public class UnitStance{
    /** List of all stances by ID. */
    public static final Seq<UnitStance> all = new Seq<>();

    public static final UnitStance

    stopStance = new UnitStance("stop", "cancel"), //not a real stance, cannot be selected, just cancels ordewrs
    shootStance = new UnitStance("shoot", "commandAttack"),
    holdFireStance = new UnitStance("holdfire", "none");

    /** Unique ID number. */
    public final int id;
    /** Named used for tooltip/description. */
    public final String name;
    /** Name of UI icon (from Icon class). */
    public final String icon;

    public UnitStance(String name, String icon){
        this.name = name;
        this.icon = icon;

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
