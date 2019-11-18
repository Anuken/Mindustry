package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class LightBlock extends Block{
    private static int lastColor = 0;

    protected float brightness = 0.9f;
    protected float radius = 200f;
    protected int topRegion;

    public LightBlock(String name){
        super(name);
        hasPower = true;
        update = true;
        topRegion = reg("-top");
        configurable = true;
        entityType = LightEntity::new;
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastColor != 0){
            tile.configure(lastColor);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);
        LightEntity entity = tile.entity();

        Draw.blend(Blending.additive);
        Draw.color(Tmp.c1.set(entity.color), entity.efficiency() * 0.3f);
        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy());
        Draw.color();
        Draw.blend();
    }

    @Override
    public void buildTable(Tile tile, Table table){
        LightEntity entity = tile.entity();

        table.addImageButton(Icon.pencilSmall, () -> {
            ui.picker.show(Tmp.c1.set(entity.color).a(0.5f), false, res -> {
                entity.color = res.rgba();
                lastColor = entity.color;
            });
            control.input.frag.config.hideConfig();
        }).size(40f);
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<LightEntity>entity().color = value;
    }

    @Override
    public void drawLight(Tile tile){
        LightEntity entity = tile.entity();
        renderer.lights.add(tile.drawx(), tile.drawy(), radius, Tmp.c1.set(entity.color), brightness * tile.entity.efficiency());
    }

    public class LightEntity extends TileEntity{
        public int color = Pal.accent.rgba();

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeInt(color);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            color = stream.readInt();
        }
    }
}
