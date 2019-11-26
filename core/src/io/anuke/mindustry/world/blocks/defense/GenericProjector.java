package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.*;

public class GenericProjector extends Block{
    protected Color color = Color.valueOf("ffffff");
    protected Color simpleOutline = Color.valueOf("000000");
    protected Color phase = Color.valueOf("ffd59e");
    protected IntSet acted = new IntSet();

    protected int timerUse = timers++;

    protected String projectorSet;
    protected float range = 80f;
    protected float reload = 60f;
    protected float effectivity = 3f;
    protected float useTime = 400f;
    // phase speed boost
    // phase range boost
    protected TextureRegion topRegion;
    /** Either: "mend" or "overdrive" currently, more if/as needed */
    protected String behaviour;
    /** 0 for circles; otherwise, its the number of sides of this projector's shape. */
    protected int sides = 0;
    protected Shape2D shape;

    // Want enums, need Strings for modding support
    String projectCondition = always; // one of always, never, withMender, withOverdrive
    static final String always = "always", never = "never", withMender = "withMender", withOverdrive = "withOverdrive";

    boolean showProjection(){
        switch(projectCondition){ // Give me switch expressions or give me death
            case always:
                return true;
            case never:
                return false;
            case withMender:
                return Core.settings.getBool("mendprojection");
            case withOverdrive:
                return Core.settings.getBool("overdriveprojection");
            default:
                throw new IllegalStateException("GenericProjector.projectCondition must be one of \"always\", \"never\", \"withMender\", \"withOverdrive\".");
        }
    }

    public GenericProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
        canOverdrive = false;
    }

    @Override
    public TileEntity newEntity(){
        return new ProjectorEntity();
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();

        // effectivity
        stats.add(BlockStat.range, range / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range, Pal.accent);
    }

    @Override
    public void drawLight(Tile tile){
        renderer.lights.add(tile.drawx(), tile.drawy(), 50f * tile.entity.efficiency(), color, 0.7f * tile.entity.efficiency());
    }

    protected boolean inShape(float pointX, float pointY, float projX, float projY){
        if(shape == null){
            if(sides == 0) shape = new Circle(0, 0, range);
            else shape = new Polygon(pointsOfPolygon(sides, range));
        }
        return shape.contains(projX - pointX, projY - pointY);
    }

    protected float[] pointsOfPolygon(int sides, float radius){
        float[] points = new float[sides * 2];
        // theta = 2π / n
        // x = a + r * cos(theta)
        // y = b + r * sin(theta)
        // where n is the number of sides
        // a and b are always 0, since we imagine this polygon to be at (0,0)
        float theta = Mathf.PI2 / sides;
        for(int i = 0; i < sides * 2; i += 2){
            points[i] = radius * Mathf.cos(theta * i / 2);
            points[i + 1] = radius * Mathf.sin(theta * i / 2);
        }
        return points;
    }

    @Override
    public void update(Tile tile){
        ProjectorEntity entity = tile.entity();

        entity.heat = Mathf.lerpDelta(entity.heat, entity.cons.valid() ? 1f : 0f, 0.08f);
        entity.charge += entity.heat * Time.delta();
        entity.rangeProg = Mathf.lerpDelta(entity.rangeProg, entity.power.status > 0 ? 1f : 0f, 0.05f);
        // phaseheat

        if(entity.timer.get(timerUse, useTime) && entity.efficiency() > 0){
            entity.cons.trigger();
        }

        if(entity.charge >= reload){
            entity.charge = 0f;
            acted.clear();
            float realRange = range; // consider phase
            int tileRange = (int)(realRange / tilesize + 1);

            //replace with inShape
            for(int x = -tileRange + tile.x; x <= tileRange + tile.x; x++){
                for(int y = -tileRange + tile.y; y <= tileRange + tile.y; y++){
                    if(!inShape(x * tilesize, y * tilesize, tile.drawx(), tile.drawy())) continue;

                    Tile other = world.ltile(x, y);

                    if(other == null) continue;

                    if(other.getTeamID() == tile.getTeamID() && !acted.contains(other.pos()) && other.entity != null){
                        if(behaviour.equals("mend")){
                            if(other.entity.health < other.entity.maxHealth()){
                                other.entity.healBy(other.entity.maxHealth() * (/*healPercent + entity.phaseHeat * phaseBoost*/effectivity) / 100f * entity.efficiency());
                                Effects.effect(Fx.healBlockFull, Tmp.c1.set(color).lerp(phase, entity.phaseHeat), other.drawx(), other.drawy(), other.block().size);
                                acted.add(other.pos());
                            }
                        }else if(behaviour.equals("overdrive"))
                            if(other.entity.timeScale <= entity.effectivity()){
                                other.entity.timeScaleDuration = Math.max(other.entity.timeScaleDuration, reload + 1f);
                                other.entity.timeScale = Math.max(other.entity.timeScale, effectivity);
                            }

                        // other behaviours
                    }
                }
            }
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        ProjectorEntity entity = tile.entity();
        float f = 1f - (Time.time() / 100f) % 1f;

        Draw.color(color, phase, entity.phaseHeat);
        Draw.alpha(entity.heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.alpha(1f);
        Lines.stroke((2f * f + 0.2f) * entity.heat);
        Lines.square(tile.drawx(), tile.drawy(), (1f - f) * 8f);

        Draw.reset();
    }

    class ProjectorEntity extends TileEntity implements ProjectorTrait{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;
        float rangeProg;

        float effectivity(){
            return effectivity; // consider phase
        }

        protected float realRadius(){
            return range * rangeProg;
        }

        @Override
        public void drawOver(){
            if(showProjection()){
                Draw.color(Color.white);
                Draw.alpha(1f - power.status);
                Fill.poly(x, y, sides, realRadius());
                Draw.color();
            }
        }

        @Override
        public void drawSimple(){
            if(showProjection()){
                float rad = realRadius();

                Draw.color(color);
                Lines.stroke(1.5f);
                Draw.alpha(1f - power.status);
                Fill.poly(x, y, sides, rad);
                Draw.alpha(1f);
                Draw.color(simpleOutline);
                Lines.poly(x, y, sides, rad);
                Draw.reset();
            }
        }

        @Override
        public Color accent(){
            return color;
        }

        @Override
        public String projectorSet(){
            return projectorSet;
        }

        @Override
        public void draw(){
            if(showProjection()){
                Draw.color(color);
                Fill.poly(x, y, sides, realRadius());
                Draw.color();
            }
        }

        @Override
        public EntityGroup targetGroup(){
            return projectorGroup;
        }
    }
}
