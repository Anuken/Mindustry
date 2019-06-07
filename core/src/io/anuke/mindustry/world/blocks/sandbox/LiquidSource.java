package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.ButtonGroup;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import java.io.*;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.control;

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
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastLiquid != null) Core.app.post(() -> Call.setLiquidSourceLiquid(null, tile, lastLiquid));
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

        Array<Liquid> items = content.liquids();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        Table cont = new Table();

        for(int i = 0; i < items.size; i++){
            final int f = i;
            ImageButton button = cont.addImageButton("clear", "clear-toggle", 24, () -> control.input().frag.config.hideConfig()).size(38).group(group).get();
            button.changed(() -> {
                Call.setLiquidSourceLiquid(null, tile, button.isChecked() ? items.get(f) : null);
                control.input().frag.config.hideConfig();
                lastLiquid = items.get(f);
            });
            button.getStyle().imageUp = new TextureRegionDrawable(items.get(i).iconRegion);
            button.setChecked(entity.source == items.get(i));

            if(i % 4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }

    @Override
    public TileEntity newEntity(){
        return new LiquidSourceEntity();
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void setLiquidSourceLiquid(Player player, Tile tile, Liquid liquid){
        LiquidSourceEntity entity = tile.entity();
        if(entity != null) entity.source = liquid;
    }

    class LiquidSourceEntity extends TileEntity{
        public Liquid source = null;

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
