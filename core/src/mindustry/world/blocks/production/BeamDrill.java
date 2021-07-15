package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BeamDrill extends Block{
    protected Rand rand = new Rand();

    public @Load("drill-laser") TextureRegion laser;
    public @Load("drill-laser-end") TextureRegion laserEnd;
    public @Load("@-top") TextureRegion topRegion;

    public float drillTime = 200f;
    public int range = 5;
    public int tier = 1;
    public float laserWidth = 0.65f;

    public Color sparkColor = Color.valueOf("fd9e81"), glowColor = Color.white;
    public float glowIntensity = 0.2f, pulseIntensity = 0.07f;
    public float glowScl = 3f;
    public int sparks = 11;
    public float sparkRange = 10f, sparkLife = 27f, sparkRecurrence = 4f, sparkSpread = 45f, sparkSize = 3.5f;

    public BeamDrill(String name){
        super(name);

        hasItems = true;
        rotate = true;
        update = true;
        solid = true;
        drawArrow = false;

        envEnabled |= Env.space;
    }

    @Override
    public void init(){
        clipSize = Math.max(clipSize, size * tilesize + (range + 2) * tilesize);
        super.init();
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion};
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(topRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Item item = null;
        boolean multiple = false;
        int count = 0;

        for(int i = 0; i < size; i++){
            getLaserPos(x, y, rotation, i, Tmp.p1);

            int j = 0;
            Item found = null;
            for(; j < range; j++){
                int rx = Tmp.p1.x + Geometry.d4x(rotation)*j, ry = Tmp.p1.y + Geometry.d4y(rotation)*j;
                Tile other = world.tile(rx, ry);
                if(other != null){
                    if(other.solid()){
                        Item drop = other.wallDrop();
                        if(drop != null && drop.hardness <= tier){
                            found = drop;
                            count ++;
                        }
                        break;
                    }
                }
            }

            if(found != null){
                //check if multiple items will be drilled
                if(item != found && item != null){
                    multiple = true;
                }
                item = found;
            }

            int len = Math.min(j, range - 1);
            Drawf.dashLine(found == null ? Pal.remove : Pal.placing,
            Tmp.p1.x * tilesize,
            Tmp.p1.y *tilesize,
            (Tmp.p1.x + Geometry.d4x(rotation)*len) * tilesize,
            (Tmp.p1.y + Geometry.d4y(rotation)*len) * tilesize
            );
        }

        if(item != null){
            float width = drawPlaceText(Core.bundle.formatFloat("bar.drillspeed", 60f / drillTime * count, 2), x, y, valid);
            if(!multiple){
                float dx = x * tilesize + offset - width/2f - 4f, dy = y * tilesize + offset + size * tilesize / 2f + 5, s = iconSmall / 4f;
                Draw.mixcol(Color.darkGray, 1f);
                Draw.rect(item.fullIcon, dx, dy - 1, s, s);
                Draw.reset();
                Draw.rect(item.fullIcon, dx, dy, s, s);
            }
        }

    }

    void getLaserPos(int tx, int ty, int rotation, int i, Point2 out){
        int cornerX = tx - (size-1)/2, cornerY = ty - (size-1)/2, s = size;
        switch(rotation){
            case 0 -> out.set(cornerX + s, cornerY + i);
            case 1 -> out.set(cornerX + i, cornerY + s);
            case 2 -> out.set(cornerX - 1, cornerY + i);
            case 3 -> out.set(cornerX + i, cornerY - 1);
        }
    }

    public class BeamDrillBuild extends Building{
        public Tile[] facing = new Tile[size];
        public Point2[] lasers = new Point2[size];
        public @Nullable Item lastItem;

        public float time;
        public float warmup;

        @Override
        public void drawSelect(){

            if(lastItem != null){
                float dx = x - size * tilesize/2f, dy = y + size * tilesize/2f, s = iconSmall / 4f;
                Draw.mixcol(Color.darkGray, 1f);
                Draw.rect(lastItem.fullIcon, dx, dy - 1, s, s);
                Draw.reset();
                Draw.rect(lastItem.fullIcon, dx, dy, s, s);
            }
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(lasers[0] == null) updateLasers();
            boolean cons = shouldConsume();

            warmup = Mathf.approachDelta(warmup, Mathf.num(consValid()), 1f / 60f);
            lastItem = null;
            boolean multiple = false;
            int dx = Geometry.d4x(rotation), dy = Geometry.d4y(rotation), ddx = Geometry.d4x(rotation + 1), ddy = Geometry.d4y(rotation + 1);

            //update facing tiles
            for(int p = 0; p < size; p++){
                Point2 l = lasers[p];
                Tile dest = null;
                for(int i = 0; i < range; i++){
                    int rx = l.x + dx*i, ry = l.y + dy*i;
                    Tile other = world.tile(rx, ry);
                    if(other != null){
                        if(other.solid()){
                            Item drop = other.wallDrop();
                            if(drop != null && drop.hardness <= tier){
                                if(lastItem != drop && lastItem != null){
                                    multiple = true;
                                }
                                lastItem = drop;
                                dest = other;
                            }
                            break;
                        }
                    }
                }

                facing[p] = dest;
            }

            //when multiple items are present, count that as no item
            if(multiple){
                lastItem = null;
            }

            time += edelta();

            if(time >= drillTime){
                for(Tile tile : facing){
                    Item drop = tile == null ? null : tile.wallDrop();
                    if(items.total() < itemCapacity && drop != null){
                        items.add(drop, 1);
                    }
                }
                time %= drillTime;
            }

            if(timer(timerDump, dumpTime)){
                dump();
            }
        }

        @Override
        public boolean shouldConsume(){
            return items.total() < itemCapacity;
        }

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            Draw.rect(topRegion, x, y, rotdeg());

            var dir = Geometry.d4(rotation);
            int ddx = Geometry.d4x(rotation + 1), ddy = Geometry.d4y(rotation + 1);

            for(int i = 0; i < size; i++){
                Tile face = facing[i];
                if(face != null){
                    Point2 p = lasers[i];
                    float lx = face.worldx() - (dir.x/2f)*tilesize, ly = face.worldy() - (dir.y/2f)*tilesize;

                    Draw.z(Layer.power - 1);
                    Draw.mixcol(glowColor, Mathf.absin(Time.time + i*5 + id*9, glowScl, glowIntensity));
                    Drawf.laser(team, laser, laserEnd,
                        (p.x - dir.x/2f) * tilesize,
                        (p.y - dir.y/2f) * tilesize,
                        lx, ly,
                        (laserWidth + Mathf.absin(Time.time + i*5 + id*9, glowScl, pulseIntensity)) * warmup);
                    Draw.mixcol();

                    Draw.z(Layer.effect);
                    Lines.stroke(warmup);
                    rand.setState(i, id);
                    Color col = face.wallDrop().color;
                    for(int j = 0; j < sparks; j++){
                        float fin = (Time.time / sparkLife + rand.random(sparkRecurrence + 1f)) % sparkRecurrence;
                        float or = rand.range(2f);
                        Tmp.v1.set(sparkRange * fin, 0).rotate(rotdeg() + rand.range(sparkSpread));

                        Draw.color(sparkColor, col, fin);
                        float px = Tmp.v1.x, py = Tmp.v1.y;
                        if(fin <= 1f) Lines.lineAngle(lx + px + or * ddx, ly + py + or * ddy, Angles.angle(px, py), Mathf.slope(fin) * sparkSize);
                    }
                    Draw.reset();
                }
            }
            Draw.blend();
            Draw.reset();
        }

        @Override
        public void onProximityUpdate(){
            //when rotated.
            updateLasers();
        }

        void updateLasers(){
            for(int i = 0; i < size; i++){
                if(lasers[i] == null) lasers[i] = new Point2();
                getLaserPos(tileX(), tileY(), rotation, i, lasers[i]);
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(time);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                time = read.f();
                warmup = read.f();
            }
        }
    }
}
