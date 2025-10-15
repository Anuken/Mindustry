package mindustry.entities.abilities;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class EnergyFieldAbility extends Ability{
    private static final Seq<Healthc> all = new Seq<>();

    public float damage = 1, reload = 100, range = 60;
    public Effect healEffect = Fx.heal, hitEffect = Fx.hitLaserBlast, damageEffect = Fx.chainLightning;
    public StatusEffect status = StatusEffects.electrified;
    public Sound shootSound = Sounds.spark;
    public float statusDuration = 60f * 6f;
    public float x, y;
    public boolean targetGround = true, targetAir = true, hitBuildings = true, hitUnits = true;
    public int maxTargets = 25;
    public float healPercent = 3f;
    /** Multiplies healing to units of the same type by this amount. */
    public float sameTypeHealMult = 1f;
    public boolean displayHeal = true;

    public float layer = Layer.bullet - 0.001f, blinkScl = 20f, blinkSize = 0.1f;
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
    public void addStats(Table t){
        if(displayHeal){
            t.add(Core.bundle.get(getBundle() + ".healdescription")).wrap().width(descriptionWidth);
        }else{
            t.add(Core.bundle.get(getBundle() + ".description")).wrap().width(descriptionWidth);
        }
        t.row();

        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(range / tilesize, 2)));
        t.row();
        t.add(abilityStat("firingrate", Strings.autoFixed(60f / reload, 2)));
        t.row();
        t.add(abilityStat("maxtargets", maxTargets));
        t.row();
        t.add(Core.bundle.format("bullet.damage", damage));
        if(status != StatusEffects.none){
            t.row();
            t.add((status.hasEmoji() ? status.emoji() : "") + "[stat]" + status.localizedName).with(l -> StatValues.withTooltip(l, status));
        }
        if(displayHeal){
            t.row();
            t.add(Core.bundle.format("bullet.healpercent", Strings.autoFixed(healPercent, 2)));
            t.row();
            t.add(abilityStat("sametypehealmultiplier", (sameTypeHealMult < 1f ? "[negstat]" : "") + Strings.autoFixed(sameTypeHealMult * 100f, 2)));
        }
    }

    @Override
    public void draw(Unit unit){
        super.draw(unit);

        Draw.z(layer);
        Draw.color(color);
        Tmp.v1.trns(unit.rotation - 90, x, y).add(unit.x, unit.y);
        float rx = Tmp.v1.x, ry = Tmp.v1.y;
        float orbRadius = effectRadius * (1f + Mathf.absin(blinkScl, blinkSize));

        Fill.circle(rx, ry, orbRadius);
        Draw.color();
        Fill.circle(rx, ry, orbRadius / 2f);

        Lines.stroke((0.7f + Mathf.absin(blinkScl, 0.7f)), color);

        for(int i = 0; i < sectors; i++){
            float rot = unit.rotation + i * 360f/sectors - Time.time * rotateSpeed;
            Lines.arc(rx, ry, orbRadius + 3f, sectorRad, rot);
        }

        Lines.stroke(Lines.getStroke() * curStroke);

        if(curStroke > 0){
            for(int i = 0; i < sectors; i++){
                float rot = unit.rotation + i * 360f/sectors + Time.time * rotateSpeed;
                Lines.arc(rx, ry, range, sectorRad, rot);
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

            if(hitUnits){
                Units.nearby(null, rx, ry, range, other -> {
                    if(other != unit && other.checkTarget(targetAir, targetGround) && other.targetable(unit.team) && (other.team != unit.team || other.damaged())){
                        all.add(other);
                    }
                });
            }

            if(hitBuildings && targetGround){
                Units.nearbyBuildings(rx, ry, range, b -> {
                    if((b.team != Team.derelict || state.rules.coreCapture) && ((b.team != unit.team && b.block.targetable) || b.damaged()) && !b.block.privileged){
                        all.add(b);
                    }
                });
            }

            all.sort(h -> h.dst2(rx, ry));
            int len = Math.min(all.size, maxTargets);
            float scaledDamage = damage * state.rules.unitDamage(unit.team) * unit.damageMultiplier;

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
                        float healMult = (other instanceof Unit u && u.type == unit.type) ? sameTypeHealMult : 1f;
                        other.heal(healPercent / 100f * other.maxHealth() * healMult);
                        healEffect.at(other);
                        damageEffect.at(rx, ry, 0f, color, other);
                        hitEffect.at(rx, ry, unit.angleTo(other), color);

                        if(other instanceof Building b){
                            Fx.healBlockFull.at(b.x, b.y, 0f, color, b.block);
                        }
                    }
                }else{
                    anyNearby = true;
                    if(other instanceof Building b){
                        b.damage(unit.team, scaledDamage);
                    }else{
                        other.damage(scaledDamage);
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
