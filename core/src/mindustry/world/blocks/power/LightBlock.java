package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class LightBlock extends Block{
    private static int lastColor = 0;

    public float brightness = 0.9f;
    public float radius = 200f;
    public int topRegion;

    public LightBlock(String name){
        super(name);
        hasPower = true;
        update = true;
        topRegion = reg("-top");
        configurable = true;
        entityType = LightEntity::new;

        config(Integer.class, (tile, value) -> tile.<LightEntity>ent().color = value);
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastColor != 0){
            tile.configure(lastColor);
        }
    }

    @Override
    public void draw(){
        super.draw(tile);
        LightEntity entity = tile.ent();

        Draw.blend(Blending.additive);
        Draw.color(Tmp.c1.set(entity.color), entity.efficiency() * 0.3f);
        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy());
        Draw.color();
        Draw.blend();
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        LightEntity entity = tile.ent();

        table.addImageButton(Icon.pencil, () -> {
            ui.picker.show(Tmp.c1.set(entity.color).a(0.5f), false, res -> {
                entity.color = res.rgba();
                lastColor = entity.color;
            });
            control.input.frag.config.hideConfig();
        }).size(40f);
    }

    @Override
    public void drawLight(Tile tile){
        LightEntity entity = tile.ent();
        renderer.lights.add(tile.drawx(), tile.drawy(), radius, Tmp.c1.set(entity.color), brightness * tile.entity.efficiency());
    }

    public class LightEntity extends TileEntity{
        public int color = Pal.accent.rgba();

        @Override
        public Integer config(){
            return color;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(color);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            color = read.i();
        }
    }
}
