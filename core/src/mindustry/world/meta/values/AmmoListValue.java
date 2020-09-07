package mindustry.world.meta.values;

import arc.*;
import arc.struct.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.ui.Cicon;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

public class AmmoListValue<T extends UnlockableContent> implements StatValue{
    private final ObjectMap<T, BulletType> map;

    public AmmoListValue(ObjectMap<T, BulletType> map){
        this.map = map;
    }

    @Override
    public void display(Table table){

        table.row();
        for(T t : map.keys()){
            BulletType type = map.get(t);
            table.image(icon(t)).size(3 * 8).padRight(4).right().top();
            table.add(t.localizedName).padRight(10).left().top();
            table.table(Tex.underline, bt -> {
                bt.left().defaults().padRight(3).left();

                if(type.damage > 0 && type.collides){
                    bt.add(Core.bundle.format("bullet.damage", type.damage));
                }

                if(type.splashDamage > 0){
                    sep(bt, Core.bundle.format("bullet.splashdamage", (int)type.splashDamage, Strings.fixed(type.splashDamageRadius / tilesize, 1)));
                }

                if(!Mathf.equal(type.ammoMultiplier, 1f))
                    sep(bt, Core.bundle.format("bullet.multiplier", (int)type.ammoMultiplier));
                if(!Mathf.equal(type.reloadMultiplier, 1f))
                    sep(bt, Core.bundle.format("bullet.reload", Strings.fixed(type.reloadMultiplier, 1)));

                if(type.knockback > 0){
                    sep(bt, Core.bundle.format("bullet.knockback", Strings.fixed(type.knockback, 1)));
                }

                if((type.status == StatusEffects.burning || type.status == StatusEffects.melting) || type.incendAmount > 0){
                    sep(bt, "$bullet.incendiary");
                }

                if(type.status == StatusEffects.freezing){
                    sep(bt, "$bullet.freezing");
                }

                if(type.status == StatusEffects.tarred){
                    sep(bt, "$bullet.tarred");
                }

                if(type.homingPower > 0.01f){
                    sep(bt, "$bullet.homing");
                }

                if(type.lightning > 0){
                    sep(bt, "$bullet.shock");
                }

                if(type.fragBullet != null){
                    sep(bt, "$bullet.frag");
                }
            }).left().padTop(-9);
            table.row();
        }
    }

    void sep(Table table, String text){
        table.row();
        table.add(text);
    }

    TextureRegion icon(T t){
        return t.icon(Cicon.medium);
    }
}
