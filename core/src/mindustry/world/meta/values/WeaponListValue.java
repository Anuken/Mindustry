package mindustry.world.meta.values;

import arc.*;
import arc.util.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

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
        for(int i = 0;i < weapons.size;i ++){
            Weapon weapon = weapons.get(i);

            if(weapon.flipSprite){
                //fliped weapons are not given stats
                continue;
            }

            table.image(Core.atlas.find(unit.name + "-weapon" + i)).size(15 * 8).right().top();
            table.table(Tex.underline, w -> {
                w.left().defaults().padRight(3).left();

                sep(w, "[lightgray]" + Stat.mirrored.localized() + ": [white]" + weapon.mirror);
                sep(w, "[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)weapon.inaccuracy + " " + StatUnit.degrees.localized());
                sep(w, "[lightgray]" + Stat.reload.localized() + ": [white]" + Strings.autoFixed(60f / weapon.reload * weapon.shots, 1));
                sep(w, "[lightgray]" + Stat.targetsAir.localized() + ": [white]" + weapon.bullet.collidesAir);
                sep(w, "[lightgray]" + Stat.targetsGround.localized() + ": [white]" + weapon.bullet.collidesGround);
                sep(w, "[lightgray]" + Stat.bullet.localized() + ":");

                AmmoListValue bullet = new AmmoListValue(OrderedMap.of(unit, weapon.bullet));
                bullet.display(w);
            }).left().padTop(-9);
            table.row();
        }
    }

    void sep(Table table, String text){
        table.row();
        table.add(text);
    }
}
