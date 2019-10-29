package io.anuke.mindustry.mod;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.ui.*;

public class ModCrashHandler{

    public static void handle(Throwable t){
        Array<Throwable> list = Strings.getCauses(t);
        Throwable modCause = list.find(e -> e instanceof ModLoadException);

        if(modCause != null && Fonts.outline != null){
            String text = "[scarlet][[A fatal crash has occured while loading a mod!][]\n\nReason:[accent] " + modCause.getMessage();
            String bottom = "[scarlet]The associated mod has been disabled. Swipe out of the app and launch it again.";
            GlyphLayout layout = new GlyphLayout();
            Core.atlas = TextureAtlas.blankAtlas();
            Colors.put("accent", Pal.accent);

            Core.app.addListener(new ApplicationListener(){
                @Override
                public void update(){
                    Core.graphics.clear(0.1f, 0.1f, 0.1f, 1f);
                    float rad = Math.min(Core.graphics.getWidth(), Core.graphics.getHeight()) / 2f / 1.3f;
                    Draw.color(Color.scarlet, Color.black, Mathf.absin(Core.graphics.getFrameId(), 15f, 0.6f));
                    Lines.stroke(Scl.scl(40f));
                    //Lines.poly2(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f, 3, rad, 0f);
                    float cx = Core.graphics.getWidth()/2f, cy = Core.graphics.getHeight()/2f;
                    for(int i = 0; i < 3; i++){
                        float angle1 = i * 120f + 90f;
                        float angle2 = (i + 1) * 120f + 90f;
                        Tmp.v1.trnsExact(angle1, rad - Lines.getStroke()/2f).add(cx, cy);
                        Tmp.v2.trnsExact(angle2, rad - Lines.getStroke()/2f).add(cx, cy);
                        Tmp.v3.trnsExact(angle1, rad + Lines.getStroke()/2f).add(cx, cy);
                        Tmp.v4.trnsExact(angle2, rad + Lines.getStroke()/2f).add(cx, cy);
                        Fill.quad(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, Tmp.v4.x, Tmp.v4.y, Tmp.v3.x, Tmp.v3.y);
                    }
                    Lines.lineAngleCenter(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f - Scl.scl(5f), 90f, rad/3.1f);
                    Fill.square(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f + rad/2f - Scl.scl(15f), Lines.getStroke()/2f);
                    Draw.reset();

                    Fonts.outline.getData().markupEnabled = true;
                    layout.setText(Fonts.outline, text, Color.white, Core.graphics.getWidth(), Align.left, true);
                    Fonts.outline.draw(text, Core.graphics.getWidth()/2f - layout.width/2f, Core.graphics.getHeight() - Scl.scl(50f), Core.graphics.getWidth(), Align.left, true);

                    layout.setText(Fonts.outline, bottom, Color.white, Core.graphics.getWidth(), Align.left, true);
                    Fonts.outline.draw(bottom, Core.graphics.getWidth()/2f - layout.width/2f, layout.height + Scl.scl(10f), Core.graphics.getWidth(), Align.left, true);
                    Draw.flush();
                }

                @Override
                public void resize(int width, int height){
                    Draw.proj().setOrtho(0, 0, width, height);
                }
            });
        }else{
            throw new RuntimeException(t);
        }
    }
}
