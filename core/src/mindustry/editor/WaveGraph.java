package mindustry.editor;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
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
    public int from = 0, to = 20;

    private Mode mode = Mode.counts;
    private int[][] values;
    private OrderedSet<UnitType> used = new OrderedSet<>();
    private int max, maxTotal;
    private float maxHealth;
    private Table colors;
    private ObjectSet<UnitType> hidden = new ObjectSet<>();

    public WaveGraph(){
        background(Tex.pane);

        rect((x, y, width, height) -> {
            Lines.stroke(Scl.scl(3f));

            GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            Font font = Fonts.outline;

            lay.setText(font, "1");

            int maxY = switch(mode){
                case counts -> nextStep(max);
                case health -> nextStep((int)maxHealth);
                case totals -> nextStep(maxTotal);
            };

            float fh = lay.height;
            float offsetX = Scl.scl(lay.width * (maxY + "").length() * 2), offsetY = Scl.scl(22f) + fh + Scl.scl(5f);

            float graphX = x + offsetX, graphY = y + offsetY, graphW = width - offsetX, graphH = height - offsetY;
            float spacing = graphW / (values.length - 1);

            if(mode == Mode.counts){
                for(UnitType type : used.orderedItems()){
                    Draw.color(color(type));
                    Draw.alpha(parentAlpha);

                    Lines.beginLine();

                    for(int i = 0; i < values.length; i++){
                        int val = values[i][type.id];
                        float cx = graphX + i * spacing, cy = graphY + val * graphH / maxY;
                        Lines.linePoint(cx, cy);
                    }

                    Lines.endLine();
                }
            }else if(mode == Mode.totals){
                Lines.beginLine();

                Draw.color(Pal.accent);
                for(int i = 0; i < values.length; i++){
                    int sum = 0;
                    for(UnitType type : used.orderedItems()){
                        sum += values[i][type.id];
                    }

                    float cx = graphX + i * spacing, cy = graphY + sum * graphH / maxY;
                    Lines.linePoint(cx, cy);
                }

                Lines.endLine();
            }else if(mode == Mode.health){
                Lines.beginLine();

                Draw.color(Pal.health);
                for(int i = 0; i < values.length; i++){
                    float sum = 0;
                    for(UnitType type : used.orderedItems()){
                        sum += (type.health) * values[i][type.id];
                    }

                    float cx = graphX + i * spacing, cy = graphY + sum * graphH / maxY;
                    Lines.linePoint(cx, cy);
                }

                Lines.endLine();
            }

            //how many numbers can fit here
            float totalMarks = Mathf.clamp(maxY, 1, 10);

            int markSpace = Math.max(1, Mathf.ceil(maxY / totalMarks));

            Draw.color(Color.lightGray);
            Draw.alpha(0.1f);

            for(int i = 0; i < maxY; i += markSpace){
                float cy = graphY + i * graphH / maxY, cx = graphX;

                Lines.line(cx, cy, cx + graphW, cy);

                lay.setText(font, "" + i);

                font.draw("" + i, cx, cy + lay.height / 2f, Align.right);
            }
            Draw.alpha(1f);

            float len = Scl.scl(4f);
            font.setColor(Color.lightGray);

            for(int i = 0; i < values.length; i++){
                float cy = y + fh, cx = graphX + graphW / (values.length - 1) * i;

                Lines.line(cx, cy, cx, cy + len);
                if(i == values.length / 2){
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
                    b.image(type.uiIcon).size(32f).padRight(20).update(i -> i.setColor(b.isChecked() ? Color.gray : Color.white)).get().act(1);
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
