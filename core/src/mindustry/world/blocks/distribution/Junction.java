package mindustry.world.blocks.distribution;

import arc.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class Junction extends Block{
    public float speed = 26; //frames taken to go through this junction
    public int capacity = 6;

    public final int timerObsolete = timers++;

    public Junction(String name){
        super(name);
        update = true;
        solid = true;
        group = BlockGroup.transportation;
        unloadable = false;
        entityType = JunctionEntity::new;
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        return 0;
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Override
    public void update(Tile tile){
        JunctionEntity entity = tile.ent();
        DirectionalItemBuffer buffer = entity.buffer;

        for(int i = 0; i < 4; i++){
            if(buffer.indexes[i] > 0){
                if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                long l = buffer.buffers[i][0];
                float time = BufferItem.time(l);

                if(Time.time() >= time + speed || Time.time() < time){

                    Item item = content.item(BufferItem.item(l));
                    Tile dest = tile.getNearby(i);
                    if(dest != null) dest = dest.link();

                    //skip blocks that don't want the item, keep waiting until they do
                    if(dest == null || !dest.block().acceptItem(item, dest, tile) || dest.getTeam() != tile.getTeam()){
                        continue;
                    }

                    dest.block().handleItem(item, dest, tile);
                    System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                    buffer.indexes[i] --;
                }
            }
        }
    }

    @Override
    public void onProximityUpdate(Tile tile){
        super.onProximityUpdate(tile);

        if(tile.entity.proximity().select(t -> t.block instanceof Conveyor).size != 4) return;
        final int[] tripod = new int[1];

        for(Point2 p : Geometry.d4){
            tripod[0] = 0;
            Tile neighbor = world.ltile(tile.x + p.x, tile.y + p.y);
            if(neighbor != null){
                tile.entity.proximity().each(t -> tripod[0] += t.rotation == neighbor.rotation ? 1 : 0);
                if(tripod[0] == 3){
                    Tile input = tile.entity.proximity().find(t -> t.rotation != neighbor.rotation);
                    if(input.front() == tile){
                        Tile air = input.getNearby(input.relativeTo(tile)).getNearby(input.relativeTo(tile)).getNearby((neighbor.rotation + 2) % 4);
                        if(air.block == Blocks.air){
                            Core.app.post(() -> {
                                Call.onEffect(Fx.healBlockFull, air.drawx(), air.drawy(), tile.block.size, Pal.bar);
                                air.setNet(neighbor.block, neighbor.getTeam(), neighbor.rotation);

                                Call.onEffect(Fx.healBlockFull, tile.drawx(), tile.drawy(), tile.block.size, Pal.bar);
                                tile.setNet(neighbor.block, neighbor.getTeam(), neighbor.rotation);

                                Tile above = tile.getNearby((neighbor.rotation + 2) % 4);
                                Call.onEffect(Fx.healBlockFull, above.drawx(), above.drawy(), tile.block.size, Pal.bar);
                                above.setNet(neighbor.block, neighbor.getTeam(), above.relativeTo(air));
                            });
                            return;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        JunctionEntity entity = tile.ent();
        int relative = source.relativeTo(tile.x, tile.y);
        entity.buffer.accept(relative, item);

        if(entity.timer.get(timerObsolete, 60 * 3)){

            if(tile.getNearbyLink(Compass.up) == null
            || tile.getNearbyLink(Compass.down) == null
            || tile.getNearbyLink(Compass.left) == null
            || tile.getNearbyLink(Compass.right) == null
            ) return;

            boolean y = tile.getNearbyLink(Compass.up).block == Blocks.air && tile.getNearbyLink(Compass.down).block == Blocks.air;
            boolean x = tile.getNearbyLink(Compass.left).block == Blocks.air && tile.getNearbyLink(Compass.right).block == Blocks.air;

            if(x && y) return;

            if(x || y){
                if(source.block instanceof Conveyor){
                    Call.onEffect(Fx.healBlockFull, tile.drawx(), tile.drawy(), source.block.size, Pal.bar);
                    Core.app.post(() -> tile.setNet(source.block, tile.getTeam(), source.rotation));
                }
            }
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        JunctionEntity entity = tile.ent();
        int relative = source.relativeTo(tile.x, tile.y);

        if(entity == null || relative == -1 || !entity.buffer.accepts(relative)) return false;
        Tile to = tile.getNearby(relative);
        return to != null && to.link().entity != null && to.getTeam() == tile.getTeam();
    }

    class JunctionEntity extends TileEntity{
        DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity, speed);

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            buffer.write(stream);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            buffer.read(stream);
        }
    }
}
