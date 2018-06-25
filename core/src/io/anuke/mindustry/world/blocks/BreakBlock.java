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
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.input.CursorType;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.world;

public class BreakBlock extends Block {
    private static final float decaySpeedScl = 6f;

    public BreakBlock(String name) {
        super(name);
        solidifes = true;
        update = true;
        size = Integer.parseInt(name.charAt(name.length()-1) + "");
        health = 1;
        layer = Layer.placement;
        consumesTap = true;
    }

    @Override
    public boolean isSolidFor(Tile tile) {
        BreakEntity entity = tile.entity();
        return entity.previous == null || entity.previous.solid;
    }

    @Override
    public CursorType getCursor(Tile tile) {
        return CursorType.hand;
    }

    @Override
    public void tapped(Tile tile, Player player) {
        player.clearBuilding();
        player.addBuildRequest(new BuildRequest(tile.x, tile.y));
    }

    @Override
    public void setBars(){
        bars.replace(new BlockBar(BarType.health, true, tile -> (float)tile.<BreakEntity>entity().progress));
    }

    @Override
    public void onDestroyed(Tile tile){
        Effects.effect(ExplosionFx.blockExplosionSmoke, tile);

        if(!tile.floor().solid && !tile.floor().isLiquid){
            RubbleDecal.create(tile.drawx(), tile.drawy(), size);
        }
    }

    @Override
    public void afterDestroyed(Tile tile, TileEntity e){
        BreakEntity entity = (BreakEntity)e;

        if(entity.previous.synthetic()){
            tile.setBlock(entity.previous);
        }
    }

    @Override
    public void draw(Tile tile){
    }

    @Override
    public void drawLayer(Tile tile) {
        BreakEntity entity = tile.entity();

        Shaders.blockbuild.color = Palette.remove;

        for(TextureRegion region : entity.previous.getBlockIcon()){
            Shaders.blockbuild.region = region;
            Shaders.blockbuild.progress = (float)(1f-entity.progress); //progress reversed
            Shaders.blockbuild.apply();

            Draw.rect(region, tile.drawx(), tile.drawy(), entity.previous.rotate ? tile.getRotation() * 90 : 0);

            Graphics.flush();
        }
    }

    @Override
    public void drawShadow(Tile tile) {
        BreakEntity entity = tile.entity();

        entity.previous.drawShadow(tile);
    }

    @Override
    public void update(Tile tile) {
        BreakEntity entity = tile.entity();

        if(entity.progress >= 1f){
            CallBlocks.onBreakFinish(tile);
        }else if(entity.progress < 0f){
            CallBlocks.onBreakDeath(tile);
        }
    }

    @Override
    public TileEntity getEntity() {
        return new BreakEntity();
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onBreakDeath(Tile tile){
        BreakEntity entity = tile.entity();

        Team team = tile.getTeam();
        tile.setBlock(entity.previous);
        tile.setTeam(team);
    }

    @Remote(called = Loc.server, in = In.blocks)
    public static void onBreakFinish(Tile tile){
        BreakEntity entity = tile.entity();

        Effects.effect(Fx.breakBlock, tile.drawx(), tile.drawy(), entity.previous.size);
        world.removeBlock(tile);
    }

    public class BreakEntity extends TileEntity{
        private double[] accumulator;

        public double progress = 0;
        public Block previous;
        public float breakTime;

        public void addProgress(TileEntity core, Unit unit, double add){
            Recipe recipe = Recipe.getByResult(previous);

            if(recipe != null) {
                ItemStack[] requirements = recipe.requirements;

                for (int i = 0; i < requirements.length; i++) {
                    accumulator[i] += requirements[i].amount * add / 2f; //add scaled amount progressed to the accumulator
                    int amount = (int) (accumulator[i]); //get amount

                    if (amount > 0) { //if it's positive, add it to the core
                        int accepting = core.tile.block().acceptStack(requirements[i].item, amount, core.tile, unit);
                        core.tile.block().handleStack(requirements[i].item, amount, core.tile, unit);

                        accumulator[i] -= accepting;
                    }
                }
            }

            progress += add;
        }

        public float progress(){
            return (float)progress;
        }

        public void set(Block previous){
            this.previous = previous;
            if(Recipe.getByResult(previous) != null){
                this.accumulator = new double[Recipe.getByResult(previous).requirements.length];
                this.breakTime = Recipe.getByResult(previous).cost;
            }else{
                this.breakTime = 20f;
            }
        }
    }
}
