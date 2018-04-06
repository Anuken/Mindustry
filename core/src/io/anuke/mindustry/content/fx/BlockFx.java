package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;

import static io.anuke.mindustry.Vars.tilesize;

public class BlockFx {
    public static final Effect

    reactorsmoke = new Effect(17, e -> {
        Angles.randLenVectors(e.id, 4, e.fin()*8f, (x, y)->{
            float size = 1f+e.fout()*5f;
            Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),
    nuclearsmoke = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 4, e.fin()*13f, (x, y)->{
            float size = e.fslope()*4f;
            Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),
    nuclearcloud = new Effect(90, 200f, e -> {
        Angles.randLenVectors(e.id, 10, e.finpow()*90f, (x, y)->{
            float size = e.fout()*14f;
            Draw.color(Color.LIME, Color.GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),
    redgeneratespark = new Effect(18, e -> {
        Angles.randLenVectors(e.id, 5, e.fin()*8f, (x, y)->{
            float len = e.fout()*4f;
            Draw.color(Color.valueOf("fbb97f"), Color.GRAY, e.fin());
            //Draw.alpha(e.fout());
            Draw.rect("circle", e.x + x, e.y + y, len, len);
            Draw.reset();
        });
    }),
    generatespark = new Effect(18, e -> {
        Angles.randLenVectors(e.id, 5, e.fin()*8f, (x, y)->{
            float len = e.fout()*4f;
            Draw.color(Color.valueOf("d2b29c"), Color.GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, len, len);
            Draw.reset();
        });
    }),
    fuelburn = new Effect(23, e -> {
        Angles.randLenVectors(e.id, 5, e.fin()*9f, (x, y)->{
            float len = e.fout()*4f;
            Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, len, len);
            Draw.reset();
        });
    }),
    plasticburn = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 5, 3f + e.fin()*5f, (x, y)->{
            Draw.color(Color.valueOf("e9ead3"), Color.GRAY, e.fin());
            Fill.circle(e.x + x, e.y + y, e.fout()*1f);
            Draw.reset();
        });
    }),
    pulverize = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 5, 3f + e.fin()*8f, (x, y)->{
            Draw.color(Palette.stoneGray);
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 2f + 0.5f, 45);
            Draw.reset();
        });
    }),
    pulverizeRed = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 5, 3f + e.fin()*8f, (x, y)->{
            Draw.color(Color.valueOf("ffa480"), Palette.stoneGray, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 2f + 0.5f, 45);
            Draw.reset();
        });
    }),
    pulverizeRedder = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 5, 3f + e.fin()*9f, (x, y)->{
            Draw.color(Color.valueOf("ff7b69"), Palette.stoneGray, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 2.5f + 0.5f, 45);
            Draw.reset();
        });
    }),
    pulverizeSmall = new Effect(30, e -> {
        Angles.randLenVectors(e.id, 3, e.fin()*5f, (x, y)->{
            Draw.color(Palette.stoneGray);
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 1f + 0.5f, 45);
            Draw.reset();
        });
    }),
    pulverizeMedium = new Effect(30, e -> {
        Angles.randLenVectors(e.id, 5, 3f + e.fin()*8f, (x, y)->{
            Draw.color(Palette.stoneGray);
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 1f + 0.5f, 45);
            Draw.reset();
        });
    }),
    producesmoke = new Effect(12, e -> {
        Angles.randLenVectors(e.id, 8, 4f + e.fin()*18f, (x, y)->{
            Draw.color(Color.WHITE, Colors.get("accent"), e.fin());
            Fill.poly(e.x + x, e.y + y, 4, 1f+e.fout()*3f, 45);
            Draw.reset();
        });
    }),
    smeltsmoke = new Effect(15, e -> {
        Angles.randLenVectors(e.id, 6, 4f + e.fin()*5f, (x, y)->{
            Draw.color(Color.WHITE, e.color, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, 0.5f+e.fout()*2f, 45);
            Draw.reset();
        });
    }),
    formsmoke = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 6, 5f + e.fin()*8f, (x, y)->{
            Draw.color(Color.valueOf("f1e479"), Color.LIGHT_GRAY, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, 0.2f+e.fout()*2f, 45);
            Draw.reset();
        });
    }),
    blastsmoke = new Effect(26, e -> {
        Angles.randLenVectors(e.id, 12, 1f + e.fin()*23f, (x, y)->{
            float size = 2f+e.fout()*6f;
            Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),
    lava = new Effect(18, e -> {
        Angles.randLenVectors(e.id, 3, 1f + e.fin()*10f, (x, y)->{
            float size = e.fslope()*4f;
            Draw.color(Color.ORANGE, Color.GRAY, e.fin());
            Draw.rect("circle", e.x + x, e.y + y, size, size);
            Draw.reset();
        });
    }),
    dooropen = new Effect(10, e -> {
        Lines.stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize / 2f + e.fin() * 2f);
        Draw.reset();
    }),
    doorclose= new Effect(10, e -> {
        Lines.stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize / 2f + e.fout() * 2f);
        Draw.reset();
    }),
    dooropenlarge = new Effect(10, e -> {
        Lines.stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize + e.fin() * 2f);
        Draw.reset();
    }),
    doorcloselarge = new Effect(10, e -> {
        Lines.stroke(e.fout() * 1.6f);
        Lines.square(e.x, e.y, tilesize + e.fout() * 2f);
        Draw.reset();
    }),
    purify = new Effect(10, e -> {
        Draw.color(Color.ROYAL, Color.GRAY, e.fin());
        Lines.stroke(2f);
        Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
        Draw.reset();
    }),
    purifyoil = new Effect(10, e -> {
        Draw.color(Color.BLACK, Color.GRAY, e.fin());
        Lines.stroke(2f);
        Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
        Draw.reset();
    }),
    purifystone = new Effect(10, e -> {
        Draw.color(Color.ORANGE, Color.GRAY, e.fin());
        Lines.stroke(2f);
        Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
        Draw.reset();
    }),
    generate = new Effect(11, e -> {
        Draw.color(Color.ORANGE, Color.YELLOW, e.fin());
        Lines.stroke(1f);
        Lines.spikes(e.x, e.y, e.fin() * 5f, 2, 8);
        Draw.reset();
    }),
    mine = new Effect(20, e -> {
        Angles.randLenVectors(e.id, 6, 3f + e.fin()*6f, (x, y)->{
            Draw.color(e.color, Color.LIGHT_GRAY, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 2f, 45);
            Draw.reset();
        });
    }),
    mineBig = new Effect(30, e -> {
        Angles.randLenVectors(e.id, 6, 4f + e.fin()*8f, (x, y)->{
            Draw.color(e.color, Color.LIGHT_GRAY, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 2f + 0.2f, 45);
            Draw.reset();
        });
    }),
    mineHuge = new Effect(40, e -> {
        Angles.randLenVectors(e.id, 8, 5f + e.fin()*10f, (x, y)->{
            Draw.color(e.color, Color.LIGHT_GRAY, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, e.fout() * 2f + 0.5f, 45);
            Draw.reset();
        });
    }),
    smelt = new Effect(20, e -> {
        Angles.randLenVectors(e.id, 6, 2f + e.fin()*5f, (x, y)->{
            Draw.color(Color.WHITE, e.color, e.fin());
            Fill.poly(e.x + x, e.y + y, 4, 0.5f+e.fout()*2f, 45);
            Draw.reset();
        });
    });
}
