package mindustry.world.blocks.sandbox;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.type.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class ItemTeleport extends Block{

    public ItemTeleport(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        posConfig = true;
        itemCapacity = 1;
        configurable = true;
        instantTransfer = true;
        group = BlockGroup.transportation;

        entityType = ItemTeleportEntity::new;
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<ItemTeleportEntity>ent().link = value;
    }

    @Override
    public void drawConfigure(Tile tile){
        float sin = Mathf.absin(Time.time(), 6f, 1f);

        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Drawf.circles(tile.drawx(), tile.drawy(), (tile.block().size / 2f + 1) * tilesize + sin - 2f, Pal.accent);

        if(linkValid(tile)){
            Tile target = world.tile(tile.<ItemTeleportEntity>ent().link);
            Drawf.circles(target.drawx(), target.drawy(), (target.block().size / 2f + 1) * tilesize + sin - 2f, Pal.place);
            Drawf.arrow(tile.drawx(), tile.drawy(), target.drawx(), target.drawy(), size * tilesize + sin, 4f + sin);
        }
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        if(tile == other) return false;

        ItemTeleportEntity entity = tile.ent();

        if(entity.link == other.pos()){
            tile.configure(-1);
            return false;
        }else if(other.block() instanceof ItemTeleport && other.getTeam() == tile.getTeam()){
            tile.configure(other.pos());
            return false;
        }

        return true;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){

        if(!control.input.frag.config.isShown()) return;
        Tile selected = control.input.frag.config.getSelectedTile();
        if(selected == null || !(selected.block() instanceof ItemTeleport)) return;

        float sin = Mathf.absin(Time.time(), 6f, 1f);
        Tmp.v1.set(x * tilesize + offset(), y * tilesize + offset()).sub(selected.drawx(), selected.drawy()).limit((size / 2f + 1) * tilesize + sin + 0.5f);
        float x2 = x * tilesize - Tmp.v1.x, y2 = y * tilesize - Tmp.v1.y,
        x1 = selected.drawx() + Tmp.v1.x, y1 = selected.drawy() + Tmp.v1.y;
        int segs = (int)(selected.dst(x * tilesize, y * tilesize) / tilesize);

        Lines.stroke(4f, Pal.gray);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Lines.stroke(2f, Pal.placing);
        Lines.dashLine(x1, y1, x2, y2, segs);
        Draw.reset();
    }

    protected boolean linkValid(Tile tile){
        if(tile == null) return false;
        ItemTeleportEntity entity = tile.ent();
        if(entity == null || entity.link == -1) return false;
        Tile link = world.tile(entity.link);

        return link != null && link.block() instanceof ItemTeleport && link.getTeam() == tile.getTeam();
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.cons.valid() && tile.entity.efficiency() == 1f && linkValid(tile) && world.tile(tile.<ItemTeleportEntity>ent().link).entity.items.total() < itemCapacity;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        world.tile(tile.<ItemTeleportEntity>ent().link).entity.items.add(item, 1);
    }

    @Override
    public void update(Tile tile){
        tryDump(tile);
    }

    class ItemTeleportEntity extends TileEntity{
        int link = -1;

        @Override
        public int config(){
            return link;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(link);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            link = stream.readInt();
        }
    }
}
