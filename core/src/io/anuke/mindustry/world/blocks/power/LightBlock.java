package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.graphics.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.renderer;

public class LightBlock extends Block{
    protected Color color = Color.royal;
    protected float radius = 200f;

    public LightBlock(String name){
        super(name);
        hasPower = true;
        update = true;
    }

    @Override
    public void drawLight(Tile tile){
        renderer.lights.add(tile.drawx(), tile.drawy(), radius, color, 0.5f * tile.entity.power.satisfaction);
    }
}
