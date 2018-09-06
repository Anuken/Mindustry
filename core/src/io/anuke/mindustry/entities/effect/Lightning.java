package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.storage.CoreBlock.CoreEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.TimedEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.SeedRandom;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Lightning extends TimedEntity implements Poolable, DrawTrait, SyncTrait{
    private static Array<SolidTrait> entities = new Array<>();
    private static Rectangle rect = new Rectangle();
    private static Rectangle hitrect = new Rectangle();
    private static int lastSeed = 0;
    private static float angle;
    private static float wetDamageMultiplier = 2;

    private Array<Vector2> lines = new Array<>();
    private Color color = Palette.lancerLaser;
    private SeedRandom random = new SeedRandom();

    /**For pooling use only. Do not call directly!*/
    public Lightning(){
    }

    /**Create a lighting branch at a location. Use Team.none to damage everyone.*/
    public static void create(Team team, Effect effect, Color color, float damage, float x, float y, float targetAngle, int length){
        Call.createLighting(lastSeed++, team, effect, color, damage, x, y, targetAngle, length);
    }

    /**Do not invoke!*/
    @Remote(called = Loc.server)
    public static void createLighting(int seed, Team team, Effect effect, Color color, float damage, float x, float y, float targetAngle, int length){
        Lightning l = Pooling.obtain(Lightning.class);

        //TODO hacky workaround
        if(checkShield(team, x, y)) return;

        l.x = x;
        l.y = y;
        l.random.setSeed(seed);
        l.color = color;

        float step = 4f;
        float range = 6f;
        float attractRange = 20f;

        angle = targetAngle;
        entities.clear();

        Units.getNearbyEnemies(team, rect, entities::add);

        for(int i = 0; i < length; i++){
            l.lines.add(new Vector2(x, y));

            float fx = x, fy = y;
            float x2 = x + Angles.trnsx(angle, step);
            float y2 = y + Angles.trnsy(angle, step);
            if(checkShield(team, x2, y2)) break;
            float fangle = angle;

            angle += Mathf.range(15f);
            rect.setSize(attractRange).setCenter(x, y);

            Units.getNearbyEnemies(team, rect, entity -> {
                float dst = entity.distanceTo(x2, y2);
                if(dst < attractRange){
                    angle = Mathf.slerp(angle, Angles.angle(x2, y2, entity.x, entity.y), (attractRange - dst) / attractRange / 4f);
                }

                entity.getHitbox(hitrect);
                hitrect.x -= range / 2f;
                hitrect.y -= range / 2f;
                hitrect.width += range / 2f;
                hitrect.height += range / 2f;

                if(hitrect.contains(x2, y2) || hitrect.contains(fx, fy)){
                    float result = damage;

                    if(entity.hasEffect(StatusEffects.wet))
                        result = (result * wetDamageMultiplier);

                    entity.damage(result);
                    Effects.effect(effect, x2, y2, fangle);
                }
            });

            if(l.random.chance(0.1f)){
                createLighting(l.random.nextInt(), team, effect, color, damage, x2, y2, angle + l.random.range(30f), length / 3);
            }

            Tile tile = world.tileWorld(x, y);
            if(tile != null && tile.entity != null && tile.getTeamID() != team.ordinal()){
                Effects.effect(effect, x, y, fangle);
                tile.entity.damage(damage/4f);
            }

            x = x2;
            y = y2;
        }

        l.lines.add(new Vector2(x, y));
        l.add();
    }

    private static boolean checkShield(Team team, float x, float y){
        if(team != Team.none){
            for(Team enemy : state.teams.enemiesOf(team)) {
                for (Tile core : state.teams.get(enemy).cores) {
                    if(core.distanceTo(x, y) <= state.mode.enemyCoreShieldRadius){
                        core.<CoreEntity>entity().shieldHeat = 1f;
                        Effects.effect(BulletFx.absorb, x, y);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSyncing(){
        return false;
    }

    @Override
    public void write(DataOutput data) throws IOException{

    }

    @Override
    public void read(DataInput data, long time) throws IOException{

    }

    @Override
    public float lifetime(){
        return 10;
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
