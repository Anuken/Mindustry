package io.anuke.mindustry.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.PixmapPacker.Page;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData.DataPosition;
import io.anuke.mindustry.world.Block;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.IndexedRenderer;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.tilesize;

public class MapRenderer implements Disposable{
    private static final int chunksize = 64;
    private IndexedRenderer[][] chunks;
    private IntSet updates = new IntSet();
    private IntSet delayedUpdates = new IntSet();
    private MapEditor editor;
    private int width, height;
    private Color tmpColor = Color.WHITE.cpy();

    private ObjectMap<Block, TextureRegion> blockIcons = new ObjectMap<>();
    private ObjectMap<String, TextureRegion> regions = new ObjectMap<>();
    private Texture blockTexture;

    public MapRenderer(MapEditor editor){
        this.editor = editor;
        createTexture();
    }


    private void createTexture(){
        Timers.mark();

        PixmapPacker packer = new PixmapPacker(512, 512, Format.RGBA8888, 2, true);
        Pixmap pixmap = Core.atlas.getPixmapOf("blank");

        for(Block block : Block.all()){
            TextureRegion[] regions = block.getBlockIcon();
            if(regions.length > 0){
                Pixmap result = new Pixmap(regions[0].getRegionWidth(), regions[0].getRegionHeight(), Format.RGBA8888);
                for(TextureRegion region : regions){
                    result.drawPixmap(pixmap, 0, 0, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight());
                }

                packer.pack(block.name, result);
                result.dispose();
            }
        }

        add("clear", packer);
        add("block-border", packer);
        add("block-elevation", packer);

        if(packer.getPages().size > 1){
            throw new IllegalArgumentException("Pixmap packer may not have more than 1 page!");
        }

        Page page = packer.getPages().first();
        page.updateTexture(TextureFilter.Nearest, TextureFilter.Nearest, false);
        blockTexture = page.getTexture();
        for(String str : page.getRects().keys()){
            if(Block.getByName(str) == null) continue;
            Rectangle rect = page.getRects().get(str);
            blockIcons.put(Block.getByName(str),
                    new TextureRegion(blockTexture,
                            (int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height));
        }

        packer.dispose();

        Core.atlas.disposePixmaps();

        Log.info("Packing elapsed: {0}", Timers.elapsed());
    }

    private void add(String name, PixmapPacker packer){
        TextureRegion region = Draw.region(name);
        Pixmap result = new Pixmap(region.getRegionWidth(), region.getRegionHeight(), Format.RGBA8888);
        result.drawPixmap(Core.atlas.getPixmapOf(region), 0, 0, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight());
        Rectangle rect = packer.pack(name, result);
        result.dispose();
        Gdx.app.postRunnable(() -> regions.put(name, new TextureRegion(packer.getPages().first().getTexture(), (int)rect.x, (int)rect.y, (int)rect.width, (int)rect.height)));
    }

    public void resize(int width, int height){
        if(chunks != null){
            for(int x = 0; x < chunks.length; x ++){
                for(int y = 0; y < chunks[0].length; y ++){
                    chunks[x][y].dispose();
                }
            }
        }

        chunks = new IndexedRenderer[(int)Math.ceil((float)width/chunksize)][(int)Math.ceil((float)height/chunksize )];

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                chunks[x][y] = new IndexedRenderer(chunksize*chunksize*2);
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
            int y = i / height;
            render(x, y);
        }
        updates.clear();

        updates.addAll(delayedUpdates);
        delayedUpdates.clear();

        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                IndexedRenderer mesh = chunks[x][y];

                mesh.getTransformMatrix().setToTranslation(tx, ty, 0).scl(tw / (width * tilesize),
                        th / (height * tilesize), 1f);
                mesh.setProjectionMatrix(Core.batch.getProjectionMatrix());

                mesh.render(blockTexture);
            }
        }

        Graphics.begin();
    }

    public void updatePoint(int x, int y){
        //TODO spread out over multiple frames?
        updates.add(x + y*width);
    }

    public void updateAll(){
        for(int x = 0; x < width; x ++){
            for(int y = 0; y < height; y ++){
                render(x, y);
            }
        }
    }

    private void render(int wx, int wy){
        int x = wx/chunksize, y = wy/chunksize;
        IndexedRenderer mesh = chunks[x][y];
        //TileDataMarker data = editor.getMap().readAt(wx, wy);
        byte bf = editor.getMap().read(wx, wy, DataPosition.floor);
        byte bw = editor.getMap().read(wx, wy, DataPosition.wall);
        byte btr = editor.getMap().read(wx, wy, DataPosition.rotationTeam);
        byte elev = editor.getMap().read(wx, wy, DataPosition.elevation);
        byte rotation = Bits.getLeftByte(btr);
        Team team = Team.values()[Bits.getRightByte(btr)];

        Block floor = Block.getByID(bf);
        Block wall = Block.getByID(bw);

        int offsetx = -(wall.size-1)/2;
        int offsety = -(wall.size-1)/2;

        TextureRegion region;

        if(bw != 0) {
            region = blockIcons.get(wall, regions.get("clear"));

            if (wall.rotate) {
                mesh.draw((wx % chunksize) + (wy % chunksize) * chunksize, region,
                        wx * tilesize + offsetx * tilesize, wy * tilesize + offsety * tilesize,
                        region.getRegionWidth(), region.getRegionHeight(), rotation * 90 - 90);
            } else {
                mesh.draw((wx % chunksize) + (wy % chunksize) * chunksize, region,
                        wx * tilesize + offsetx * tilesize, wy * tilesize + offsety * tilesize,
                        region.getRegionWidth(), region.getRegionHeight());
            }
        }else{
            region = blockIcons.get(floor, regions.get("clear"));

            mesh.draw((wx % chunksize) + (wy % chunksize)*chunksize, region, wx * tilesize, wy * tilesize, 8, 8);
        }

        if(wall.update || wall.destructible) {
            mesh.setColor(team.color);
            region = regions.get("block-border");
        }else if(elev > 0 && checkElevation(elev, wx, wy)){
            mesh.setColor(tmpColor.fromHsv((360f * elev/127f * 4f) % 360f, 0.5f + (elev / 4f) % 0.5f, 1f));
            region = regions.get("block-elevation");
        }else{
            region = regions.get("clear");
        }

        mesh.draw((wx % chunksize) + (wy % chunksize)*chunksize + chunksize*chunksize, region,
                wx * tilesize + offsetx*tilesize, wy * tilesize  + offsety * tilesize,
                region.getRegionWidth(), region.getRegionHeight());
        mesh.setColor(Color.WHITE);
    }

    private boolean checkElevation(byte elev, int x, int y){
        for(GridPoint2 p : Geometry.d4){
            int wx = x + p.x, wy = y + p.y;
            if(!Mathf.inBounds(wx, wy, editor.getMap().width(), editor.getMap().height())){
                return true;
            }
            byte value = editor.getMap().read(wx, wy, DataPosition.elevation);

            if(value < elev){
                return true;
            }else if(value > elev){
                delayedUpdates.add(wx + wy*width);
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        if(chunks == null){
            return;
        }
        for(int x = 0; x < chunks.length; x ++){
            for(int y = 0; y < chunks[0].length; y ++){
                if(chunks[x][y] != null){
                    chunks[x][y].dispose();
                }
            }
        }
    }
}
