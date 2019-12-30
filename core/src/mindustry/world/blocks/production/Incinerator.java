package mindustry.world.blocks.production;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Effects;
import mindustry.entities.Effects.Effect;
import mindustry.entities.type.TileEntity;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;

public class Incinerator extends Block{
    public Effect effect = Fx.fuelburn;
    public Color flameColor = Color.valueOf("ffad9d");

    public Incinerator(String name){
        super(name);
        hasPower = true;
        hasLiquids = true;
        update = true;
        solid = true;
        entityType = IncineratorEntity::new;
    }

    @Override
    public void update(Tile tile){
        IncineratorEntity entity = tile.ent();

        if(entity.cons.valid()){
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.04f);
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.02f);
        }
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        IncineratorEntity entity = tile.ent();

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
        IncineratorEntity entity = tile.ent();
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
        IncineratorEntity entity = tile.ent();
        return entity.heat > 0.5f;
    }

    public static class IncineratorEntity extends TileEntity{
        public float heat;
    }
}
