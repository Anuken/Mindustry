package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

public class WaveGraph extends Table{
    public Seq<SpawnGroup> groups = new Seq<>();

    private Mode mode = Mode.counts;
    private int[][] values;
    private OrderedSet<UnitType> used = new OrderedSet<>();
    private int max, maxTotal;
    private float maxHealth;
    private Table colors;
    private ObjectSet<UnitType> hidden = new ObjectSet<>();
    private StringBuilder countStr = new StringBuilder();

    private float pan;
    private float zoom = 1f;
    private int from = 0, to = 20;
    private int lastFrom = -1, lastTo = -1;
    private float lastZoom = -1f;

    private float defaultSpace = Scl.scl(40f);
    private FloatSeq points = new FloatSeq(40);

    public WaveGraph(){
        background(Tex.pane);

        scrolled((scroll) -> {
            zoom -= scroll * 2f / 10f * zoom;
            clampZoom();
        });

        touchable = Touchable.enabled;
        addListener(new InputListener(){

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                requestScroll();
            }
        });

        addListener(new ElementGestureListener(){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                pan -= deltaX/zoom;
            }

            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(lastZoom < 0) lastZoom = zoom;

                zoom = distance / initialDistance * lastZoom;
                clampZoom();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                lastZoom = zoom;
            }
        });

        rect((x, y, width, height) -> {
            Lines.stroke(Scl.scl(3f));
            countStr.setLength(0);

            Vec2 mouse = stageToLocalCoordinates(Core.input.mouse());

            GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            Font font = Fonts.outline;

            int maxY = switch(mode){
                case counts -> nextStep(max);
                case health -> nextStep((int)maxHealth);
                case totals -> nextStep(maxTotal);
            };

            lay.setText(font, "1");

            float spacing = zoom * defaultSpace;
            pan = Math.max(pan, (width/2f)/zoom-defaultSpace);

            float fh = lay.height;
            float offsetX = 0f, offsetY = Scl.scl(22f) + fh + Scl.scl(5f);
            float graphX = x + offsetX - pan * zoom + width/2f, graphY = y + offsetY, graphW = width - offsetX, graphH = height - offsetY;

            float left = (x-graphX)/spacing, right = (x + width - graphX)/spacing;

            //int radius = Mathf.ceil(graphW / spacing / 2f);

            from = (int)left - 1;
            to = (int)right + 1;

            if(lastFrom != from || lastTo != to){
                rebuild();
            }

            lastFrom = from;
            lastTo = to;

            if(!clipBegin(x + offsetX, y + offsetY, graphW, graphH)) return;

            int selcol = Rect.contains(x, y, width, height, mouse.x, mouse.y) ? Mathf.round((mouse.x - graphX - (from * spacing)) / spacing) : -1;
            if(selcol + from <= -1) selcol = -1;

            if(mode == Mode.counts){
                for(UnitType type : used.orderedItems()){
                    Draw.color(color(type));
                    Draw.alpha(parentAlpha);

                    beginLine();

                    for(int i = 0; i < values.length; i++){
                        int val = values[i][type.id];
                        float cx = graphX + (i+from) * spacing, cy = graphY + val * graphH / maxY;
                        linePoint(cx, cy);
                    }

                    endLine();
                }
            }else if(mode == Mode.totals){
                beginLine();

                Draw.color(Pal.accent);
                for(int i = 0; i < values.length; i++){
                    int sum = 0;
                    for(UnitType type : used.orderedItems()){
                        sum += values[i][type.id];
                    }

                    float cx = graphX + (i+from) * spacing, cy = graphY + sum * graphH / maxY;
                    linePoint(cx, cy);
                }

                endLine();
            }else if(mode == Mode.health){
                beginLine();

                Draw.color(Pal.health);
                for(int i = 0; i < values.length; i++){
                    float sum = 0;
                    for(UnitType type : used.orderedItems()){
                        sum += (type.health) * values[i][type.id];
                    }

                    float cx = graphX + (i+from) * spacing, cy = graphY + sum * graphH / maxY;
                    linePoint(cx, cy);
                }

                endLine();
            }


            if(selcol >= 0 && selcol < values.length){
                Draw.color(1f, 0f, 0f, 0.2f);
                Fill.crect((selcol+from) * spacing + graphX - spacing/2f, graphY, spacing, graphH);
                Draw.color();
                font.getData().setScale(1.5f);
                for(UnitType type : used.orderedItems()){
                    int amount = values[Mathf.clamp(selcol, 0, values.length - 1)][type.id];
                    if(amount > 0){
                        countStr.append(type.emoji()).append(" ").append(amount).append("\n");
                    }
                }
                float pad = Scl.scl(5f);
                font.draw(countStr, (selcol+from) * spacing + graphX - spacing/2f + pad, graphY + graphH - pad);
                font.getData().setScale(1f);
            }

            clipEnd();

            //how many numbers can fit here
            float totalMarks = Mathf.clamp(maxY, 1, 10);

            int markSpace = Math.max(1, Mathf.ceil(maxY / totalMarks));

            Draw.color(Color.lightGray);
            Draw.alpha(0.1f);

            for(int i = 0; i < maxY; i += markSpace){
                float cy = graphY + i * graphH / maxY, cx = x;

                Lines.line(cx, cy, cx + graphW, cy);

                lay.setText(font, "" + i);

                font.draw("" + i, cx, cy + lay.height / 2f, Align.left);
            }
            Draw.alpha(1f);

            float len = Scl.scl(4f);
            font.setColor(Color.lightGray);

            for(int i = 0; i < values.length; i++){
                float cy = y + fh, cx = graphX + spacing * (i + from);

                if(cx >= x + offsetX && cx <= x + offsetX + graphW){
                    Lines.line(cx, cy, cx, cy + len);
                }
                if(i == selcol){
                    font.draw("" + (i + from + 1), cx, cy - Scl.scl(2f), Align.center);
                }
            }
            font.setColor(Color.white);

            Pools.free(lay);

            Draw.reset();
        }).pad(4).padBottom(10).grow();

        row();

        table(t -> colors = t).growX();

        row();

        table(t -> {
            t.left();
            ButtonGroup<Button> group = new ButtonGroup<>();

            for(Mode m : Mode.all){
                t.button("@wavemode." + m.name(), Styles.fullTogglet, () -> {
                    mode = m;
                }).group(group).height(35f).update(b -> b.setChecked(m == mode)).width(130f);
            }
        }).growX();
    }

    private void clampZoom(){
        zoom = Mathf.clamp(zoom, 0.5f / Scl.scl(1f), 40f / Scl.scl(1f));
    }

    private void linePoint(float x, float y){
        points.add(x, y);
    }

    private void beginLine(){
        points.clear();
    }

    private void endLine(){
        var items = points.items;
        for(int i = 0; i < points.size - 2; i += 2){
            Lines.line(items[i], items[i + 1], items[i + 2], items[i + 3], false);
            Fill.circle(items[i], items[i + 1], Lines.getStroke()/2f);
        }
        Fill.circle(items[points.size - 2], items[points.size - 1], Lines.getStroke());
        points.clear();
    }

    public void rebuild(){
        values = new int[to - from + 1][Vars.content.units().size];
        used.clear();
        max = maxTotal = 1;
        maxHealth = 1f;

        for(int i = from; i <= to; i++){
            int index = i - from;
            float healthsum = 0f;
            int sum = 0;

            for(SpawnGroup spawn : groups){
                int spawned = spawn.getSpawned(i);
                values[index][spawn.type.id] += spawned;
                if(spawned > 0){
                    used.add(spawn.type);
                }
                max = Math.max(max, values[index][spawn.type.id]);
                healthsum += spawned * (spawn.type.health);
                sum += spawned;
            }
            maxTotal = Math.max(maxTotal, sum);
            maxHealth = Math.max(maxHealth, healthsum);
        }

        used.orderedItems().sort();

        ObjectSet<UnitType> usedCopy = new ObjectSet<>(used);

        colors.clear();
        colors.left();
        colors.button("@waves.units.hide", Styles.flatt, () -> {
            if(hidden.size == usedCopy.size){
                hidden.clear();
            }else{
                hidden.addAll(usedCopy);
            }

            used.clear();
            used.addAll(usedCopy);
            for(UnitType o : hidden) used.remove(o);
        }).update(b -> b.setText(hidden.size == usedCopy.size ? "@waves.units.show" : "@waves.units.hide")).height(32f).width(130f);
        colors.pane(t -> {
            t.left();
            for(UnitType type : used){
                t.button(b -> {
                    Color tcolor = color(type).cpy();
                    b.image().size(32f).update(i -> i.setColor(b.isChecked() ? Tmp.c1.set(tcolor).mul(0.5f) : tcolor)).get().act(1);
                    b.image(type.uiIcon).size(32f).scaling(Scaling.fit).padRight(20).update(i -> i.setColor(b.isChecked() ? Color.gray : Color.white)).get().act(1);
                    b.margin(0f);
                }, Styles.fullTogglet, () -> {
                    if(!hidden.add(type)){
                        hidden.remove(type);
                    }

                    used.clear();
                    used.addAll(usedCopy);
                    for(UnitType o : hidden) used.remove(o);
                }).update(b -> b.setChecked(hidden.contains(type)));
            }
        }).scrollY(false);

        colors.act(0.000001f);

        for(UnitType type : hidden){
            used.remove(type);
        }
    }

    Color color(UnitType type){
        return Tmp.c1.fromHsv(type.id / (float)Vars.content.units().size * 360f, 0.7f, 1f);
    }

    int nextStep(float value){
        int order = 1;
        while(order < value){
            if(order * 2 > value){
                return order * 2;
            }
            if(order * 5 > value){
                return order * 5;
            }
            if(order * 10 > value){
                return order * 10;
            }
            order *= 10;
        }
        return order;
    }

    enum Mode{
        counts, totals, health;

        static Mode[] all = values();
    }
}
