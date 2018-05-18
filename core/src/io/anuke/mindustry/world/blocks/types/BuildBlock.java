package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Rubble;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;

public class BuildBlock extends Block {
    private static final float decaySpeedScl = 4f;

    public BuildBlock(String name) {
        super(name);
        solid = true;
        update = true;
        size = Integer.parseInt(name.charAt(name.length()-1) + "");
        health = 1;
        layer = Layer.placement;
    }

    @Override
    public void setBars(){
        bars.replace(new BlockBar(BarType.health, true, tile -> tile.<BuildEntity>entity().progress));
    }

    @Override
    public void onDestroyed(Tile tile){
        Effects.effect(ExplosionFx.blockExplosionSmoke, tile);

        if(!tile.floor().solid && !tile.floor().liquid){
            Rubble.create(tile.drawx(), tile.drawy(), size);
        }
    }

    @Override
    public void draw(Tile tile){

    }

    @Override
    public void drawLayer(Tile tile) {
        BuildEntity entity = tile.entity();

        Shaders.blockbuild.color = Colors.get("accent");

        for(TextureRegion region : entity.result.getBlockIcon()){
            Shaders.blockbuild.region = region;
            Shaders.blockbuild.progress = entity.progress;
            Shaders.blockbuild.apply();

            Draw.rect(region, tile.drawx(), tile.drawy(), entity.result.rotate ? tile.getRotation() * 90 : 0);

            Graphics.flush();
        }
    }

    @Override
    public void drawShadow(Tile tile) {
        BuildEntity entity = tile.entity();

        entity.result.drawShadow(tile);
    }

    @Override
    public void update(Tile tile) {
        BuildEntity entity = tile.entity();
        entity.progress -= 1f/entity.result.health/decaySpeedScl;
        if(entity.progress > 1f){
            Team team = tile.getTeam();
            tile.setBlock(entity.result);
            tile.setTeam(team);
            Effects.effect(Fx.placeBlock, tile.drawx(), tile.drawy(), 0f, (float)size);
        }else if(entity.progress < 0f){
            entity.damage(entity.health + 1);
        }
    }

    @Override
    public TileEntity getEntity() {
        return new BuildEntity();
    }

    public class BuildEntity extends TileEntity{
        public Block result;
        public Recipe recipe;
        public float progress = 0.05f;
    }
}
