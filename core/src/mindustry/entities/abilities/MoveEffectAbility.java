package mindustry.entities.abilities;

import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class MoveEffectAbility extends Ability{
    public float minVelocity = 0.08f;
    public float interval = 3f, chance = 0f;
    public int amount = 1;
    public float x, y, rotation, rangeX, rangeY, rangeLengthMin, rangeLengthMax;
    public boolean rotateEffect = false;
    public float effectParam = 3f;
    public boolean teamColor = false;
    public boolean parentizeEffects;
    public Color color = Color.white;
    public Effect effect = Fx.missileTrail;

    protected float counter;

    public MoveEffectAbility(float x, float y, Color color, Effect effect, float interval){
        this.x = x;
        this.y = y;
        this.color = color;
        this.effect = effect;
        this.interval = interval;
        display = false;
    }

    public MoveEffectAbility(){
        display = false;
    }

    @Override
    public void update(Unit unit){
        if(Vars.headless) return;

        counter += Time.delta;
        if(unit.vel.len2() >= minVelocity * minVelocity && (counter >= interval || (chance > 0 && Mathf.chanceDelta(chance))) && !unit.inFogTo(Vars.player.team())){
            if(rangeLengthMax > 0){
                Tmp.v1.trns(unit.rotation - 90f, x, y).add(Tmp.v2.rnd(Mathf.random(rangeLengthMin, rangeLengthMax)));
            }else{
                Tmp.v1.trns(unit.rotation - 90f, x + Mathf.range(rangeX), y + Mathf.range(rangeY));
            }

            counter %= interval;
            for(int i = 0; i < amount; i++){
                effect.at(Tmp.v1.x + unit.x, Tmp.v1.y + unit.y, (rotateEffect ? unit.rotation : effectParam) + rotation, teamColor ? unit.team.color : color, parentizeEffects ? unit : null);
            }
        }
    }
}
