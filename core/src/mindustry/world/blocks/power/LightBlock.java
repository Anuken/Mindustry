package mindustry.world.blocks.power;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class LightBlock extends Block{
    public float brightness = 0.9f;
    public float radius = 200f;
    public @Load("@-top") TextureRegion topRegion;

    public LightBlock(String name){
        super(name);
        hasPower = true;
        update = true;
        configurable = true;
        saveConfig = true;

        config(Integer.class, (LightBuild tile, Integer value) -> tile.color = value);
    }

    public class LightBuild extends Building{
        public int color = Pal.accent.rgba();
        public float smoothTime = 1f;

        @Override
        public void draw(){
            super.draw();
            Draw.blend(Blending.additive);
            Draw.color(Tmp.c1.set(color), efficiency() * 0.3f);
            Draw.rect(topRegion, x, y);
            Draw.color();
            Draw.blend();
        }

        @Override
        public void updateTile(){
            smoothTime = Mathf.lerpDelta(smoothTime, timeScale, 0.1f);
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, () -> {
                ui.picker.show(Tmp.c1.set(color).a(0.5f), false, res -> configure(res.rgba()));
                deselect();
            }).size(40f);
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, radius * Math.min(smoothTime, 2f), Tmp.c1.set(color), brightness * efficiency());
        }

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
