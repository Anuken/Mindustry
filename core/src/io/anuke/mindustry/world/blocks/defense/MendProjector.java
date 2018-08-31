package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.UnitFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class MendProjector extends Block{
    private static Color color = Color.valueOf("84f491");
    private static Color phase = Color.valueOf("ffd59e");
    private static IntSet healed = new IntSet();

    protected int timerUse = timers ++;

    protected TextureRegion topRegion;
    protected float reload = 250f;
    protected float range = 50f;
    protected float healPercent = 6f;
    protected float phaseBoost = 10f;
    protected float useTime = 300f;

    public MendProjector(String name){
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
        MendEntity entity = tile.entity();
        entity.heat = Mathf.lerpDelta(entity.heat, entity.cons.valid() ? 1f : 0f, 0.08f);
        entity.charge += entity.heat * Timers.delta();

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, (float)entity.items.get(consumes.item()) / itemCapacity, 0.1f);

        if(entity.timer.get(timerUse, useTime) && entity.items.total() > 0){
            entity.items.remove(consumes.item(), 1);
        }

        if(entity.charge >= reload){
            float realRange = range + entity.phaseHeat * 20f;

            Effects.effect(UnitFx.healWaveMend, Hue.mix(color, phase, entity.phaseHeat), tile.drawx(), tile.drawy(), realRange);
            entity.charge = 0f;

            Timers.run(10f, () -> {
                int tileRange = (int)(realRange / tilesize);
                healed.clear();

                for(int x = -tileRange + tile.x; x <= tileRange + tile.x; x++){
                    for(int y = -tileRange + tile.y; y <= tileRange + tile.y; y++){
                        if(Vector2.dst(x, y, tile.x, tile.y) > realRange) continue;

                        Tile other = world.tile(x, y);

                        if(other == null) continue;
                        other = other.target();

                        if(other.getTeamID() == tile.getTeamID() && !healed.contains(other.packedPosition()) && other.entity != null && other.entity.health < other.entity.maxHealth()){
                            other.entity.healBy(other.entity.maxHealth() * (healPercent + entity.phaseHeat*phaseBoost)/100f);
                            Effects.effect(BlockFx.healBlockFull, Hue.mix(color, phase, entity.phaseHeat), other.drawx(), other.drawy(), other.block().size);
                            healed.add(other.packedPosition());
                        }
                    }
                }
            });
        }
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(color);
        Lines.dashCircle(tile.drawx(), tile.drawy() - 1f, range);
        Draw.color();
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        MendEntity entity = tile.entity();
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
        return new MendEntity();
    }

    class MendEntity extends TileEntity{
        float heat;
        float charge;
        float phaseHeat;
    }
}
