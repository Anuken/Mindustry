package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.OreBlock;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.world;

public class SelectionTable extends Table{
    Tile lastTile;

    public SelectionTable(){
        super("clear");

        margin(5f);

        update(() -> {
            Block result;
            Tile tile = world.tileWorld(Graphics.mouseWorld().x, Graphics.mouseWorld().y);
            if(tile != null){
                tile = tile.target();
                result = tile.block().synthetic() ? tile.block() : tile.floor() instanceof OreBlock ? tile.floor() : null;
            }else{
                result = null;
            }

            if(result != null){
                lastTile = tile;
            }

            getTranslation().y = Mathf.lerp(getTranslation().y, result == null ? -getHeight() : 0f, 0.2f);
        });

        Image image = new Image(new TextureRegionDrawable(new TextureRegion(Draw.region("clear"))));
        image.update(() ->
            ((TextureRegionDrawable)image.getDrawable()).setRegion(lastTile == null ? Draw.getBlankRegion() :
            (lastTile.block().synthetic() ? lastTile.block() : lastTile.floor()).getDisplayIcon(lastTile)));

        add(image).size(8*5).padRight(4);
        label(() -> lastTile == null ? "" : (lastTile.block().synthetic() ? lastTile.block() : lastTile.floor()).getDisplayName(lastTile));

        pack();
        getTranslation().y = - getHeight();
    }
}
