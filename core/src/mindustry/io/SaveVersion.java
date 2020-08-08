package mindustry.io;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

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
        return new SaveMeta(
            map.getInt("version"),
            map.getLong("saved"),
            map.getLong("playtime"),
            map.getInt("build"),
            map.get("mapname"),
            map.getInt("wave"),
            JsonIO.read(Rules.class, map.get("rules", "{}")),
            JsonIO.read(SectorInfo.class, map.get("secinfo", "{}")),
            map
        );
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
        //prepare campaign data for writing
        if(state.isCampaign()){
            state.secinfo.prepare();
        }

        //flush tech node progress
        for(TechNode node : TechTree.all){
            node.save();
        }

        writeStringMap(stream, StringMap.of(
            "saved", Time.millis(),
            "playtime", headless ? 0 : control.saves.getTotalPlaytime(),
            "build", Version.build,
            "mapname", state.map.name(),
            "wave", state.wave,
            "wavetime", state.wavetime,
            "stats", JsonIO.write(state.stats),
            "secinfo", state.isCampaign() ? JsonIO.write(state.secinfo) : "{}",
            "rules", JsonIO.write(state.rules),
            "mods", JsonIO.write(mods.getModStrings().toArray(String.class)),
            "width", world.width(),
            "height", world.height(),
            "viewpos", Tmp.v1.set(player == null ? Vec2.ZERO : player).toString(),
            "controlledType", headless || control.input.controlledType == null ? "null" : control.input.controlledType.name,
            "nocores", state.rules.defaultTeam.cores().isEmpty(),
            "playerteam", player == null ? state.rules.defaultTeam.id : player.team().id
        ).merge(tags));
    }

    public void readMeta(DataInput stream) throws IOException{
        StringMap map = readStringMap(stream);

        state.wave = map.getInt("wave");
        state.wavetime = map.getFloat("wavetime", state.rules.waveSpacing);
        state.stats = JsonIO.read(Stats.class, map.get("stats", "{}"));
        state.secinfo = JsonIO.read(SectorInfo.class, map.get("secinfo", "{}"));
        state.rules = JsonIO.read(Rules.class, map.get("rules", "{}"));
        if(state.rules.spawns.isEmpty()) state.rules.spawns = defaultWaves.get();
        lastReadBuild = map.getInt("build", -1);

        //load time spent on sector into state
        if(state.rules.sector != null){
            state.secinfo.internalTimeSpent = state.rules.sector.getStoredTimeSpent();
        }

        if(!headless){
            Tmp.v1.tryFromString(map.get("viewpos"));
            Core.camera.position.set(Tmp.v1);
            player.set(Tmp.v1);

            control.input.controlledType = content.getByName(ContentType.unit, map.get("controlledType", "<none>"));
            Team team = Team.get(map.getInt("playerteam", state.rules.defaultTeam.id));
            if(!net.client() && team != Team.derelict){
                player.team(team);
            }
        }

        Map worldmap = maps.byName(map.get("mapname", "\\\\\\"));
        state.map = worldmap == null ? new Map(StringMap.of(
            "name", map.get("mapname", "Unknown"),
            "width", 1,
            "height", 1
        )) : worldmap;
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

            boolean savedata = tile.block().saveData;
            byte packed = (byte)((tile.build != null ? 1 : 0) | (savedata ? 2 : 0));

            //make note of whether there was an entity/rotation here
            stream.writeByte(packed);

            //only write the entity for multiblocks once - in the center
            if(tile.build != null){
                if(tile.isCenter()){
                    stream.writeBoolean(true);
                    writeChunk(stream, true, out -> {
                        out.writeByte(tile.build.version());
                        tile.build.writeAll(Writes.get(out));
                    });
                }else{
                    stream.writeBoolean(false);
                }
            }else if(savedata){
                stream.writeByte(tile.data);
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
                if(content.block(floorid) == Blocks.air) floorid = Blocks.stone.id;

                context.create(x, y, floorid, oreid, (short)0);

                for(int j = i + 1; j < i + 1 + consecutives; j++){
                    int newx = j % width, newy = j / width;
                    context.create(newx, newy, floorid, oreid, (short)0);
                }

                i += consecutives;
            }

            //read blocks
            for(int i = 0; i < width * height; i++){
                Block block = content.block(stream.readShort());
                Tile tile = context.tile(i);
                if(block == null) block = Blocks.air;
                boolean isCenter = true;
                byte packedCheck = stream.readByte();
                boolean hadEntity = (packedCheck & 1) != 0;
                boolean hadData = (packedCheck & 2) != 0;

                if(hadEntity){
                    isCenter = stream.readBoolean();
                }

                //set block only if this is the center; otherwise, it's handled elsewhere
                if(isCenter){
                    tile.setBlock(block);
                }

                if(hadEntity){
                    if(isCenter){ //only read entity for center blocks
                        if(block.hasEntity()){
                            try{
                                readChunk(stream, true, in -> {
                                    byte revision = in.readByte();
                                    tile.build.readAll(Reads.get(in), revision);
                                });
                            }catch(Throwable e){
                                throw new IOException("Failed to read tile entity of block: " + block, e);
                            }
                        }else{
                            //skip the entity region, as the entity and its IO code are now gone
                            skipChunk(stream, true);
                        }
                    }
                }else if(hadData){
                    tile.setBlock(block);
                    tile.data = stream.readByte();
                }else{
                    int consecutives = stream.readUnsignedByte();

                    for(int j = i + 1; j < i + 1 + consecutives; j++){
                        context.tile(j).setBlock(block);
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
        Seq<TeamData> data = state.teams.getActive();
        stream.writeInt(data.size);
        for(TeamData team : data){
            stream.writeInt(team.team.id);
            stream.writeInt(team.blocks.size);
            for(BlockPlan block : team.blocks){
                stream.writeShort(block.x);
                stream.writeShort(block.y);
                stream.writeShort(block.rotation);
                stream.writeShort(block.block);
                TypeIO.writeObject(Writes.get(stream), block.config);
            }
        }

        stream.writeInt(Groups.all.count(Entityc::serialize));
        for(Entityc entity : Groups.all){
            if(!entity.serialize()) continue;

            writeChunk(stream, true, out -> {
                out.writeByte(entity.classId());
                entity.write(Writes.get(out));
            });
        }
    }

    public void readEntities(DataInput stream) throws IOException{
        int teamc = stream.readInt();
        for(int i = 0; i < teamc; i++){
            Team team = Team.get(stream.readInt());
            TeamData data = team.data();
            int blocks = stream.readInt();
            for(int j = 0; j < blocks; j++){
                data.blocks.addLast(new BlockPlan(stream.readShort(), stream.readShort(), stream.readShort(), content.block(stream.readShort()).id, TypeIO.readObject(Reads.get(stream))));
            }
        }

        int amount = stream.readInt();
        for(int j = 0; j < amount; j++){
            readChunk(stream, true, in -> {
                byte typeid = in.readByte();
                Entityc entity = (Entityc)EntityMapping.map(typeid).get();
                entity.read(Reads.get(in));
                entity.add();
            });
        }
    }

    public void readContentHeader(DataInput stream) throws IOException{
        byte mapped = stream.readByte();

        MappableContent[][] map = new MappableContent[ContentType.all.length][0];

        for(int i = 0; i < mapped; i++){
            ContentType type = ContentType.all[stream.readByte()];
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
        Seq<Content>[] map = content.getContentMap();

        int mappable = 0;
        for(Seq<Content> arr : map){
            if(arr.size > 0 && arr.first() instanceof MappableContent){
                mappable++;
            }
        }

        stream.writeByte(mappable);
        for(Seq<Content> arr : map){
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
