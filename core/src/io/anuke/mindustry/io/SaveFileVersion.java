package io.anuke.mindustry.io;

import io.anuke.arc.collection.*;
import io.anuke.arc.collection.ObjectMap.Entry;
import io.anuke.arc.util.Pack;
import io.anuke.arc.util.io.ReusableByteOutStream;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.Serialization;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import java.io.*;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.world;

/**
 * Format:
 *
 * Everything is compressed. Use a DeflaterStream to begin reading.
 *
 * 1. version of format / int
 * 2. meta tags
 *  - length / short
 *  - continues with (string, string) pairs indicating key, value
 */
public abstract class SaveFileVersion{
    public final int version;

    private final ReusableByteOutStream byteOutput = new ReusableByteOutStream();
    private final DataOutputStream dataBytes = new DataOutputStream(byteOutput);
    private final ObjectMap<String, String> fallback = ObjectMap.of(
        "alpha-dart-mech-pad", "dart-mech-pad"
    );

    public SaveFileVersion(int version){
        this.version = version;
    }

    /** Write a chunk of input to the stream. An integer of some length is written first, followed by the data. */
    public void writeChunk(DataOutput output, boolean isByte, IORunner<DataOutput> runner) throws IOException{
        //reset output position
        byteOutput.position(0);
        //writer the needed info
        runner.accept(dataBytes);
        int length = byteOutput.position();
        //write length (either int or byte) followed by the output bytes
        if(!isByte){
            output.writeInt(length);
        }else{
            if(length > 255){
                throw new IOException("Byte write length exceeded: " + length + " > 255");
            }
            output.writeByte(length);
        }
        output.write(byteOutput.getBytes(), 0, length);
    }

    /** Reads a chunk of some length. Use the runner for reading to catch more descriptive errors. */
    public int readChunk(DataInput input, boolean isByte, IORunner<DataInput> runner) throws IOException{
        int length = isByte ? input.readUnsignedByte() : input.readInt();
        //TODO descriptive error with chunk name
        runner.accept(input);
        return length;
    }

    /** Skip a chunk completely. */
    public void skipChunk(DataInput input, boolean isByte) throws IOException{
        int length = readChunk(input, isByte, t -> {});
        int skipped = input.skipBytes(length);
        if(length != skipped){
            throw new IOException("Could not skip bytes. Expected length: " + length + "; Actual length: " + skipped);
        }
    }

    public SaveMeta getData(DataInputStream stream) throws IOException{
        long time = stream.readLong();
        long playtime = stream.readLong();
        int build = stream.readInt();

        Rules rules = Serialization.readRulesStreamJson(stream);
        String map = stream.readUTF();
        int wave = stream.readInt();
        return new SaveMeta(version, time, playtime, build, map, wave, rules);
    }

    public void writeMeta(DataOutputStream stream, ObjectMap<String, String> map) throws IOException{
        stream.writeShort(map.size);
        for(Entry<String, String> entry : map.entries()){
            stream.writeUTF(entry.key);
            stream.writeUTF(entry.value);
        }
    }

    public StringMap readMeta(DataInputStream stream) throws IOException{
        StringMap map = new StringMap();
        short size = stream.readShort();
        for(int i = 0; i < size; i++){
            map.put(stream.readUTF(), stream.readUTF());
        }
        return map;
    }

    public void writeMap(DataOutputStream stream) throws IOException{
        //write world size
        stream.writeShort(world.width());
        stream.writeShort(world.height());

        //floor first
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i % world.width(), i / world.width());
            stream.writeByte(tile.getFloorID());
            stream.writeByte(tile.getOverlayID());
            int consecutives = 0;

            for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                Tile nextTile = world.tile(j % world.width(), j / world.width());

                if(nextTile.getFloorID() != tile.getFloorID() || nextTile.getOverlayID() != tile.getOverlayID()){
                    break;
                }

