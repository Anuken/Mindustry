package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class DirectionalUnloader extends Block{
    public @Load(value = "@-center", fallback = "unloader-center") TextureRegion centerRegion;
    public @Load("@-top") TextureRegion topRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;

    public float speed = 1f;
    public boolean allowCoreUnload = false;

    public DirectionalUnloader(String name){
        super(name);

        update = true;
        solid = true;
        hasItems = true;
        configurable = true;
        saveConfig = true;
        rotate = true;
        itemCapacity = 0;
        noUpdateDisabled = true;
        unloadable = false;
        isDuct = true;
        envDisabled = Env.none;
        clearOnDoubleTap = true;
        priority = TargetPriority.transport;

        config(Item.class, (DirectionalUnloaderBuild tile, Item item) -> tile.unloadItem = item);
        configClear((DirectionalUnloaderBuild tile) -> tile.unloadItem = null);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.speed, 60f / speed, StatUnit.itemsSecond);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(topRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        drawPlanConfig(plan, list);
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list){
        drawPlanConfigCenter(plan, plan.config, "duct-unloader-center");
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("items");
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegion, arrowRegion};
    }

    public class DirectionalUnloaderBuild extends Building{
        public float unloadTimer = 0f;
        public Item unloadItem = null;
        public int offset = 0;

        @Override
        public void updateTile(){
            if((unloadTimer += edelta()) >= speed){
                Building front = front(), back = back();

                if(front != null && back != null && back.items != null && front.team == team && back.team == team && back.canUnload() && (allowCoreUnload || !(back instanceof CoreBuild))){
                    if(unloadItem == null){
                        var itemseq = content.items();
                        int itemc = itemseq.size;
                        for(int i = 0; i < itemc; i++){
                            Item item = itemseq.get((i + offset) % itemc);
                            if(back.items.has(item) && front.acceptItem(this, item)){
                                front.handleItem(this, item);
                                back.items.remove(item, 1);
                                back.itemTaken(item);
                                offset ++;
                                offset %= itemc;
                                break;
                            }
                        }
                    }else if(back.items.has(unloadItem) && front.acceptItem(this, unloadItem)){
                        front.handleItem(this, unloadItem);
                        back.items.remove(unloadItem, 1);
                        back.itemTaken(unloadItem);
                    }
                }

                unloadTimer %= speed;
            }
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            Draw.rect(topRegion, x, y, rotdeg());

            if(unloadItem != null){
                Draw.color(unloadItem.color);
                Draw.rect(centerRegion, x, y);
                Draw.color();
            }else{
                Draw.rect(arrowRegion, x, y, rotdeg());
            }

        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(DirectionalUnloader.this, table, content.items(), () -> unloadItem, this::configure);
        }

        @Override
        public Item config(){
            return unloadItem;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(unloadItem == null ? -1 : unloadItem.id);
            write.s(offset);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int id = read.s();
            unloadItem = id == -1 ? null : content.items().get(id);
            offset = read.s();
        }
    }
}
