package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.modules.LiquidModule;
import io.anuke.ucore.graphics.Draw;

public class LiquidBlock extends Block{
    protected TextureRegion liquidRegion, bottomRegion, topRegion;

    public LiquidBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasLiquids = true;
        group = BlockGroup.liquids;
        outputsLiquid = true;
    }

    @Override
    public void load(){
        super.load();

        liquidRegion = Draw.region(name + "-liquid");
        topRegion = Draw.region(name + "-top");
        bottomRegion = Draw.region(name + "-bottom");
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name() + "-bottom"), Draw.region(name() + "-top")};
    }

    @Override
    public void draw(Tile tile){
        LiquidModule mod = tile.entity.liquids;

        int rotation = rotate ? tile.getRotation() * 90 : 0;

        Draw.rect(bottomRegion, tile.drawx(), tile.drawy(), rotation);

        if(mod.total() > 0.001f){
            Draw.color(mod.current().color);
            Draw.alpha(mod.total() / liquidCapacity);
            Draw.rect(liquidRegion, tile.drawx(), tile.drawy(), rotation);
            Draw.color();
        }

        Draw.rect(topRegion, tile.drawx(), tile.drawy(), rotation);
    }
}
