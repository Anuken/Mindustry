package mindustry.world.blocks.production;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class BeamDrill extends Block{
    public @Load("minelaser") TextureRegion laser;
    public @Load("minelaser-end") TextureRegion laserEnd;
    public @Load("@-top") TextureRegion topRegion;

    public float drillTime = 200f;
    public int range = 5;
    public int tier = 1;
    public float laserWidth = 0.7f;
    /** Effect randomly played while drilling. */
    public Effect updateEffect = Fx.mineSmall;

    public BeamDrill(String name){
        super(name);

        hasItems = true;
        rotate = true;
        update = true;
        solid = true;
        drawArrow = false;
    }

    @Override
    public void init(){
        clipSize = Math.max(clipSize, size * tilesize + (range + 1) * tilesize);
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

            warmup = Mathf.lerpDelta(warmup, Mathf.num(consValid()), 0.1f);
            lastItem = null;
            boolean multiple = false;

            //update facing tiles
            for(int p = 0; p < size; p++){
                Point2 l = lasers[p];
                Tile dest = null;
                for(int i = 0; i < range; i++){
                    int rx = l.x + Geometry.d4x(rotation)*i, ry = l.y + Geometry.d4y(rotation)*i;
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
                if(cons && dest != null && Mathf.chanceDelta(0.05 * warmup)){
                    updateEffect.at(dest.worldx() + Mathf.range(4f), dest.worldy() + Mathf.range(4f), dest.wallDrop().color);
                }
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

            Draw.z(Layer.power - 1);
            var dir = Geometry.d4(rotation);

            for(int i = 0; i < size; i++){
                Tile face = facing[i];
                if(face != null){
                    Point2 p = lasers[i];
                    Drawf.laser(team, laser, laserEnd, (p.x - dir.x/2f) * tilesize, (p.y - dir.y/2f) * tilesize,
                        face.worldx() - (dir.x/2f)*(tilesize), face.worldy() - (dir.y/2f)*(tilesize),
                    (laserWidth + Mathf.absin(Time.time + i*4 + (id%20)*6, 3f, 0.07f)) * warmup);
                }
            }
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
    }
}
