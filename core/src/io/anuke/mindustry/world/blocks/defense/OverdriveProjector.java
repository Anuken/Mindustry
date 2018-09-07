package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

public class OverdriveProjector extends Block{
    private static Color color = Color.valueOf("feb380");
    private static Color phase = Color.valueOf("ffd59e");

    protected int timerUse = timers ++;
    protected int timerApply = timers ++;

    protected TextureRegion topRegion;
    protected float reload = 60f;
    protected float range = 100f;
    protected float phaseBoost = 30f;
    protected float useTime = 300f;

    public OverdriveProjector(String name){
        super(name);
        solid = true;
        update = true;
        hasPower = true;
        hasItems = true;
        itemCapacity = 10;
    }

    @Override
    public void load(){
        super.load();
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void update(Tile tile){
        OverdriveEntity entity = tile.entity();

        entity.heat = Mathf.lerpDelta(entity.heat, entity.cons.valid() ? 1f : 0f, 0.08f);
        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, (float)entity.items.get(consumes.item()) / itemCapacity, 0.1f);

        if(entity.timer.get(timerUse, useTime) && entity.items.total() > 0){
            entity.items.remove(consumes.item(), 1);
        }

        if(entity.heat > 0.5f && entity.timer.get(timerApply, 10)){
            float realRange = range + entity.phaseHeat * phaseBoost;

            Units.getNearby(tile.getTeam(), tile.drawx(), tile.drawy(), realRange, unit -> unit.applyEffect(StatusEffects.overdrive, 1f + entity.phaseHeat));
        }
    }

    @Override
    public void drawSelect(Tile tile){
        OverdriveEntity entity = tile.entity();
        float realRange = range + entity.phaseHeat * phaseBoost;

        Draw.color(color);
        Lines.poly(tile.drawx(), tile.drawy(), 300, realRange);
        Draw.color();
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        OverdriveEntity entity = tile.entity();
        float f = 1f - (Timers.time() / 100f) % 1f;

        Draw.color(color, phase, entity.phaseHeat);
        Draw.alpha(entity.heat * Mathf.absin(Timers.time(), 10f, 1f) * 0.5f);
        Graphics.setAdditiveBlending();
        Draw.rect(topRegion, tile.drawx(), tile.drawy());

        Graphics.setNormalBlending();
        Draw.alpha(1f);
        Lines.stroke((2f  * f + 0.2f)* entity.heat);
        Lines.circle(tile.drawx(), tile.drawy(), (1f-f) * 9f);

        Draw.reset();
    }

    @Override
    public TileEntity getEntity(){
        return new OverdriveEntity();
    }

    class OverdriveEntity extends TileEntity{
        float heat;
        float phaseHeat;
    }
}