package mindustry.entities;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

//TODO move into a different class
public class Lightning{
    private static final Rand random = new Rand();
    private static final Rect rect = new Rect();
    private static final Array<Unitc> entities = new Array<>();
    private static final IntSet hit = new IntSet();
    private static final int maxChain = 8;
    private static final float hitRange = 30f;
    private static boolean bhit = false;
    private static int lastSeed = 0;

    /** Create a lighting branch at a location. Use Team.none to damage everyone. */
    public static void create(Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        createLightingInternal(lastSeed++, team, color, damage, x, y, targetAngle, length);
    }

    //TODO remote method
    //@Remote(called = Loc.server, unreliable = true)
    private static void createLightingInternal(int seed, Team team, Color color, float damage, float x, float y, float rotation, int length){
        random.setSeed(seed);
        hit.clear();

        Array<Vec2> lines = new Array<>();
        bhit = false;

        for(int i = 0; i < length / 2; i++){
            Bullets.damageLightning.create(null, team, x, y, 0f, damage, 1f, 1f, null);
            lines.add(new Vec2(x + Mathf.range(3f), y + Mathf.range(3f)));

            if(lines.size > 1){
                bhit = false;
                Vec2 from = lines.get(lines.size - 2);
                Vec2 to = lines.get(lines.size - 1);
                world.raycastEach(world.toTile(from.getX()), world.toTile(from.getY()), world.toTile(to.getX()), world.toTile(to.getY()), (wx, wy) -> {

                    Tile tile = world.tile(wx, wy);
                    if(tile != null && tile.block().insulated){
                        bhit = true;
                        //snap it instead of removing
                        lines.get(lines.size -1).set(wx * tilesize, wy * tilesize);
                        return true;
                    }
                    return false;
                });
                if(bhit) break;
            }

            rect.setSize(hitRange).setCenter(x, y);
            entities.clear();
            if(hit.size < maxChain){
                Units.nearbyEnemies(team, rect, u -> {
                    if(!hit.contains(u.id())){
                        entities.add(u);
                    }
                });
            }

            Unitc furthest = Geometry.findFurthest(x, y, entities);

            if(furthest != null){
                hit.add(furthest.id());
                x = furthest.x();
                y = furthest.y();
            }else{
                rotation += random.range(20f);
                x += Angles.trnsx(rotation, hitRange / 2f);
                y += Angles.trnsy(rotation, hitRange / 2f);
            }
        }

        Fx.lightning.at(x, y, rotation, color, lines);
    }
}
