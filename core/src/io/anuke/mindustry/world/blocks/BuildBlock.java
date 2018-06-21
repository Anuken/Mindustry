package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Rubble;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.CursorType;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.modules.InventoryModule;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.threads;

public class BuildBlock extends Block {
    private static final float decaySpeedScl = 6f;

    public BuildBlock(String name) {
        super(name);
        solid = true;
        update = true;
        size = Integer.parseInt(name.charAt(name.length()-1) + "");
        health = 1;
        layer = Layer.placement;
        consumesTap = true;
    }

    @Override
    public CursorType getCursor(Tile tile) {
        return CursorType.hand;
    }

    @Override
    public void tapped(Tile tile, Player player) {
        BuildEntity entity = tile.entity();

        player.clearBuilding();
        player.addBuildRequest(new BuildRequest(tile.x, tile.y, tile.getRotation(), entity.recipe));
    }

    @Override
    public void setBars(){
        bars.replace(new BlockBar(BarType.health, true, tile -> (float)tile.<BuildEntity>entity().progress));
    }

    @Override
    public void onDestroyed(Tile tile){
        Effects.effect(ExplosionFx.blockExplosionSmoke, tile);

        if(!tile.floor().solid && !tile.floor().isLiquid){
            Rubble.create(tile.drawx(), tile.drawy(), size);
        }
    }

    @Override
    public void afterDestroyed(Tile tile, TileEntity e){
        BuildEntity entity = (BuildEntity)e;

        if(entity.previous.synthetic()){
            tile.setBlock(entity.previous);
        }
    }

    @Override
    public void draw(Tile tile){
        BuildEntity entity = tile.entity();

        if(entity.previous.synthetic()) {
            for (TextureRegion region : entity.previous.getBlockIcon()) {
                Draw.rect(region, tile.drawx(), tile.drawy(), entity.recipe.result.rotate ? tile.getRotation() * 90 : 0);
            }
        }
    }

    @Override
    public void drawLayer(Tile tile) {
        BuildEntity entity = tile.entity();

        Shaders.blockbuild.color = Palette.accent;

        for(TextureRegion region : entity.recipe.result.getBlockIcon()){
            Shaders.blockbuild.region = region;
            Shaders.blockbuild.progress = (float)entity.progress;
            Shaders.blockbuild.apply();

            Draw.rect(region, tile.drawx(), tile.drawy(), entity.recipe.result.rotate ? tile.getRotation() * 90 : 0);

            Graphics.flush();
        }
    }

    @Override
    public void drawShadow(Tile tile) {
        BuildEntity entity = tile.entity();

        entity.recipe.result.drawShadow(tile);
    }

    @Override
    public void update(Tile tile) {
        BuildEntity entity = tile.entity();

        if(entity.progress >= 1f){
            CallBlocks.onBuildFinish(tile, entity.lastBuilder);
        }else if(entity.progress < 0f){
            CallBlocks.onBuildDeath(tile);
        }

        if(!entity.updated){
            entity.progress -= 1f/entity.recipe.cost/decaySpeedScl;
        }

        entity.updated = false;
    }

    @Override
    public TileEntity getEntity() {
        return new BuildEntity();
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onBuildDeath(Tile tile){
        tile.entity.damage(tile.entity.health + 1);
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onBuildFinish(Tile tile, Player lastBuilder){
        BuildEntity entity = tile.entity();

        Team team = tile.getTeam();
        tile.setBlock(entity.recipe.result);
        tile.setTeam(team);
        Effects.effect(Fx.placeBlock, tile.drawx(), tile.drawy(), entity.recipe.result.size);

        //last builder was this local client player, call placed()
        if(lastBuilder != null && lastBuilder.isLocal){
            //this is run delayed, since if this is called on the server, all clients need to recieve the onBuildFinish()
            //event first before they can recieve the placed() event modification results
            threads.runDelay(() -> tile.block().placed(tile));
        }
    }

    public class BuildEntity extends TileEntity{
        public Recipe recipe;

        public double progress = 0;
        public Block previous;
        public Player lastBuilder;

        private double[] accumulator;
        private boolean updated;

        public void addProgress(InventoryModule inventory, double amount){
            double maxProgress = amount;

            for(int i = 0; i < recipe.requirements.length; i ++){
                accumulator[i] += recipe.requirements[i].amount*amount; //add amount progressed to the accumulator
                int required = (int)(accumulator[i]); //calculate items that are required now

                if(required > 0){ //if this amount is positive...
                    //calculate how many items it can actually use
                    int maxUse = Math.min(required, inventory.getItem(recipe.requirements[i].item));
                    //get this as a fraction
                    double fraction = maxUse / (double)required;

                    //move max progress down if this fraction is less than 1
                    maxProgress = Math.min(maxProgress, maxProgress*fraction);

                    //remove stuff that is actually used
                    accumulator[i] -= maxUse;
                    inventory.removeItem(recipe.requirements[i].item, maxUse);
                }
                //else, no items are required yet, so just keep going
            }

            progress += maxProgress;
            updated = true;
        }

        public float progress(){
            return (float)progress;
        }

        public void set(Block previous, Recipe recipe){
            updated = true;
            this.recipe = recipe;
            this.previous = previous;
            this.accumulator = new double[recipe.requirements.length];
        }
    }
}
