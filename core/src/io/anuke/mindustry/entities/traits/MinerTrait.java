package io.anuke.mindustry.entities.traits;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.*;

public interface MinerTrait extends Entity{

    /** Returns the range at which this miner can mine blocks.*/
    default float getMiningRange(){
        return 70f;
    }

    default boolean isMining(){
        return getMineTile() != null;
    }

    /** Returns the tile this builder is currently mining. */
    Tile getMineTile();

    /** Sets the tile this builder is currently mining. */
    void setMineTile(Tile tile);

    /** Returns the mining speed of this miner. 1 = standard, 0.5 = half speed, 2 = double speed, etc. */
    float getMinePower();

    /** Returns whether or not this builder can mine a specific item type. */
    boolean canMine(Item item);

    default void updateMining(){
        Unit unit = (Unit)this;
        Tile tile = getMineTile();
        TileEntity core = unit.getClosestCore();

        if(tile == null || core == null || tile.block() != Blocks.air || dst(tile.worldx(), tile.worldy()) > getMiningRange()
                || tile.drop() == null || !unit.acceptsItem(tile.drop()) || !canMine(tile.drop())){
            setMineTile(null);
        }else{
            Item item = tile.drop();
            unit.rotation = Mathf.slerpDelta(unit.rotation, unit.angleTo(tile.worldx(), tile.worldy()), 0.4f);

            if(Mathf.chance(Time.delta() * (0.06 - item.hardness * 0.01) * getMinePower())){

                if(unit.dst(core) < mineTransferRange && core.tile.block().acceptStack(item, 1, core.tile, unit) == 1){
                    Call.transferItemTo(item, 1,
                            tile.worldx() + Mathf.range(tilesize / 2f),
                            tile.worldy() + Mathf.range(tilesize / 2f), core.tile);
                }else if(unit.acceptsItem(item)){
                    Call.transferItemToUnit(item,
                            tile.worldx() + Mathf.range(tilesize / 2f),
                            tile.worldy() + Mathf.range(tilesize / 2f),
                            unit);
                }
            }

            if(Mathf.chance(0.06 * Time.delta())){
                Effects.effect(Fx.pulverizeSmall,
                        tile.worldx() + Mathf.range(tilesize / 2f),
                        tile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
            }
        }
    }

    default void drawMining(){
        Unit unit = (Unit)this;
        Tile tile = getMineTile();

        if(tile == null) return;

        float focusLen = 4f + Mathf.absin(Time.time(), 1.1f, 0.5f);
        float swingScl = 12f, swingMag = tilesize / 8f;
        float flashScl = 0.3f;

        float px = unit.x + Angles.trnsx(unit.rotation, focusLen);
        float py = unit.y + Angles.trnsy(unit.rotation, focusLen);

        float ex = tile.worldx() + Mathf.sin(Time.time() + 48, swingScl, swingMag);
        float ey = tile.worldy() + Mathf.sin(Time.time() + 48, swingScl + 2f, swingMag);

        Draw.color(Color.lightGray, Color.white, 1f - flashScl + Mathf.absin(Time.time(), 0.5f, flashScl));

        Drawf.laser(Core.atlas.find("minelaser"), Core.atlas.find("minelaser-end"), px, py, ex, ey, 0.75f);

        if(unit instanceof Player && ((Player)unit).isLocal){
            Lines.stroke(1f, Pal.accent);
            Lines.poly(tile.worldx(), tile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Time.time());
        }

        Draw.color();
    }
}
