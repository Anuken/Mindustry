package mindustry.ui.fragments;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.core.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.core.PerfCounter.*;

public class PerformanceFragment{
    private static final ObjectMap<PerfCounter, Color> counterToColor = ObjectMap.of(
        buildingUpdate, Pal.items,
        powerUpdate, Pal.powerLight,
        unitUpdate, Color.royal,
        ui, Pal.remove,
        render, Pal.reactorPurple,
        stateUpdate, Color.cyan,
        other, Color.pink,
        entityMisc, Color.sky,
        bulletUpdate, Pal.redDust
    );

    private static Color color(PerfCounter counter){
        return counterToColor.get(counter, Color.white);
    }

    public void build(Group parent){
        parent.fill(t -> {
            t.visible(() -> {
                t.toFront();
                return Core.settings.getBool("showperformance");
            });
            t.touchable = Touchable.disabled;
            t.top().left();

            var outer = t.table(Styles.black8, i -> {
                i.margin(6f);
                i.add(new PerfBar()).size(400f, 50f);
            }).padBottom(8f);

            t.row();

            t.table(Styles.black8, i -> {
                i.margin(3f);
                i.left().top();

                float fscl = 1f;
                i.defaults().left();
                for(PerfCounter counter : displayedCounters){
                    i.label(() -> counter.name() + ": " + Strings.fixed(counter.valueMs(), 1)).style(Styles.outlineLabel).color(color(counter)).with(l -> l.setFontScale(fscl)).row();
                }

                i.add().height(30f).row();

                for(PerfCounter counter : all){
                    if(Structs.contains(displayedCounters, counter)) continue;

                    i.label(() -> counter.name() + ": " + Strings.fixed(counter.valueMs(), 1)).style(Styles.outlineLabel).with(l -> l.setFontScale(fscl)).color(color(counter)).row();
                }
            }).left();

            outer.get().toFront();
        });
    }

    static class PerfBar extends Element{

        @Override
        public void draw(){
            Vec2 mouse = screenToLocalCoordinates(Core.input.mouse()).add(x, y);
            float total = frame.valueMs();
            float accumulator = 0f;
            PerfCounter hoveredCounter = null;

            Draw.color(Color.darkGray);
            Fill.crect(x, y, width, height);

            for(var counter : PerfCounter.displayedCounters){
                float value = counter.valueMs();
                Draw.color(color(counter));

                float rx = x + accumulator / total * width, ry = y, rw = width * value/total, rh = height;

                Fill.crect(rx, ry, rw, rh);
                if(Rect.contains(rx, ry, rw, rh, mouse.x, mouse.y)){
                    hoveredCounter = counter;
                }
                accumulator += value;
            }
            Draw.color();

            if(hoveredCounter != null){
                float offset = Scl.scl(30f);
                String text = Strings.insertSpaces(Strings.capitalize(hoveredCounter.name())) + ": " + Strings.fixed(hoveredCounter.valueMs(), 1);
                GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
                layout.setText(Fonts.outline, text);
                Draw.color(0f, 0f, 0f, 0.5f);
                float pad = Scl.scl(4f);
                Fill.crect(mouse.x + offset - pad, mouse.y - offset - layout.height - pad, layout.width + pad*2f, layout.height + pad*2f);
                Draw.color();
                Fonts.outline.draw(text, mouse.x + offset, mouse.y - offset);
                Pools.free(layout);
            }
        }
    }
}
