package mindustry.logic;

import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.world.*;

public class LogicFx{
    private static OrderedMap<String, EffectEntry> map = new OrderedMap<>();

    static{
        map.putAll(
        "warn", new EffectEntry(Fx.unitCapKill),
        "cross", new EffectEntry(Fx.unitEnvKill),
        "blockFall", new EffectEntry(Fx.blockCrash).data(Block.class).bounds(100f),
        "placeBlock", new EffectEntry(Fx.placeBlock).size(),
        "placeBlockSpark", new EffectEntry(Fx.coreLaunchConstruct).size(),
        "breakBlock", new EffectEntry(Fx.breakBlock).size(),
        "spawn", new EffectEntry(Fx.spawn),
        "trail", new EffectEntry(Fx.colorTrail).size().color(),
        "breakProp", new EffectEntry(Fx.breakProp).size().color(),
        "smokeCloud", new EffectEntry(Fx.missileTrailSmoke).color(),
        "vapor", new EffectEntry(Fx.vapor).color(),
        "hit", new EffectEntry(Fx.hitBulletColor).color(),
        "hitSquare", new EffectEntry(Fx.hitSquaresColor).color(),
        "shootSmall", new EffectEntry(Fx.shootSmall).color().rotate(),
        "shootBig", new EffectEntry(Fx.shootTitan).color().rotate(),
        "smokeSmall", new EffectEntry(Fx.shootSmallSmoke).rotate(),
        "smokeBig", new EffectEntry(Fx.shootBigSmoke).rotate(),
        "smokeColor", new EffectEntry(Fx.shootSmokeTitan).rotate().color(),
        "smokeSquare", new EffectEntry(Fx.shootSmokeSquare).rotate().color(),
        "smokeSquareBig", new EffectEntry(Fx.shootSmokeSquareBig).rotate().color(),
        "spark", new EffectEntry(Fx.hitLaserBlast).color(),
        "sparkBig", new EffectEntry(Fx.circleColorSpark).color(),
        "sparkShoot", new EffectEntry(Fx.colorSpark).rotate().color(),
        "sparkShootBig", new EffectEntry(Fx.randLifeSpark).rotate().color(),
        "drill", new EffectEntry(Fx.mine).color(),
        "drillBig", new EffectEntry(Fx.mineHuge).color(),
        "lightBlock", new EffectEntry(Fx.lightBlock).size().color(),
        "explosion", new EffectEntry(Fx.dynamicExplosion).size(),
        "smokePuff", new EffectEntry(Fx.smokePuff).color(),
        "sparkExplosion", new EffectEntry(Fx.titanExplosion).color(),
        "crossExplosion", new EffectEntry(Fx.dynamicSpikes).size().color(),
        "wave", new EffectEntry(Fx.dynamicWave).size(),
        "bubble", new EffectEntry(Fx.airBubble)
        );

        map.each((n, e) -> e.name = n);
    }

    public static Iterable<EffectEntry> entries(){
        return map.orderedKeys().map(s -> map.get(s));
    }

    public static @Nullable EffectEntry get(String name){
        return map.get(name);
    }

    public static String[] all(){
        return map.orderedKeys().toArray(String.class);
    }

    public static class EffectEntry{
        public String name = "";
        public Effect effect;
        public boolean size, rotate, color;
        public @Nullable Class<?> data;
        /** cached bounds for this effect, negative if unset */
        public float bounds = -1f;

        public EffectEntry(Effect effect){
            this.effect = effect;
        }

        public EffectEntry bounds(float bounds){
            this.bounds = bounds;
            return this;
        }

        public EffectEntry name(String name){
            this.name = name;
            return this;
        }

        public EffectEntry size(){
            size = true;
            return this;
        }

        public EffectEntry rotate(){
            rotate = true;
            return this;
        }

        public EffectEntry color(){
            color = true;
            return this;
        }

        public EffectEntry data(Class<?> data){
            this.data = data;
            return this;
        }
    }
}
