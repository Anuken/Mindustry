package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

public class Incinerator extends Block{
    protected Effect effect = Fx.fuelburn;
    protected Color flameColor = Color.valueOf("ffad9d");

    public Incinerator(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        update = true;
        solid = true;
    }

    @Override
    public void update(Tile tile){
        IncineratorEntity entity = tile.entity();

        if(entity.cons.valid()){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.04f);
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.02f);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        IncineratorEntity entity = tile.entity();

        if(entity.heat > 0f){
            float g = 0.3f;
            float r = 0.06f;

            Draw.alpha(((1f - g) + Mathf.absin(Time.time(), 8f, g) + Mathf.random(r) - r) * entity.heat);

            Draw.tint(flameColor);
            Fill.circle(tile.drawx(), tile.drawy(), 2f);
            Draw.color(1f, 1f, 1f, entity.heat);
            Fill.circle(tile.drawx(), tile.drawy(), 1f);

            Draw.color();
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(Mathf.chance(0.3)){
            Effects.effect(effect, tile.drawx(), tile.drawy());
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        IncineratorEntity entity = tile.entity();
        return entity.heat > 0.5f;
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        if(Mathf.chance(0.02)){
            Effects.effect(effect, tile.drawx(), tile.drawy());
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        IncineratorEntity entity = tile.entity();
        return entity.heat > 0.5f;
    }

    @Override
    public TileEntity newEntity(){
        return new IncineratorEntity();
    }

    public static class IncineratorEntity extends TileEntity{
        public float heat;
    }
}
