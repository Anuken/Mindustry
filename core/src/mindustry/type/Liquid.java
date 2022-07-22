package mindustry.type;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.entities.Puddles.*;

/** A better name for this class would be "fluid", but it's too late for that. */
public class Liquid extends UnlockableContent implements Senseable{
    //must be static and global so conduits don't conflict - DO NOT INTERACT WITH THESE IN MODS OR I WILL PERSONALLY YELL AT YOU
    public static final int animationFrames = 50;
    public static float animationScaleGas = 190f, animationScaleLiquid = 230f;

    protected static final Rand rand = new Rand();

    /** If true, this fluid is treated as a gas (and does not create puddles) */
    public boolean gas = false;
    /** Color used in pipes and on the ground. */
    public Color color;
    /** Color of this liquid in gas form. */
    public Color gasColor = Color.lightGray.cpy();
    /** Color used in bars. */
    public @Nullable Color barColor;
    /** Color used to draw lights. Note that the alpha channel is used to dictate brightness. */
    public Color lightColor = Color.clear.cpy();
    /** 0-1, 0 is completely not flammable, anything above that may catch fire when exposed to heat, 0.5+ is very flammable. */
    public float flammability;
    /** temperature: 0.5 is 'room' temperature, 0 is very cold, 1 is molten hot */
    public float temperature = 0.5f;
    /** how much heat this liquid can store. 0.4=water (decent), anything lower is probably less dense and bad at cooling. */
    public float heatCapacity = 0.5f;
    /** how thick this liquid is. 0.5=water (relatively viscous), 1 would be something like tar (very slow). */
    public float viscosity = 0.5f;
    /** how prone to exploding this liquid is, when heated. 0 = nothing, 1 = nuke */
    public float explosiveness;
    /** if false, this liquid cannot be a coolant */
    public boolean coolant = true;
    /** The associated status effect. */
    public StatusEffect effect = StatusEffects.none;
    /** Effect shown in puddles. */
    public Effect particleEffect = Fx.none;
    /** Particle effect rate spacing in ticks. */
    public float particleSpacing = 60f;
    /** Temperature at which this liquid vaporizes. This isn't just boiling. */
    public float boilPoint = 2f;
    /** If true, puddle size is capped. */
    public boolean capPuddles = true;
    /** Effect when this liquid vaporizes. */
    public Effect vaporEffect = Fx.vapor;
    /** If true, this liquid is hidden in most UI. */
    public boolean hidden;

    public Liquid(String name, Color color){
        super(name);
        this.color = new Color(color);
    }

    /** For modding only.*/
    public Liquid(String name){
        this(name, new Color(Color.black));
    }

    @Override
    public void init(){
        super.init();

        if(gas){
            //gases can't be coolants
            coolant = false;
            //always "boils", it's a gas
            boilPoint = -1;
            //ensure no accidental global mutation
            color = color.cpy();
            //all gases are transparent
            color.a = 0.6f;
            //for gases, gas color is implicitly their color
            gasColor = color;
            if(barColor == null){
                barColor = color.cpy().a(1f);
            }
        }
    }

    @Override
    public boolean isHidden(){
        return hidden;
    }

    public int getAnimationFrame(){
        return (int)(Time.time / (gas ? animationScaleGas : animationScaleLiquid) * animationFrames + id*5) % animationFrames;
    }

    /** @return true if this liquid will boil in this global environment. */
    public boolean willBoil(){
        return Attribute.heat.env() >= boilPoint;
    }

    public boolean canExtinguish(){
        return flammability < 0.1f && temperature <= 0.5f;
    }

    public Color barColor(){
        return barColor == null ? color : barColor;
    }

    /** Draws a puddle of this liquid on the floor. */
    public void drawPuddle(Puddle puddle){
        float amount = puddle.amount, x = puddle.x, y = puddle.y;
        float f = Mathf.clamp(amount / (maxLiquid / 1.5f));
        float smag = puddle.tile.floor().isLiquid ? 0.8f : 0f, sscl = 25f;

        Draw.color(Tmp.c1.set(color).shiftValue(-0.05f));
        Fill.circle(x + Mathf.sin(Time.time + id * 532, sscl, smag), y + Mathf.sin(Time.time + id * 53, sscl, smag), f * 8f);

        float length = f * 6f;
        rand.setSeed(id);
        for(int i = 0; i < 3; i++){
            Tmp.v1.trns(rand.random(360f), rand.random(length));
            float vx = x + Tmp.v1.x, vy = y + Tmp.v1.y;

            Fill.circle(
            vx + Mathf.sin(Time.time + i * 532, sscl, smag),
            vy + Mathf.sin(Time.time + i * 53, sscl, smag),
            f * 5f);
        }

        Draw.color();

        if(lightColor.a > 0.001f && f > 0){
            Drawf.light(x, y, 30f * f, lightColor, color.a * f * 0.8f);
        }
    }

    /** Runs when puddles update. */
    public void update(Puddle puddle){

    }

    //TODO proper API for this (do not use yet!)
    public float react(Liquid other, float amount, Tile tile, float x, float y){
        return 0f;
    }

    @Override
    public void setStats(){
        stats.addPercent(Stat.explosiveness, explosiveness);
        stats.addPercent(Stat.flammability, flammability);
        stats.addPercent(Stat.temperature, temperature);
        stats.addPercent(Stat.heatCapacity, heatCapacity);
        stats.addPercent(Stat.viscosity, viscosity);
    }

    @Override
    public double sense(LAccess sensor){
        if(sensor == LAccess.color) return color.toFloatBits();
        return 0;
    }

    @Override
    public Object senseObject(LAccess sensor){
        if(sensor == LAccess.name) return name;
        return noSensed;
    }

    @Override
    public String toString(){
        return localizedName;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.liquid;
    }
}
