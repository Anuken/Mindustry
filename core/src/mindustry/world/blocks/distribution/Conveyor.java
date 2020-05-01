package mindustry.world.blocks.distribution;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Conveyor extends Block implements Autotiler{
    private static final float itemSpace = 0.4f;
    private static final int capacity = 4;

    private final Vec2 tr1 = new Vec2();
    private final Vec2 tr2 = new Vec2();
    private TextureRegion[][] regions = new TextureRegion[7][4];

    public float speed = 0f;
    public float displayedSpeed = 0f;

    protected Conveyor(String name){
        super(name);
        rotate = true;
        update = true;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = 4;
        conveyorPlacement = true;

        idleSound = Sounds.conveyor;
        idleSoundVolume = 0.004f;
        unloadable = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        
        //have to add a custom calculated speed, since the actual movement speed is apparently not linear
        stats.add(BlockStat.itemsMoved, displayedSpeed, StatUnit.itemsSecond);
    }

    @Override
    public void load(){
        super.load();

        for(int i = 0; i < regions.length; i++){
            for(int j = 0; j < 4; j++){
                regions[i][j] = Core.atlas.find(name + "-" + i + "-" + j);
            }
        }
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        TextureRegion region = regions[bits[0]][0];
        Draw.rect(region, req.drawx(), req.drawy(), region.getWidth() * bits[1] * Draw.scl * req.animScale, region.getHeight() * bits[2] * Draw.scl * req.animScale, req.rotation * 90);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsItems() && lookingAt(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-0-0")};
    }

    @Override
    public boolean isAccessible(){
        return true;
    }

    @Override
    public Block getReplacement(BuildRequest req, Array<BuildRequest> requests){
        Boolf<Point2> cont = p -> requests.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && o.rotation == req.rotation && (req.block instanceof Conveyor || req.block instanceof Junction));
        return cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof Conveyor &&
            Mathf.mod(req.tile().rotation() - req.rotation, 2) == 1 ? Blocks.junction : this;
    }

    public class ConveyorEntity extends TileEntity{
        //parallel array data
        Item[] ids = new Item[capacity];
        float[] xs = new float[capacity];
        float[] ys = new float[capacity];
        //amount of items, always < capacity
        int len = 0;
        //next entity
        @Nullable Tilec next;
        @Nullable ConveyorEntity nextc;
        //whether the next conveyor's rotation == tile rotation
        boolean aligned;

        int lastInserted, mid;
        float minitem = 1;

        int blendbits;
        int blendsclx, blendscly;

        float clogHeat = 0f;

        @Override
        public void draw(){
            byte rotation = tile.rotation();
            int frame = clogHeat <= 0.5f ? (int)(((Time.time() * speed * 8f * timeScale())) % 4) : 0;
            Draw.rect(regions[Mathf.clamp(blendbits, 0, regions.length - 1)][Mathf.clamp(frame, 0, regions[0].length - 1)], x, y,
            tilesize * blendsclx, tilesize * blendscly, rotation * 90);

            //TODO is clustering necessary? does it create garbage?
            Draw.z(Layer.blockOver);

            for(int i = 0; i < len; i++){
                Item item = ids[i];
                tr1.trns(rotation * 90, tilesize, 0);
                tr2.trns(rotation * 90, -tilesize / 2f, xs[i] * tilesize / 2f);

                Draw.rect(item.icon(Cicon.medium),
                (tile.x * tilesize + tr1.x * ys[i] + tr2.x),
                (tile.y * tilesize + tr1.y * ys[i] + tr2.y), itemSize, itemSize);
            }
        }

        @Override
        public boolean shouldIdleSound(){
            return clogHeat <= 0.5f ;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int[] bits = buildBlending(tile, rotation(), null, true);
            blendbits = bits[0];
            blendsclx = bits[1];
            blendscly = bits[2];

            if(tile.front() != null && tile.front() != null){
                next = tile.front();
                nextc = next instanceof ConveyorEntity && next.team() == team ? (ConveyorEntity)next : null;
                aligned = nextc != null && tile.rotation() == next.tile().rotation();
            }
        }

        @Override
        public void unitOn(Unitc unit){
            if(clogHeat > 0.5f){
                return;
            }

            noSleep();

            float mspeed = speed * tilesize / 2.4f;
            float centerSpeed = 0.1f;
            float centerDstScl = 3f;
            float tx = Geometry.d4[tile.rotation()].x, ty = Geometry.d4[tile.rotation()].y;

            float centerx = 0f, centery = 0f;

            if(Math.abs(tx) > Math.abs(ty)){
                centery = Mathf.clamp((y - unit.y()) / centerDstScl, -centerSpeed, centerSpeed);
                if(Math.abs(y - unit.y()) < 1f) centery = 0f;
            }else{
                centerx = Mathf.clamp((x - unit.x()) / centerDstScl, -centerSpeed, centerSpeed);
                if(Math.abs(x - unit.x()) < 1f) centerx = 0f;
            }

            if(len * itemSpace < 0.9f){
                unit.impulse((tx * mspeed + centerx) * delta(), (ty * mspeed + centery) * delta());
            }
        }

        @Override
        public void updateTile(){
            minitem = 1f;
            mid = 0;

            //skip updates if possible
            if(len == 0){
                clogHeat = 0f;
                sleep();
                return;
            }

            float nextMax = aligned ? 1f - Math.max(itemSpace - nextc.minitem, 0) : 1f;

            for(int i = len - 1; i >= 0; i--){
                float nextpos = (i == len - 1 ? 100f : ys[i + 1]) - itemSpace;
                float maxmove = Mathf.clamp(nextpos - ys[i], 0, speed * delta());

                ys[i] += maxmove;

                if(ys[i] > nextMax) ys[i] = nextMax;
                if(ys[i] > 0.5 && i > 0) mid = i - 1;
                xs[i] = Mathf.approachDelta(xs[i], 0, speed*2);

                if(ys[i] >= 1f && moveForward(ids[i])){
                    //align X position if passing forwards
                    if(aligned){
                        nextc.xs[nextc.lastInserted] = xs[i];
                    }
                    //remove last item
                    items.remove(ids[i], len - i);
                    len = Math.min(i, len);
                }else if(ys[i] < minitem){
                    minitem = ys[i];
                }
            }

            if(minitem < itemSpace + (blendbits == 1 ? 0.3f : 0f)){
                clogHeat = Mathf.lerpDelta(clogHeat, 1f, 0.02f);
            }else{
                clogHeat = 0f;
            }

            noSleep();
        }

        @Override
        public int removeStack(Item item, int amount){
            noSleep();
            int removed = 0;

            for(int j = 0; j < amount; j++){
                for(int i = 0; i < len; i++){
                    if(ids[i] == item){
                        remove(i);
                        removed ++;
                        break;
                    }
                }
            }

            items.remove(item, removed);
            return removed;
        }

        @Override
        public void getStackOffset(Item item, Vec2 trns){
            trns.trns(tile.rotation() * 90 + 180f, tilesize / 2f);
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return Math.min((int)(minitem / itemSpace), amount);
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            amount = Math.min(amount, itemCapacity - len);

            for(int i = amount - 1; i >= 0; i--){
                add(0);
                xs[0] = 0;
                ys[0] = i * itemSpace;
                ids[0] = item;
                items.add(item, 1);
            }

            noSleep();
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            if(len >= capacity) return false;
            Tile facing = Edges.getFacingEdge(source.tile(), tile);
            int direction = Math.abs(facing.relativeTo(tile.x, tile.y) - tile.rotation());
            return (((direction == 0) && minitem >= itemSpace) || ((direction % 2 == 1) && minitem > 0.7f)) && !(source.block().rotate && (source.rotation() + 2) % 4 == tile.rotation());
        }

        @Override
        public void handleItem(Tilec source, Item item){
            if(len >= capacity) return;

            byte r = tile.rotation();
            Tile facing = Edges.getFacingEdge(source.tile(), tile);
            int ang = ((facing.relativeTo(tile.x, tile.y) - r));
            float x = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;

            noSleep();
            items.add(item, 1);

            if(Math.abs(facing.relativeTo(tile.x, tile.y) - r) == 0){ //idx = 0
                add(0);
                xs[0] = x;
                ys[0] = 0;
                ids[0] = item;
            }else{ //idx = mid
                add(mid);
                xs[mid] = x;
                ys[mid] = 0.5f;
                ids[mid] = item;
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(len);

            for(int i = 0; i < len; i++){
                write.i(Pack.intBytes((byte)ids[i].id, (byte)(xs[i] * 127), (byte)(ys[i] * 255 - 128), (byte)0));
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int amount = read.i();
            len = Math.min(amount, capacity);

            for(int i = 0; i < amount; i++){
                int val = read.i();
                byte id = (byte)(val >> 24);
                float x = (float)((byte)(val >> 16)) / 127f;
                float y = ((float)((byte)(val >> 8)) + 128f) / 255f;
                if(i < capacity){
                    ids[i] = content.item(id);
                    xs[i] = x;
                    ys[i] = y;
                }
            }
        }


        final void add(int o){
            for(int i = Math.max(o + 1, len); i > o; i--){
                ids[i] = ids[i - 1];
                xs[i] = xs[i - 1];
                ys[i] = ys[i - 1];
            }

            len++;
        }

        final void remove(int o){
            for(int i = o; i < len - 1; i++){
                ids[i] = ids[i + 1];
                xs[i] = xs[i + 1];
                ys[i] = ys[i + 1];
            }

            len--;
        }
    }
}
