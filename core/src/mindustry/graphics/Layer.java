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

    //things drawn over blocks (intermediate layer)
    blockOver = 35,

    //blocks currently in progress *shaders used*
    blockBuilding = 40,

    //turrets
    turret = 50,

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

    //overlaied UI, like block config guides
    overlayUI = 120,

    //build beam effects
    buildBeam = 122,

    //shield effects
    shields = 125,

    //high bloom *bloom begin*
    highBullet = 130,

    //high bloom *bloom end*
    highEffect = 140,

    //weather effects, e.g. rain and snow TODO draw before overlay UI?
    weather = 145,

    //light rendering *shaders used*
    light = 150,

    //names of players in the game
    playerName = 160,

    //space effects, currently only the land and launch effects
    space = 170,

    //the end of all layers
    end = 200,

    //things after pixelation - used for text
    endPixeled = 210,

    //max layer
    max = 220

    ;
}
