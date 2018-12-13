package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongArray;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Conveyor extends Block{
    private static final float itemSpace = 0.135f * 2.2f;
    private static final float offsetScl = 128f * 3f;
    private static final float minmove = 1f / (Short.MAX_VALUE - 2);
    private static ItemPos drawpos = new ItemPos();
    private static ItemPos pos1 = new ItemPos();
    private static ItemPos pos2 = new ItemPos();
    private final Translator tr1 = new Translator();
    private final Translator tr2 = new Translator();

    private TextureRegion[][] regions = new TextureRegion[7][4];

    protected float speed = 0f;
    protected float carryCapacity = 8f;

    protected Conveyor(String name){
        super(name);
        rotate = true;
        update = true;
        layer = Layer.overlay;
        group = BlockGroup.transportation;
        hasItems = true;
        autoSleep = true;
        itemCapacity = 4;
    }

    private static int compareItems(long a, long b){
        pos1.set(a, ItemPos.packShorts);
        pos2.set(b, ItemPos.packShorts);
        return Float.compare(pos1.y, pos2.y);
    }

    @Override
    public void setBars(){}

    @Override
    public void setStats(){
        super.setStats();
        stats.add(BlockStat.itemSpeed, speed * 60, StatUnit.pixelsSecond);
    }

    @Override
    public void load(){
        super.load();

        for(int i = 0; i < regions.length; i++){
            for(int j = 0; j < 4; j++){
                regions[i][j] = Draw.region(name + "-" + i + "-" + j);
            }
        }
    }

    @Override
    public void drawShadow(Tile tile){
        //fixes build block crash
        if(!(tile.entity instanceof ConveyorEntity)){
            super.drawShadow(tile);
            return;
        }

        ConveyorEntity entity = tile.entity();

        if(entity.blendshadowrot == -1){
            super.drawShadow(tile);
        }else{
            Draw.rect("shadow-corner", tile.drawx(), tile.drawy(), (tile.getRotation() + 3 + entity.blendshadowrot) * 90);
        }
    }

    @Override
    public void draw(Tile tile){
        ConveyorEntity entity = tile.entity();
        byte rotation = tile.getRotation();

        int frame = entity.clogHeat <= 0.5f ? (int) (((Timers.time() * speed * 8f * entity.timeScale)) % 4) : 0;
        Draw.rect(regions[Mathf.clamp(entity.blendbits, 0, regions.length - 1)][Mathf.clamp(frame, 0, regions[0].length - 1)], tile.drawx(), tile.drawy(),
            tilesize * entity.blendsclx, tilesize * entity.blendscly, rotation*90);
    }

    @Override
    public void onProximityUpdate(Tile tile){
        super.onProximityUpdate(tile);

        ConveyorEntity entity = tile.entity();
        entity.blendbits = 0;
        entity.blendsclx = entity.blendscly = 1;
        entity.blendshadowrot = -1;

        if(blends(tile, 2) && blends(tile, 1) && blends(tile, 3)){
            entity.blendbits = 3;
        }else if(blends(tile, 1) && blends(tile, 3)){
            entity.blendbits = 4;
        }else if(blends(tile, 1) && blends(tile, 2)){
            entity.blendbits = 2;
        }else if(blends(tile, 3) && blends(tile, 2)){
            entity.blendbits = 2;
            entity.blendscly = -1;
        }else if(blends(tile, 1)){
            entity.blendbits = 1;
            entity.blendscly = -1;
            entity.blendshadowrot = 0;
        }else if(blends(tile, 3)){
            entity.blendbits = 1;
            entity.blendshadowrot = 1;
        }
    }

    private boolean blends(Tile tile, int direction){
        Tile other = tile.getNearby(Mathf.mod(tile.getRotation() - direction, 4));
        if(other != null) other = other.target();

        return other != null && other.block().outputsItems()
        && ((tile.getNearby(tile.getRotation()) == other) || (!other.block().rotate || other.getNearby(other.getRotation()) == tile));
    }

    @Override
    public TextureRegion[] getIcon(){
        if(icon == null){
            icon = new TextureRegion[]{Draw.region(name + "-0-0")};
        }
        return super.getIcon();
    }

    @Override
    public void drawLayer(Tile tile){
        ConveyorEntity entity = tile.entity();

        byte rotation = tile.getRotation();

        try{

            for(int i = 0; i < entity.convey.size; i++){
                ItemPos pos = drawpos.set(entity.convey.get(i), ItemPos.drawShorts);

                if(pos.item == null) continue;

                tr1.trns(rotation * 90, tilesize, 0);
                tr2.trns(rotation * 90, -tilesize / 2f, pos.x * tilesize / 2f);

                Draw.rect(pos.item.region,
                        (int) (tile.x * tilesize + tr1.x * pos.y + tr2.x),
                        (int) (tile.y * tilesize + tr1.y * pos.y + tr2.y), itemSize, itemSize);
            }

        }catch(IndexOutOfBoundsException e){
            Log.err(e);
        }
    }

    @Override
    public void unitOn(Tile tile, Unit unit){
        ConveyorEntity entity = tile.entity();

        entity.noSleep();

        float speed = this.speed * tilesize / 2.3f;
        float centerSpeed = 0.1f;
        float centerDstScl = 3f;
        float tx = Geometry.d4[tile.getRotation()].x, ty = Geometry.d4[tile.getRotation()].y;

        float centerx = 0f, centery = 0f;

        if(Math.abs(tx) > Math.abs(ty)){
            centery = Mathf.clamp((tile.worldy() - unit.y) / centerDstScl, -centerSpeed, centerSpeed);
            if(Math.abs(tile.worldy() - unit.y) < 1f) centery = 0f;
        }else{
            centerx = Mathf.clamp((tile.worldx() - unit.x) / centerDstScl, -centerSpeed, centerSpeed);
            if(Math.abs(tile.worldx() - unit.x) < 1f) centerx = 0f;
        }

        if(entity.convey.size * itemSpace < 0.9f){
            unit.getVelocity().add((tx * speed + centerx) * entity.delta(), (ty * speed + centery) * entity.delta());
        }
    }

    @Override
    public void update(Tile tile){

        ConveyorEntity entity = tile.entity();
        entity.minitem = 1f;

        int minremove = Integer.MAX_VALUE;

        for(int i = entity.convey.size - 1; i >= 0; i--){
            long value = entity.convey.get(i);
            ItemPos pos = pos1.set(value, ItemPos.updateShorts);

            //..this should never happen, but in case it does, remove it and stop here
            if(pos.item == null){
                entity.convey.removeValue(value);
                break;
            }

            float nextpos = (i == entity.convey.size - 1 ? 100f : pos2.set(entity.convey.get(i + 1), ItemPos.updateShorts).y) - itemSpace;
            float maxmove = Math.min(nextpos - pos.y, speed * entity.delta());

            if(maxmove > minmove){
                pos.y += maxmove;
                if(Mathf.in(pos.x, 0, 0.1f)){
                    pos.x = 0f;
                }
                pos.x = Mathf.lerpDelta(pos.x, 0, 0.1f);
            }else{
                pos.x = Mathf.lerpDelta(pos.x, pos.seed / offsetScl, 0.1f);
            }

            pos.y = Mathf.clamp(pos.y);

            if(pos.y >= 0.9999f && offloadDir(tile, pos.item)){
                Tile next = tile.getNearby(tile.getRotation());
                if(next.block() instanceof Conveyor){
                    ConveyorEntity othere = next.entity();

                    ItemPos ni = pos2.set(othere.convey.get(othere.lastInserted), ItemPos.updateShorts);

                    if(next.getRotation() == tile.getRotation()){
                        ni.x = pos.x;
                    }
                    othere.convey.set(othere.lastInserted, ni.pack());
                }
                minremove = Math.min(i, minremove);
                tile.entity.items.remove(pos.item, 1);
            }else{
                value = pos.pack();

                if(pos.y < entity.minitem)
                    entity.minitem = pos.y;
                entity.convey.set(i, value);
            }
        }

        if(entity.minitem < itemSpace){
            entity.clogHeat = Mathf.lerpDelta(entity.clogHeat, 1f, 0.02f);
        }else{
            entity.clogHeat = Mathf.lerpDelta(entity.clogHeat, 0f, 1f);
        }

        if(entity.items.total() == 0){
            entity.sleep();
        }else{
            entity.noSleep();
        }

        if(minremove != Integer.MAX_VALUE) entity.convey.truncate(minremove);
    }

    @Override
    public boolean isAccessible(){
        return true;
    }

    @Override
    public int removeStack(Tile tile, Item item, int amount){
        ConveyorEntity entity = tile.entity();
        entity.noSleep();
        int removed = 0;

        for(int j = 0; j < amount; j++){
            for(int i = 0; i < entity.convey.size; i++){
                long val = entity.convey.get(i);
                ItemPos pos = pos1.set(val, ItemPos.drawShorts);
                if(pos.item == item){
                    entity.convey.removeValue(val);
                    entity.items.remove(item, 1);
                    removed++;
                    break;
                }
            }
        }
        return removed;
    }

    @Override
    public void getStackOffset(Item item, Tile tile, Translator trns){
        trns.trns(tile.getRotation() * 90 + 180f, tilesize / 2f);
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        ConveyorEntity entity = tile.entity();
        return Math.min((int)(entity.minitem / itemSpace), amount);
    }

    @Override
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        ConveyorEntity entity = tile.entity();

        for(int i = amount - 1; i >= 0; i--){
            long result = ItemPos.packItem(item, 0f, i * itemSpace, (byte) Mathf.random(255));
            entity.convey.insert(0, result);
            entity.items.add(item, 1);
        }

        entity.noSleep();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        int direction = source == null ? 0 : Math.abs(source.relativeTo(tile.x, tile.y) - tile.getRotation());
        float minitem = tile.<ConveyorEntity>entity().minitem;
        return (((direction == 0) && minitem > itemSpace) ||
                ((direction % 2 == 1) && minitem > 0.52f)) && (source == null || !(source.block().rotate && (source.getRotation() + 2) % 4 == tile.getRotation()));
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        byte rotation = tile.getRotation();

        int ch = Math.abs(source.relativeTo(tile.x, tile.y) - rotation);
        int ang = ((source.relativeTo(tile.x, tile.y) - rotation));

        float pos = ch == 0 ? 0 : ch % 2 == 1 ? 0.5f : 1f;
        float y = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;

        ConveyorEntity entity = tile.entity();
        entity.noSleep();
        long result = ItemPos.packItem(item, y * 0.9f, pos, (byte) Mathf.random(255));

        tile.entity.items.add(item, 1);

        for(int i = 0; i < entity.convey.size; i++){
            if(compareItems(result, entity.convey.get(i)) < 0){
                entity.convey.insert(i, result);
                entity.lastInserted = (byte)i;
                return;
            }
        }

        //this item must be greater than anything there...
        entity.convey.add(result);
        entity.lastInserted = (byte)(entity.convey.size-1);
    }

    @Override
    public Array<Object> getDebugInfo(Tile tile){
        ConveyorEntity entity = tile.entity();
        Array<Object> arr = super.getDebugInfo(tile);
        arr.addAll(Array.with(
                "clogHeat", entity.clogHeat,
                "sleeping", entity.isSleeping()
        ));
        return arr;
    }

    @Override
    public TileEntity newEntity(){
        return new ConveyorEntity();
    }

    public static class ConveyorEntity extends TileEntity{

        LongArray convey = new LongArray();
        byte lastInserted;
        float minitem = 1;

        int blendshadowrot = -1;
        int blendbits;
        int blendsclx, blendscly;

        float clogHeat = 0f;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeInt(convey.size);

            for(int i = 0; i < convey.size; i++){
                stream.writeInt(ItemPos.toInt(convey.get(i)));
            }
        }

        @Override
        public void read(DataInput stream) throws IOException{
            convey.clear();
            int amount = stream.readInt();
            convey.ensureCapacity(Math.min(amount, 10));

            for(int i = 0; i < amount; i++){
                convey.add(ItemPos.toLong(stream.readInt()));
            }
        }
    }

    //Container class. Do not instantiate.
    static class ItemPos{
        private static short[] writeShort = new short[4];
        private static byte[] writeByte = new byte[4];

        private static short[] packShorts = new short[4];
        private static short[] drawShorts = new short[4];
        private static short[] updateShorts = new short[4];

        Item item;
        float x, y;
        byte seed;

        private ItemPos(){
        }

        static long packItem(Item item, float x, float y, byte seed){
            short[] shorts = packShorts;
            shorts[0] = (short) item.id;
            shorts[1] = (short) (x * Short.MAX_VALUE);
            shorts[2] = (short) ((y - 1f) * Short.MAX_VALUE);
            shorts[3] = seed;
            return Bits.packLong(shorts);
        }

        static int toInt(long value){
            short[] values = Bits.getShorts(value, writeShort);

            short itemid = values[0];
            float x = values[1] / (float) Short.MAX_VALUE;
            float y = ((float) values[2]) / Short.MAX_VALUE + 1f;
            byte seed = (byte) values[3];

            byte[] bytes = writeByte;
            bytes[0] = (byte) itemid;
            bytes[1] = (byte) (x * 127);
            bytes[2] = (byte) (y * 255 - 128);
            bytes[3] = seed;

            return Bits.packInt(bytes);
        }

        static long toLong(int value){
            byte[] values = Bits.getBytes(value, writeByte);

            byte itemid = values[0];
            float x = values[1] / 127f;
            float y = ((int) values[2] + 128) / 255f;
            byte seed = values[3];

            short[] shorts = writeShort;
            shorts[0] = (short) itemid;
            shorts[1] = (short) (x * Short.MAX_VALUE);
            shorts[2] = (short) ((y - 1f) * Short.MAX_VALUE);
            shorts[3] = seed;
            return Bits.packLong(shorts);
        }

        ItemPos set(long lvalue, short[] values){
            Bits.getShorts(lvalue, values);

            if(values[0] >= content.items().size || values[0] < 0)
                item = null;
            else
                item = content.items().get(values[0]);

            x = values[1] / (float) Short.MAX_VALUE;
            y = ((float) values[2]) / Short.MAX_VALUE + 1f;
            seed = (byte) values[3];
            return this;
        }

        long pack(){
            return packItem(item, x, y, seed);
        }
    }
}