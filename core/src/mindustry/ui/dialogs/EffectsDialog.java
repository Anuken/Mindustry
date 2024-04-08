package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.logic.LogicFx.*;
import mindustry.ui.*;
import mindustry.world.*;

public class EffectsDialog extends BaseDialog{
    static BoundsBatch bounds = new BoundsBatch();

    Iterable<EffectEntry> entries;
    @Nullable Cons<EffectEntry> listener;

    public EffectsDialog(Iterable<EffectEntry> entries){
        super("Effects");

        this.entries = entries;

        addCloseButton();
        makeButtonOverlay();
        onResize(this::setup);
        shown(this::setup);

        setup();
    }

    public EffectsDialog(){
        this(LogicFx.entries());
    }

    public static EffectsDialog withAllEffects(){
        return new EffectsDialog(Seq.select(Fx.class.getFields(), f -> f.getType() == Effect.class).map(f -> new EffectEntry(Reflect.get(f)).name(f.getName())));
    }

    public Dialog show(Cons<EffectEntry> listener){
        this.listener = listener;
        return super.show();
    }

    @Override
    public Dialog show(){
        this.listener = null;
        return super.show();
    }

    void setup(){
        float size = 280f;
        int cols = (int)Math.max(1, Core.graphics.getWidth() / Scl.scl(size + 12f));

        cont.clearChildren();
        cont.pane(t -> {
            int i = 0;
            for(var entry : entries){
                float bounds = calculateSize(entry);

                if(bounds <= 0) continue;

                ClickListener cl = new ClickListener();

                t.stack(
                new EffectCell(entry, cl),
                new Table(af -> af.add(entry.name).grow().labelAlign(Align.bottomLeft).style(Styles.outlineLabel).bottom().left())
                ).size(size).with(a -> {
                    a.clicked(() -> {
                        if(listener != null){
                            listener.get(entry);
                            hide();
                        }
                    });
                    a.addListener(cl);
                    a.addListener(new HandCursorListener(() -> listener != null, true));
                });

                if(++i % cols == 0) t.row();
            }
        }).grow().scrollX(false);
    }

    static Object getData(Class<?> type){
        if(type == Block.class) return Blocks.router;
        return null;
    }

    static float calculateSize(EffectEntry entry){
        if(entry.bounds >= 0) return entry.bounds;


        var effect = entry.effect;
        try{
            effect.init();
            Batch prev = Core.batch;
            bounds.reset();
            Core.batch = bounds;
            Object data = getData(entry.data);

            float lifetime = effect.lifetime;
            float rot = 1f;
            int steps = 60;
            int seeds = 4;
            for(int s = 0; s < seeds; s++){
                for(int i = 0; i <= steps; i++){
                    effect.render(1, Color.white, i / (float)steps * lifetime, lifetime, rot, 0f, 0f, data);
                }
            }

            Core.batch = prev;

            return entry.bounds = bounds.max * 2f;
        }catch(Exception e){
            //might crash with invalid data
            return -1f;
        }
    }

    static class BoundsBatch extends Batch{
        float max;

        void reset(){
            max = 0f;
        }

        void max(float... xs){
            for(float f : xs){
                if(Float.isNaN(f)) continue;
                max = Math.max(max, Math.abs(f));
            }
        }

        @Override
        protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
            for(int i = offset; i < count; i += SpriteBatch.VERTEX_SIZE){
                max(spriteVertices[i], spriteVertices[i + 1]);
            }
        }

        @Override
        protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
            float worldOriginX = x + originX;
            float worldOriginY = y + originY;
            float fx = -originX;
            float fy = -originY;
            float fx2 = width - originX;
            float fy2 = height - originY;
            float cos = Mathf.cosDeg(rotation);
            float sin = Mathf.sinDeg(rotation);
            float x1 = cos * fx - sin * fy + worldOriginX;
            float y1 = sin * fx + cos * fy + worldOriginY;
            float x2 = cos * fx - sin * fy2 + worldOriginX;
            float y2 = sin * fx + cos * fy2 + worldOriginY;
            float x3 = cos * fx2 - sin * fy2 + worldOriginX;
            float y3 = sin * fx2 + cos * fy2 + worldOriginY;

            max(x1, y1, x2, y2, x3, y3, x1 + (x3 - x2), y3 - (y2 - y1));
        }

        @Override
        protected void flush(){}
    }

    class EffectCell extends Element{
        EffectEntry effect;
        float size = -1f;

        int id = 1;
        float time = 0f;
        float lifetime;
        float rotation = 1f;
        Object data;
        ClickListener cl;

        public EffectCell(EffectEntry effect, ClickListener cl){
            this.effect = effect;
            this.lifetime = effect.effect.lifetime;
            this.cl = cl;

            data = getData(effect.data);
        }

        @Override
        public void draw(){
            if(size < 0){
                size = calculateSize(effect) + 1f;
            }

            color.fromHsv((Time.globalTime * 2f) % 360f, 1f, 1f);

            if(clipBegin(x, y, width, height)){
                Draw.colorl(cl.isOver() && listener != null ? 0.4f : 0.5f);
                Draw.alpha(parentAlpha);
                Tex.alphaBg.draw(x, y, width, height);
                Draw.reset();
                Draw.flush();

                float scale = width / size;
                Tmp.m1.set(Draw.trans());
                Draw.trans().translate(x + width/2f, y + height/2f).scale(scale, scale);
                Draw.flush();
                this.lifetime = effect.effect.render(id, color, time, lifetime, rotation, 0f, 0f, data);

                Draw.flush();
                Draw.trans().set(Tmp.m1);
                clipEnd();
            }

            Lines.stroke(Scl.scl(3f), Color.black);
            Lines.rect(x, y, width, height);
            Draw.reset();
        }

        @Override
        public void act(float delta){
            super.act(delta);

            time += Time.delta;
            if(time >= lifetime){
                id ++;
            }
            time %= lifetime;
        }
    }
}
