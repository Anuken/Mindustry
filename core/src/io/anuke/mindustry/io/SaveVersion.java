package io.anuke.mindustry.io;

import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Teams.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public abstract class SaveVersion extends SaveFileReader{
    public int version;

    //HACK stores the last read build of the save file, valid after read meta call
    protected int lastReadBuild;

    public SaveVersion(int version){
        this.version = version;
    }

    public SaveMeta getMeta(DataInput stream) throws IOException{
        stream.readInt(); //length of data, doesn't matter here
        StringMap map = readStringMap(stream);
        return new SaveMeta(map.getInt("version"), map.getLong("saved"), map.getLong("playtime"), map.getInt("build"), map.get("mapname"), map.getInt("wave"), JsonIO.read(Rules.class, map.get("rules", "{}")), map);
    }

    @Override
    public final void write(DataOutputStream stream) throws IOException{
        write(stream, new StringMap());
    }

    @Override
    public final void read(DataInputStream stream, CounterInputStream counter, WorldContext context) throws IOException{
        region("meta", stream, counter, this::readMeta);
        region("content", stream, counter, this::readContentHeader);

        try{
            region("map", stream, counter, in -> readMap(in, context));
            region("entities", stream, counter, this::readEntities);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public final void write(DataOutputStream stream, StringMap extraTags) throws IOException{
        region("meta", stream, out -> writeMeta(out, extraTags));
        region("content", stream, this::writeContentHeader);
        region("map", stream, this::writeMap);
        region("entities", stream, this::writeEntities);
    }

    public void writeMeta(DataOutput stream, StringMap tags) throws IOException{
        writeStringMap(stream, StringMap.of(
            "saved", Time.millis(),
            "playtime", headless ? 0 : control.saves.getTotalPlaytime(),
            "build", Version.build,
            "mapname", world.getMap() == null ? "unknown" : world.getMap().name(),
            "wave", state.wave,
            "wavetime", state.wavetime,
            "stats", JsonIO.write(state.stats),
            "rules", JsonIO.write(state.rules),
            "mods", JsonIO.write(mods.getModStrings().toArray(String.class)),
            "width", world.width(),
            "height", world.height()
        ).merge(tags));
    }

    public void readMeta(DataInput stream) throws IOException{
        StringMap map = readStringMap(stream);

        state.wave = map.getInt("wave");
        state.wavetime = map.getFloat("wavetime", state.rules.waveSpacing);
        state.stats = JsonIO.read(Stats.class, map.get("stats", "{}"));
        state.rules = JsonIO.read(Rules.class, map.get("rules", "{}"));
        if(state.rules.spawns.isEmpty()) state.rules.spawns = defaultWaves.get();
        lastReadBuild = map.getInt("build", -1);
        String[] mods = JsonIO.read(String[].class, map.get("mods", "[]"));

        Map worldmap = maps.byName(map.get("mapname", "\\\\\\"));
        world.setMap(worldmap == null ? new Map(StringMap.of(
            "name", map.get("mapname", "Unknown"),
            "width", 1,
            "height", 1
        )) : worldmap);
    }

    public void writeMap(DataOutput stream) throws IOException{
        //write world size
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        //floor + overlay
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.rawTile(i % world.width(), i / world.width());
            stream.writeShort(tile.floorID());
            stream.writeShort(tile.overlayID());
            int consecutives = 0;

            for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                Tile nextTile = world.rawTile(j % world.width(), j / world.width());

                if(nextTile.floorID() != tile.floorID() || nextTile.overlayID() != tile.overlayID()){
                    break;
                }

                consecutives++;
            }

            stream.writeByte(consecutives);
            i += consecutives;
        }

        //blocks
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.rawTile(i % world.width(), i / world.width());
            stream.writeShort(tile.blockID());

            if(tile.entity != null){
                writeChunk(stream, true, out -> {
                    out.writeByte(tile.entity.version());
                    tile.entity.write(out);
                });
            }else{
                //write consecutive non-entity blocks
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.rawTile(j % world.width(), j / world.width());

                    if(nextTile.blockID() != tile.blockID()){
                        break;
                    }

                    consecutives++;
                }

                stream.writeByte(consecutives);
                i += consecutives;
            }
        }
    }

    public void readMap(DataInput stream, WorldContext context) throws IOException{
        int width = stream.readUnsignedShort();
        int height = stream.readUnsignedShort();

        boolean generating = context.isGenerating();

        if(!generating) context.begin();
        try{

            context.resize(width, height);

            //read floor and create tiles first
            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                short floorid = stream.readShort();
                short oreid = stream.readShort();
                int consecutives = stream.readUnsignedByte();

                context.create(x, y, floorid, oreid, (short)0);

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    context.create(newx, newy, floorid, oreid, (short)0);
                }

                i += consecutives;
            }

            //read blocks
            for(int i = 0; i < width * height; i++){
                int x = i % width, y = i / width;
                Block block = content.block(stream.readShort());
                Tile tile = context.tile(x, y);
                tile.setBlock(block);

                if(tile.entity != null){
                    try{
                        readChunk(stream, true, in -> {
                            byte version = in.readByte();
                            tile.entity.read(in, version);
                        });
                    }catch(Exception e){
                        throw new IOException("Failed to read tile entity of block: " + block, e);
                    }
                }else{
                    int consecutives = stream.readUnsignedByte();

                    for(int j = i + 1; j < i + 1 + consecutives; j++){
                        int newx = j % width, newy = j / width;
                        context.tile(newx, newy).setBlock(block);
                    }

                    i += consecutives;
                }
            }
        }finally{
            if(!generating) context.end();
        }
    }

    public void writeEntities(DataOutput stream) throws IOException{
        //write team data with entities.
        Array<TeamData> data = state.teams.getActive();
        stream.writeInt(data.size);
        for(TeamData team : data){
            stream.writeInt(team.team.ordinal());
            stream.writeInt(team.brokenBlocks.size);
            for(BrokenBlock block : team.brokenBlocks){
                stream.writeShort(block.x);
                stream.writeShort(block.y);
                stream.writeShort(block.rotation);
                stream.writeShort(block.block);
                stream.writeInt(block.config);
            }
        }

        //write entity chunk
        int groups = 0;

        for(EntityGroup<?> group : entities.all()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                groups++;
            }
        }

        stream.writeByte(groups);

        for(EntityGroup<?> group : entities.all()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                stream.writeInt(group.size());
                for(Entity entity : group.all()){
                    SaveTrait save = (SaveTrait)entity;
                    //each entity is a separate chunk.
                    writeChunk(stream, true, out -> {
                        out.writeByte(save.getTypeID().id);
                        out.writeByte(save.version());
                        save.writeSave(out);
                    });
                }
            }
        }
    }

    public void readEntities(DataInput stream) throws IOException{
        int teamc = stream.readInt();
        for(int i = 0; i < teamc; i++){
            Team team = Team.all[stream.readInt()];
            TeamData data = state.teams.get(team);
            int blocks = stream.readInt();
            for(int j = 0; j < blocks; j++){
                data.brokenBlocks.addLast(new BrokenBlock(stream.readShort(), stream.readShort(), stream.readShort(), content.block(stream.readShort()).id, stream.readInt()));
            }
        }

        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                //TODO throw exception on read fail
                readChunk(stream, true, in -> {
                    byte typeid = in.readByte();
                    byte version = in.readByte();
                    SaveTrait trait = (SaveTrait)content.<TypeID>getByID(ContentType.typeid, typeid).constructor.get();
                    trait.readSave(in, version);
                });
            }
        }
    }

    public void readContentHeader(DataInput stream) throws IOException{
        byte mapped = stream.readByte();

        MappableContent[][] map = new MappableContent[ContentType.values().length][0];

        for(int i = 0; i < mapped; i++){
            ContentType type = ContentType.values()[stream.readByte()];
            short total = stream.readShort();
            map[type.ordinal()] = new MappableContent[total];

            for(int j = 0; j < total; j++){
                String name = stream.readUTF();
                map[type.ordinal()][j] = content.getByName(type, fallback.get(name, name));
            }
        }

        content.setTemporaryMapper(map);
    }

    public void writeContentHeader(DataOutput stream) throws IOException{
        Array<Content>[] map = content.getContentMap();

        int mappable = 0;
        for(Array<Content> arr : map){
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                mappable++;
            }
        }

        stream.writeByte(mappable);
        for(Array<Content> arr : map){
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                stream.writeByte(arr.first().getContentType().ordinal());
                stream.writeShort(arr.size);
                for(Content c : arr){
                    stream.writeUTF(((MappableContent)c).name);
                }
            }
        }
    }
}
