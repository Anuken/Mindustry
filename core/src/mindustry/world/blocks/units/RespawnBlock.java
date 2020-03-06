package mindustry.world.blocks.units;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.net;

//TODO remove ?
public class RespawnBlock{
    
    public static void drawRespawn(float heat, float progress, float time, Playerc player, UnitType to){
        progress = Mathf.clamp(progress);

        Draw.color(Pal.darkMetal);
        Lines.stroke(2f * heat);
        Fill.poly(x, y, 4, 10f * heat);

        Draw.reset();
        if(player != null){
            TextureRegion region = to.icon(Cicon.full);

            Draw.color(0f, 0f, 0f, 0.4f * progress);
            Draw.rect("circle-shadow", x, y, region.getWidth() / 3f, region.getWidth() / 3f);
            Draw.color();

            Shaders.build.region = region;
            Shaders.build.progress = progress;
            Shaders.build.color.set(Pal.accent);
            Shaders.build.time = -time / 10f;

            Draw.shader(Shaders.build, true);
            Draw.rect(region, x, y);
            Draw.shader();

            Draw.color(Pal.accentBack);

            float pos = Mathf.sin(time, 6f, 8f);

            Lines.lineAngleCenter(x + pos, y, 90, 16f - Math.abs(pos) * 2f);

            Draw.reset();
        }

        Lines.stroke(2f * heat);

        Draw.color(Pal.accentBack);
        Lines.poly(x, y, 4, 8f * heat);

        float oy = -7f, len = 6f * heat;
        Lines.stroke(5f);
        Draw.color(Pal.darkMetal);
        Lines.line(x - len, y + oy, x + len, y + oy, CapStyle.none);
        for(int i : Mathf.signs){
            Fill.tri(x + len * i, y + oy - Lines.getStroke()/2f, x + len * i, y + oy + Lines.getStroke()/2f, x + (len + Lines.getStroke() * heat) * i, y + oy);
        }

        Lines.stroke(3f);
        Draw.color(Pal.accent);
        Lines.line(x - len, y + oy, x - len + len*2 * progress, y + oy, CapStyle.none);
        for(int i : Mathf.signs){
            Fill.tri(x + len * i, y + oy - Lines.getStroke()/2f, x + len * i, y + oy + Lines.getStroke()/2f, x + (len + Lines.getStroke() * heat) * i, y + oy);
        }
        Draw.reset();

        if(net.active() && player != null){
            tile.drawPlaceText(player.name(), tile.x, tile.y - (Math.max((tile.block().size-1)/2, 0)), true);
        }
    }
}
