package io.anuke.mindustry.world.blocks.others;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.scene.ui.Button;
import io.anuke.arc.scene.ui.ButtonGroup;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.units.CommandCenter;
import io.anuke.mindustry.world.meta.BlockGroup;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import static io.anuke.mindustry.Vars.iconsizesmall;
import static io.anuke.mindustry.Vars.player;

public class LED extends Block {

    public LED(String name) {
        super(name);
        group = BlockGroup.none;
        update = true;
        hasPower = true;
        configurable = true;
        layer = Layer.overlay;

    }

    @Override
    public void buildTable(Tile tile, Table table){
        LEDEntity entity = tile.entity();
        ButtonGroup<Button> group = new ButtonGroup<>();
        Table buttons = new Table();

        for(int hColor : getColors()){

            Color color = getColor(hColor);

            buttons.addButton(button -> button.setColor(color), () -> entity.color = color)
                    .size(44)
                    .group(group)
                    .update(button -> button.setChecked(button.color == entity.color));
        }
        table.add(buttons);
        table.row();
    }

    @Override
    public void update(Tile tile) {

    }

    @Override
    public void draw(Tile tile) {
        LEDEntity entity = tile.entity();
        int[] arrayHSV = Color.RGBtoHSV(entity.color);
        Color color = Color.HSVtoRGB(arrayHSV[0], 100, entity.power.satisfaction*100);

        Draw.color(color);
        Draw.rect(region, tile.drawx(), tile.drawy());
        Draw.color();
    }

    @Override
    public TileEntity newEntity() {
        return new LEDEntity();
    }

    private List<Integer> getColors(){

        List<Integer> colors = new ArrayList<>();

        for(int h = 0; h < 350; h+=25){
            colors.add(h);
        }
        return colors;

    }

    private Color getColor(int h){
        return Color.HSVtoRGB(h, 100, 100);
    }

    public static class LEDEntity extends TileEntity{
        public Color color = new Color(255, 255, 255);

        /*@Override
        public void write(DataOutput stream) throws IOException {
            super.write(stream);
            stream.write(Color.RGBtoHSV(color)[0]);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException {
            super.read(stream, revision);
            color = Color.HSVtoRGB(stream.readInt(), 0, 0);
        }*/
    }
}
