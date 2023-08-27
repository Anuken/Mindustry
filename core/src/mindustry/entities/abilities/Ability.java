package mindustry.entities.abilities;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;

public abstract class Ability implements Cloneable{
    /** If false, this ability does not show in unit stats. */
    public boolean display = true;
    //the one and only data variable that is synced.
    public float data;

    public void update(Unit unit){}
    public void draw(Unit unit){}
    public void death(Unit unit){}
    public void init(UnitType type){}
    public void displayBars(Unit unit, Table bars){}
    public void addStats(Table t){}

    public Ability copy(){
        try{
            return (Ability)clone();
        }catch(CloneNotSupportedException e){
            //I am disgusted
            throw new RuntimeException("java sucks", e);
        }
    }

    /** @return localized ability name; mods should override this. */
    public String localized(){
        var type = getClass();
        return Core.bundle.get("ability." + (type.isAnonymousClass() ? type.getSuperclass() : type).getSimpleName().replace("Ability", "").toLowerCase());
    }
}
