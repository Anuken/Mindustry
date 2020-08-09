package mindustry.world.blocks.campaign;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

public class ResearchBlock extends Block{
    public float researchSpeed = 1f;
    public @Load("@-top") TextureRegion topRegion;

    public ResearchBlock(String name){
        super(name);

        update = true;
        solid = true;
        hasPower = true;
        hasItems = true;
        configurable = true;
        itemCapacity = 100;

        //TODO requirements shrink as time goes on
        consumes.add(new ConsumeItemDynamic((ResearchBlockEntity entity) -> entity.researching != null ? entity.researching.requirements : ItemStack.empty));
        config(TechNode.class, ResearchBlockEntity::setTo);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("progress", (ResearchBlockEntity e) -> new Bar("bar.progress", Pal.ammo, () -> e.researching == null ? 0f : e.researching.progress / e.researching.time));
    }

    public class ResearchBlockEntity extends Building{
        public @Nullable TechNode researching;

        public double[] accumulator;
        public double[] totalAccumulator;

        @Override
        public void updateTile(){
            if(researching != null){
                //don't research something that is already researched
                if(researching.content.unlocked()){
                    setTo(null);
                }else{
                    double totalTicks = researching.time * 60.0;
                    double amount = researchSpeed * edelta() / totalTicks;

                    double maxProgress = checkRequired(amount, false);

                    for(int i = 0; i < researching.requirements.length; i++){
                        int reqamount = Math.round(state.rules.buildCostMultiplier * researching.requirements[i].amount);
                        accumulator[i] += Math.min(reqamount * maxProgress, reqamount - totalAccumulator[i] + 0.00001); //add min amount progressed to the accumulator
                        totalAccumulator[i] = Math.min(totalAccumulator[i] + reqamount * maxProgress, reqamount);
                    }

                    maxProgress = checkRequired(maxProgress, true);

                    float increment = (float)(maxProgress * researching.time);
                    researching.progress += increment;

                    //check if it has been researched
                    if(researching.progress >= researching.time){
                        researching.content.unlock();

                        setTo(null);
                    }
                }
            }
        }

        private double checkRequired(double amount, boolean remove){
            double maxProgress = amount;

            for(int i = 0; i < researching.requirements.length; i++){
                int ramount = researching.requirements[i].amount;
                int required = (int)(accumulator[i]); //calculate items that are required now

                if(!items.has(researching.requirements[i].item) && ramount > 0){
                    maxProgress = 0f;
                }else if(required > 0){ //if this amount is positive...
                    //calculate how many items it can actually use
                    int maxUse = Math.min(required, items.get(researching.requirements[i].item));
                    //get this as a fraction
                    double fraction = maxUse / (double)required;

                    //move max progress down if this fraction is less than 1
                    maxProgress = Math.min(maxProgress, maxProgress * fraction);

                    accumulator[i] -= maxUse;

                    //remove stuff that is actually used
                    if(remove){
                        items.remove(researching.requirements[i].item, maxUse);
                    }
                }
                //else, no items are required yet, so just keep going
            }

            return maxProgress;
        }

        private void setTo(@Nullable TechNode value){
            researching = value;
            if(value != null){
                accumulator = new double[researching.requirements.length];
                totalAccumulator = new double[researching.requirements.length];
            }
        }

        @Override
        public void display(Table table){
            super.display(table);

            TextureRegionDrawable reg = new TextureRegionDrawable();

            table.row();
            table.table(t -> {
                t.image().update(i -> {
                    i.setDrawable(researching == null ? Icon.cancel : reg.set(researching.content.icon(Cicon.medium)));
                    i.setScaling(Scaling.fit);
                    i.setColor(researching == null ? Color.lightGray : Color.white);
                }).size(32).pad(3);
                t.label(() -> researching == null ? "@none" : researching.content.localizedName).color(Color.lightGray);
            }).left().padTop(4);
        }

        @Override
        public void buildConfiguration(Table table){

        }

        @Override
        public void draw(){
            super.draw();

            Draw.mixcol(Color.white, Mathf.absin(10f, 0.2f));
            Draw.rect(topRegion, x, y);
            Draw.reset();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            if(researching == null) return 0;
            for(int i = 0; i < researching.requirements.length; i++){
                if(researching.requirements[i].item == item) return researching.requirements[i].amount;
            }
            return 0;
        }

        @Override
        public boolean configTapped(){
            //configure with tech node
            ui.showInfo("this does nothing");
            //ui.research.show(node -> {
            //    configure(node);
            //    ui.research.hide();
            //});

            return false;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            if(researching != null){
                write.b(researching.content.getContentType().ordinal());
                write.s(researching.content.id);
            }else{
                write.b(-1);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            byte type = read.b();
            if(type != -1){
                setTo(TechTree.get(content.getByID(ContentType.all[type], read.s())));
            }else{
                researching = null;
            }
        }
    }
}
