package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.Streams.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Schematic.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.input.PlaceUtils.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import java.io.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.*;

/** Handles schematics.*/
public class Schematics implements Loadable{
    private static final byte[] header = {'m', 's', 'c', 'h'};
    private static final byte version = 0;

    private static final int padding = 2;
    private static final int resolution = 32;

    private OptimizedByteArrayOutputStream out = new OptimizedByteArrayOutputStream(1024);
    private Array<Schematic> all = new Array<>();
    private OrderedMap<Schematic, FrameBuffer> previews = new OrderedMap<>();
    private FrameBuffer shadowBuffer;

    public Schematics(){
        Events.on(DisposeEvent.class, e -> {
            previews.each((schem, m) -> m.dispose());
            previews.clear();
            shadowBuffer.dispose();
        });
    }

    @Override
    public void loadSync(){
        load();
    }

    /** Load all schematics in the folder immediately.*/
    public void load(){
        all.clear();

        for(FileHandle file : schematicDirectory.list()){
            loadFile(file);
        }

        platform.getWorkshopContent(Schematic.class).each(this::loadFile);

        all.sort();

        Core.app.post(() -> {
            shadowBuffer = new FrameBuffer(maxSchematicSize + padding + 8, maxSchematicSize + padding + 8);
        });
    }

    public void overwrite(Schematic target, Schematic newSchematic){
        if(previews.containsKey(target)){
            previews.get(target).dispose();
            previews.remove(target);
        }

        target.tiles.clear();
        target.tiles.addAll(newSchematic.tiles);
        target.width = newSchematic.width;
        target.height = newSchematic.height;
        newSchematic.tags.putAll(target.tags);
        newSchematic.file = target.file;

        try{
            write(newSchematic, target.file);
        }catch(Exception e){
            Log.err(e);
            ui.showException(e);
        }
    }

    private void loadFile(FileHandle file){
        if(!file.extension().equals(schematicExtension)) return;

        try{
            Schematic s = read(file);
            all.add(s);

            //external file from workshop
            if(!s.file.parent().equals(schematicDirectory)){
                s.tags.put("steamid", s.file.parent().name());
            }
        }catch(IOException e){
            Log.err(e);
        }
    }

    public Array<Schematic> all(){
        return all;
    }

    public void saveChanges(Schematic s){
        if(s.file != null){
            try{
                write(s, s.file);
            }catch(Exception e){
                ui.showException(e);
            }
        }
    }

