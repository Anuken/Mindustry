package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;

public class BuildBlock extends Block {
    private static final float buildTime = 120f;

    public BuildBlock(String name) {
        super(name);
        solid = true;
        update = true;
        size = Integer.parseInt(name.charAt(name.length()-1) + "");
        health = 1;
        layer = Layer.placement;
    }

    @Override
    public void draw(Tile tile){

    }

    @Override
    public void drawLayer(Tile tile) {
        BuildEntity entity = tile.entity();

        Shaders.inline.color = Colors.get("accent");

        for(TextureRegion region : entity.result.getBlockIcon()){
            Shaders.inline.region = region;
            Shaders.inline.progress = entity.progress;
            Shaders.inline.apply();
            Draw.rect(region, tile.drawx(), tile.drawy());
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
        entity.progress += 1f/buildTime;
        if(entity.progress > 1f){
            Team team = tile.getTeam();
            tile.setBlock(entity.result);
            tile.setTeam(team);
        }
    }

    @Override
    public TileEntity getEntity() {
        return new BuildEntity();
    }

    public class BuildEntity extends TileEntity{
        public Block result;
        public float progress;
    }
}
