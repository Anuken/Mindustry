package mindustry.entities.abilities;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

public abstract class Ability implements Cloneable{
    protected static final float descriptionWidth = 350f;
    /** If false, this ability does not show in unit stats. */
    public boolean display = true;
    //the one and only data variable that is synced.
    public float data;

    public void update(Unit unit){}

    public void draw(Unit unit){}

    public void death(Unit unit){}

    public void created(Unit unit){}

    public void init(UnitType type){}

    public void displayBars(Unit unit, Table bars){}

    public void display(Table t){
        t.table(Styles.grayPanel, a -> {
            a.add("[accent]" + localized()).padBottom(4).center().top().expandX();
            a.row();
            a.left().top().defaults().left();
            addStats(a);
        }).pad(5).margin(10).growX().top().uniformX();
    }

    public void addStats(Table t){
        if(Core.bundle.has(getBundle() + ".description")){
            t.add(Core.bundle.get(getBundle() + ".description")).wrap().width(descriptionWidth);
            t.row();
        }
    }

    public String abilityStat(String stat, Object... values){
        return Core.bundle.format("ability.stat." + stat, values);
    }

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
        return Core.bundle.get(getBundle());
    }

    public String getBundle(){
        var type = getClass();
        return "ability." + (type.isAnonymousClass() ? type.getSuperclass() : type).getSimpleName().replace("Ability", "").toLowerCase();
    }
}