    public void savePreview(Schematic schematic, FileHandle file){
        FrameBuffer buffer = getBuffer(schematic);
        Draw.flush();
        buffer.begin();
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, buffer.getWidth(), buffer.getHeight());
        file.writePNG(pixmap);
        buffer.end();
    }

    public Texture getPreview(Schematic schematic){
        return getBuffer(schematic).getTexture();
    }

    public boolean hasPreview(Schematic schematic){
        return previews.containsKey(schematic);
    }

    public FrameBuffer getBuffer(Schematic schematic){
        if(!previews.containsKey(schematic)){
            Draw.blend();
            Draw.reset();
            Tmp.m1.set(Draw.proj());
            Tmp.m2.set(Draw.trans());
            FrameBuffer buffer = new FrameBuffer((schematic.width + padding) * resolution, (schematic.height + padding) * resolution);

            shadowBuffer.beginDraw(Color.clear);

            Draw.trans().idt();
            Draw.proj().setOrtho(0, 0, shadowBuffer.getWidth(), shadowBuffer.getHeight());

            Draw.color();
            schematic.tiles.each(t -> {
                int size = t.block.size;
                int offsetx = -(size - 1) / 2;
                int offsety = -(size - 1) / 2;
                for(int dx = 0; dx < size; dx++){
                    for(int dy = 0; dy < size; dy++){
                        int wx = t.x + dx + offsetx;
                        int wy = t.y + dy + offsety;
                        Fill.square(padding/2f + wx + 0.5f, padding/2f + wy + 0.5f, 0.5f);
                    }
                }
            });

            shadowBuffer.endDraw();

            buffer.beginDraw(Color.clear);

            Draw.proj().setOrtho(0, buffer.getHeight(), buffer.getWidth(), -buffer.getHeight());

            Tmp.tr1.set(shadowBuffer.getTexture(), 0, 0, schematic.width + padding, schematic.height + padding);
            Draw.color(0f, 0f, 0f, 1f);
            Draw.rect(Tmp.tr1, buffer.getWidth()/2f, buffer.getHeight()/2f, buffer.getWidth(), -buffer.getHeight());
            Draw.color();

            Array<BuildRequest> requests = schematic.tiles.map(t -> new BuildRequest(t.x, t.y, t.rotation, t.block).configure(t.config));

            Draw.flush();
            //scale each request to fit schematic
            Draw.trans().scale(resolution / tilesize, resolution / tilesize).translate(tilesize*1.5f, tilesize*1.5f);

            //draw requests
            requests.each(req -> {
                req.animScale = 1f;
                req.worldContext = false;
                req.block.drawRequestRegion(req, requests::each);
            });

            requests.each(req -> req.block.drawRequestConfigTop(req, requests::each));

            Draw.flush();
            Draw.trans().idt();

            buffer.endDraw();

            Draw.proj(Tmp.m1);
            Draw.trans(Tmp.m2);

            previews.put(schematic, buffer);
        }

        return previews.get(schematic);
    }

    /** Creates an array of build requests from a schematic's data, centered on the provided x+y coordinates. */
    public Array<BuildRequest> toRequests(Schematic schem, int x, int y){
        return schem.tiles.map(t -> new BuildRequest(t.x + x - schem.width/2, t.y + y - schem.height/2, t.rotation, t.block).original(t.x, t.y, schem.width, schem.height).configure(t.config)).removeAll(s -> !s.block.isVisible());
    }

    /** Adds a schematic to the list, also copying it into the files.*/
    public void add(Schematic schematic){
        all.add(schematic);
        try{
            FileHandle file = schematicDirectory.child(Time.millis() + "." + schematicExtension);
            write(schematic, file);
            schematic.file = file;
        }catch(Exception e){
            ui.showException(e);
            Log.err(e);
        }
    }

    public void remove(Schematic s){
        all.remove(s);
        if(s.file != null){
            s.file.delete();
        }

        if(previews.containsKey(s)){
            previews.get(s).dispose();
            previews.remove(s);
        }
    }

    /** Creates a schematic from a world selection. */
    public Schematic create(int x, int y, int x2, int y2){
        NormalizeResult result = PlaceUtils.normalizeArea(x, y, x2, y2, 0, false, maxSchematicSize);
        x = result.x;
        y = result.y;
        x2 = result.x2;
        y2 = result.y2;

        int ox = x, oy = y, ox2 = x2, oy2 = y2;

        Array<Stile> tiles = new Array<>();

        int minx = x2, miny = y2, maxx = x, maxy = y;
        boolean found = false;
        for(int cx = x; cx <= x2; cx++){
            for(int cy = y; cy <= y2; cy++){
                Tile linked = world.ltile(cx, cy);

                if(linked != null && linked.entity != null && linked.entity.block.isVisible()){
                    int top = linked.block().size/2;
                    int bot = linked.block().size % 2 == 1 ? -linked.block().size/2 : -(linked.block().size - 1)/2;
                    minx = Math.min(linked.x + bot, minx);
                    miny = Math.min(linked.y + bot, miny);
                    maxx = Math.max(linked.x + top, maxx);
                    maxy = Math.max(linked.y + top, maxy);
                    found = true;
                }
            }
        }

        if(found){
            x = minx;
            y = miny;
            x2 = maxx;
            y2 = maxy;
        }else{
            return new Schematic(new Array<>(), new StringMap(), 1, 1);
        }

        int width = x2 - x + 1, height = y2 - y + 1;
        int offsetX = -x, offsetY = -y;
        IntSet counted = new IntSet();
        for(int cx = ox; cx <= ox2; cx++){
            for(int cy = oy; cy <= oy2; cy++){
                Tile tile = world.ltile(cx, cy);

                if(tile != null && tile.entity != null && !counted.contains(tile.pos())){
                    int config = tile.entity.config();
                    if(tile.block().posConfig){
                        config = Pos.get(Pos.x(config) + offsetX, Pos.y(config) + offsetY);
                    }

                    tiles.add(new Stile(tile.block(), tile.x + offsetX, tile.y + offsetY, config, tile.rotation()));
                    counted.add(tile.pos());
                }
            }
        }

        return new Schematic(tiles, new StringMap(), width, height);
    }

    /** Converts a schematic to base64. Note that the result of this will always start with 'bXNjaAB'.*/
    public String writeBase64(Schematic schematic){
        try{
            out.reset();
            write(schematic, out);
            return new String(Base64Coder.encode(out.getBuffer(), out.size()));
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Loads a schematic from base64. May throw an exception. */
    public Schematic readBase64(String schematic) throws IOException{
        return read(new ByteArrayInputStream(Base64Coder.decode(schematic)));
    }

    //region IO methods

    public static Schematic read(FileHandle file) throws IOException{
        Schematic s = read(new DataInputStream(file.read(1024)));
        if(!s.tags.containsKey("name")){
            s.tags.put("name", file.nameWithoutExtension());
        }
        s.file = file;
        return s;
    }

    public static Schematic read(InputStream input) throws IOException{
        for(byte b : header){
            if(input.read() != b){
                throw new IOException("Not a schematic file (missing header).");
            }
        }

        int ver;
        if((ver = input.read()) != version){
            throw new IOException("Unknown version: " + ver);
        }

        try(DataInputStream stream = new DataInputStream(new InflaterInputStream(input))){
            short width = stream.readShort(), height = stream.readShort();

            StringMap map = new StringMap();
            byte tags = stream.readByte();
            for(int i = 0; i < tags; i++){
                map.put(stream.readUTF(), stream.readUTF());
            }

            IntMap<Block> blocks = new IntMap<>();
            byte length = stream.readByte();
            for(int i = 0; i < length; i++){
                Block block = Vars.content.getByName(ContentType.block, stream.readUTF());
                blocks.put(i, block == null ? Blocks.air : block);
            }

            int total = stream.readInt();
            Array<Stile> tiles = new Array<>(total);
            for(int i = 0; i < total; i++){
                Block block = blocks.get(stream.readByte());
                int position = stream.readInt();
                int config = stream.readInt();
                byte rotation = stream.readByte();
                if(block != Blocks.air){
                    tiles.add(new Stile(block, Pos.x(position), Pos.y(position), config, rotation));
                }
            }

            return new Schematic(tiles, map, width, height);
        }
    }

    public static void write(Schematic schematic, FileHandle file) throws IOException{
        write(schematic, file.write(false, 1024));
    }

    public static void write(Schematic schematic, OutputStream output) throws IOException{
        output.write(header);
        output.write(version);

        try(DataOutputStream stream = new DataOutputStream(new DeflaterOutputStream(output))){

            stream.writeShort(schematic.width);
            stream.writeShort(schematic.height);

            stream.writeByte(schematic.tags.size);
            for(ObjectMap.Entry<String, String> e : schematic.tags.entries()){
                stream.writeUTF(e.key);
                stream.writeUTF(e.value);
            }

            OrderedSet<Block> blocks = new OrderedSet<>();
            schematic.tiles.each(t -> blocks.add(t.block));

            //create dictionary
            stream.writeByte(blocks.size);
            for(int i = 0; i < blocks.size; i++){
                stream.writeUTF(blocks.orderedItems().get(i).name);
            }

            stream.writeInt(schematic.tiles.size);
            //write each tile
            for(Stile tile : schematic.tiles){
                stream.writeByte(blocks.orderedItems().indexOf(tile.block));
                stream.writeInt(Pos.get(tile.x, tile.y));
                stream.writeInt(tile.config);
                stream.writeByte(tile.rotation);
            }
        }
    }

    //endregion
}
