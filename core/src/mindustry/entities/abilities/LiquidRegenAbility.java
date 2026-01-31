package mindustry.entities.abilities;

import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LiquidRegenAbility extends Ability{
    public float slurpSpeed = 5f;
    public float regenPerSlurp = 6f;
    public float slurpEffectChance = 0.4f;
    public Effect slurpEffect = Fx.heal;

    public Seq<Liquid> liquids = new Seq<>();
    public ObjectMap<Liquid, Float> liqSlurpSpeed = new ObjectMap<>(), liqRegenPerSlurp = new ObjectMap<>(), liqSlurpEffectChance = new ObjectMap<>();
    public ObjectMap<Liquid, Effect> liqSlurpEffect = new ObjectMap<>();

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.row();
        t.table(c -> {
            for(Liquid liq : liquids){
                float regen = liqRegenPerSlurp.get(liq, regenPerSlurp);
                float speed = liqSlurpSpeed.get(liq, slurpSpeed);
  
                c.table(Styles.grayPanelDark, b -> {
                    b.left().top().defaults().padRight(3).left();
                    b.add((liq.hasEmoji() ? liq.emoji() : "") + "[stat]" + liq.localizedName).row();
                    if(regen > 0f){
                        b.add(abilityStat("slurpheal", Strings.autoFixed(regen, 2))).row();
                        b.row();
                    }
                    if(speed > 0f){
                        b.add(abilityStat("slurpspeed", Strings.autoFixed(speed * 60f, 2))).row();
                    }
                }).padTop(5).padBottom(5).growX().margin(10);
                c.row();
            }
        }).growX().colspan(t.getColumns());
    }

    //making a new class would be cleaner, specially with a lot of fields
    public void setLiquidStats(Liquid liq, float regenAmount){
        setLiquidStats(liq, regenAmount, -1f, -1f, null);
    }

    public void setLiquidStats(Liquid liq, Effect effect){
        setLiquidStats(liq, -1f, -1f, -1f, effect);
    }

    public void setLiquidStats(Liquid liq, float regenAmount, float slurpSpd, float effectChance, @Nullable Effect effect){
        liquids.add(liq);
        liqRegenPerSlurp.put(liq, regenAmount > 0f ? regenAmount : regenPerSlurp);
        liqSlurpSpeed.put(liq, slurpSpd > 0f ? slurpSpd : slurpSpeed);
        liqSlurpEffectChance.put(liq, effectChance > 0f ? effectChance : slurpEffectChance);
        liqSlurpEffect.put(liq, effect != null ? effect : slurpEffect);
    }

    @Override
    public void update(Unit unit){
        //TODO timer?

        //TODO effects?
        if(unit.damaged() && !unit.isFlying()){
            int tx = unit.tileX(), ty = unit.tileY();
            int rad = Math.max((int)(unit.hitSize / tilesize * 0.6f), 1);
            for(int x = -rad; x <= rad; x++){
                for(int y = -rad; y <= rad; y++){
                    if(x*x + y*y <= rad*rad){

                        Tile tile = world.tile(tx + x, ty + y);
                        if(tile != null){
                            Puddle puddle = Puddles.get(tile);
                            if(puddle != null){
                                boolean healed = false;
                                for(Liquid liq : liquids){
                                    if(puddle.liquid == liq){
                                        healed = true;
                                        break;
                                    }
                                }
                                
                                if(healed){
                                    float speed = liqSlurpSpeed.get(puddle.liquid);
                                    puddle.amount -= Math.min(puddle.amount, speed * Time.delta);
                                    unit.heal(Math.min(puddle.amount, (speed * Time.delta)) * liqRegenPerSlurp.get(puddle.liquid));
                                    
                                    if(Mathf.chanceDelta(liqSlurpEffectChance.get(puddle.liquid))){
                                        Tmp.v1.rnd(Mathf.random(unit.hitSize/2f));
                                        liqSlurpEffect.get(puddle.liquid).at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, unit.rotation, unit);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
