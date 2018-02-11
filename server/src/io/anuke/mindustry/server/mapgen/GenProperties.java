package io.anuke.mindustry.server.mapgen;

public class GenProperties {
    public long seed;
    public SpawnStyle spawns;
    public MapStyle maps;
    public OreStyle ores;
    public RiverType riverType;
    public RiverStyle rivers;
    public TerrainStyle terrains;
    public FoliageStyle foliage;
    public EnvironmentStyle environment;

    enum SpawnStyle{
        /**spawn in a wide arc with branching paths*/
        arc,
        /**spawn in one big group*/
        grouped,
        /**surround player spawn*/
        surround
    }

    enum MapStyle{
        /**256x512*/
        longY,
        /**128x256*/
        smallY,
        /**128x128*/
        small,
        /**256x256*/
        normal
    }

    enum OreStyle{
        /**'vanilla' noise-distributed ores*/
        normal,
        /**ores hug the walls*/
        nearWalls,
        /**ores hug all liquid rivers*/
        nearRivers,
        /**large veins*/
        largeVeins
    }

    enum RiverType{
        lava,
        water,
        oil,
        none
    }

    enum RiverStyle{
        /**long thin river spanning entire map*/
        longThin,
        /**long river branching into many others*/
        longBranch,
        /**one long, thick river*/
        longThick,
        /**short, thick river that ends in a lake*/
        shortLake
    }

    enum TerrainStyle{
        /**bordered around by the normal material*/
        normal,
        /**everything is islands*/
        waterIslands,
        /**everything is islands: lava edition*/
        lavaIslands
    }

    enum FoliageStyle{
        patches,
        veins,
        blobs,
        ridges
    }

    enum FoilageType{
        grass,
        sand,
        darkStone,
        ice,
    }

    enum EnvironmentStyle{
        desert,
        stoneDesert,
        grassy,
        dark,
        darkStone,
        stone,
        icy,
    }
}
