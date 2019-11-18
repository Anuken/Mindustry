package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.renderer;

public class LightBlock extends Block{
    protected Color color = Color.royal;
    protected float brightness = 0.5f;
    protected float radius = 200f;
    protected int topRegion;

    public LightBlock(String name){
        super(name);
        hasPower = true;
        update = true;
        topRegion = reg("-top");
        configurable = true;
        entityType = LightEntity::new;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);
        Draw.blend(Blending.additive);
        Draw.color(color, tile.entity.efficiency() * 0.3f);
        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy());
        Draw.color();
        Draw.blend();
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.<LightEntity>entity().color = value;
    }

    @Override
    public void drawLight(Tile tile){
        renderer.lights.add(tile.drawx(), tile.drawy(), radius, color, brightness * tile.entity.efficiency());
    }

    public class LightEntity extends TileEntity{
        public int color;
    }
}
