package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class ExtendingItemBridge extends ItemBridge{

    public ExtendingItemBridge(String name){
        super(name);
        hasItems = true;
    }
    
    public class ExtendingItemBridgeEntity extends ItemBridgeEntity{
        @Override
        public void draw(){
            Draw.rect(region, x, y);

            Draw.z(Layer.power);

            Tile other = world.tile(link);
            if(!linkValid(tile, other)) return;

            int i = tile.absoluteRelativeTo(other.x, other.y);

            float ex = other.worldx() - x - Geometry.d4[i].x * tilesize / 2f,
                ey = other.worldy() - y - Geometry.d4[i].y * tilesize / 2f;

            float uptime = state.isEditor() ? 1f : this.uptime;

            ex *= uptime;
            ey *= uptime;

            float opacity = Core.settings.getInt("bridgeopacity") / 100f;
            if(Mathf.zero(opacity)) return;
            Draw.alpha(opacity);

            Lines.stroke(8f);
            Lines.line(bridgeRegion,
            x + Geometry.d4[i].x * tilesize / 2f,
            y + Geometry.d4[i].y * tilesize / 2f,
            x + ex,
            y + ey, CapStyle.none, 0f);

            Draw.rect(endRegion, x, y, i * 90 + 90);
            Draw.rect(endRegion,
            x + ex + Geometry.d4[i].x * tilesize / 2f,
            y + ey + Geometry.d4[i].y * tilesize / 2f, i * 90 + 270);

            int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

            int arrows = (dist) * tilesize / 6 - 1;

            Draw.color();

            for(int a = 0; a < arrows; a++){
                Draw.alpha(Mathf.absin(a / (float)arrows - time / 100f, 0.1f, 1f) * uptime * opacity);
                Draw.rect(arrowRegion,
                x + Geometry.d4[i].x * (tilesize / 2f + a * 6f + 2) * uptime,
                y + Geometry.d4[i].y * (tilesize / 2f + a * 6f + 2) * uptime,
                    i * 90f);
            }
            Draw.reset();
        }
    }
}
