package mindustry.io;

import arc.*;
import arc.func.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.io.*;
import java.util.*;

import static mindustry.Vars.*;

public abstract class SaveVersion extends SaveFileReader{
    protected static OrderedMap<String, CustomChunk> customChunks = new OrderedMap<>();

    public final int version;

    //HACK stores the last read build of the save file, valid after read meta call
    protected int lastReadBuild;
    //stores entity mappings for use after readEntityMapping
    //if null, fall back to EntityMapping's values
    protected @Nullable Prov[] entityMapping;

    /**
     * Registers a custom save chunk reader/writer by name. This is mostly used for mods that need to save extra data.
     * @param name a mod-specific, unique name for identifying this chunk. Prefixing is recommended.
     * */
    public static void addCustomChunk(String name, CustomChunk chunk){
        customChunks.put(name, chunk);
    }

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
            map
        );
    }

    @Override
    public final void write(DataOutputStream stream) throws IOException{
        write(stream, new StringMap());
    }

    @Override
    public void read(DataInputStream stream, CounterInputStream counter, WorldContext context) throws IOException{
        region("meta", stream, counter, in -> readMeta(in, context));
        region("content", stream, counter, this::readContentHeader);

        try{
            region("map", stream, counter, in -> readMap(in, context));
            region("entities", stream, counter, this::readEntities);
            if(version >= 8) region("markers", stream, counter, this::readMarkers);
            region("custom", stream, counter, this::readCustomChunks);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public void write(DataOutputStream stream, StringMap extraTags) throws IOException{
        region("meta", stream, out -> writeMeta(out, extraTags));
        region("content", stream, this::writeContentHeader);
        region("map", stream, this::writeMap);
        region("entities", stream, this::writeEntities);
        region("markers", stream, this::writeMarkers);
        region("custom", stream, s -> writeCustomChunks(s, false));
    }

    public void writeCustomChunks(DataOutput stream, boolean net) throws IOException{
        var chunks = customChunks.orderedKeys().select(s -> customChunks.get(s).shouldWrite() && (!net || customChunks.get(s).writeNet()));
        stream.writeInt(chunks.size);
        for(var chunkName : chunks){
            var chunk = customChunks.get(chunkName);
            stream.writeUTF(chunkName);

            writeChunk(stream, false, chunk::write);
        }
    }

    public void readCustomChunks(DataInput stream) throws IOException{
        int amount = stream.readInt();
        for(int i = 0; i < amount; i++){
            String name = stream.readUTF();
            var chunk = customChunks.get(name);
            if(chunk != null){
                readChunk(stream, false, chunk::read);
            }else{
                skipChunk(stream);
            }
        }
    }

    public void writeMeta(DataOutput stream, StringMap tags) throws IOException{
        //prepare campaign data for writing
        if(state.isCampaign()){
            state.rules.sector.info.prepare(state.rules.sector);
            state.rules.sector.saveInfo();
        }

        //flush tech node progress
        for(TechNode node : TechTree.all){
            node.save();
        }

        StringMap result = new StringMap();
        result.putAll(tags);

        writeStringMap(stream, result.merge(StringMap.of(
            "saved", Time.millis(),
            "playtime", headless ? 0 : control.saves.getTotalPlaytime(),
            "build", Version.build,
            "mapname", state.map.name(),
            "wave", state.wave,
            "tick", state.tick,
            "wavetime", state.wavetime,
            "stats", JsonIO.write(state.stats),
            "rules", JsonIO.write(state.rules),
            "locales", JsonIO.write(state.mapLocales),
            "mods", JsonIO.write(mods.getModStrings().toArray(String.class)),
            "controlGroups", headless || control == null ? "null" : JsonIO.write(control.input.controlGroups),
            "width", world.width(),
            "height", world.height(),
            "viewpos", Tmp.v1.set(player == null ? Vec2.ZERO : player).toString(),
            "controlledType", headless || control.input.controlledType == null ? "null" : control.input.controlledType.name,
            "nocores", state.rules.defaultTeam.cores().isEmpty(),
            "playerteam", player == null ? state.rules.defaultTeam.id : player.team().id
        )));
    }

    public void readMeta(DataInput stream, WorldContext context) throws IOException{
        StringMap map = readStringMap(stream);

        state.wave = map.getInt("wave");
        state.wavetime = map.getFloat("wavetime", state.rules.waveSpacing);
        state.tick = map.getFloat("tick");
        state.stats = JsonIO.read(GameStats.class, map.get("stats", "{}"));
        state.rules = JsonIO.read(Rules.class, map.get("rules", "{}"));
        state.mapLocales = JsonIO.read(MapLocales.class, map.get("locales", "{}"));
        if(state.rules.spawns.isEmpty()) state.rules.spawns = waves.get();
        lastReadBuild = map.getInt("build", -1);

        if(context.getSector() != null){
            state.rules.sector = context.getSector();
            if(state.rules.sector != null){
                state.rules.sector.planet.applyRules(state.rules);
            }
        }

        //replace the default serpulo env with erekir
        if(state.rules.planet == Planets.serpulo && state.rules.hasEnv(Env.scorching)){
            state.rules.planet = Planets.erekir;
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

            var groups = JsonIO.read(IntSeq[].class, map.get("controlGroups", "null"));
            if(groups != null && groups.length == control.input.controlGroups.length){
                control.input.controlGroups = groups;
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

            boolean savedata = tile.floor().saveData || tile.overlay().saveData || tile.block().saveData;

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
                        if(block.hasBuilding()){
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

                        context.onReadBuilding();
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

    public void writeTeamBlocks(DataOutput stream) throws IOException{
        //write team data with entities.
        Seq<TeamData> data = state.teams.getActive().copy();
        if(!data.contains(Team.sharded.data())) data.add(Team.sharded.data());
        stream.writeInt(data.size);
        for(TeamData team : data){
            stream.writeInt(team.team.id);
            stream.writeInt(team.plans.size);
            for(BlockPlan block : team.plans){
                stream.writeShort(block.x);
                stream.writeShort(block.y);
                stream.writeShort(block.rotation);
                stream.writeShort(block.block.id);
                TypeIO.writeObject(Writes.get(stream), block.config);
            }
        }
    }

    public void writeWorldEntities(DataOutput stream) throws IOException{
        stream.writeInt(Groups.all.count(Entityc::serialize));
        for(Entityc entity : Groups.all){
            if(!entity.serialize()) continue;

            writeChunk(stream, true, out -> {
                out.writeByte(entity.classId());
                out.writeInt(entity.id());
                entity.beforeWrite();
                entity.write(Writes.get(out));
            });
        }
    }

    public void writeEntityMapping(DataOutput stream) throws IOException{
        stream.writeShort(EntityMapping.customIdMap.size);
        for(var entry : EntityMapping.customIdMap.entries()){
            stream.writeShort(entry.key);
            stream.writeUTF(entry.value);
        }
    }

    public void writeEntities(DataOutput stream) throws IOException{
        writeEntityMapping(stream);
        writeTeamBlocks(stream);
        writeWorldEntities(stream);
    }

    public void writeMarkers(DataOutput stream) throws IOException{
        state.markers.write(stream);
    }

    public void readMarkers(DataInput stream) throws IOException{
        state.markers.read(stream);
    }

    public void readTeamBlocks(DataInput stream) throws IOException{
        int teamc = stream.readInt();

        for(int i = 0; i < teamc; i++){
            Team team = Team.get(stream.readInt());
            TeamData data = team.data();
            int blocks = stream.readInt();
            data.plans.clear();
            data.plans.ensureCapacity(Math.min(blocks, 1000));
            var reads = Reads.get(stream);
            var set = new IntSet();

            for(int j = 0; j < blocks; j++){
                short x = stream.readShort(), y = stream.readShort(), rot = stream.readShort(), bid = stream.readShort();
                var obj = TypeIO.readObject(reads);
                //cannot have two in the same position
                if(set.add(Point2.pack(x, y))){
                    data.plans.addLast(new BlockPlan(x, y, rot, content.block(bid), obj));
                }
            }
        }
    }

    public void readWorldEntities(DataInput stream) throws IOException{
        //entityMapping is null in older save versions, so use the default
        var mapping = this.entityMapping == null ? EntityMapping.idMap : this.entityMapping;

        int amount = stream.readInt();
        for(int j = 0; j < amount; j++){
            readChunk(stream, true, in -> {
                int typeid = in.readUnsignedByte();
                if(mapping[typeid] == null){
                    in.skipBytes(lastRegionLength - 1);
                    return;
                }

                int id = in.readInt();

                Entityc entity = (Entityc)mapping[typeid].get();
                EntityGroup.checkNextId(id);
                entity.id(id);
                entity.read(Reads.get(in));
                entity.add();
            });
        }

        Groups.all.each(Entityc::afterReadAll);
    }

    public void readEntityMapping(DataInput stream) throws IOException{
        //copy entityMapping for further mutation; will be used in readWorldEntities
        entityMapping = Arrays.copyOf(EntityMapping.idMap, EntityMapping.idMap.length);

        short amount = stream.readShort();
        for(int i = 0; i < amount; i++){
            //everything that corresponded to this ID in this save goes by this name
            //so replace the prov in the current mapping with the one found with this name
            short id = stream.readShort();
            String name = stream.readUTF();
            entityMapping[id] = EntityMapping.map(name);
        }
    }

    public void readEntities(DataInput stream) throws IOException{
        readEntityMapping(stream);
        readTeamBlocks(stream);
        readWorldEntities(stream);
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
                //fallback only for blocks
                map[type.ordinal()][j] = content.getByName(type, type == ContentType.block ? fallback.get(name, name) : name);
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
