package io.anuke.mindustry.entities.effect;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.pooling.Pools;
import io.anuke.mindustry.content.Bullets;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.type.TimedEntity;
import io.anuke.mindustry.entities.traits.DrawTrait;
import io.anuke.mindustry.entities.traits.TimeTrait;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Pal;

import static io.anuke.mindustry.Vars.bulletGroup;

public class Lightning extends TimedEntity implements DrawTrait, TimeTrait{
    public static final float lifetime = 10f;

    private static final RandomXS128 random = new RandomXS128();
    private static final Rectangle rect = new Rectangle();
    private static final Array<Unit> entities = new Array<>();
    private static final IntSet hit = new IntSet();
    private static final int maxChain = 8;
    private static final float hitRange = 30f;
    private static int lastSeed = 0;

    private Array<Position> lines = new Array<>();
    private Color color = Pal.lancerLaser;

    /** For pooling use only. Do not call directly! */
    public Lightning(){
    }

    /** Create a lighting branch at a location. Use Team.none to damage everyone. */
    public static void create(Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        Call.createLighting(lastSeed++, team, color, damage, x, y, targetAngle, length);
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

        for(int i = 0; i < length / 2; i++){
            Bullet.create(Bullets.damageLightning, l, team, x, y, 0f, 1f, 1f, dmg);
            l.lines.add(new Vector2(x + Mathf.range(3f), y + Mathf.range(3f)));

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
