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
import mindustry.game.EventType.*;
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
        readRegion("meta", stream, counter, in -> readMeta(in, context));
        readRegion("content", stream, counter, this::readContentHeader);

        try{
            if(version >= 11) readRegion("patches", stream, counter, this::readContentPatches);
            readRegion("map", stream, counter, in -> readMap(in, context));
            readRegion("entities", stream, counter, this::readEntities);
            if(version >= 8) readRegion("markers", stream, counter, this::readMarkers);
            readRegion("custom", stream, counter, this::readCustomChunks);
        }finally{
            content.setTemporaryMapper(null);
        }
    }

    public void write(DataOutputStream stream, StringMap extraTags) throws IOException{
        writeRegion("meta", stream, out -> writeMeta(out, extraTags));
        writeRegion("content", stream, this::writeContentHeader);
        writeRegion("patches", stream, this::writeContentPatches);
        writeRegion("map", stream, this::writeMap);
        writeRegion("entities", stream, this::writeEntities);
        writeRegion("markers", stream, this::writeMarkers);
        writeRegion("custom", stream, s -> writeCustomChunks(s, false));
    }

    public void writeCustomChunks(DataOutput stream, boolean net) throws IOException{
        var chunks = customChunks.orderedKeys().select(s -> customChunks.get(s).shouldWrite() && (!net || customChunks.get(s).writeNet()));
        stream.writeInt(chunks.size);
        for(var chunkName : chunks){
            var chunk = customChunks.get(chunkName);
            stream.writeUTF(chunkName);

            writeChunk(stream, writes -> chunk.write(writes.output));
        }
    }

    public void readCustomChunks(DataInput stream) throws IOException{
        int amount = stream.readInt();
        for(int i = 0; i < amount; i++){
            String name = stream.readUTF();
            var chunk = customChunks.get(name);
            if(chunk != null){
                readChunk(stream, chunk::read);
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
            "sectorPreset", state.rules.sector != null && state.rules.sector.preset != null ? state.rules.sector.preset.name : "", //empty string is a placeholder for null (null is possible but may be finicky)
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
            Tile tile = world.tiles.geti(i);
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
            Tile tile = world.tiles.geti(i);
            stream.writeShort(tile.blockID());

            boolean savedata = tile.shouldSaveData();

            //in the old version, the second bit was set to indicate presence of data, but that approach was flawed - it didn't allow buildings + data on the same tile
            //so now the third bit is used instead
            byte packed = (byte)((tile.build != null ? 1 : 0) | (savedata ? 4 : 0));

            //make note of whether there was an entity or custom tile data here
            stream.writeByte(packed);

            if(savedata){
                //the new 'extra data' format writes 7 bytes of data instead of 1
                stream.writeByte(tile.data);
                stream.writeByte(tile.floorData);
                stream.writeByte(tile.overlayData);
                stream.writeInt(tile.extraData);
            }

            //only write the entity for multiblocks once - in the center
            if(tile.build != null){
                if(tile.isCenter()){
                    stream.writeBoolean(true);
                    writeChunk(stream, out -> {
                        out.b(tile.build.version());
                        tile.build.writeAll(out);
                    });
                }else{
                    stream.writeBoolean(false);
                }
            }else if(!savedata){ //don't write consecutive blocks when there is custom data
                //write consecutive non-entity blocks
                int consecutives = 0;

                for(int j = i + 1; j < world.width() * world.height() && consecutives < 255; j++){
                    Tile nextTile = world.rawTile(j % world.width(), j / world.width());

                    if(nextTile.blockID() != tile.blockID() || savedata != nextTile.shouldSaveData()){
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
                //data check (bit 3): 7 bytes (3x block-specific bytes + 1x 4-byte extra data int)
                boolean hadData = (packedCheck & 4) != 0;

                byte data = 0, floorData = 0, overlayData = 0;
                int extraData = 0;

                if(hadData){
                    data = stream.readByte();
                    floorData = stream.readByte();
                    overlayData = stream.readByte();
                    extraData = stream.readInt();
                }

                if(hadEntity){
                    isCenter = stream.readBoolean();
                }

                //set block only if this is the center; otherwise, it's handled elsewhere
                if(isCenter){
                    tile.setBlock(block);
                    if(tile.build != null) tile.build.enabled = true;
                }

                //must be assigned after setBlock, because that can reset data
                if(hadData){
                    tile.data = data;
                    tile.floorData = floorData;
                    tile.overlayData = overlayData;
                    tile.extraData = extraData;
                    context.onReadTileData();
                }

                if(hadEntity){
                    if(isCenter){ //only read entity for center blocks
                        if(block.hasBuilding()){
                            try{
                                readChunkReads(stream, (in, len) -> {
                                    byte revision = in.b();
                                    tile.build.readAll(in, revision);
                                });
                            }catch(Throwable e){
                                throw new IOException("Failed to read tile entity of block: " + block, e);
                            }
                        }else{
                            //skip the entity region, as the entity and its IO code are now gone
                            skipChunk(stream);
                        }

                        context.onReadBuilding();
                    }
                }else if(!hadData){ //never read consecutive blocks if there's data
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

        Writes writes = new Writes(stream);

        stream.writeInt(data.size);
        for(TeamData team : data){
            stream.writeInt(team.team.id);
            stream.writeInt(team.plans.size);
            for(BlockPlan block : team.plans){
                stream.writeShort(block.x);
                stream.writeShort(block.y);
                stream.writeShort(block.rotation);
                stream.writeShort(block.block.id);
                TypeIO.writeObject(writes, block.config);
            }
        }
    }

    public void writeWorldEntities(DataOutput stream) throws IOException{
        stream.writeInt(Groups.all.count(Entityc::serialize));
        for(Entityc entity : Groups.all){
            if(!entity.serialize()) continue;

            writeChunk(stream, out -> {
                out.b(entity.classId());
                out.i(entity.id());
                entity.beforeWrite();
                entity.write(out);
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

        var reads = new Reads(stream);

        for(int i = 0; i < teamc; i++){
            Team team = Team.get(stream.readInt());
            TeamData data = team.data();
            int blocks = stream.readInt();
            data.plans.clear();
            data.plans.ensureCapacity(Math.min(blocks, 1000));
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

    public void readWorldEntities(DataInput stream, Prov[] mapping) throws IOException{
        IntSet used = new IntSet();
        Seq<Entityc> reassign = new Seq<>();

        int amount = stream.readInt();
        for(int j = 0; j < amount; j++){
            readChunkReads(stream, (in, len) -> {
                int typeid = in.ub();
                if(mapping[typeid] == null){
                    in.skip(len - 1);
                    return;
                }

                int id = in.i();

                Entityc entity = (Entityc)mapping[typeid].get();
                EntityGroup.checkNextId(id);
                entity.id(id);
                entity.read(in);
                if(used.add(id)){
                    entity.add();
                }else{
                    Log.warn("Duplicate entity ID in save: @ (@)", id, entity);
                    reassign.add(entity);
                }
            });
        }

        for(var ent : reassign){
            ent.id(EntityGroup.nextId());
            ent.add();
        }

        Groups.all.each(Entityc::afterReadAll);
    }

    public Prov[] readEntityMapping(DataInput stream) throws IOException{
        //copy entityMapping for further mutation; will be used in readWorldEntities
        Prov[] entityMapping = Arrays.copyOf(EntityMapping.idMap, EntityMapping.idMap.length);

        short amount = stream.readShort();
        for(int i = 0; i < amount; i++){
            //everything that corresponded to this ID in this save goes by this name
            //so replace the prov in the current mapping with the one found with this name
            short id = stream.readShort();
            String name = stream.readUTF();
            entityMapping[id] = EntityMapping.map(name);
        }

        return entityMapping;
    }

    public void readEntities(DataInput stream) throws IOException{
        var mapping = readEntityMapping(stream);
        readTeamBlocks(stream);
        readWorldEntities(stream, mapping);
    }

    public void skipContentPatches(DataInput stream) throws IOException{
        int amount = stream.readUnsignedByte();
        for(int i = 0; i < amount; i++){
            int len = stream.readInt();
            stream.skipBytes(len);
        }
    }

    public void readContentPatches(DataInput stream) throws IOException{
        Seq<String> patches = new Seq<>();

        int amount = stream.readUnsignedByte();
        if(amount > 0){
            for(int i = 0; i < amount; i++){
                int len = stream.readInt();
                byte[] bytes = new byte[len];
                stream.readFully(bytes);
                patches.add(new String(bytes, Strings.utf8));
            }
        }

        Events.fire(new ContentPatchLoadEvent(patches));

        if(patches.size > 0){
            try{
                state.patcher.apply(patches);
            }catch(Throwable e){
                Log.err("Failed to apply patches: " + patches, e);
            }
        }
    }

    public void writeContentPatches(DataOutput stream) throws IOException{
        if(state.patcher.patches.size > 0){
            var patches = state.patcher.patches;
            stream.writeByte(patches.size);
            for(var patchset : patches){
                byte[] bytes = patchset.patch.getBytes(Strings.utf8);
                stream.writeInt(bytes.length);
                stream.write(bytes);
            }
        }else{
            stream.writeByte(0);
        }
    }

    public void readContentHeader(DataInput stream) throws IOException{
        int mapped = stream.readUnsignedByte();

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

        //HACK: versions below 11 don't read the patch chunk, which means the event for reading patches is never triggered.
        //manually fire the event here for older versions.
        if(version < 11){
            Seq<String> patches = new Seq<>();
            Events.fire(new ContentPatchLoadEvent(patches));

            if(patches.size > 0){
                try{
                    state.patcher.apply(patches);
                }catch(Throwable e){
                    Log.err("Failed to apply patches: " + patches, e);
                }
            }
        }
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
