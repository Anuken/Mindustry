package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class FusionReactor extends PowerGenerator{
    protected int plasmas = 4;
    protected float warmupSpeed = 0.001f;

    protected Color plasma1 = Color.valueOf("ffd06b"), plasma2 = Color.valueOf("ff361b");
    protected Color ind1 = Color.valueOf("858585"), ind2 = Color.valueOf("fea080");

    public FusionReactor(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        powerProduction = 2.0f;
        liquidCapacity = 30f;
        hasItems = true;
    }

    @Override
    public void update(Tile tile){
        FusionReactorEntity entity = tile.entity();

        float increaseOrDecrease = 1.0f;
        if(entity.cons.valid()){
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, warmupSpeed);
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.01f);
            increaseOrDecrease = -1.0f;
        }

        float efficiencyAdded = Mathf.pow(entity.warmup, 4f) * Timers.delta();
        entity.productionEfficiency = Mathf.clamp(entity.productionEfficiency + efficiencyAdded * increaseOrDecrease);

        tile.entity.power.graph.update();
    }

    @Override
    public float handleDamage(Tile tile, float amount){
        FusionReactorEntity entity = tile.entity();

        if(entity.warmup < 0.4f) return amount;

        float healthFract = tile.entity.health / health;

        //5% chance to explode when hit at <50% HP with a normal bullet
        if(amount > 5f && healthFract <= 0.5f && Mathf.chance(0.05)){
            return health;
            //10% chance to explode when hit at <25% HP with a powerful bullet
        }else if(amount > 8f && healthFract <= 0.2f && Mathf.chance(0.1)){
            return health;
        }

        return amount;
    }

    @Override
    public void draw(Tile tile){
        FusionReactorEntity entity = tile.entity();

        Draw.rect(name + "-bottom", tile.drawx(), tile.drawy());

        Graphics.setAdditiveBlending();

        for(int i = 0; i < plasmas; i++){
            float r = 29f + Mathf.absin(Timers.time(), 2f + i * 1f, 5f - i * 0.5f);

            Draw.color(plasma1, plasma2, (float) i / plasmas);
            Draw.alpha((0.3f + Mathf.absin(Timers.time(), 2f + i * 2f, 0.3f + i * 0.05f)) * entity.warmup);
            Draw.rect(name + "-plasma-" + i, tile.drawx(), tile.drawy(), r, r, Timers.time() * (12 + i * 6f) * entity.warmup);
        }

        Draw.color();

        Graphics.setNormalBlending();

        Draw.rect(region, tile.drawx(), tile.drawy());

        Draw.rect(name + "-top", tile.drawx(), tile.drawy());

        Draw.color(ind1, ind2, entity.warmup + Mathf.absin(entity.productionEfficiency, 3f, entity.warmup * 0.5f));
        Draw.rect(name + "-light", tile.drawx(), tile.drawy());

        Draw.color();
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name + "-bottom"), Draw.region(name), Draw.region(name + "-top")};
    }

    @Override
    public TileEntity newEntity(){
        return new FusionReactorEntity();
    }

    @Override
    public void onDestroyed(Tile tile){
        super.onDestroyed(tile);

        FusionReactorEntity entity = tile.entity();

        if(entity.warmup < 0.4f) return;

        //TODO catastrophic failure
    }

    public static class FusionReactorEntity extends GeneratorEntity{
        public float warmup;

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            super.read(stream);
            warmup = stream.readFloat();
        }
    }
}
