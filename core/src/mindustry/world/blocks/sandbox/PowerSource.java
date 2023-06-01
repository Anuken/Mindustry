package mindustry.world.blocks.sandbox;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class PowerSource extends PowerNode{
    /** Kept for backwards compatibility. Defines the default power production amount. */
    public float powerProduction = 10000f;

    public PowerSource(String name){
        super(name);
        maxNodes = 100;
        outputsPower = true;
        consumesPower = false;
        //TODO maybe don't?
        envEnabled = Env.any;

        config(Object[].class, (PowerSourceBuild build, Object[] configs) -> {
            for(Object obj : configs){
                build.configure(obj);
            }
        });

        config(Float.class, (PowerSourceBuild build, Float f) -> build.configPower = f);
    }

    public class PowerSourceBuild extends PowerNodeBuild{
        public float configPower = powerProduction;

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.config) return configPower;

            return super.senseObject(sensor);
        }

        @Override
        public float getPowerProduction(){
            return enabled ? configPower / 60f : 0f;
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black5, t -> {
                t.marginLeft(6f).marginRight(6f).right();
                t.field(String.valueOf(configPower), text -> {
                    configure(Strings.parseFloat(text));
                }).width(120).valid(Strings::canParsePositiveFloat).get().setFilter(TextFieldFilter.floatsOnly);
                t.add(StatUnit.powerSecond.localized()).left();
            });
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            if(renderer.pixelator.enabled()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            CharSequence text = "[#" + Pal.powerBar.toString() + "]" + Strings.autoFixed(configPower, 2) + " " + StatUnit.powerSecond.localized();

            l.setText(font, text, Color.white, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
            Draw.color();
            font.setColor(Color.white);
            font.draw(text, x - l.width/2f, y - tilesize/2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        @Override
        public Object config(){
            return new Object[]{super.config(), configPower};
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(configPower);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                configPower = read.f();
            }
        }

        @Override
        public byte version(){
            return 1;
        }
    }

}
