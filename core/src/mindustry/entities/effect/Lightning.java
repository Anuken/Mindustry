package mindustry.entities.effect;

import mindustry.annotations.Annotations.Loc;
import mindustry.annotations.Annotations.Remote;
import arc.struct.Array;
import arc.struct.IntSet;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.pooling.Pools;
import mindustry.content.Bullets;
import mindustry.entities.EntityGroup;
import mindustry.entities.Units;
import mindustry.entities.type.Bullet;
import mindustry.entities.type.TimedEntity;
import mindustry.entities.traits.DrawTrait;
import mindustry.entities.traits.TimeTrait;
import mindustry.entities.type.Unit;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.graphics.Pal;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class Lightning extends TimedEntity implements DrawTrait, TimeTrait{
    public static final float lifetime = 10f;

    private static final Rand random = new Rand();
    private static final Rect rect = new Rect();
    private static final Array<Unit> entities = new Array<>();
    private static final IntSet hit = new IntSet();
    private static final int maxChain = 8;
    private static final float hitRange = 30f;
    private static int lastSeed = 0;

    private Array<Vec2> lines = new Array<>();
    private Color color = Pal.lancerLaser;

    /** For pooling use only. Do not call directly! */
    public Lightning(){
    }

    /** Create a lighting branch at a location. Use Team.none to damage everyone. */
    public static void create(Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        Call.createLighting(nextSeed(), team, color, damage, x, y, targetAngle, length);
    }

    public static int nextSeed(){
        return lastSeed++;
    }

    /** Do not invoke! */
    @Remote(called = Loc.server, unreliable = true)
    public static void createLighting(int seed, Team team, Color color, float damage, float x, float y, float rotation, int length){

        Lightning l = Pools.obtain(Lightning.class, Lightning::new);
        Float dmg = damage;

        l.x = x;
        l.y = y;
        l.color = color;
        l.add();

        random.setSeed(seed);
        hit.clear();

        boolean[] bhit = {false};

        for(int i = 0; i < length / 2; i++){
            Bullet.create(Bullets.damageLightning, l, team, x, y, 0f, 1f, 1f, dmg);
            l.lines.add(new Vec2(x + Mathf.range(3f), y + Mathf.range(3f)));

            if(l.lines.size > 1){
                bhit[0] = false;
                Position from = l.lines.get(l.lines.size - 2);
                Position to   = l.lines.get(l.lines.size - 1);
                world.raycastEach(world.toTile(from.getX()), world.toTile(from.getY()), world.toTile(to.getX()), world.toTile(to.getY()), (wx, wy) -> {

                    Tile tile = world.ltile(wx, wy);
                    if(tile != null && tile.block().insulated){
                        bhit[0] = true;
                        //snap it instead of removing
                        l.lines.get(l.lines.size -1).set(wx * tilesize, wy * tilesize);
                        return true;
                    }
                    return false;
                });
                if(bhit[0]) break;
            }

            rect.setSize(hitRange).setCenter(x, y);
            entities.clear();
            if(hit.size < maxChain){
                Units.nearbyEnemies(team, rect, u -> {
                    if(!hit.contains(u.getID())){
                        entities.add(u);
                    }
                });
            }

            Unit furthest = Geometry.findFurthest(x, y, entities);

            if(furthest != null){
                hit.add(furthest.getID());
                x = furthest.x;
                y = furthest.y;
            }else{
                rotation += random.range(20f);

                x += Angles.trnsx(rotation, hitRange / 2f);
                y += Angles.trnsy(rotation, hitRange / 2f);
            }
        }
    }

    @Override
    public float lifetime(){
        return lifetime;
    }

    @Override
    public void reset(){
        super.reset();
        color = Pal.lancerLaser;
        lines.clear();
    }

    @Override
    public void removed(){
        super.removed();
        Pools.free(this);
    }

    @Override
    public void draw(){
        Lines.stroke(3f * fout());
        Draw.color(color, Color.white, fin());
        Lines.beginLine();

        Lines.linePoint(x, y);
        for(Position p : lines){
            Lines.linePoint(p.getX(), p.getY());
        }
        Lines.endLine();

        int i = 0;

        for(Position p : lines){
            Fill.square(p.getX(), p.getY(), (5f - (float)i++ / lines.size * 2f) * fout(), 45);
        }
        Draw.reset();
    }

    @Override
    public EntityGroup targetGroup(){
        return bulletGroup;
    }
}
