package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.TimedEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.PosTrait;
import io.anuke.ucore.entities.trait.TimeTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInput;
import java.io.DataOutput;

import static io.anuke.mindustry.Vars.bulletGroup;

public class Lightning extends TimedEntity implements DrawTrait, SyncTrait, TimeTrait{
    public static final float lifetime = 10f;

    private static final SeedRandom random = new SeedRandom();
    private static final Rectangle rect = new Rectangle();
    private static final Array<Unit> entities = new Array<>();
    private static final IntSet hit = new IntSet();
    private static final int maxChain = 8;
    private static final float hitRange = 30f;
    private static int lastSeed = 0;

    private Array<PosTrait> lines = new Array<>();
    private Color color = Palette.lancerLaser;

    /**For pooling use only. Do not call directly!*/
    public Lightning(){
    }

    /**Create a lighting branch at a location. Use Team.none to damage everyone.*/
    public static void create(Team team, Color color, float damage, float x, float y, float targetAngle, int length){
        Call.createLighting(lastSeed++, team, color, damage, x, y, targetAngle, length);
    }

    /**Do not invoke!*/
    @Remote(called = Loc.server)
    public static void createLighting(int seed, Team team, Color color, float damage, float x, float y, float rotation, int length){

        Lightning l = Pooling.obtain(Lightning.class, Lightning::new);
        Float dmg = damage;

        l.x = x;
        l.y = y;
        l.color = color;
        l.add();

        random.setSeed(seed);
        hit.clear();

        for (int i = 0; i < length/2; i++) {
            Bullet.create(TurretBullets.damageLightning, l, team, x, y, 0f, 1f, 1f, dmg);
            l.lines.add(new Translator(x + Mathf.range(3f), y + Mathf.range(3f)));

            rect.setSize(hitRange).setCenter(x, y);
            entities.clear();
            if(hit.size < maxChain){
                Units.getNearbyEnemies(team, rect, u -> {
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
                x += Angles.trnsx(rotation, hitRange/2f);
                y += Angles.trnsy(rotation, hitRange/2f);
            }
        }
    }

    @Override
    public boolean isSyncing(){
        return false;
    }

    @Override
    public void write(DataOutput data){}

    @Override
    public void read(DataInput data, long time){}

    @Override
    public float lifetime(){
        return lifetime;
    }

    @Override
    public void reset(){
        super.reset();
        color = Palette.lancerLaser;
        lines.clear();
    }

    @Override
    public void removed(){
        super.removed();
        Pooling.free(this);
    }

    @Override
    public void draw(){
        float lx = x, ly = y;
        Draw.color(color, Color.WHITE, fin());
        for(int i = 0; i < lines.size; i++){
            PosTrait v = lines.get(i);

            float f = (float) i / lines.size;

            Lines.stroke(fout() * 3f * (1.5f - f));

            Lines.stroke(Lines.getStroke() * 4f);
            Draw.alpha(0.3f);
            Lines.line(lx, ly, v.getX(), v.getY());

            Lines.stroke(Lines.getStroke()/4f);
            Draw.alpha(1f);
            Lines.line(lx, ly, v.getX(), v.getY());

            Lines.stroke(3f * fout() * (1f - f));
           // Lines.lineAngleCenter(lx, ly, Angles.angle(lx, ly, v.getX(), v.getY()) + 90f, 20f);

            lx = v.getX();
            ly = v.getY();
        }
        Draw.color();
    }

    @Override
    public EntityGroup targetGroup(){
        return bulletGroup;
    }
}
