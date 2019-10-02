package io.anuke.mindustry.world.blocks;

import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.net;

public class RespawnBlock{
    
    public static void drawRespawn(Tile tile, float heat, float progress, float time, Player player, Mech to){
        progress = Mathf.clamp(progress);

        Draw.color(Pal.darkMetal);
        Lines.stroke(2f * heat);
        Fill.poly(tile.drawx(), tile.drawy(), 4, 10f * heat);

        Draw.reset();
        if(player != null){
            TextureRegion region = player.getIconRegion();

            Draw.color(0f, 0f, 0f, 0.4f * progress);
            Draw.rect("circle-shadow", tile.drawx(), tile.drawy(), region.getWidth() / 3f, region.getWidth() / 3f);
            Draw.color();

            Shaders.build.region = region;
            Shaders.build.progress = progress;
            Shaders.build.color.set(Pal.accent);
            Shaders.build.time = -time / 10f;

            Draw.shader(Shaders.build, true);
            Draw.rect(region, tile.drawx(), tile.drawy());
            Draw.shader();

            Draw.color(Pal.accentBack);

            float pos = Mathf.sin(time, 6f, 8f);

            Lines.lineAngleCenter(tile.drawx() + pos, tile.drawy(), 90, 16f - Math.abs(pos) * 2f);

            Draw.reset();
        }

        Lines.stroke(2f * heat);

        Draw.color(Pal.accentBack);
        Lines.poly(tile.drawx(), tile.drawy(), 4, 8f * heat);

        float oy = -7f, len = 6f * heat;
        Lines.stroke(5f);
        Draw.color(Pal.darkMetal);
        Lines.line(tile.drawx() - len, tile.drawy() + oy, tile.drawx() + len, tile.drawy() + oy, CapStyle.none);
        for(int i : Mathf.signs){
            Fill.tri(tile.drawx() + len * i, tile.drawy() + oy - Lines.getStroke()/2f, tile.drawx() + len * i, tile.drawy() + oy + Lines.getStroke()/2f, tile.drawx() + (len + Lines.getStroke() * heat) * i, tile.drawy() + oy);
        }

        Lines.stroke(3f);
        Draw.color(Pal.accent);
        Lines.line(tile.drawx() - len, tile.drawy() + oy, tile.drawx() - len + len*2 * progress, tile.drawy() + oy, CapStyle.none);
        for(int i : Mathf.signs){
            Fill.tri(tile.drawx() + len * i, tile.drawy() + oy - Lines.getStroke()/2f, tile.drawx() + len * i, tile.drawy() + oy + Lines.getStroke()/2f, tile.drawx() + (len + Lines.getStroke() * heat) * i, tile.drawy() + oy);
        }
        Draw.reset();

        if(net.active() && player != null){
            tile.block().drawPlaceText(player.name, tile.x, tile.y - (Math.max((tile.block().size-1)/2, 0)), true);
        }
    }
}
