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
import mindustry.logic.*;
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

    @Override
    public void init(){
        lightRadius = radius;
        emitLight = true;
        super.init();
    }

    public class LightBuild extends Building{
        public int color = Pal.accent.rgba();
        public float smoothTime = 1f;

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            if(type == LAccess.color){
                color = Color.rgba8888((float)p1, (float)p2, (float)p3, 1f);
            }

            super.control(type, p1, p2, p3, p4);
        }

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
            Drawf.light(team, x, y, lightRadius * Math.min(smoothTime, 2f), Tmp.c1.set(color), brightness * efficiency());
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
