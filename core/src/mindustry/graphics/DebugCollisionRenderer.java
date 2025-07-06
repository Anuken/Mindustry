package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.world.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class DebugCollisionRenderer{

    public static void draw(){
        Rect rect = camera.bounds(new Rect());
        Draw.draw(Layer.overlayUI, () -> {
            //hitboxes
            Draw.color(Color.green, 0.3f);
            Groups.draw.each(d -> {
                if(d instanceof Hitboxc h && rect.overlaps(Tmp.r1.setCentered(d.x(), d.y(), d.clipSize()))){
                    Fill.square(d.x(), d.y(), h.hitSize()/2f);
                }
            });

            //tile hitboxes for units
            Lines.stroke(0.3f, Color.magenta);

            int rx = Mathf.clamp((int)(Core.camera.width / tilesize / 2) + 1, 0, world.width()/2);
            int ry = Mathf.clamp((int)(Core.camera.height / tilesize / 2) + 1, 0, world.height()/2);

            for(int x = -rx; x <= rx; x++){
                for(int y = -ry; y <= ry; y++){
                    int wx = World.toTile(Core.camera.position.x) + x;
                    int wy = World.toTile(Core.camera.position.y) + y;
                    Tile tile = world.tile(wx, wy);
                    if(tile != null && tile.solid()){
                        Draw.color(tile.legSolid() ? Color.pink : Color.magenta);
                        Lines.rect(wx * tilesize - tilesize/2f, wy * tilesize - tilesize/2f, tilesize, tilesize);
                    }
                }
            }


            Groups.draw.each(d -> {
                if(d instanceof Unit u && rect.overlaps(Tmp.r1.setCentered(u.x, u.y, d.clipSize()))){
                    u.hitboxTile(Tmp.r1);

                    Lines.rect(Tmp.r1);
                }
            });

            Lines.stroke(0.5f);
            //physics
            Draw.color(Color.red, 0.5f);
            Groups.draw.each(d -> {
                if(d instanceof Unit u && rect.overlaps(Tmp.r1.setCentered(u.x, u.y, u.clipSize()))){
                    Lines.circle(u.x, u.y, u.hitSize * unitCollisionRadiusScale);
                }
            });
            Draw.reset();

        });


        Draw.reset();
    }
}
