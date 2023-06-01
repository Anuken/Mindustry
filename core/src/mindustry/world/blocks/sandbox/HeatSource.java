package mindustry.world.blocks.sandbox;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class HeatSource extends Block{
    public DrawBlock drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());

    public HeatSource(String name){
        super(name);

        update = true;
        solid = true;
        configurable = true;
        saveConfig = true;
        noUpdateDisabled = true;
        envEnabled = Env.any;
        rotateDraw = false;
        rotate = true;
        canOverdrive = false;
        drawArrow = true;

        config(Float.class, (HeatSourceBuild build, Float h) -> build.heat = h);
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out){
        drawer.getRegionsToOutline(this, out);
    }

    public class HeatSourceBuild extends Building implements HeatBlock{
        public float heat = 1000f;

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.config) return heat;

            return super.senseObject(sensor);
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black5, t -> {
                t.marginLeft(6f).marginRight(6f).right();
                t.field(String.valueOf(heat), text -> {
                    configure(Strings.parseFloat(text));
                }).width(120).valid(Strings::canParsePositiveFloat).get().setFilter(TextFieldFilter.floatsOnly);
                t.add(StatUnit.heatUnits.localized()).left();
            });
        }

        @Override
        public void drawSelect(){
            if(renderer.pixelator.enabled()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            CharSequence text = "[#" + Pal.lightOrange.toString() + "]" + Strings.autoFixed(heat, 2) + " " + StatUnit.heatUnits.localized();

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
        public Float config(){
            return heat;
        }

        @Override
        public float warmup(){
            return 1f;
        }

        @Override
        public float heatFrac(){
            return 1f;
        }

        @Override
        public float heat(){
            return heat;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision == 0){ //Discard old save data from GenericCrafter
                read.f();
                read.f();
            }

            heat = read.f();
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
