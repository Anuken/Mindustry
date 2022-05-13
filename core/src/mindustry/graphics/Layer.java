package mindustry.graphics;

/** Stores constants for sorting layers. Values should be stored in increments of 10. */
public class Layer{

    public static final float

    //min layer
    min = -11,

    //background, which may be planets or an image or nothing at all
    background = -10,

    //floor tiles
    floor = 0,

    //scorch marks on the floor
    scorch = 10,

    //things such as spent casings or rubble
    debris = 20,

    //stuff under blocks, like connections of conveyors/conduits
    blockUnder = 29.5f,

    //base block layer - most blocks go here
    block = 30,

    //layer for cracks over blocks, batched to prevent excessive texture swaps
    blockCracks = 30f + 0.1f,

    //some blocks need to draw stuff after cracks
    blockAfterCracks = 30f + 0.2f,

    //informal layer used for additive blending overlay, grouped together to reduce draw calls
    blockAdditive = 31,

    //things drawn over blocks (intermediate layer)
    blockOver = 35,

    //blocks currently in progress *shaders used*
    blockBuilding = 40,

    //turrets
    turret = 50,

    //special layer for turret additive blending heat stuff
    turretHeat = 50.1f,

    //ground units
    groundUnit = 60,

    //power lines
    power = 70,

    //certain multi-legged units
    legUnit = 75f,

    //darkness over block clusters
    darkness = 80,

    //building plans
    plans = 85,

    //flying units (low altitude)
    flyingUnitLow = 90,

    //bullets *bloom begin*
    bullet = 100,

    //effects *bloom end*
    effect = 110,

    //flying units
    flyingUnit = 115,

    //overlaid UI, like block config guides
    overlayUI = 120,

    //build beam effects
    buildBeam = 122,

    //shield effects
    shields = 125,

    //weather effects, e.g. rain and snow TODO draw before overlay UI?
    weather = 130,

    //light rendering *shaders used*
    light = 140,

    //names of players in the game
    playerName = 150,

    //fog of war effect, if applicable
    fogOfWar = 155,

    //space effects, currently only the land and launch effects
    space = 160,

    //the end of all layers
    end = 200,

    //things after pixelation - used for text
    endPixeled = 210,

    //max layer
    max = 220

    ;
}
