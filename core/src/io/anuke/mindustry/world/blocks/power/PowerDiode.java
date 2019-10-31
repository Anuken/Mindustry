package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.world;

public class PowerDiode extends Block {

    protected TextureRegion arrow;

    public PowerDiode(String name){
        super(name);
        rotate = true;
        update = true;
        solid = true;
        insulated = true;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        Tile back = getNearby(tile, (tile.rotation() + 2) % 4);
        Tile front = getNearby(tile, tile.rotation());

        if(back.block() != null && back.block() instanceof Battery &&
          front.block() != null && front.block() instanceof Battery){

            float backCapacity = back.block().consumes.getPower().capacity;
            float frontCapacity = front.block().consumes.getPower().capacity;

            float backPower = backCapacity * back.entity.power.satisfaction;
            float frontPower = frontCapacity * front.entity.power.satisfaction;

            if(backPower > frontPower && front.entity.power.satisfaction < 1f){
                float send = Mathf.clamp((backPower - frontPower) / 2, 0f, frontCapacity);

                back.entity.power.satisfaction -= send / backCapacity;
                front.entity.power.satisfaction += send / frontCapacity;
            }
            tile.entity.noSleep();
        }else{
            tile.entity.sleep();
        }
    }

    @Override
    public void load(){
        super.load();
        arrow = Core.atlas.find(name + "-arrow");
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(region, tile.drawx(), tile.drawy(), 0);
        Draw.rect(arrow, tile.drawx(), tile.drawy(), rotate ? tile.rotation() * 90 : 0);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-arrow"), Core.atlas.find(name)};
    }

    public Tile getNearby(Tile tile, int rotation){
        int x = tile.x;
        int y = tile.y;

        if(rotation == 0) return world.ltile(x + 1, y);
        if(rotation == 1) return world.ltile(x, y + 1);
        if(rotation == 2) return world.ltile(x - 1, y);
        if(rotation == 3) return world.ltile(x, y - 1);
        return null;
    }
}
