package mindustry.entities.abilities;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class EnergyFieldAbility extends Ability{
    private static final Seq<Healthc> all = new Seq<>();

    public float damage = 1, reload = 100, range = 60;
    public Effect healEffect = Fx.heal, hitEffect = Fx.hitLaserBlast, damageEffect = Fx.chainLightning;
    public StatusEffect status = StatusEffects.electrified;
    public Sound shootSound = Sounds.spark;
    public float statusDuration = 60f * 6f;
    public float x, y;
    public boolean hitBuildings = true;
    public int maxTargets = 25;
    public float healPercent = 2.5f;

    public float layer = Layer.bullet - 0.001f, blinkScl = 20f;
    public float effectRadius = 5f, sectorRad = 0.14f, rotateSpeed = 0.5f;
    public int sectors = 5;
    public Color color = Pal.heal;
    public boolean useAmmo = true;

    protected float timer, curStroke;
    protected boolean anyNearby = false;

    EnergyFieldAbility(){}

    public EnergyFieldAbility(float damage, float reload, float range){
        this.damage = damage;
        this.reload = reload;
        this.range = range;
    }

    @Override
    public String localized(){
        return Core.bundle.format("ability.energyfield", damage, range / Vars.tilesize, maxTargets);
    }

    @Override
    public void draw(Unit unit){
        super.draw(unit);

        Draw.z(layer);
        Draw.color(color);
        Tmp.v1.trns(unit.rotation - 90, x, y).add(unit.x, unit.y);
        float rx = Tmp.v1.x, ry = Tmp.v1.y;
        float orbRadius = effectRadius * (1f + Mathf.absin(blinkScl, 0.1f));

        Fill.circle(rx, ry, orbRadius);
        Draw.color();
        Fill.circle(rx, ry, orbRadius / 2f);

        Lines.stroke((0.7f + Mathf.absin(blinkScl, 0.7f)), color);

        for(int i = 0; i < sectors; i++){
            float rot = unit.rotation + i * 360f/sectors - Time.time * rotateSpeed;
            Lines.swirl(rx, ry, orbRadius + 3f, sectorRad, rot);
        }

        Lines.stroke(Lines.getStroke() * curStroke);

        if(curStroke > 0){
            for(int i = 0; i < sectors; i++){
                float rot = unit.rotation + i * 360f/sectors + Time.time * rotateSpeed;
                Lines.swirl(rx, ry, range, sectorRad, rot);
            }
        }

        Drawf.light(rx, ry, range * 1.5f, color, curStroke * 0.8f);

        Draw.reset();
    }

    @Override
    public void update(Unit unit){

        curStroke = Mathf.lerpDelta(curStroke, anyNearby ? 1 : 0, 0.09f);

        if((timer += Time.delta) >= reload && (!useAmmo || unit.ammo > 0 || !state.rules.unitAmmo)){
            Tmp.v1.trns(unit.rotation - 90, x, y).add(unit.x, unit.y);
            float rx = Tmp.v1.x, ry = Tmp.v1.y;
            anyNearby = false;

            all.clear();

            Units.nearby(null, rx, ry, range, other -> {
                if(other != unit){
                    all.add(other);
                }
            });

            if(hitBuildings){
                Units.nearbyBuildings(rx, ry, range, all::add);
            }

            all.sort(h -> h.dst2(rx, ry));
            int len = Math.min(all.size, maxTargets);
            for(int i = 0; i < len; i++){
                Healthc other = all.get(i);

                //lightning gets absorbed by plastanium
                var absorber = Damage.findAbsorber(unit.team, rx, ry, other.getX(), other.getY());
                if(absorber != null){
                    other = absorber;
                }

                if(((Teamc)other).team() == unit.team){
                    if(other.damaged()){
                        anyNearby = true;
                        other.heal(healPercent / 100f * other.maxHealth());
                        healEffect.at(other);
                        damageEffect.at(rx, ry, 0f, color, other);
                        hitEffect.at(rx, ry, unit.angleTo(other), color);

                        if(other instanceof Building b){
                            Fx.healBlockFull.at(b.x, b.y, b.block.size, color);
                        }
                    }
                }else{
                    anyNearby = true;
                    if(other instanceof Building b){
                        b.damage(unit.team, damage);
                    }else{
                        other.damage(damage);
                    }
                    if(other instanceof Statusc s){
                        s.apply(status, statusDuration);
                    }
                    hitEffect.at(other.x(), other.y(), unit.angleTo(other), color);
                    damageEffect.at(rx, ry, 0f, color, other);
                    hitEffect.at(rx, ry, unit.angleTo(other), color);
                }
            }

            if(anyNearby){
                shootSound.at(unit);

                if(useAmmo && state.rules.unitAmmo){
                    unit.ammo --;
                }
            }

            timer = 0f;
        }
    }
}
