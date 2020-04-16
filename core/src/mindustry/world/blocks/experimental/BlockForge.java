package mindustry.world.blocks.experimental;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;

public class BlockForge extends Block{
    public float buildSpeed = 0.4f;

    public BlockForge(String name){
        super(name);

        size = 3;
        update = true;
        outputsPayload = true;
        hasItems = true;
        configurable = true;
        hasPower = true;

        config(Block.class, (tile, block) -> ((BlockForgeEntity)tile).recipe = block);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("progress", entity -> new Bar("bar.progress", Pal.ammo, () -> ((BlockForgeEntity)entity).progress));
    }

    public class BlockForgeEntity extends TileEntity{
        public @Nullable Payload payload;
        public @Nullable Block recipe;
        public float progress, time, heat;

        @Override
        public boolean acceptItem(Tilec source, Item item){
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
        public void updateTile(){
            boolean produce = recipe != null && consValid() && payload == null && items.has(recipe.requirements);

            if(produce){
                progress += buildSpeed * edelta();

                if(progress >= recipe.buildCost){
                    items.remove(recipe.requirements);
                    payload = new BlockPayload(recipe);
                    progress = 0f;
                }
            }else{
                progress = 0;
            }

            heat = Mathf.lerpDelta(heat, Mathf.num(produce), 0.3f);
            time += heat * delta();

            if(payload != null && dumpPayload(payload)){
                payload = null;
            }
        }

        @Override
        public void buildConfiguration(Table table){
            Array<Block> blocks = Vars.content.blocks().select(b -> b.isVisible() && b.size <= 2 && b.requirements.length <= 3);

            ItemSelection.buildTable(table, blocks, () -> recipe, block -> recipe = block);
        }

        @Override
        public Object config(){
            return recipe;
        }

        @Override
        public void draw(){
            super.draw();

            if(payload != null){
                payload.draw(x, y, 0);
            }

            if(recipe != null){
                TextureRegion region = recipe.icon(Cicon.full);

                Shaders.build.region = region;
                Shaders.build.progress = progress / recipe.buildCost;
                Shaders.build.color.set(Pal.accent);
                Shaders.build.color.a = heat;
                Shaders.build.time = -time / 20f;

                Draw.shader(Shaders.build);
                Draw.rect(region, x, y);
                Draw.shader();

                Draw.color(Pal.accent);
                Draw.alpha(heat);

                Lines.lineAngleCenter(x + Mathf.sin(time, 20f, Vars.tilesize / 2f * size - 2f), y, 90, size * Vars.tilesize - 4f);

                Draw.reset();
            }
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
