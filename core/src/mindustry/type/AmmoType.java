package mindustry.type;

import arc.graphics.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/** Type of ammo that a unit uses. */
public class AmmoType extends Content{
    public String icon = Iconc.itemCopper + "";
    public Color color = Pal.ammo;
    public Color barColor = Pal.ammo;

    public AmmoType(char icon, Color color){
        this.icon = icon + "";
        this.color = color;
    }

    public AmmoType(){
    }

    public void resupply(Unit unit){}

    @Override
    public ContentType getContentType(){
        return ContentType.ammo;
    }
}
