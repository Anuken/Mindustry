package mindustry.world.blocks.production;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.*;
import mindustry.gen.*;
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
    }

    @Override
    public void updateTile(){
        if(consValid()){
            heat = Mathf.lerpDelta(heat, 1f, 0.04f);
        }else{
            heat = Mathf.lerpDelta(heat, 0f, 0.02f);
        }
    }

    @Override
    public void draw(){
        super.draw();

        if(heat > 0f){
            float g = 0.3f;
            float r = 0.06f;

            Draw.alpha(((1f - g) + Mathf.absin(Time.time(), 8f, g) + Mathf.random(r) - r) * heat);

            Draw.tint(flameColor);
            Fill.circle(x, y, 2f);
            Draw.color(1f, 1f, 1f, heat);
            Fill.circle(x, y, 1f);

            Draw.color();
        }
    }

    @Override
    public void handleItem(Tilec source, Item item){
        if(Mathf.chance(0.3)){
            effect.at(x, y);
        }
    }

    @Override
    public boolean acceptItem(Tilec source, Item item){
        return heat > 0.5f;
    }

    @Override
    public void handleLiquid(Tile source, Liquid liquid, float amount){
        if(Mathf.chance(0.02)){
            effect.at(x, y);
        }
    }

    @Override
    public boolean acceptLiquid(Tile source, Liquid liquid, float amount){
        return heat > 0.5f;
    }

    public class IncineratorEntity extends TileEntity{
        public float heat;
    }
}
