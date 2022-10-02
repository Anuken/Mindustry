package mindustry.entities.abilities;

import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class LiquidRegenAbility extends Ability{
    public Liquid liquid;
    public float slurpSpeed = 9f;
    public float regenPerSlurp = 2.9f;
    public float slurpEffectChance = 0.4f;
    public Effect slurpEffect = Fx.heal;

    @Override
    public void update(Unit unit){
        //TODO timer?

        //TODO effects?
        if(unit.damaged()){
            boolean healed = false;
            int tx = unit.tileX(), ty = unit.tileY();
            int rad = Math.max((int)(unit.hitSize / tilesize * 0.6f), 1);
            for(int x = -rad; x <= rad; x++){
                for(int y = -rad; y <= rad; y++){
                    if(x*x + y*y <= rad*rad){

                        Tile tile = world.tile(tx + x, ty + y);
                        if(tile != null){
                            Puddle puddle = Puddles.get(tile);
                            if(puddle != null && puddle.liquid == liquid){
                                float fractionTaken = Math.min(puddle.amount, (slurpSpeed * Time.delta));
                                puddle.amount -= Math.min(puddle.amount, slurpSpeed * Time.delta);
                                unit.heal(fractionTaken * regenPerSlurp);
                                healed = true;
                            }
                        }
                    }
                }
            }

            if(healed && Mathf.chanceDelta(slurpEffectChance)){
                Tmp.v1.rnd(Mathf.random(unit.hitSize/2f));
                slurpEffect.at(unit.x + Tmp.v1.x, unit.y + Tmp.v1.y, unit.rotation, unit);
            }
        }
    }
}
