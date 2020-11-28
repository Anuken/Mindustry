package mindustry.world.meta.values;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class WeaponListValue implements StatValue{
    private final Seq<Weapon> weapons;
    private final UnitType unit;

    public WeaponListValue(UnitType unit, Seq<Weapon> weapons){
        this.weapons = weapons;
        this.unit = unit;
    }

    @Override
    public void display(Table table){
        table.row();
        for(int i = 0; i < weapons.size;i ++){
            Weapon weapon = weapons.get(i);

            if(weapon.flipSprite){
                //flipped weapons are not given stats
                continue;
            }

            TextureRegion region = !weapon.name.equals("") && weapon.outlineRegion.found() ? weapon.outlineRegion : unit.icon(Cicon.full);

            table.image(region).size(60).scaling(Scaling.bounded).right().top();

            table.table(Tex.underline,  w -> {
                w.left().defaults().padRight(3).left();

                if(weapon.inaccuracy > 0){
                    sep(w, "[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)weapon.inaccuracy + " " + StatUnit.degrees.localized());
                }
                sep(w, "[lightgray]" + Stat.reload.localized() + ": " + (weapon.mirror ? "2x " : "") + "[white]" + Strings.autoFixed(60f / weapon.reload * weapon.shots, 1));

                var bullet = new AmmoListValue<UnitType>(OrderedMap.of(unit, weapon.bullet));
                bullet.display(w);
            }).padTop(-9).left();
            table.row();
        }
    }

    void sep(Table table, String text){
        table.row();
        table.add(text);
    }
}
