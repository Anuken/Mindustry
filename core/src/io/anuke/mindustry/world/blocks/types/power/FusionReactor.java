package io.anuke.mindustry.world.blocks.types.power;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.GenericCrafter.GenericCrafterEntity;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class FusionReactor extends PowerGenerator {
    protected int plasmas = 4;
    protected float powerUsage = 0.5f;
    protected float maxPowerProduced = 1.5f;
    protected float liquidUsage = 1f;
    protected Liquid inputLiquid = Liquids.water;
    protected float warmupSpeed = 0.001f;

    public FusionReactor(String name) {
        super(name);
        hasPower = true;
        hasLiquids = true;
        powerCapacity = 100f;
        liquidCapacity = 30f;
        hasInventory = true;
    }

    @Override
    public void update(Tile tile){
        FusionReactorEntity entity = tile.entity();

        float powerUse = Math.min(powerCapacity, powerUsage * Timers.delta());
        float liquidUse = Math.min(liquidCapacity, liquidUsage * Timers.delta());

        if(entity.power.amount >= powerUse && entity.liquid.amount >= liquidUse){
            entity.power.amount -= powerUse;
            entity.liquid.amount -= liquidUse;
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, warmupSpeed);
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.01f);
        }

        float powerAdded = Math.min(powerCapacity - entity.power.amount, maxPowerProduced * Mathf.pow(entity.warmup, 3f) * Timers.delta());
        entity.power.amount += powerAdded;
        entity.totalProgress += entity.warmup * Timers.delta();

        distributePower(tile);
    }

    @Override
    public void draw(Tile tile) {
        FusionReactorEntity entity = tile.entity();

        Draw.rect(name + "-bottom", tile.drawx(), tile.drawy());

        Graphics.setAdditiveBlending();

        for(int i = 0; i < plasmas; i ++){
            float r = 29f + Mathf.absin(Timers.time(), 2f + i*1f, 5f - i*0.5f);

            Draw.color(Color.valueOf("ffd06b"), Color.valueOf("ff361b"), (float)i/plasmas);
            Draw.alpha((0.3f + Mathf.absin(Timers.time(), 2f+i*2f, 0.3f+i*0.05f)) * entity.warmup);
            Draw.rect(name + "-plasma-" + i, tile.drawx(), tile.drawy(), r, r, Timers.time()*(12+i*6f) * entity.warmup);
        }

        Draw.color();

        Graphics.setNormalBlending();

        Draw.rect(name, tile.drawx(), tile.drawy());

        Draw.rect(name + "-top", tile.drawx(), tile.drawy());

        Draw.color(Color.valueOf("858585"), Color.valueOf("fea080"), entity.warmup + Mathf.absin(entity.totalProgress, 3f, entity.warmup*0.5f));
        Draw.rect(name + "-light", tile.drawx(), tile.drawy());

        Draw.color();
    }

    @Override
    public TextureRegion[] getIcon() {
        return new TextureRegion[]{Draw.region(name + "-bottom"), Draw.region(name), Draw.region(name + "-top")};
    }

    @Override
    public TileEntity getEntity() {
        return new FusionReactorEntity();
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
        return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
    }

    public static class FusionReactorEntity extends GenericCrafterEntity{

    }
}
