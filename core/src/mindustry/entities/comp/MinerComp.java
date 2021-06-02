package mindustry.entities.comp;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

@Component
abstract class MinerComp implements Itemsc, Posc, Teamc, Rotc, Drawc{
    @Import float x, y, rotation, hitSize;
    @Import UnitType type;

    transient float mineTimer;
    @Nullable @SyncLocal Tile mineTile;

    public boolean canMine(Item item){
        return type.mineTier >= item.hardness;
    }

    public boolean offloadImmediately(){
        return this.<Unit>self().isPlayer();
    }

    boolean mining(){
        return mineTile != null && !this.<Unit>self().activelyBuilding();
    }

    public boolean validMine(Tile tile, boolean checkDst){
        return !(tile == null || tile.block() != Blocks.air || (!within(tile.worldx(), tile.worldy(), type.miningRange) && checkDst)
        || tile.drop() == null || !canMine(tile.drop()));
    }

    public boolean validMine(Tile tile){
        return validMine(tile, true);
    }

    public boolean canMine(){
        return type.mineSpeed > 0 && type.mineTier >= 0;
    }

    @Override
    public void update(){
        Building core = closestCore();

        if(core != null && mineTile != null && mineTile.drop() != null && !acceptsItem(mineTile.drop()) && within(core, mineTransferRange) && !offloadImmediately()){
            int accepted = core.acceptStack(item(), stack().amount, this);
            if(accepted > 0){
                Call.transferItemTo(self(), item(), accepted,
                mineTile.worldx() + Mathf.range(tilesize / 2f),
                mineTile.worldy() + Mathf.range(tilesize / 2f), core);
                clearItem();
            }
        }

        if((!net.client() || isLocal()) && !validMine(mineTile)){
            mineTile = null;
            mineTimer = 0f;
        }else if(mining()){
            Item item = mineTile.drop();
            mineTimer += Time.delta *type.mineSpeed;

            if(Mathf.chance(0.06 * Time.delta)){
                Fx.pulverizeSmall.at(mineTile.worldx() + Mathf.range(tilesize / 2f), mineTile.worldy() + Mathf.range(tilesize / 2f), 0f, item.color);
            }

            if(mineTimer >= 50f + item.hardness*15f){
                mineTimer = 0;

                if(state.rules.sector != null && team() == state.rules.defaultTeam) state.rules.sector.info.handleProduction(item, 1);

                if(core != null && within(core, mineTransferRange) && core.acceptStack(item, 1, this) == 1 && offloadImmediately()){
                    //add item to inventory before it is transferred
                    if(item() == item && !net.client()) addItem(item);
                    Call.transferItemTo(self(), item, 1,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f), core);
                }else if(acceptsItem(item)){
                    //this is clientside, since items are synced anyway
                    InputHandler.transferItemToUnit(item,
                    mineTile.worldx() + Mathf.range(tilesize / 2f),
                    mineTile.worldy() + Mathf.range(tilesize / 2f),
                    this);
                }else{
                    mineTile = null;
                    mineTimer = 0f;
                }
            }

            if(!headless){
                control.sound.loop(type.mineSound, this, type.mineSoundVolume);
            }
        }
    }

    @Override
    public void draw(){
        if(!mining()) return;
        float focusLen = hitSize / 2f + Mathf.absin(Time.time, 1.1f, 0.5f);
        float swingScl = 12f, swingMag = tilesize / 8f;
        float flashScl = 0.3f;

        float px = x + Angles.trnsx(rotation, focusLen);
        float py = y + Angles.trnsy(rotation, focusLen);

        float ex = mineTile.worldx() + Mathf.sin(Time.time + 48, swingScl, swingMag);
        float ey = mineTile.worldy() + Mathf.sin(Time.time + 48, swingScl + 2f, swingMag);

        Draw.z(Layer.flyingUnit + 0.1f);

        Draw.color(Color.lightGray, Color.white, 1f - flashScl + Mathf.absin(Time.time, 0.5f, flashScl));

        Drawf.laser(team(), Core.atlas.find("minelaser"), Core.atlas.find("minelaser-end"), px, py, ex, ey, 0.75f);

        if(isLocal()){
            Lines.stroke(1f, Pal.accent);
            Lines.poly(mineTile.worldx(), mineTile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Time.time);
        }

        Draw.color();
    }
}