                consecutives++;
            }

            stream.writeByte(consecutives);
            i += consecutives;
        }

        //blocks
        for(int i = 0; i < world.width() * world.height(); i++){
            Tile tile = world.tile(i % world.width(), i / world.width());
            stream.writeByte(tile.getBlockID());

            if(tile.block() == Blocks.part){
                stream.writeByte(tile.getLinkByte());
            }else if(tile.entity != null){
                stream.writeByte(Pack.byteByte(tile.getTeamID(), tile.getRotation())); //team + rotation
                stream.writeShort((short)tile.entity.health); //health

                if(tile.entity.items != null) tile.entity.items.write(stream);
                if(tile.entity.power != null) tile.entity.power.write(stream);
                if(tile.entity.liquids != null) tile.entity.liquids.write(stream);
                if(tile.entity.cons != null) tile.entity.cons.write(stream);

                tile.entity.write(stream);
            }else{
                //write consecutive non-entity blocks
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.tile(j % world.width(), j / world.width());

                    if(nextTile.block() != tile.block()){
                        break;
                    }

                    consecutives++;
                }

                stream.writeByte(consecutives);
                i += consecutives;
            }
        }
    }

    public void readMap(DataInputStream stream) throws IOException{
        short width = stream.readShort();
        short height = stream.readShort();

        world.beginMapLoad();

        Tile[][] tiles = world.createTiles(width, height);

        //read floor and create tiles first
        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            byte floorid = stream.readByte();
            byte oreid = stream.readByte();
            int consecutives = stream.readUnsignedByte();
            Block ore = content.block(oreid);

            tiles[x][y] = new Tile(x, y, floorid, (byte)0);
            tiles[x][y].setOverlay(ore);

            for(int j = i + 1; j < i + 1 + consecutives; j++){
                int newx = j % width, newy = j / width;
                Tile newTile = new Tile(newx, newy, floorid, (byte)0);
                newTile.setOverlay(ore);
                tiles[newx][newy] = newTile;
            }

            i += consecutives;
        }

        //read blocks
        for(int i = 0; i < width * height; i++){
            int x = i % width, y = i / width;
            Block block = content.block(stream.readByte());
            Tile tile = tiles[x][y];
            tile.setBlock(block);

            if(block == Blocks.part){
                tile.setLinkByte(stream.readByte());
            }else if(tile.entity != null){
                byte tr = stream.readByte();
                short health = stream.readShort();

                byte team = Pack.leftByte(tr);
                byte rotation = Pack.rightByte(tr);

                tile.setTeam(Team.all[team]);
                tile.entity.health = health;
                tile.setRotation(rotation);

                if(tile.entity.items != null) tile.entity.items.read(stream);
                if(tile.entity.power != null) tile.entity.power.read(stream);
                if(tile.entity.liquids != null) tile.entity.liquids.read(stream);
                if(tile.entity.cons != null) tile.entity.cons.read(stream);

                tile.entity.read(stream);
            }else{
                int consecutives = stream.readUnsignedByte();

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    tiles[newx][newy].setBlock(block);
                }

                i += consecutives;
            }
        }

        content.setTemporaryMapper(null);
        world.endMapLoad();
    }

    public void writeEntities(DataOutputStream stream) throws IOException{
        int groups = 0;

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                groups++;
            }
        }

        stream.writeByte(groups);

        for(EntityGroup<?> group : Entities.getAllGroups()){
            if(!group.isEmpty() && group.all().get(0) instanceof SaveTrait){
                stream.writeInt(group.size());
                for(Entity entity : group.all()){
                    stream.writeByte(((SaveTrait)entity).getTypeID());
                    ((SaveTrait)entity).writeSave(stream);
                }
            }
        }
    }

    public void readEntities(DataInputStream stream) throws IOException{
        byte groups = stream.readByte();

        for(int i = 0; i < groups; i++){
            int amount = stream.readInt();
            for(int j = 0; j < amount; j++){
                byte typeid = stream.readByte();
                SaveTrait trait = (SaveTrait)TypeTrait.getTypeByID(typeid).get();
                trait.readSave(stream);
            }
        }
    }

    public MappableContent[][] readContentHeader(DataInputStream stream) throws IOException{

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

        return map;
    }

    public void writeContentHeader(DataOutputStream stream) throws IOException{
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

    public abstract void read(DataInputStream stream) throws IOException;

    public abstract void write(DataOutputStream stream) throws IOException;

    public interface IORunner<T>{
        void accept(T stream) throws IOException;
    }
}
