package mindustry.world.blocks.units;

import arc.graphics.g2d.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.UnitAssembler.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class UnitAssemblerModule extends PayloadBlock{
    public @Load("@-side1") TextureRegion sideRegion1;
    public @Load("@-side2") TextureRegion sideRegion2;

    public int tier = 1;

    public UnitAssemblerModule(String name){
        super(name);
        rotate = true;
        rotateDraw = false;
        acceptsPayload = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.moduleTier, tier);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        var link = getLink(player.team(), x, y, rotation);
        if(link != null){
            link.block.drawPlace(link.tile.x, link.tile.y, link.rotation, true);
        }
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        return getLink(team, tile.x, tile.y, rotation) != null;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(plan.rotation >= 2 ? sideRegion2 : sideRegion1, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, sideRegion1, topRegion};
    }

    public @Nullable UnitAssemblerBuild getLink(Team team, int x, int y, int rotation){
        var results = Vars.indexer.getFlagged(team, BlockFlag.unitAssembler).<UnitAssemblerBuild>as();

        return results.find(b -> b.moduleFits(this, x * tilesize + offset, y * tilesize + offset, rotation));
    }

    public class UnitAssemblerModuleBuild extends PayloadBlockBuild<Payload>{
        public UnitAssemblerBuild link;
        public int lastChange = -2;

        public void findLink(){
            if(link != null){
                link.removeModule(this);
            }
            link = getLink(team, tile.x, tile.y, rotation);
            if(link != null){
                link.updateModules(this);
            }
        }

        public int tier(){
            return tier;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input conveyors
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            Draw.rect(rotation >= 2 ? sideRegion2 : sideRegion1, x, y, rotdeg());

            Draw.z(Layer.blockOver);
            payRotation = rotdeg();
            drawPayload();
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return link != null && this.payload == null && link.acceptPayload(this, payload);
        }

        @Override
        public void drawSelect(){
            //TODO draw area?
            if(link != null){
                Drawf.selected(link, Pal.accent);
            }
        }

        @Override
        public void onRemoved(){
            super.onRemoved();

            if(link != null){
                link.removeModule(this);
            }
        }

        @Override
        public void updateTile(){
            if(lastChange != world.tileChanges){
                lastChange = world.tileChanges;
                findLink();
            }

            if(moveInPayload() && link != null && link.moduleFits(block, x, y, rotation) && !link.wasOccupied && link.acceptPayload(this, payload) && efficiency > 0){
                link.yeetPayload(payload);
                payload = null;
            }
        }

    }
}
