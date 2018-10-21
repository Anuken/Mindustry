package io.anuke.mindustry.content.fx;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.effect.GroundEffectEntity.GroundEffect;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.game.ContentList;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.tilesize;

public class BlockFx extends FxList implements ContentList{
    public static Effect reactorsmoke, nuclearsmoke, nuclearcloud, redgeneratespark, generatespark, fuelburn, plasticburn,
    pulverize, pulverizeRed, pulverizeRedder, pulverizeSmall, pulverizeMedium, producesmoke, smeltsmoke, formsmoke, blastsmoke,
    lava, dooropen, doorclose, dooropenlarge, doorcloselarge, purify, purifyoil, purifystone, generate, mine, mineBig, mineHuge,
    smelt, teleportActivate, teleport, teleportOut, ripple, bubble, commandSend, healBlock, healBlockFull, healWaveMend, overdriveWave,
    overdriveBlockFull, shieldBreak;

    @Override
    public void load(){

        reactorsmoke = new Effect(17, e -> {
            Angles.randLenVectors(e.id, 4, e.fin() * 8f, (x, y) -> {
                float size = 1f + e.fout() * 5f;
                Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
                Draw.reset();
            });
        });
        nuclearsmoke = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 4, e.fin() * 13f, (x, y) -> {
                float size = e.fslope() * 4f;
                Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
                Draw.reset();
            });
        });
        nuclearcloud = new Effect(90, 200f, e -> {
            Angles.randLenVectors(e.id, 10, e.finpow() * 90f, (x, y) -> {
                float size = e.fout() * 14f;
                Draw.color(Color.LIME, Color.GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
                Draw.reset();
            });
        });
        redgeneratespark = new Effect(18, e -> {
            Angles.randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
                float len = e.fout() * 4f;
                Draw.color(Palette.redSpark, Color.GRAY, e.fin());
                //Draw.alpha(e.fout());
                Draw.rect("circle", e.x + x, e.y + y, len, len);
                Draw.reset();
            });
        });
        generatespark = new Effect(18, e -> {
            Angles.randLenVectors(e.id, 5, e.fin() * 8f, (x, y) -> {
                float len = e.fout() * 4f;
                Draw.color(Palette.orangeSpark, Color.GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, len, len);
                Draw.reset();
            });
        });
        fuelburn = new Effect(23, e -> {
            Angles.randLenVectors(e.id, 5, e.fin() * 9f, (x, y) -> {
                float len = e.fout() * 4f;
                Draw.color(Color.LIGHT_GRAY, Color.GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, len, len);
                Draw.reset();
            });
        });
        plasticburn = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 5f, (x, y) -> {
                Draw.color(Color.valueOf("e9ead3"), Color.GRAY, e.fin());
                Fill.circle(e.x + x, e.y + y, e.fout() * 1f);
                Draw.reset();
            });
        });
        pulverize = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
                Draw.color(Palette.stoneGray);
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
                Draw.reset();
            });
        });
        pulverizeRed = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
                Draw.color(Palette.redDust, Palette.stoneGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
                Draw.reset();
            });
        });
        pulverizeRedder = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 9f, (x, y) -> {
                Draw.color(Palette.redderDust, Palette.stoneGray, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2.5f + 0.5f, 45);
                Draw.reset();
            });
        });
        pulverizeSmall = new Effect(30, e -> {
            Angles.randLenVectors(e.id, 3, e.fin() * 5f, (x, y) -> {
                Draw.color(Palette.stoneGray);
                Fill.square(e.x + x, e.y + y, e.fout() * 1f + 0.5f, 45);
                Draw.reset();
            });
        });
        pulverizeMedium = new Effect(30, e -> {
            Angles.randLenVectors(e.id, 5, 3f + e.fin() * 8f, (x, y) -> {
                Draw.color(Palette.stoneGray);
                Fill.square(e.x + x, e.y + y, e.fout() * 1f + 0.5f, 45);
                Draw.reset();
            });
        });
        producesmoke = new Effect(12, e -> {
            Angles.randLenVectors(e.id, 8, 4f + e.fin() * 18f, (x, y) -> {
                Draw.color(Color.WHITE, Palette.accent, e.fin());
                Fill.square(e.x + x, e.y + y, 1f + e.fout() * 3f, 45);
                Draw.reset();
            });
        });
        smeltsmoke = new Effect(15, e -> {
            Angles.randLenVectors(e.id, 6, 4f + e.fin() * 5f, (x, y) -> {
                Draw.color(Color.WHITE, e.color, e.fin());
                Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
                Draw.reset();
            });
        });
        formsmoke = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 6, 5f + e.fin() * 8f, (x, y) -> {
                Draw.color(Palette.plasticSmoke, Color.LIGHT_GRAY, e.fin());
                Fill.square(e.x + x, e.y + y, 0.2f + e.fout() * 2f, 45);
                Draw.reset();
            });
        });
        blastsmoke = new Effect(26, e -> {
            Angles.randLenVectors(e.id, 12, 1f + e.fin() * 23f, (x, y) -> {
                float size = 2f + e.fout() * 6f;
                Draw.color(Color.LIGHT_GRAY, Color.DARK_GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
                Draw.reset();
            });
        });
        lava = new Effect(18, e -> {
            Angles.randLenVectors(e.id, 3, 1f + e.fin() * 10f, (x, y) -> {
                float size = e.fslope() * 4f;
                Draw.color(Color.ORANGE, Color.GRAY, e.fin());
                Draw.rect("circle", e.x + x, e.y + y, size, size);
                Draw.reset();
            });
        });
        dooropen = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize / 2f + e.fin() * 2f);
            Draw.reset();
        });
        doorclose = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize / 2f + e.fout() * 2f);
            Draw.reset();
        });
        dooropenlarge = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize + e.fin() * 2f);
            Draw.reset();
        });
        doorcloselarge = new Effect(10, e -> {
            Lines.stroke(e.fout() * 1.6f);
            Lines.square(e.x, e.y, tilesize + e.fout() * 2f);
            Draw.reset();
        });
        purify = new Effect(10, e -> {
            Draw.color(Color.ROYAL, Color.GRAY, e.fin());
            Lines.stroke(2f);
            Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
            Draw.reset();
        });
        purifyoil = new Effect(10, e -> {
            Draw.color(Color.BLACK, Color.GRAY, e.fin());
            Lines.stroke(2f);
            Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
            Draw.reset();
        });
        purifystone = new Effect(10, e -> {
            Draw.color(Color.ORANGE, Color.GRAY, e.fin());
            Lines.stroke(2f);
            Lines.spikes(e.x, e.y, e.fin() * 4f, 2, 6);
            Draw.reset();
        });
        generate = new Effect(11, e -> {
            Draw.color(Color.ORANGE, Color.YELLOW, e.fin());
            Lines.stroke(1f);
            Lines.spikes(e.x, e.y, e.fin() * 5f, 2, 8);
            Draw.reset();
        });
        mine = new Effect(20, e -> {
            Angles.randLenVectors(e.id, 6, 3f + e.fin() * 6f, (x, y) -> {
                Draw.color(e.color, Color.LIGHT_GRAY, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f, 45);
                Draw.reset();
            });
        });
        mineBig = new Effect(30, e -> {
            Angles.randLenVectors(e.id, 6, 4f + e.fin() * 8f, (x, y) -> {
                Draw.color(e.color, Color.LIGHT_GRAY, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.2f, 45);
                Draw.reset();
            });
        });
        mineHuge = new Effect(40, e -> {
            Angles.randLenVectors(e.id, 8, 5f + e.fin() * 10f, (x, y) -> {
                Draw.color(e.color, Color.LIGHT_GRAY, e.fin());
                Fill.square(e.x + x, e.y + y, e.fout() * 2f + 0.5f, 45);
                Draw.reset();
            });
        });
        smelt = new Effect(20, e -> {
            Angles.randLenVectors(e.id, 6, 2f + e.fin() * 5f, (x, y) -> {
                Draw.color(Color.WHITE, e.color, e.fin());
                Fill.square(e.x + x, e.y + y, 0.5f + e.fout() * 2f, 45);
                Draw.reset();
            });
        });
        teleportActivate = new Effect(50, e -> {
            Draw.color(e.color);

            e.scaled(8f, e2 -> {
                Lines.stroke(e2.fout() * 4f);
                Lines.circle(e2.x, e2.y, 4f + e2.fin() * 27f);
            });

            Lines.stroke(e.fout() * 2f);

            Angles.randLenVectors(e.id, 30, 4f + 40f * e.fin(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fin() * 4f + 1f);
            });

            Draw.reset();
        });
        teleport = new Effect(60, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fin() * 2f);
            Lines.circle(e.x, e.y, 7f + e.fout() * 8f);

            Angles.randLenVectors(e.id, 20, 6f + 20f * e.fout(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fin() * 4f + 1f);
            });

            Draw.reset();
        });
        teleportOut = new Effect(20, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 2f);
            Lines.circle(e.x, e.y, 7f + e.fin() * 8f);

            Angles.randLenVectors(e.id, 20, 4f + 20f * e.fin(), (x, y) -> {
                Lines.lineAngle(e.x + x, e.y + y, Mathf.atan2(x, y), e.fslope() * 4f + 1f);
            });

            Draw.reset();
        });
        ripple = new GroundEffect(false, 30, e -> {
            Draw.color(Hue.shift(Tmp.c1.set(e.color), 2, 0.1f));
            Lines.stroke(e.fout() + 0.4f);
            Lines.circle(e.x, e.y, 2f + e.fin() * 4f);
            Draw.reset();
        });

        bubble = new Effect(20, e -> {
            Draw.color(Hue.shift(Tmp.c1.set(e.color), 2, 0.1f));
            Lines.stroke(e.fout() + 0.2f);
            Angles.randLenVectors(e.id, 2, 8f, (x, y) -> {
                Lines.circle(e.x + x, e.y + y, 1f + e.fin() * 3f);
            });
            Draw.reset();
        });

        commandSend = new Effect(28, e -> {
            Draw.color(Palette.command);
            Lines.stroke(e.fout() * 2f);
            Lines.poly(e.x, e.y, 40, 4f + e.finpow() * 120f);
            Draw.color();
        });

        healWaveMend = new Effect(40, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 2f);
            Lines.poly(e.x, e.y, 30, e.finpow() * e.rotation);
            Draw.color();
        });

        overdriveWave = new Effect(50, e -> {
            Draw.color(e.color);
            Lines.stroke(e.fout() * 1f);
            Lines.poly(e.x, e.y, 30, e.finpow() * e.rotation);
            Draw.color();
        });

        healBlock = new Effect(20, e -> {
            Draw.color(Palette.heal);
            Lines.stroke(2f * e.fout() + 0.5f);
            Lines.square(e.x, e.y, 1f + (e.fin() * e.rotation * tilesize/2f-1f));
            Draw.color();
        });

        healBlockFull = new Effect(20, e -> {
            Draw.color(e.color);
            Draw.alpha(e.fout());
            Fill.square(e.x, e.y, e.rotation * tilesize);
            Draw.color();
        });

        overdriveBlockFull = new Effect(60, e -> {
            Draw.color(e.color);
            Draw.alpha(e.fslope() * 0.4f);
            Fill.square(e.x, e.y, e.rotation * tilesize);
            Draw.color();
        });

        shieldBreak = new Effect(40, e -> {
            Draw.color(Palette.accent);
            Lines.stroke(3f * e.fout());
            Lines.poly(e.x, e.y, 6, e.rotation + e.fin(), 90);
            Draw.reset();
        });
    }
}
