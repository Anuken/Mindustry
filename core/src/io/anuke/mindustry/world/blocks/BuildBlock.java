package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.effect.RubbleDecal;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.CursorType;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.modules.ItemModule;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class BuildBlock extends Block{

    public BuildBlock(String name){
        super(name);
        update = true;
        size = Integer.parseInt(name.charAt(name.length() - 1) + "");
        health = 10;
        layer = Layer.placement;
        consumesTap = true;
        solidifes = true;
    }

    @Remote(called = Loc.server)
    public static void onDeconstructFinish(Tile tile, Block block){
        Effects.effect(Fx.breakBlock, tile.drawx(), tile.drawy(), block.size);
        world.removeBlock(tile);
    }

    @Remote(called = Loc.server)
    public static void onConstructFinish(Tile tile, Block block, int builderID, byte rotation, Team team){
        if(tile == null) return;
        tile.setRotation(rotation);
        world.setBlock(tile, block, team);
        Effects.effect(Fx.placeBlock, tile.drawx(), tile.drawy(), block.size);
        threads.runDelay(() -> tile.block().placed(tile));

        //last builder was this local client player, call placed()
        if(!headless && builderID == players[0].id){
            //this is run delayed, since if this is called on the server, all clients need to recieve the onBuildFinish()
            //event first before they can recieve the placed() event modification results
            threads.runDelay(() -> tile.block().playerPlaced(tile));
        }
    }

    @Override
    public String getDisplayName(Tile tile){
        BuildEntity entity = tile.entity();
        return Bundles.format("block.constructing", entity.recipe == null ? entity.previous.formalName : entity.recipe.result.formalName);
    }

    @Override
    public TextureRegion getDisplayIcon(Tile tile){
        BuildEntity entity = tile.entity();
        return (entity.recipe == null ? entity.previous : entity.recipe.result).getEditorIcon();
    }

    @Override
    public boolean isSolidFor(Tile tile){
        BuildEntity entity = tile.entity();
        return entity == null || (entity.recipe != null && entity.recipe.result.solid) || entity.previous == null || entity.previous.solid;
    }

    @Override
    public CursorType getCursor(Tile tile){
        return CursorType.hand;
    }

    @Override
    public void tapped(Tile tile, Player player){
        BuildEntity entity = tile.entity();

        //if the target is constructible, begin constructing
        if(entity.recipe != null){
            player.clearBuilding();
            player.addBuildRequest(new BuildRequest(tile.x, tile.y, tile.getRotation(), entity.recipe));
        }
    }

    @Override
    public void setBars(){
        bars.replace(new BlockBar(BarType.health, true, tile -> tile.<BuildEntity>entity().progress));
    }

    @Override
    public void onDestroyed(Tile tile){
        Effects.effect(ExplosionFx.blockExplosionSmoke, tile);

        if(!tile.floor().solid && !tile.floor().isLiquid){
            RubbleDecal.create(tile.drawx(), tile.drawy(), size);
        }
    }

    @Override
    public void draw(Tile tile){
        BuildEntity entity = tile.entity();

        //When breaking, don't draw the previous block... since it's the thing you were breaking
        if(entity.recipe != null && entity.previous == entity.recipe.result){
            return;
        }

        if(entity.previous == null) return;

        for(TextureRegion region : entity.previous.getBlockIcon()){
            Draw.rect(region, tile.drawx(), tile.drawy(), entity.previous.rotate ? tile.getRotation() * 90 : 0);
        }
    }

    @Override
    public void drawLayer(Tile tile){

        BuildEntity entity = tile.entity();

        Shaders.blockbuild.color = Palette.accent;

        Block target = entity.recipe == null ? entity.previous : entity.recipe.result;

        if(target == null) return;

        for(TextureRegion region : target.getBlockIcon()){
            Shaders.blockbuild.region = region;
            Shaders.blockbuild.progress = entity.progress;
            Shaders.blockbuild.apply();

            Draw.rect(region, tile.drawx(), tile.drawy(), target.rotate ? tile.getRotation() * 90 : 0);

            Graphics.flush();
        }
    }

    @Override
    public void drawShadow(Tile tile){
        BuildEntity entity = tile.entity();

        Recipe recipe = entity.recipe;
        Block previous = entity.previous;

        if(recipe != null){
            Draw.rect(recipe.result.shadowRegion, tile.drawx(), tile.drawy());
        }else if(previous != null && !(previous instanceof BuildBlock)){
            previous.drawShadow(tile);
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
        public Recipe recipe;

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

        public void construct(Unit builder, TileEntity core, float amount){
            if(recipe == null){
                damage(99999);
                return;
            }

            float maxProgress = checkRequired(core.items, amount, false);

            for(int i = 0; i < recipe.requirements.length; i++){
                accumulator[i] += Math.min(recipe.requirements[i].amount * maxProgress, recipe.requirements[i].amount - totalAccumulator[i] + 0.00001f); //add min amount progressed to the accumulator
                totalAccumulator[i] = Math.min(totalAccumulator[i] + recipe.requirements[i].amount * maxProgress, recipe.requirements[i].amount);
            }

            maxProgress = checkRequired(core.items, maxProgress, true);

            progress = Mathf.clamp(progress + maxProgress);

            if(builder instanceof Player){
                builderID = builder.getID();
            }
            
            if(progress >= 1f || state.mode.infiniteResources){
                Call.onConstructFinish(tile, recipe.result, builderID, tile.getRotation(), builder.getTeam());
            }
        }

        public void deconstruct(Unit builder, TileEntity core, float amount){
            Recipe recipe = Recipe.getByResult(previous);

            if(recipe != null){
                ItemStack[] requirements = recipe.requirements;
                if(requirements.length != accumulator.length || totalAccumulator.length != requirements.length){
                    setDeconstruct(previous);
                }

                for(int i = 0; i < requirements.length; i++){
                    accumulator[i] += Math.min(requirements[i].amount * amount / 2f, requirements[i].amount/2f - totalAccumulator[i]); //add scaled amount progressed to the accumulator
                    totalAccumulator[i] = Math.min(totalAccumulator[i] + requirements[i].amount * amount / 2f, requirements[i].amount);

                    int accumulated = (int) (accumulator[i]); //get amount

                    if(amount > 0 && accumulated > 0){ //if it's positive, add it to the core
                        int accepting = core.tile.block().acceptStack(requirements[i].item, accumulated, core.tile, builder);
                        core.tile.block().handleStack(requirements[i].item, accepting, core.tile, builder);

                        accumulator[i] -= accepting;
                    }
                }
            }

            progress = Mathf.clamp(progress - amount);

            if(progress <= 0 || state.mode.infiniteResources){
                Call.onDeconstructFinish(tile, this.recipe == null ? previous : this.recipe.result);
            }
        }

        private float checkRequired(ItemModule inventory, float amount, boolean remove){
            float maxProgress = amount;

            for(int i = 0; i < recipe.requirements.length; i++){
                int required = (int) (accumulator[i]); //calculate items that are required now

                if(inventory.get(recipe.requirements[i].item) == 0){
                    maxProgress = 0f;
                }else if(required > 0){ //if this amount is positive...
                    //calculate how many items it can actually use
                    int maxUse = Math.min(required, inventory.get(recipe.requirements[i].item));
                    //get this as a fraction
                    float fraction = maxUse / (float) required;

                    //move max progress down if this fraction is less than 1
                    maxProgress = Math.min(maxProgress, maxProgress * fraction);

                    accumulator[i] -= maxUse;

                    //remove stuff that is actually used
                    if(remove){
                        inventory.remove(recipe.requirements[i].item, maxUse);
                    }
                }
                //else, no items are required yet, so just keep going
            }

            return maxProgress;
        }

        public float progress(){
            return progress;
        }

        public void setConstruct(Block previous, Recipe recipe){
            this.recipe = recipe;
            this.previous = previous;
            this.accumulator = new float[recipe.requirements.length];
            this.totalAccumulator = new float[recipe.requirements.length];
            this.buildCost = recipe.cost;
        }

        public void setDeconstruct(Block previous){
            this.previous = previous;
            this.progress = 1f;
            if(Recipe.getByResult(previous) != null){
                this.recipe = Recipe.getByResult(previous);
                this.accumulator = new float[Recipe.getByResult(previous).requirements.length];
                this.totalAccumulator = new float[Recipe.getByResult(previous).requirements.length];
                this.buildCost = Recipe.getByResult(previous).cost;
            }else{
                this.buildCost = 20f; //default no-recipe build cost is 20
            }
        }

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeFloat(progress);
            stream.writeShort(previous == null ? -1 : previous.id);
            stream.writeShort(recipe == null ? -1 : recipe.result.id);

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
        public void read(DataInput stream) throws IOException{
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
            if(rid != -1) recipe = Recipe.getByResult(content.block(rid));

            if(recipe != null){
                buildCost = recipe.cost;
            }else{
                buildCost = 20f;
            }
        }
    }
}
