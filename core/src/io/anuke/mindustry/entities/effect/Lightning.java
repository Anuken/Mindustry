package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.AbsorbTrait;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TeamTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.SolidEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.entities.trait.TimeTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.SeedRandom;

import java.io.DataInput;
import java.io.DataOutput;

import static io.anuke.mindustry.Vars.bulletGroup;
import static io.anuke.mindustry.Vars.world;

public class Lightning extends SolidEntity implements Poolable, DrawTrait, SyncTrait, AbsorbTrait, TeamTrait, TimeTrait{
    private static final Array<SolidTrait> entities = new Array<>();
    private static final Rectangle rect = new Rectangle();
    private static final Rectangle hitrect = new Rectangle();
    private static final float wetDamageMultiplier = 2;
    private static final float step = 4f, range = 6f, attractRange = 20f;

    private static int lastSeed = 0;
    private static float angle;

    private Array<Vector2> lines = new Array<>();
    private Color color = Palette.lancerLaser;
    private Lightning parent;
    private SeedRandom random = new SeedRandom();
    private float damage, time;
    private int activeFrame;
    private Effect effect;
    private Team team;

    /**For pooling use only. Do not call directly!*/
    public Lightning(){
    }

    /**Create a lighting branch at a location. Use Team.none to damage everyone.*/
    public static void create(Team team, Effect effect, Color color, float damage, float x, float y, float targetAngle, int length){
        Call.createLighting(lastSeed++, team, effect, color, damage, x, y, targetAngle, length);
    }

    /**Do not invoke!*/
    @Remote(called = Loc.server)
    public static Lightning createLighting(int seed, Team team, Effect effect, Color color, float damage, float x, float y, float targetAngle, int length){
        Lightning l = Pooling.obtain(Lightning.class, Lightning::new);

        l.x = x;
        l.y = y;
        l.damage = damage;
        l.effect = effect;
        l.team = team;
        l.random.setSeed(seed);
        l.color = color;

        angle = targetAngle;
        entities.clear();

        Units.getNearbyEnemies(team, rect, entities::add);

        for(int i = 0; i < length; i++){
            l.lines.add(new Vector2(x, y));

            float x2 = x + Angles.trnsx(angle, step);
            float y2 = y + Angles.trnsy(angle, step);

            angle += Mathf.range(15f);
            rect.setSize(attractRange).setCenter(x, y);

            Units.getNearbyEnemies(team, rect, entity -> {
                float dst = entity.distanceTo(x2, y2);
                if(dst < attractRange){
                    angle = Mathf.slerp(angle, Angles.angle(x2, y2, entity.x, entity.y), (attractRange - dst) / attractRange / 4f);
                }
            });

            if(l.random.chance(0.1f)){
                createLighting(l.random.nextInt(), team, effect, color, damage, x2, y2, angle + l.random.range(30f), length / 3)
                .parent = l;
            }

            x = x2;
            y = y2;
        }

        l.lines.add(new Vector2(x, y));
        l.add();

        return l;
    }



    @Override
    public void getHitbox(Rectangle rectangle){}

    @Override
    public void getHitboxTile(Rectangle rectangle){}

    @Override
    public void absorb(){
        activeFrame = 99;
        if(parent != null){
            parent.absorb();
        }
    }

    @Override
    public boolean collides(SolidTrait other){
        return false;
    }

    @Override
    public void time(float time){
        this.time = time;
    }

    @Override
    public boolean canBeAbsorbed(){
        return activeFrame < 3;
    }

    @Override
    public float time(){
        return time;
    }

    @Override
    public float fin(){
        return time/lifetime();
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
        return 10;
    }

    @Override
    public Team getTeam(){
        return team;
    }

    @Override
    public void update(){
        updateTime();

        if(activeFrame == 2){
            for(Vector2 vec : lines){
                rect.setSize(range).setCenter(x, y);

                Units.getNearbyEnemies(team, rect, unit -> {
                    unit.getHitbox(hitrect);
                    if(rect.overlaps(hitrect)){
                        unit.damage(damage * (unit.hasEffect(StatusEffects.wet) ? wetDamageMultiplier : 1f));
                        Effects.effect(effect, vec.x, vec.y, 0f);
                    }
                });

                Tile tile = world.tileWorld(vec.x, vec.y);
                if(tile != null && tile.entity != null && tile.getTeamID() != team.ordinal()){
                    Effects.effect(effect, vec.x, vec.y, 0f);
                    tile.entity.damage(damage/4f);
                }
            }
        }

        activeFrame ++;
    }

    @Override
    public void reset(){
        time = 0f;
        color = Palette.lancerLaser;
        lines.clear();
        parent = null;
        activeFrame = 0;
    }

    @Override
    public void removed(){
        super.removed();
        Pooling.free(this);
    }

    @Override
    public float getDamage(){
        return damage/10f;
    }

    @Override
    public void draw(){
        float lx = x, ly = y;
        Draw.color(color, Color.WHITE, fin());
        for(int i = 0; i < lines.size; i++){
            Vector2 v = lines.get(i);
            Lines.stroke(fout() * 3f * (1.5f - (float) i / lines.size));
            Lines.line(lx, ly, v.x, v.y);
            lx = v.x;
            ly = v.y;
        }
        Draw.color();
    }

    @Override
    public EntityGroup targetGroup(){
        return bulletGroup;
    }
}
