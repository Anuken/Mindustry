package mindustry.entities.comp;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.pooling.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

/** Component/entity for labels in world space. Useful for servers. Does not save in files - create only on world load. */
@EntityDef(value = {WorldLabelc.class}, serialize = false)
@Component(base = true, genInterface = false)
public abstract class WorldLabelComp implements Posc, Drawc, Syncc{
    @Import int id;
    @Import float x, y;

    public static final byte flagBackground = 1, flagOutline = 2;

    public String text = "sample text";
    public float fontSize = 1f, z = Layer.playerName + 1;
    /** Flags are packed into a byte for sync efficiency; see the flag static values. */
    public byte flags = flagBackground | flagOutline;

    @Replace
    public float clipSize(){
        return text.length() * 10f * fontSize;
    }

    @Override
    public void draw(){
        drawAt(text, x, y, z, flags, fontSize, Align.center, Align.center);
    }

    public static void drawAt(String text, float x, float y, float layer, int flags, float fontSize, int align, int lineAlign){
        Draw.z(layer);
        float z = Drawf.text();

        Font font = (flags & flagOutline) != 0 ? Fonts.outline : Fonts.def;
        GlyphLayout layout = Pools.obtain(GlyphLayout.class, GlyphLayout::new);

        boolean ints = font.usesIntegerPositions();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.25f / Scl.scl(1f) * fontSize);
        layout.setText(font, text);

        int border = (flags & flagBackground) != 0 ? 1 : 0;

        if(Align.isBottom(align)){
            y += layout.height + border * 1.5f;
        }else if(Align.isTop(align)){
            y -= border * 1.5f;
        }else{
            y += layout.height / 2;
        }

        if(Align.isLeft(align)){
            x += layout.width / 2 + border;
        }else if(Align.isRight(align)){
            x -= layout.width / 2 + border;
        }

        if((flags & flagBackground) != 0){
            Draw.color(0f, 0f, 0f, 0.3f);
            Fill.rect(x, y - layout.height / 2, layout.width + 2, layout.height + 3);
            Draw.color();
        }

        float tx = Align.isLeft(lineAlign) ? -layout.width * 0.5f : Align.isRight(lineAlign) ? layout.width * 0.5f : 0;

        font.setColor(Color.white);
        font.draw(text, x + tx, y, 0, lineAlign, false);

        Draw.reset();
        Pools.free(layout);
        font.getData().setScale(1f);
        font.setColor(Color.white);
        font.setUseIntegerPositions(ints);

        Draw.z(z);
    }

    /** This MUST be called instead of remove()! */
    public void hide(){
        remove();
        Call.removeWorldLabel(id);
    }
}
