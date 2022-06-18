package mindustry.entities.abilities;

import arc.graphics.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class MoveEffectAbility extends Ability{
    public float minVelocity = 0.08f;
    public float interval = 3f;
    public float x, y;
    public boolean rotateEffect = false;
    public float effectParam = 3f;
    public boolean teamColor = false;
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
    }

    @Override
    public void update(Unit unit){
        counter += Time.delta;
        if(unit.vel.len2() >= minVelocity * minVelocity && (counter >= interval)){
            Tmp.v1.trns(unit.rotation - 90f, x, y);
            counter %= interval;
            effect.at(Tmp.v1.x + unit.x, Tmp.v1.y + unit.y, rotateEffect ? unit.rotation : effectParam, teamColor ? unit.team.color : color);
        }
    }
}
