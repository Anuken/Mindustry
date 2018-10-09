package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.TimedEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.PosTrait;
import io.anuke.ucore.entities.trait.TimeTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Pooling;
import io.anuke.ucore.util.SeedRandom;

import java.io.DataInput;
import java.io.DataOutput;

import static io.anuke.mindustry.Vars.bulletGroup;

public class Lightning extends TimedEntity implements DrawTrait, SyncTrait, TimeTrait{
    private static int lastSeed = 0;
    private static SeedRandom random = new SeedRandom();

    private Array<PosTrait> lines = new Array<>();
    private Color color = Palette.lancerLaser;

    /**For pooling use only. Do not call directly!*/
    public Lightning(){
    }

    /**Create a lighting branch at a location. Use Team.none to damage everyone.*/
    public static void create(Team team, Effect effect, Color color, float damage, float x, float y, float targetAngle, int length){
        Call.createLighting(lastSeed++, team, effect, color, damage, x, y, targetAngle, length);
    }

    /**Do not invoke!*/
    @Remote(called = Loc.server)
    public static void createLighting(int seed, Team team, Color color, float damage, float x, float y, int length){
        Lightning l = Pooling.obtain(Lightning.class, Lightning::new);

        l.x = x;
        l.y = y;
        l.color = color;
        l.add();

        for (int i = 0; i < length; i++) {
            Effect
        }

        random.setSeed(seed);
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
    public void reset(){
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

            Lines.stroke(Lines.getStroke() * 4f);
            Draw.alpha(0.3f);
            Lines.line(lx, ly, v.x, v.y);

            Lines.stroke(Lines.getStroke()/4f);
            Draw.alpha(1f);
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
