package mindustry.world.meta.values;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class AmmoListValue<T extends UnlockableContent> implements StatValue{
    private final ObjectMap<T, BulletType> map;
    
    private final float indent;

    public AmmoListValue(ObjectMap<T, BulletType> map){
        this.map = map;
        this.indent = 0f;
    }

    public AmmoListValue(ObjectMap<T, BulletType> map, float indent){
        this.map = map;
        this.indent = indent;
    }

    @Override
    public void display(Table table){

        table.row();

        for(T t : map.keys()){
            boolean unit = t instanceof UnitType;

            BulletType type = map.get(t);
            boolean isFrag = indent > 0f;

            //no point in displaying unit icon twice
            if(!unit && !isFrag){
                table.image(icon(t)).size(3 * 8).padRight(4).right().top();
                table.add(t.localizedName).padRight(10).left().top();
            }

            table.table(bt -> {
                bt.left().defaults().padRight(3).left();

                if(type.damage > 0 && (type.collides || type.splashDamage <= 0)){
                    bt.add(Core.bundle.format("bullet.damage", type.damage));
                }

                if(type.buildingDamageMultiplier != 1){
                    sep(bt, Core.bundle.format("bullet.buildingdamage", (int)(type.buildingDamageMultiplier * 100)));
                }

                if(type.splashDamage > 0){
                    sep(bt, Core.bundle.format("bullet.splashdamage", (int)type.splashDamage, Strings.fixed(type.splashDamageRadius / tilesize, 1)));
                }

                if(!unit && !Mathf.equal(type.ammoMultiplier, 1f) && !(type instanceof LiquidBulletType) && !isFrag){
                    sep(bt, Core.bundle.format("bullet.multiplier", (int)type.ammoMultiplier));
                }

                if(!Mathf.equal(type.reloadMultiplier, 1f) && !isFrag){
                    sep(bt, Core.bundle.format("bullet.reload", Strings.autoFixed(type.reloadMultiplier, 2)));
                }

                if(type.knockback > 0){
                    sep(bt, Core.bundle.format("bullet.knockback", Strings.autoFixed(type.knockback, 2)));
                }

                if(type.healPercent > 0f){
                    sep(bt, Core.bundle.format("bullet.healpercent", (int)type.healPercent));
                }

                if(type.pierce || type.pierceCap != -1){
                    sep(bt, type.pierceCap == -1 ? "@bullet.infinitepierce" : Core.bundle.format("bullet.pierce", type.pierceCap));
                }

                if(type.status == StatusEffects.burning || type.status == StatusEffects.melting || type.incendAmount > 0){
                    sep(bt, "@bullet.incendiary");
                }

                if(type.status == StatusEffects.freezing){
                    sep(bt, "@bullet.freezing");
                }

                if(type.status == StatusEffects.tarred){
                    sep(bt, "@bullet.tarred");
                }

                if(type.status == StatusEffects.sapped){
                    sep(bt, "@bullet.sapping");
                }

                if(type.homingPower > 0.01f){
                    sep(bt, "@bullet.homing");
                }

                if(type.lightning > 0){
                    sep(bt, "@bullet.shock");
                }

                if(type.fragBullet != null){
                    sep(bt, Core.bundle.format("bullet.fragbullets", type.fragBullets));
                    sep(bt, "@bullet.frag.stats");
                    bt.row();

                    ObjectMap<T, BulletType> fragMap = new ObjectMap<>();
                    fragMap = OrderedMap.of(t, type.fragBullet);
                    float i = indent + 1f;
                    StatValue fragStatValue = new AmmoListValue<>(fragMap, i);

                    fragStatValue.display(bt);
                }
            }).padTop(unit || isFrag ? 0 : -9).padLeft(indent * 8).left().get().background(unit || isFrag ? null : Tex.underline);

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
