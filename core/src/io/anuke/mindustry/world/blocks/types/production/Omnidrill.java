package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Tmp;

public class Omnidrill extends Drill {

    public Omnidrill(String name){
        super(name);
        drillEffect = Fx.sparkbig;
        resource = null;
        result = null;
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        if(tile.floor().drops == null) return;
        Item item = tile.floor().drops.item;

        TextureRegion region = item.region;
        Tmp.tr1.setRegion(region, 4, 4, 1, 1);

        Draw.rect(Tmp.tr1, tile.worldx(), tile.worldy(), 2f, 2f);
    }

    @Override
    public boolean canReplace(Block other) {
        return other instanceof Drill && other != this;
    }

    @Override
    public void update(Tile tile){
        TileEntity entity = tile.entity;

        if(tile.floor().drops != null && entity.timer.get(timerDrill, 60 * time)){
            offloadNear(tile, tile.floor().drops.item);
            Effects.effect(drillEffect, tile.worldx(), tile.worldy());
        }

        if(entity.timer.get(timerDump, 30)){
            tryDump(tile);
        }
    }

    @Override
    public boolean isLayer(Tile tile){
        return tile.floor().drops == null;
    }
}
