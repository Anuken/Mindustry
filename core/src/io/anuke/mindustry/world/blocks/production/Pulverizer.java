package io.anuke.mindustry.world.blocks.production;

import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Tile;
import io.anuke.arc.graphics.Draw;

public class Pulverizer extends GenericCrafter{
    protected TextureRegion rotatorRegion;

    public Pulverizer(String name){
        super(name);
        hasItems = true;
    }

    @Override
    public void load(){
        super.load();

        rotatorRegion = Core.atlas.find(name + "-rotator");
    }

    @Override
    public void draw(Tile tile){
        GenericCrafterEntity entity = tile.entity();

        Draw.rect(region, tile.drawx(), tile.drawy());
        Draw.rect(rotatorRegion, tile.drawx(), tile.drawy(), entity.totalProgress * 2f);
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator")};
    }
}
