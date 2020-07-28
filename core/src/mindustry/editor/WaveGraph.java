package mindustry.editor;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

public class WaveGraph extends Table{
    public Seq<SpawnGroup> groups = new Seq<>();
    public int from, to = 20;

    private int[][] values;
    private OrderedSet<UnitType> used = new OrderedSet<>();
    private int max;
    private Table colors;
    private ObjectSet<UnitType> hidden = new ObjectSet<>();

    public WaveGraph(){
        background(Tex.pane);

        rect((x, y, width, height) -> {
            Lines.stroke(Scl.scl(3f));
            Lines.precise(true);

            float offsetX = Scl.scl(30f), offsetY = Scl.scl(20f);

            float graphX = x + offsetX, graphY = y + offsetY, graphW = width - offsetX, graphH = height - offsetY;
            float spacing = graphW / (values.length - 1);

            for(UnitType type : used){
                Draw.color(color(type));
                Draw.alpha(parentAlpha);
                Lines.beginLine();
                for(int i = 0; i < values.length; i++){
                    int val = values[i][type.id];

                    float cx = graphX + i*spacing, cy = 2f + graphY + val * (graphH - 4f) / max;
                    Lines.linePoint(cx, cy);
                }
                Lines.endLine();
            }

            GlyphLayout lay = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            BitmapFont font = Fonts.outline;

            lay.setText(font, "1");

            //how many numbers can fit here
            float totalMarks = (height - offsetY - getMarginBottom() *2f - 1f) / (lay.height * 2);

            int markSpace = Math.max(1, Mathf.ceil(max / totalMarks));

            Draw.color(Color.lightGray);
            for(int i = 0; i < max; i += markSpace){
                float cy = 2f + y + i * (height - 4f) / max + offsetY, cx = x + offsetX;
                //Lines.line(cx, cy, cx + len, cy);

                lay.setText(font, "" + i);

                font.draw("" + i, cx, cy + lay.height/2f - Scl.scl(3f), Align.right);
            }

            float len = 4f;

            for(int i = 0; i < values.length; i++){
                float cy = y, cx = x + graphW / (values.length - 1) * i + offsetX;

                Lines.line(cx, cy, cx, cy + len);
            }

            Pools.free(lay);

            Lines.precise(false);
            Draw.reset();
        }).pad(4).padBottom(10).grow();

        row();

        table(t -> colors = t).growX();
    }

    public void rebuild(){
        values = new int[to - from + 1][Vars.content.units().size];
        used.clear();
        max = 1;

        for(int i = from; i <= to; i++){
            int index = i - from;

            for(SpawnGroup spawn : groups){
                int spawned = spawn.getUnitsSpawned(i);
                values[index][spawn.type.id] += spawned;
                if(spawned > 0){
                    used.add(spawn.type);
                }
                max = Math.max(max, values[index][spawn.type.id]);
            }
        }

        ObjectSet<UnitType> usedCopy = new ObjectSet<>(used);

        colors.clear();
        colors.left();
        for(UnitType type : used){
            colors.button(b -> {
                Color tcolor = color(type).cpy();
                b.image().size(32f).update(i -> i.setColor(b.isChecked() ? Tmp.c1.set(tcolor).mul(0.5f) : tcolor)).get().act(1);
                b.image(type.icon(Cicon.medium)).padRight(20).update(i -> i.setColor(b.isChecked() ? Color.gray : Color.white)).get().act(1);
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

        for(UnitType type : hidden){
            used.remove(type);
        }

    }

    Color color(UnitType type){
        return Tmp.c1.fromHsv(type.id / (float)Vars.content.units().size * 360f, 0.7f, 1f);
    }
}
