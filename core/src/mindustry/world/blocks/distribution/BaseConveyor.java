package mindustry.world.blocks.distribution;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.*;
import mindustry.entities.type.*;
import mindustry.entities.traits.BuilderTrait.*;

import static mindustry.Vars.tilesize;

abstract public class BaseConveyor extends Block implements Autotiler{
    TextureRegion[][] regions = new TextureRegion[7][4];

    public float speed = 0f;

    public BaseConveyor(String name){
        super(name);

        rotate = true;
        update = true;
        hasItems = true;
        itemCapacity = 4;
        unloadable = false;
        layer = Layer.overlay;
        idleSoundVolume = 0.004f;
        conveyorPlacement = true;
        idleSound = Sounds.conveyor;
        group = BlockGroup.transportation;
    }

    @Override
    public void draw(Tile tile){
        BaseConveyorEntity entity = tile.ent();
        byte rotation = tile.rotation();

        int frame = entity.clogHeat <= 0.5f ? (int)(((Time.time() * speed * 8f * entity.timeScale)) % 4) : 0;
        Draw.rect(regions[Mathf.clamp(entity.blendbits, 0, regions.length - 1)][Mathf.clamp(frame, 0, regions[0].length - 1)], tile.drawx(), tile.drawy(),
        tilesize * entity.blendsclx, tilesize * entity.blendscly, rotation * 90);
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        int[] bits = getTiling(req, list);

        if(bits == null) return;

        TextureRegion region = regions[bits[0]][0];
        Draw.rect(region, req.drawx(), req.drawy(), region.getWidth() * bits[1] * Draw.scl * req.animScale, region.getHeight() * bits[2] * Draw.scl * req.animScale, req.rotation * 90);
    }

    @Override
    public void onProximityUpdate(Tile tile){
        super.onProximityUpdate(tile);

        BaseConveyorEntity entity = tile.ent();
        int[] bits = buildBlending(tile, tile.rotation(), null, true);
        entity.blendbits = bits[0];
        entity.blendsclx = bits[1];
        entity.blendscly = bits[2];
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return otherblock.outputsItems() && lookingAt(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name + "-0-0")};
    }

    @Override
    public boolean shouldIdleSound(Tile tile){
        BaseConveyorEntity entity = tile.ent();
        return entity.clogHeat <= 0.5f;
    }

    @Override
    public boolean isAccessible(){
        return true;
    }

    static abstract class BaseConveyorEntity extends TileEntity{
        int blendbits;
        int blendsclx, blendscly;

        float clogHeat = 0f;
    }
}
