package mindustry.world.blocks.defense;

import static mindustry.Vars.*;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class ShockwaveTower extends Block{
    public int timerCheck = timers ++;

    public float range = 170f;
    public float reload = 45f;
    /** Base damage dealt to bullets. */
    public float bulletDamage = 80;
    /** Multiplier for damage dealt to missile units. */
    public float unitDamageMultiplier = -1f;
    /** Linearly decreases {@link #bulletDamage} when the number of bullets is higher than this value. */
    public int falloffCount = 12;
    /** Linearly decreases bullet speed up to this multiplier, proportional to (damage dealt to bullet / initial bullet damage). */
    public float slowdownMultiplier = 0.3f;
    /** Checking for bullets every frame is costly, so only do it at intervals even when ready. */
    public float checkInterval = 8f;
    /** % of reload randomly added or subtracted from the reload counter. Used for desyncing. <=0f to disable. */
    public float randReloadRange = 0.1f;
    /** What % of reload is needed to consider firing an extra time. The remaining time will be added to reload.  */
    public float quickFirePercentage = 0.5f;
    /** When cumulative bullet damage reaches {@link #bulletDamage} multiplied by this value, consider firing an extra time. */
    public float quickFireThreshold = 5f;

    public Sound shootSound = Sounds.shockwaveTower;
    public Color waveColor = Pal.accent, heatColor = Pal.turretHeat, shapeColor = Color.valueOf("f29c83");
    public float cooldownMultiplier = 1f;
    public Effect hitEffect = Fx.hitSquaresColor;
    public Effect waveEffect = Fx.pointShockwave;
    public float shake = 2f;

    /** Status effect applied to missile units hit. */
    public StatusEffect status = StatusEffects.none;
    public float statusDuration = 60f;

    //TODO switch to drawers eventually or something
    public float shapeRotateSpeed = 1f, shapeRadius = 6f;
    public int shapeSides = 4;

    public @Load("@-heat") TextureRegion heatRegion;

    public ShockwaveTower(String name){
        super(name);
        update = true;
        solid = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.damage, t -> {
            t.add("[stat]" + Strings.autoFixed(bulletDamage, 2) +
            (unitDamageMultiplier > 0f ? "[lightgray] ~ [stat]" + Strings.autoFixed(bulletDamage * unitDamageMultiplier, 2) + "[white] " + Core.bundle.get("bar.appliedmissiles") : "") +
            (falloffCount > 0 ? "[lightgray] ~ [white]" + Core.bundle.format("bar.falloffprojectile", Strings.format("[negstat]@+[lightgray]", falloffCount)) : ""));
        });
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.reload, 60f / reload, StatUnit.perSecond);
        if(status != StatusEffects.none || slowdownMultiplier > 0f){
            stats.add(Stat.slowdown, table -> {
                table.table(t -> {
                    t.defaults().left();
                    if(slowdownMultiplier > 0f){
                        t.add(Core.bundle.format("bar.upto", Strings.format("[stat]@", Strings.autoFixed((slowdownMultiplier - 1f) * 100f, 0)))
                        + StatUnit.percent.localized() + "[white] " + StatUnit.bulletSpeed.localized()).left().row();
                    }
                    if(status != StatusEffects.none){
                        t.add(StatValues.statusText(status, statusDuration) + "[white] " + Core.bundle.get("bar.appliedmissiles")).left();
                    }
                });
            });
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, waveColor);
    }

    public class ShockwaveTowerBuild extends Building{
        public float reloadCounter = Mathf.random(reload);
        public float damageSum = 0f, heat = 0f, setReload = reload;
        public boolean wasReady, isQuickFire;
        public Seq<Bullet> bullets = new Seq<>();
        public Seq<Unit> units = new Seq<>();

        @Override
        public void updateTile(){
            if(potentialEfficiency > 0){
                reloadCounter += edelta();
                boolean fire = reloadCounter >= setReload;

                //wasReady is used to immediately force a scan once when the tower is ready
                if((wasReady || timer(timerCheck, checkInterval)) && (fire || (!isQuickFire && reloadCounter >= setReload * quickFirePercentage))){
                    findTargets();

                    isQuickFire = !fire && damageSum >= bulletDamage * quickFireThreshold;
                    if((bullets.size > 0 || units.size > 0) && (fire || isQuickFire)){
                        fireEffect();
                    }else{
                        wasReady = false;
                    }
                }
            }

            heat = Mathf.clamp(heat - Time.delta / reload * cooldownMultiplier);
        }

        public void findTargets(){
            bullets.clear();
            units.clear();
            damageSum = 0f;

            if (Groups.bullet.isEmpty()) return;

            Groups.bullet.intersect(x - range, y - range, range * 2, range * 2, b -> {
                if(b.team != team && b.type.hittable && b.within(x, y, range + 1f)){
                    bullets.add(b);
                    damageSum += b.damage;
                }
            });

            if(status != StatusEffects.none){
                Units.nearby(x - range, y - range, range * 2, range * 2, u -> {
                    if(u.team != team && u.isMissile() && u.within(x, y, range + 1f)){
                        units.add(u);
                        damageSum += u.type.damageEstimate;
                    }
                });
            }
        }

        public void fireEffect(){
            heat = 1f;
            wasReady = true;

            setReload = isQuickFire ? 2f * reload - reloadCounter : reload;
            reloadCounter = randReloadRange > 0f ? Mathf.range(reload * randReloadRange) : 0f;

            waveEffect.at(x, y, range, waveColor);
            shootSound.at(x, y, 1f + Mathf.range(0.15f), 1f);
            Effect.shake(shake, shake, this);

            float totalTargets = bullets.size + units.size;
            float waveDamage = Math.min(bulletDamage, bulletDamage * falloffCount / totalTargets);

            for(var bullet : bullets){
                float ratio = Math.min(waveDamage / Math.max(bullet.type.damage, 1f), 1f);
                if(bullet.damage > waveDamage){
                    bullet.damage -= waveDamage;
                    bullet.vel.scl(Mathf.lerp(1f, slowdownMultiplier, ratio));
                }else{
                    bullet.remove();
                }
                hitEffect.at(bullet.x, bullet.y, waveColor);
            }

            for(var unit : units){
                if(unitDamageMultiplier > 0f) unit.damage(waveDamage * unitDamageMultiplier);
                unit.apply(status, statusDuration * falloffCount / totalTargets);
                hitEffect.at(unit.x, unit.y, waveColor);
            }

            if(team == state.rules.defaultTeam) Events.fire(Trigger.shockwaveTowerUse);
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case progress -> Mathf.clamp(reloadCounter / reload);
                case heat -> heat;
                default -> super.sense(sensor);
            };
        }

        @Override
        public float warmup(){
            return heat;
        }

        @Override
        public boolean shouldConsume(){
            return reloadCounter < setReload;
        }

        @Override
        public void draw(){
            super.draw();
            Drawf.additive(heatRegion, heatColor, heat, x, y, 0f, Layer.blockAdditive);

            Draw.z(Layer.effect);
            Draw.color(shapeColor, waveColor, Mathf.pow(heat, 2f));
            Fill.poly(x, y, shapeSides, shapeRadius * potentialEfficiency, Time.time * shapeRotateSpeed);
            Draw.color();
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, waveColor);
        }
    }
}
