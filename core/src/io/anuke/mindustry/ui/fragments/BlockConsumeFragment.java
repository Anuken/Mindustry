package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.Consume;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.tilesize;

public class BlockConsumeFragment extends Fragment {
    private Table table;
    private boolean visible;

    @Override
    public void build(Group parent) {
        table = new Table();
        table.setVisible(() -> !state.is(State.menu) && visible);
        table.setTransform(true);
        parent.setTransform(true);
        parent.addChild(table);
    }

    public void show(Tile tile){
        ObjectSet<Consume> consumers = new ObjectSet<>();
        TileEntity entity = tile.entity;
        Block block = tile.block();

        //table.background("clear");
        rebuild(block, entity);
        visible = true;

        table.update(() -> {

            if(tile.entity == null){
                hide();
                return;
            }

            boolean rebuild = false;

            for(Consume c : block.consumes.array()){
                boolean valid = c.isOptional() || c.valid(block, entity);

                if(consumers.contains(c) == valid){
                    if(valid){
                        consumers.remove(c);
                    }else{
                        consumers.add(c);
                    }
                    rebuild = true;
                }
            }

            if(rebuild){
                rebuild(block, entity);
            }

            Vector2 v =  Graphics.screen(tile.drawx() - tile.block().size * tilesize/2f, tile.drawy() + tile.block().size * tilesize/2f);
            table.pack();
            table.setPosition(v.x, v.y, Align.topRight);
        });

        table.act(Gdx.graphics.getDeltaTime());
    }

    public void hide(){
        table.clear();
        table.update(() -> {});
        visible = false;
    }

    private void rebuild(Block block, TileEntity entity){
        table.clearChildren();

        for(Consume c : block.consumes.array()){
            if(!c.isOptional() && !c.valid(block, entity)){
                c.build(table);
                table.row();
            }
        }
    }
}
