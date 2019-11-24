package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.arc.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import java.io.*;

import static io.anuke.mindustry.Vars.content;

public class LiquidSource extends Block{
    private static Liquid lastLiquid;

    public LiquidSource(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        liquidCapacity = 100f;
        configurable = true;
        outputsLiquid = true;
        entityType = LiquidSourceEntity::new;
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastLiquid != null){
            Core.app.post(() -> tile.configure(lastLiquid.id));
        }
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.remove("liquid");
    }

    @Override
    public void update(Tile tile){
        LiquidSourceEntity entity = tile.entity();

        if(entity.source == null){
            tile.entity.liquids.clear();
        }else{
            tile.entity.liquids.add(entity.source, liquidCapacity);
            tryDumpLiquid(tile, entity.source);
        }
    }

    @Override
    public void drawRequestConfig(BuildRequest req, Eachable<BuildRequest> list){
        drawRequestConfigCenter(req, content.liquid(req.config), "center");
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        LiquidSourceEntity entity = tile.entity();

        if(entity.source != null){
            Draw.color(entity.source.color);
            Draw.rect("center", tile.worldx(), tile.worldy());
            Draw.color();
        }
    }

    @Override
    public void buildTable(Tile tile, Table table){
        LiquidSourceEntity entity = tile.entity();
        ItemSelection.buildTable(table, content.liquids(), () -> entity.source, liquid -> {
            lastLiquid = liquid;
            tile.configure(liquid == null ? -1 : liquid.id);
        });
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<LiquidSourceEntity>entity().source = value == -1 ? null : content.liquid(value);
    }

    class LiquidSourceEntity extends TileEntity{
        public @Nullable Liquid source = null;

        @Override
        public int config(){
            return source == null ? -1 : source.id;
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(source == null ? -1 : source.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            byte id = stream.readByte();
            source = id == -1 ? null : content.liquid(id);
        }
    }
}
