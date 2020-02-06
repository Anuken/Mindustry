package mindustry.entities.def;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

@Component
abstract class MinerComp implements Itemsc, Posc, Teamc, Rotc{
    transient float x, y, rotation;

    @Nullable Tile mineTile;

    abstract boolean canMine(Item item);

    abstract float miningSpeed();

    abstract boolean offloadImmediately();

    boolean mining(){
        return mineTile != null;
    }

    void updateMining(){
        Tilec core = closestCore();

        if(core != null && mineTile != null && mineTile.drop() != null && !acceptsItem(mineTile.drop()) && dst(core) < mineTransferRange){
            int accepted = core.tile().block().acceptStack(item(), stack().amount, core.tile(), this);
            if(accepted > 0){
                Call.transferItemTo(item(), accepted,
                mineTile.worldx() + Mathf.range(tilesize / 2f),
                mineTile.worldy() + Mathf.range(tilesize / 2f), core.tile());
                clearItem();
            }
        }

        if(mineTile == null || core == null || mineTile.block() != Blocks.air || dst(mineTile.worldx(), mineTile.worldy()) > miningRange
        || mineTile.drop() == null || !acceptsItem(mineTile.drop()) || !canMine(mineTile.drop())){
            mineTile = null;
        }else{
            Item item = mineTile.drop();
            rotation(Mathf.slerpDelta(rotation(), angleTo(mineTile.worldx(), mineTile.worldy()), 0.4f));

            if(Mathf.chance(Time.delta() * (0.06 - item.hardness * 0.01) * miningSpeed())){

                if(dst(core) < mineTransferRange && core.tile().block().acceptStack(item, 1, core.tile(), this) == 1 && offloadImmediately()){
                    Call.transferItemTo(item, 1,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f), core.tile());
                }else if(acceptsItem(item)){
                    //this is clientside, since items are synced anyway
                    InputHandler.transferItemToUnit(item,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f),
                    this);
                }
            }

            if(Mathf.chance(0.06 * Time.delta())){
                Fx.pulverizeSmall.at(mineTile.worldx() + Mathf.range(tilesize / 2f), mineTile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
            }
        }
    }

    void drawOver(){
        if(!mining()) return;
        float focusLen = 4f + Mathf.absin(Time.time(), 1.1f, 0.5f);
        float swingScl = 12f, swingMag = tilesize / 8f;
        float flashScl = 0.3f;

        float px = x + Angles.trnsx(rotation, focusLen);
        float py = y + Angles.trnsy(rotation, focusLen);

        float ex = mineTile.worldx() + Mathf.sin(Time.time() + 48, swingScl, swingMag);
        float ey = mineTile.worldy() + Mathf.sin(Time.time() + 48, swingScl + 2f, swingMag);

        Draw.color(Color.lightGray, Color.white, 1f - flashScl + Mathf.absin(Time.time(), 0.5f, flashScl));

        Drawf.laser(Core.atlas.find("minelaser"), Core.atlas.find("minelaser-end"), px, py, ex, ey, 0.75f);

        //TODO hack?
        if(isLocal()){
            Lines.stroke(1f, Pal.accent);
            Lines.poly(mineTile.worldx(), mineTile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Time.time());
        }

        Draw.color();
    }
}
