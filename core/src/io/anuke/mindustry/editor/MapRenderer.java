package io.anuke.mindustry.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.MapTileData.DataPosition;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.IndexedRenderer;
import io.anuke.ucore.util.Structs;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Geometry;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer implements Disposable{
    private static final int chunksize = 64;
    private IndexedRenderer[][] chunks;
    private IntSet updates = new IntSet();
    private IntSet delayedUpdates = new IntSet();
    private MapEditor editor;
    private int width, height;
    private Color tmpColor = Color.WHITE.cpy();

    public MapRenderer(MapEditor editor){
        this.editor = editor;
    }

    public void resize(int width, int height){
        if(chunks != null){
            for(int x = 0; x < chunks.length; x++){
                for(int y = 0; y < chunks[0].length; y++){
                    chunks[x][y].dispose();
                }
            }
        }

        chunks = new IndexedRenderer[(int) Math.ceil((float) width / chunksize)][(int) Math.ceil((float) height / chunksize)];

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                chunks[x][y] = new IndexedRenderer(chunksize * chunksize * 2);
            }
        }
        this.width = width;
        this.height = height;
        updateAll();
    }


    public void draw(float tx, float ty, float tw, float th){
        Graphics.end();

        IntSetIterator it = updates.iterator();
        while(it.hasNext){
            int i = it.next();
            int x = i % width;
            int y = i / width;
            render(x, y);
        }
        updates.clear();

        updates.addAll(delayedUpdates);
        delayedUpdates.clear();

        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                IndexedRenderer mesh = chunks[x][y];

                if(mesh == null){
                    chunks[x][y] = new IndexedRenderer(chunksize * chunksize * 2);
                    mesh = chunks[x][y];
                }

                mesh.getTransformMatrix().setToTranslation(tx, ty, 0).scl(tw / (width * tilesize),
                        th / (height * tilesize), 1f);
                mesh.setProjectionMatrix(Core.batch.getProjectionMatrix());

                mesh.render(Core.atlas.getTextures().first());
            }
        }

        Graphics.begin();
    }

    public void updatePoint(int x, int y){
        //TODO spread out over multiple frames?
        updates.add(x + y * width);
    }

    public void updateAll(){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                render(x, y);
            }
        }
    }

    private void render(int wx, int wy){
        int x = wx / chunksize, y = wy / chunksize;
        IndexedRenderer mesh = chunks[x][y];
        //TileDataMarker data = editor.getMap().readAt(wx, wy);
        byte bf = editor.getMap().read(wx, wy, DataPosition.floor);
        byte bw = editor.getMap().read(wx, wy, DataPosition.wall);
        byte btr = editor.getMap().read(wx, wy, DataPosition.rotationTeam);
        byte elev = editor.getMap().read(wx, wy, DataPosition.elevation);
        byte rotation = Bits.getLeftByte(btr);
        Team team = Team.all[Bits.getRightByte(btr)];

        Block floor = content.block(bf);
        Block wall = content.block(bw);

        TextureRegion region;

        if(bw != 0){
            region = wall.getEditorIcon();

            if(wall.rotate){
                mesh.draw((wx % chunksize) + (wy % chunksize) * chunksize, region,
                        wx * tilesize + wall.offset(), wy * tilesize + wall.offset(),
                        region.getRegionWidth(), region.getRegionHeight(), rotation * 90 - 90);
            }else{
                mesh.draw((wx % chunksize) + (wy % chunksize) * chunksize, region,
                        wx * tilesize + wall.offset() + (tilesize - region.getRegionWidth())/2f,
                        wy * tilesize + wall.offset() + (tilesize - region.getRegionHeight())/2f,
                        region.getRegionWidth(), region.getRegionHeight());
            }
        }else{
            region = floor.getEditorIcon();

            mesh.draw((wx % chunksize) + (wy % chunksize) * chunksize, region, wx * tilesize, wy * tilesize, 8, 8);
        }

        boolean check = checkElevation(elev, wx, wy);

        if(wall.update || wall.destructible){
            mesh.setColor(team.color);
            region = Draw.region("block-border");
        }else if(elev > 0 && check){
            mesh.setColor(tmpColor.fromHsv((360f * elev / 127f * 4f) % 360f, 0.5f + (elev / 4f) % 0.5f, 1f));
            region = Draw.region("block-elevation");
        }else if(elev == -1){
            region = Draw.region("block-slope");
        }else{
            region = Draw.region("clear");
        }

        mesh.draw((wx % chunksize) + (wy % chunksize) * chunksize + chunksize * chunksize, region,
                wx * tilesize - (wall.size/3) * tilesize, wy * tilesize - (wall.size/3) * tilesize,
                region.getRegionWidth(), region.getRegionHeight());
        mesh.setColor(Color.WHITE);
    }

    private boolean checkElevation(byte elev, int x, int y){
        for(GridPoint2 p : Geometry.d4){
            int wx = x + p.x, wy = y + p.y;
            if(!Structs.inBounds(wx, wy, editor.getMap().width(), editor.getMap().height())){
                return true;
            }
            byte value = editor.getMap().read(wx, wy, DataPosition.elevation);

            if(value < elev){
                return true;
            }else if(value > elev){
                delayedUpdates.add(wx + wy * width);
            }
        }
        return false;
    }

    @Override
    public void dispose(){
        if(chunks == null){
            return;
        }
        for(int x = 0; x < chunks.length; x++){
            for(int y = 0; y < chunks[0].length; y++){
                if(chunks[x][y] != null){
                    chunks[x][y].dispose();
                }
            }
        }
    }
}
