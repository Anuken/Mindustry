package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class RegenProjector extends Block{
    private static final IntSet taken = new IntSet();
    //map ID to mend amount
    private static final IntFloatMap mendMap = new IntFloatMap();
    private static long lastUpdateFrame = -1;

    //cached points per-block for drawing convenience
    protected @Nullable Vec2[] drawPoints;

    public int rangeLength = 14, rangeWidth = 5;
    //per frame
    public float healPercent = 12f / 60f;

    public DrawBlock drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawSideRegion(true));

    public float effectChance = 0.011f;
    public Effect effect = Fx.regenParticle;

    public float beamSpacing = 60f * 7f, beamWidening = 5f, beamLenScl = 1.2f, beamStroke = 2f;
    public Interp beamInterp = Interp.pow2Out;
    public int beams = 6;

    public RegenProjector(String name){
        super(name);
        solid = true;
        update = true;
        rotate = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
        emitLight = true;
        envEnabled |= Env.space;
        rotateDraw = false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        drawBounds(x * tilesize + offset, y * tilesize + offset, rotation);
    }

    public void drawBounds(float x, float y, int rotation){
        if(drawPoints == null){
            drawPoints = new Vec2[]{
            new Vec2(1f, -rangeWidth),
            new Vec2(1f + rangeLength, -rangeWidth),
            new Vec2(1f + rangeLength, rangeWidth),
            new Vec2(1f + 0f, rangeWidth),
            };
            for(var v : drawPoints){
                v.scl(tilesize);
            }
        }

        for(int i = 0; i < 4; i++){
            Tmp.v1.set(drawPoints[i]).rotate(rotation * 90).add(x, y);
            Tmp.v2.set(drawPoints[(i + 1) % 4]).rotate(rotation * 90).add(x, y);

            Drawf.dashLine(Pal.accent, Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y);
        }
    }

    @Override
    public void drawRequestRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void load(){
        super.load();
        drawer.load(this);
    }

    public class RegenProjectorBuild extends Building{
        public Seq<Building> targets = new Seq<>();
        public int lastChange = -2;
        public float warmup, totalTime;

        public void updateTargets(){
            targets.clear();
            taken.clear();
            float rot = rotation * 90;

            for(int cy = -rangeWidth; cy <= rangeWidth; cy++){
                for(int cx = 1; cx <= rangeLength + 1; cx++){

                    //TODO handle offset
                    float wx = x/tilesize + Angles.trnsx(rot, cx, cy), wy = y/tilesize + Angles.trnsy(rot, cx, cy);

                    Building build = world.build((int)wx, (int)wy);

                    if(build != null && build.team == team && taken.add(build.id)){
                        targets.add(build);
                    }
                }
            }
        }

        @Override
        public void updateTile(){
            if(lastChange != world.tileChanges){
                lastChange = world.tileChanges;
                updateTargets();
            }

            warmup = Mathf.approachDelta(warmup, consValid() ? 1f : 0f, 1f / 70f);
            totalTime += warmup * Time.delta;

            if(consValid()){
                //use Math.max to prevent stacking
                for(Building build : targets){
                    if(!build.damaged()) continue;

                    int pos = build.pos();
                    //TODO periodic effect
                    float value = mendMap.get(pos);
                    mendMap.put(pos, Math.min(Math.max(value, healPercent * edelta() * build.block.health / 100f), build.block.health - build.health));

                    if(Mathf.chanceDelta(effectChance)){
                        effect.at(build.x + Mathf.range(build.block.size * tilesize/2f - 1f), build.y + Mathf.range(build.block.size * tilesize/2f - 1f));
                    }
                }
            }

            if(lastUpdateFrame != Core.graphics.getFrameId()){
                lastUpdateFrame = Core.graphics.getFrameId();

                for(var entry : mendMap.entries()){
                    var build = world.build(entry.key);
                    if(build != null){
                        build.heal(entry.value);
                    }
                }
                mendMap.clear();
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            drawBounds(x, y, rotation);
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalTime;
        }

        @Override
        public void draw(){
            drawer.drawBase(this);

            for(int i = 0; i < beams; i++){
                float life = beamInterp.apply((totalTime / beamSpacing + i / (float)beams) % 1f);
                float len = life * rangeLength*beamLenScl * tilesize + size * tilesize/2f;
                float width = Math.min(life * rangeWidth * 2f * tilesize * beamWidening, rangeWidth * 2f * tilesize);
                float stroke = (0.5f + beamStroke * life) * warmup;
                Draw.z(Layer.effect);

                Lines.stroke(stroke, Pal.accent);
                Draw.alpha(1f - Mathf.curve(life, 0.5f));
                Lines.lineAngleCenter(
                x + Angles.trnsx(rotdeg(), len),
                y + Angles.trnsy(rotdeg(), len),
                rotdeg() + 90f,
                width
                );

                Draw.reset();
            }

        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLights(this);
        }
    }
}
