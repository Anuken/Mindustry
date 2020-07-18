package mindustry.world.blocks.experimental;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;

public class BlockForge extends PayloadAcceptor{
    public float buildSpeed = 0.4f;

    public BlockForge(String name){
        super(name);

        size = 3;
        update = true;
        outputsPayload = true;
        hasItems = true;
        configurable = true;
        hasPower = true;
        rotate = true;

        config(Block.class, (BlockForgeEntity tile, Block block) -> tile.recipe = block);

        consumes.add(new ConsumeItemDynamic((BlockForgeEntity e) -> e.recipe != null ? e.recipe.requirements : ItemStack.empty));
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("progress", entity -> new Bar("bar.progress", Pal.ammo, () -> ((BlockForgeEntity)entity).progress));
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    public class BlockForgeEntity extends PayloadAcceptorEntity<BlockPayload>{
        public @Nullable Block recipe;
        public float progress, time, heat;

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            if(recipe == null) return 0;
            for(ItemStack stack : recipe.requirements){
                if(stack.item == item) return stack.amount * 2;
            }
            return 0;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }

        @Override
        public void updateTile(){
            boolean produce = recipe != null && consValid() && payload == null;

            if(produce){
                progress += buildSpeed * edelta();

                if(progress >= recipe.buildCost){
                    consume();
                    payload = new BlockPayload(recipe, team);
                    progress = 0f;
                }
            }else{
                progress = 0;
            }

            heat = Mathf.lerpDelta(heat, Mathf.num(produce), 0.3f);
            time += heat * delta();

            moveOutPayload();
        }

        @Override
        public void buildConfiguration(Table table){
            Seq<Block> blocks = Vars.content.blocks().select(b -> b.isVisible() && b.size <= 2);

            ItemSelection.buildTable(table, blocks, () -> recipe, block -> recipe = block);
        }

        @Override
        public Object config(){
            return recipe;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());

            if(recipe != null){
                Draw.draw(Layer.blockOver, () -> Drawf.construct(this, recipe, 0, progress / recipe.buildCost, heat, time));
            }

            drawPayload();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(recipe == null ? -1 : recipe.id);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            recipe = Vars.content.block(read.s());
            progress = read.f();
        }
    }
}
