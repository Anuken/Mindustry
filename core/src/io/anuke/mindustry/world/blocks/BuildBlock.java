package io.anuke.mindustry.world.blocks;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.Graphics.Cursor;
import io.anuke.arc.Graphics.Cursor.SystemCursor;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.effect.RubbleDecal;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.BlockBuildEndEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.modules.ItemModule;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class BuildBlock extends Block{
    public static final int maxSize = 9;
    private static final BuildBlock[] buildBlocks = new BuildBlock[maxSize];

    public BuildBlock(int size){
        super("build" + size);
        this.size = size;
        update = true;
        health = 20;
        layer = Layer.placement;
        consumesTap = true;
        solidifes = true;

        buildBlocks[size - 1] = this;
    }

    /** Returns a BuildBlock by size. */
    public static BuildBlock get(int size){
        if(size > maxSize) throw new IllegalArgumentException("No. Don't place BuildBlocks of size greater than " + maxSize);
        return buildBlocks[size - 1];
    }

    @Remote(called = Loc.server)
    public static void onDeconstructFinish(Tile tile, Block block){
        Team team = tile.getTeam();
        Effects.effect(Fx.breakBlock, tile.drawx(), tile.drawy(), block.size);
        world.removeBlock(tile);
        Events.fire(new BlockBuildEndEvent(tile, team, true));
    }

    @Remote(called = Loc.server)
    public static void onConstructFinish(Tile tile, Block block, int builderID, byte rotation, Team team){
        if(tile == null) return;
        float healthf = tile.entity == null ? 1f : tile.entity.healthf();
        world.setBlock(tile, block, team, rotation);
        if(tile.entity != null){
            tile.entity.health = block.health * healthf;
        }
        Effects.effect(Fx.placeBlock, tile.drawx(), tile.drawy(), block.size);
        Core.app.post(() -> tile.block().placed(tile));

        //last builder was this local client player, call placed()
        if(!headless && builderID == player.id){
            //this is run delayed, since if this is called on the server, all clients need to recieve the onBuildFinish()
            //event first before they can recieve the placed() event modification results
            Core.app.post(() -> tile.block().playerPlaced(tile));
        }
        Core.app.post(() -> Events.fire(new BlockBuildEndEvent(tile, team, false)));
    }

    @Override
    public boolean isHidden(){
        return true;
    }

    @Override
    public String getDisplayName(Tile tile){
        BuildEntity entity = tile.entity();
        return Core.bundle.format("block.constructing", entity.cblock == null ? entity.previous.localizedName : entity.cblock.localizedName);
    }

    @Override
    public TextureRegion getDisplayIcon(Tile tile){
        BuildEntity entity = tile.entity();
        return (entity.cblock == null ? entity.previous : entity.cblock).icon(Icon.full);
    }

    @Override
    public boolean isSolidFor(Tile tile){
        BuildEntity entity = tile.entity();
        return entity == null || (entity.cblock != null && entity.cblock.solid) || entity.previous == null || entity.previous.solid;
    }

    @Override
    public Cursor getCursor(Tile tile){
        return SystemCursor.hand;
    }

    @Override
    public void tapped(Tile tile, Player player){
        BuildEntity entity = tile.entity();

        //if the target is constructible, begin constructing
        if(entity.cblock != null){
            player.clearBuilding();
            player.addBuildRequest(new BuildRequest(tile.x, tile.y, tile.rotation(), entity.cblock));
        }
    }

    @Override
    public void onDestroyed(Tile tile){
        Effects.effect(Fx.blockExplosionSmoke, tile);

        if(!tile.floor().solid && !tile.floor().isLiquid){
            RubbleDecal.create(tile.drawx(), tile.drawy(), size);
        }
    }

    @Override
    public void draw(Tile tile){
        BuildEntity entity = tile.entity();

        //When breaking, don't draw the previous block... since it's the thing you were breaking
        if(entity.cblock != null && entity.previous == entity.cblock){
            return;
        }

        if(entity.previous == null) return;

        if(Core.atlas.isFound(entity.previous.icon(Icon.full))){
            Draw.rect(entity.previous.icon(Icon.full), tile.drawx(), tile.drawy(), entity.previous.rotate ? tile.rotation() * 90 : 0);
        }
    }

    @Override
    public void drawLayer(Tile tile){

        BuildEntity entity = tile.entity();

        Shaders.blockbuild.color = Pal.accent;

        Block target = entity.cblock == null ? entity.previous : entity.cblock;

        if(target == null) return;

        for(TextureRegion region : target.getGeneratedIcons()){
            Shaders.blockbuild.region = region;
            Shaders.blockbuild.progress = entity.progress;

            Draw.rect(region, tile.drawx(), tile.drawy(), target.rotate ? tile.rotation() * 90 : 0);
            Draw.flush();
        }
    }

    @Override
    public TileEntity newEntity(){
        return new BuildEntity();
    }

    public class BuildEntity extends TileEntity{
        /**
         * The recipe of the block that is being constructed.
         * If there is no recipe for this block, as is the case with rocks, 'previous' is used.
         */
        public Block cblock;

        public float progress = 0;
        public float buildCost;
        /**
         * The block that used to be here.
         * If a non-recipe block is being deconstructed, this is the block that is being deconstructed.
         */
        public Block previous;
        public int builderID = -1;

        private float[] accumulator;
        private float[] totalAccumulator;

        public void construct(Unit builder, @Nullable TileEntity core, float amount){
            if(cblock == null){
                kill();
                return;
            }

            float maxProgress = core == null ? amount : checkRequired(core.items, amount, false);

            for(int i = 0; i < cblock.buildRequirements.length; i++){
                int reqamount = Math.round(state.rules.buildCostMultiplier * cblock.buildRequirements[i].amount);
                accumulator[i] += Math.min(reqamount * maxProgress, reqamount - totalAccumulator[i] + 0.00001f); //add min amount progressed to the accumulator
                totalAccumulator[i] = Math.min(totalAccumulator[i] + reqamount * maxProgress, reqamount);
            }

            maxProgress = core == null ? maxProgress : checkRequired(core.items, maxProgress, true);

            progress = Mathf.clamp(progress + maxProgress);

            if(builder instanceof Player){
                builderID = builder.getID();
            }

            if(progress >= 1f || state.rules.infiniteResources){
                Call.onConstructFinish(tile, cblock, builderID, tile.rotation(), builder.getTeam());
            }
        }

        public void deconstruct(Unit builder, @Nullable TileEntity core, float amount){
            float deconstructMultiplier = 0.5f;

            if(cblock != null){
                ItemStack[] requirements = cblock.buildRequirements;
                if(requirements.length != accumulator.length || totalAccumulator.length != requirements.length){
                    setDeconstruct(previous);
                }

                for(int i = 0; i < requirements.length; i++){
                    int reqamount = Math.round(state.rules.buildCostMultiplier * requirements[i].amount);
                    accumulator[i] += Math.min(amount * deconstructMultiplier * reqamount, deconstructMultiplier * reqamount - totalAccumulator[i]); //add scaled amount progressed to the accumulator
                    totalAccumulator[i] = Math.min(totalAccumulator[i] + reqamount * amount * deconstructMultiplier, reqamount);

                    int accumulated = (int)(accumulator[i]); //get amount

                    if(amount > 0 && accumulated > 0){ //if it's positive, add it to the core
                        if(core != null){
                            int accepting = core.tile.block().acceptStack(requirements[i].item, accumulated, core.tile, builder);
                            core.tile.block().handleStack(requirements[i].item, accepting, core.tile, builder);
                            accumulator[i] -= accepting;
                        }else{
                            accumulator[i] -= accumulated;
                        }
                    }
                }
            }

            progress = Mathf.clamp(progress - amount);

            if(progress <= 0 || state.rules.infiniteResources){
                Call.onDeconstructFinish(tile, this.cblock == null ? previous : this.cblock);
            }
        }

        private float checkRequired(ItemModule inventory, float amount, boolean remove){
            float maxProgress = amount;

            for(int i = 0; i < cblock.buildRequirements.length; i++){
                int sclamount = Math.round(state.rules.buildCostMultiplier * cblock.buildRequirements[i].amount);
                int required = (int)(accumulator[i]); //calculate items that are required now

                if(inventory.get(cblock.buildRequirements[i].item) == 0 && sclamount != 0){
                    maxProgress = 0f;
                }else if(required > 0){ //if this amount is positive...
                    //calculate how many items it can actually use
                    int maxUse = Math.min(required, inventory.get(cblock.buildRequirements[i].item));
                    //get this as a fraction
                    float fraction = maxUse / (float)required;

                    //move max progress down if this fraction is less than 1
                    maxProgress = Math.min(maxProgress, maxProgress * fraction);

                    accumulator[i] -= maxUse;

                    //remove stuff that is actually used
                    if(remove){
                        inventory.remove(cblock.buildRequirements[i].item, maxUse);
                    }
                }
                //else, no items are required yet, so just keep going
            }

            return maxProgress;
        }

        public float progress(){
            return progress;
        }

        public void setConstruct(Block previous, Block block){
            this.cblock = block;
            this.previous = previous;
            this.accumulator = new float[block.buildRequirements.length];
            this.totalAccumulator = new float[block.buildRequirements.length];
            this.buildCost = block.buildCost * state.rules.buildCostMultiplier;
        }

        public void setDeconstruct(Block previous){
            this.previous = previous;
            this.progress = 1f;
            if(previous.buildCost >= 0.01f){
                this.cblock = previous;
                this.accumulator = new float[previous.buildRequirements.length];
                this.totalAccumulator = new float[previous.buildRequirements.length];
                this.buildCost = previous.buildCost * state.rules.buildCostMultiplier;
            }else{
                this.buildCost = 20f; //default no-requirement build cost is 20
            }
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeFloat(progress);
            stream.writeShort(previous == null ? -1 : previous.id);
            stream.writeShort(cblock == null ? -1 : cblock.id);

            if(accumulator == null){
                stream.writeByte(-1);
            }else{
                stream.writeByte(accumulator.length);
                for(int i = 0; i < accumulator.length; i++){
                    stream.writeFloat(accumulator[i]);
                    stream.writeFloat(totalAccumulator[i]);
                }
            }
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            progress = stream.readFloat();
            short pid = stream.readShort();
            short rid = stream.readShort();
            byte acsize = stream.readByte();

            if(acsize != -1){
                accumulator = new float[acsize];
                totalAccumulator = new float[acsize];
                for(int i = 0; i < acsize; i++){
                    accumulator[i] = stream.readFloat();
                    totalAccumulator[i] = stream.readFloat();
                }
            }

            if(pid != -1) previous = content.block(pid);
            if(rid != -1) cblock = content.block(rid);

            if(cblock != null){
                buildCost = cblock.buildCost * state.rules.buildCostMultiplier;
            }else{
                buildCost = 20f;
            }
        }
    }
}
