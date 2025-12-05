package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.Pathfinder.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.logic.*;
import mindustry.type.ammo.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static arc.graphics.g2d.Draw.*;
import static mindustry.Vars.*;

public class UnitType extends UnlockableContent implements Senseable{
    public static final float shadowTX = -12, shadowTY = -13;
    private static final Vec2 legOffset = new Vec2();
    private static final Seq<UnitStance> tmpStances = new Seq<>();

    /** Environmental flags that are *all* required for this unit to function. 0 = any environment */
    public int envRequired = 0;
    /** The environment flags that this unit can function in. If the env matches any of these, it will be enabled. */
    public int envEnabled = Env.terrestrial;
    /** The environment flags that this unit *cannot* function in. If the env matches any of these, it will explode or be disabled. */
    public int envDisabled = Env.scorching;

    /** movement speed (world units/t) */
    public float speed = 1.1f,
    /** multiplier for speed when boosting */
    boostMultiplier = 1f,
    /** how affected this unit is by terrain */
    floorMultiplier = 1f,
    /** body rotation speed in degrees/t */
    rotateSpeed = 5f,
    /** mech base rotation speed in degrees/t*/
    baseRotateSpeed = 5f,
    /** movement drag as fraction */
    drag = 0.3f,
    /** acceleration as fraction of speed */
    accel = 0.5f,
    /** size of one side of the hitbox square */
    hitSize = 6f,
    /** shake on unit death */
    deathShake = -1f,
    /** shake on each step for leg/mech units */
    stepShake = -1f,
    /** ripple / dust size for legged units */
    rippleScale = 1f,
    /** boosting rise speed as fraction */
    riseSpeed = 0.08f,
    /** how fast this unit falls when not boosting */
    fallSpeed = 0.018f,
    /** how many ticks it takes this missile to accelerate to full speed */
    missileAccelTime = 0f,
    /** raw health amount */
    health = 200f,
    /** incoming damage is reduced by this amount */
    armor = 0f,
    /** minimum range of any weapon; used for approaching targets. can be overridden by setting a value > 0. */
    range = -1,
    /** maximum range of any weapon */
    maxRange = -1f,
    /** range at which this unit can mine ores */
    mineRange = 70f,
    /** range at which this unit can build */
    buildRange = Vars.buildingRange,
    /** radius for circleTarget, if true */
    circleTargetRadius = 80f,
    /** multiplier for damage this (flying) unit deals when crashing on enemy things */
    crashDamageMultiplier = 1f,
    /** multiplier for health that this flying unit has for its wreck, based on its max health. */
    wreckHealthMultiplier = 0.25f,
    /** a VERY ROUGH estimate of unit DPS; initialized in init() */
    dpsEstimate = -1,
    /** graphics clipping size; <0 to calculate automatically */
    clipSize = -1,
    /** multiplier for how slowly this unit drowns - higher numbers, slower drowning. */
    drownTimeMultiplier = 1f,
    /** fractional movement speed penalty for this unit when it is moving in the opposite direction that it is facing */
    strafePenalty = 0.5f,
    /** multiplier for cost of research in tech tree */
    researchCostMultiplier = 50,

    /** for ground units, the layer upon which this unit is drawn */
    groundLayer = Layer.groundUnit,
    /** For units that fly, the layer upon which this unit is drawn. If no value is set, defaults to Layer.flyingUnitLow or Layer.flyingUnit depending on lowAltitude */
    flyingLayer = -1,
    /** Payload capacity of this unit in world units^2 */
    payloadCapacity = 8,
    /** building speed multiplier; <0 to disable. */
    buildSpeed = -1f,
    /** Minimum distance from this unit that weapons can target. Prevents units from firing "inside" the unit. */
    aimDst = -1f,
    /** Visual offset of the build beam from the front. */
    buildBeamOffset = 3.8f,
    /** Visual offset of the mining beam from the front. Defaults to half the hitsize. */
    mineBeamOffset = Float.NEGATIVE_INFINITY,
    /** WIP: Units of low priority will always be ignored in favor of those with higher priority, regardless of distance. */
    targetPriority = 0f,
    /** Elevation of shadow drawn under this (ground) unit. Visual only. */
    shadowElevation = -1f,
    /** Scale for length of shadow drawn under this unit. Does nothing if this unit has no shadow. */
    shadowElevationScl = 1f,
    /** backwards engine offset from center of unit */
    engineOffset = 5f,
    /** main engine radius */
    engineSize = 2.5f,
    /** layer of all engines (<0 for default) */
    engineLayer = -1f,
    /** visual backwards offset of items on unit */
    itemOffsetY = 3f,
    /** radius of light emitted, <0 for default */
    lightRadius = -1f,
    /** light color opacity*/
    lightOpacity = 0.6f,
    /** scale of soft shadow - its size is calculated based off of region size */
    softShadowScl = 1f,
    /** fog view radius in tiles. <0 for automatic radius. */
    fogRadius = -1f,

    /** horizontal offset of wave trail in naval units */
    waveTrailX = 4f,
    /** vertical offset of wave trail in naval units  */
    waveTrailY = -3f,
    /** width of all trails (including naval ones) */
    trailScl = 1f;

    /** if true, this unit counts as an enemy in the wave counter (usually false for support-only units) */
    public boolean isEnemy = true,
    /** if true, the unit is always at elevation 1 */
    flying = false,
    /** whether this flying unit should wobble around */
    wobble = true,
    /** whether this unit tries to attack air units */
    targetAir = true,
    /** whether this unit tries to attack ground units */
    targetGround = true,
    /** if true, this unit will attempt to face its target when shooting/aiming at it */
    faceTarget = true,
    /** AI flag: if true, this flying unit circles around its target like a bomber */
    circleTarget = false,
    /** AI flag: if true, this unit will drop bombs under itself even when it is not next to its 'real' target. used for carpet bombers */
    autoDropBombs = false,
    /** For the mobile version only: If false, this unit will not auto-target buildings to attach when a player controls it. */
    targetBuildingsMobile = true,
    /** if true, this unit can boost into the air if a player/processors controls it*/
    canBoost = false,
    /** if true, this unit will always boost when using builder AI */
    boostWhenBuilding = true,
    /** if true, this unit will always boost when using miner AI */
    boostWhenMining = true,
    /** if false, logic processors cannot control this unit */
    logicControllable = true,
    /** if false, players cannot control this unit */
    playerControllable = true,
    /** If true, the unit can be selected with the global selection hotkey (shift+g). */
    controlSelectGlobal = true,
    /** if false, this unit cannot be moved into payloads */
    allowedInPayloads = true,
    /** if false, this unit cannot be hit by bullets or explosions*/
    hittable = true,
    /** if false, this unit does not take damage and cannot be kill() / destroy()-ed. */
    killable = true,
    /** if false, this unit is not targeted by anything. */
    targetable = true,
    /** if true, this unit can be hit/targeted when it has payloads (assuming hittable/targetable is false) */
    vulnerableWithPayloads = false,
    /** if true, this payload unit can pick up units */
    pickupUnits = true,
    /** if false, this unit does not physically collide with others. */
    physics = true,
    /** if true, this ground unit will drown in deep liquids. */
    canDrown = true,
    /** if false, this unit ignores the unit cap and can be spawned infinitely */
    useUnitCap = true,
    /** if true, this core unit will "dock" to other units, making it re-appear when "undocking". */
    coreUnitDock = false,
    /** if false, no falling "corpse" is created when this unit dies. */
    createWreck = true,
    /** if false, no scorch marks are created when this unit dies */
    createScorch = true,
    /** if true, this unit will be drawn under effects/bullets; this is a visual change only. */
    lowAltitude = false,
    /** if true, this unit will look at whatever it is building */
    rotateToBuilding = true,
    /** if true and this is a legged unit, this unit can walk over blocks. */
    allowLegStep = false,
    /** for legged units, setting this to false forces it to be on the ground physics layer. */
    legPhysicsLayer = true,
    /** if true, this unit will not be affected by the floor under it. */
    hovering = false,
    /** if true, this unit can move in any direction regardless of rotation. if false, this unit can only move in the direction it is facing. */
    omniMovement = true,
    /** if true, the unit faces its moving direction before actually moving. */
    rotateMoveFirst = false,
    /** if true, this unit flashes when being healed */
    healFlash = true,
    /** whether the unit can heal blocks. Initialized in init() */
    canHeal = false,
    /** if true, all weapons will attack the same target. */
    singleTarget = false,
    /** if true, this unit will be able to have multiple targets, even if it only has one mirrored weapon. */
    forceMultiTarget = false,
    /** if false, this unit has no weapons that can attack. */
    canAttack = true,
    /** if true, this unit won't show up in the database or various other UIs. */
    hidden = false,
    /** if true, this unit is for internal use only and does not have a sprite generated. */
    internal = false,
    /** For certain units, generating sprites is still necessary, despite being internal. */
    internalGenerateSprites = false,
    /** If false, this unit is not pushed away from map edges. */
    bounded = true,
    /** if true, this unit is detected as naval - do NOT assign this manually! Initialized in init() */
    naval = false,
    /** if false, RTS AI controlled units do not automatically attack things while moving. This is automatically assigned. */
    autoFindTarget = true,
    /** If false, 'under' blocks like conveyors are not targeted. */
    targetUnderBlocks = true,
    /** if true, this unit will always shoot while moving regardless of slowdown */
    alwaysShootWhenMoving = false,

    /** whether this unit has a hover tooltip */
    hoverable = true,
    /** if true, this modded unit always has a -outline region generated for its base. Normally, outlines are ignored if there are no top = false weapons. */
    alwaysCreateOutline = false,
    /** for vanilla content only - if false, skips the full icon generation step. */
    generateFullIcon = true,
    /** if true, this unit has a square shadow. */
    squareShape = false,
    /** if true, this unit will draw its building beam towards blocks. */
    drawBuildBeam = true,
    /** if true, this unit will draw its mining beam towards blocks */
    drawMineBeam = true,
    /** if false, the team indicator/cell is not drawn. */
    drawCell = true,
    /** if false, carried items are not drawn. */
    drawItems = true,
    /** if false, the unit shield (usually seen in waves) is not drawn. */
    drawShields = true,
    /** if false, the unit body is not drawn. */
    drawBody = true,
    /** if false, the soft shadow is not drawn. */
    drawSoftShadow = true,
    /** if false, the unit is not drawn on the minimap. */
    drawMinimap = true;

    /** The default AI controller to assign on creation. */
    public Prov<? extends UnitController> aiController = () -> !flying ? new GroundAI() : new FlyingAI();
    /** Function that chooses AI controller based on unit entity. */
    public Func<Unit, ? extends UnitController> controller = u -> !playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? aiController.get() : new CommandAI();
    /** Creates a new instance of this unit class. */
    public Prov<? extends Unit> constructor;

    /** list of "abilities", which are various behaviors that update each frame */
    public Seq<Ability> abilities = new Seq<>();
    /** All weapons that this unit will shoot with. */
    public Seq<Weapon> weapons = new Seq<>();
    /** None of the status effects in this set can be applied to this unit. */
    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();

    /** color that this unit flashes when getting healed (if healFlash is true) */
    public Color healColor = Pal.heal;
    /** Color of light that this unit produces when lighting is enabled in the map. */
    public Color lightColor = Pal.powerLight;
    /** override for unit shield colour. */
    public @Nullable Color shieldColor;
    /** sound played when this unit explodes (*not* when it is shot down) */
    public Sound deathSound = Sounds.bang;
    /** sound played on loop when this unit is around. */
    public Sound loopSound = Sounds.none;
    /** volume of loop sound */
    public float loopSoundVolume = 0.5f;
    /** sound played when this mech/insect unit does a step */
    public Sound stepSound = Sounds.none;
    /** volume of step sound */
    public float stepSoundVolume = 0.5f;
    /** base pitch of step sound */
    public float stepSoundPitch = 1f;
    /** effect that this unit emits when falling */
    public Effect fallEffect = Fx.fallSmoke;
    /** effect created at engine when unit falls. */
    public Effect fallEngineEffect = Fx.fallSmoke;
    /** effect created when this unit dies */
    public Effect deathExplosionEffect = Fx.dynamicExplosion;
    /** optional effect created when this tank moves */
    public @Nullable Effect treadEffect;
    /** extra (usually animated) visual parts */
    public Seq<DrawPart> parts = new Seq<>(DrawPart.class);
    /** list of engines, or "thrusters" */
    public Seq<UnitEngine> engines = new Seq<>();
    /** if false, the thruster is always displayed at its normal size regardless of elevation */
    public boolean useEngineElevation = true;
    /** override for all engine colors */
    public @Nullable Color engineColor = null;
    /** color for inner portions of engines */
    public Color engineColorInner = Color.white;
    /** length of engine trail (if flying) or wave trail (if naval) */
    public int trailLength = 0;
    /** override for engine trail color */
    public @Nullable Color trailColor;

    /** Cost type ID for flow field/enemy AI pathfinding. */
    public int flowfieldPathType = -1;
    /** Function used for calculating cost of moving with ControlPathfinder. Does not affect "normal" flow field pathfinding. */
    public @Nullable PathCost pathCost;
    /** ID for path cost, to be used in the control path finder. This is the value that actually matters; do not assign manually. Set in init(). */
    public int pathCostId;
    /** A sample of the unit that this type creates. Do not modify! */
    public @Nullable Unit sample;

    /** Flags to target based on priority. Null indicates that the closest target should be found. The closest enemy core is used as a fallback. */
    public BlockFlag[] targetFlags = {null};

    /** A value of false is used to hide command changing UI in unit factories. */
    public boolean allowChangeCommands = true;
    /** Commands available to this unit through RTS controls. An empty array means commands will be assigned based on unit capabilities in init(). */
    public Seq<UnitCommand> commands = new Seq<>();
    /** Command to assign to this unit upon creation. Null indicates the first command in the array. */
    public @Nullable UnitCommand defaultCommand;
    /** Stances this unit can have.  An empty array means stances will be assigned based on unit capabilities in init(). */
    public Seq<UnitStance> stances = new Seq<>();

    /** color for outline generated around sprites */
    public Color outlineColor = Pal.darkerMetal;
    /** thickness for sprite outline  */
    public int outlineRadius = 3;
    /** if false, no sprite outlines are generated */
    public boolean outlines = true;

    /** amount of items this unit can carry; <0 to determine based on hitSize. */
    public int itemCapacity = -1;
    /** amount of ammo this unit can hold (if the rule is enabled); <0 to determine based on weapon fire rate. */
    public int ammoCapacity = -1;
    /** ammo this unit uses, if that system is enabled. */
    public AmmoType ammoType = new ItemAmmoType(Items.copper);

    /** max hardness of ore that this unit can mine (<0 to disable) */
    public int mineTier = -1;
    /** mining speed in weird arbitrary units */
    public float mineSpeed = 1f;
    /** whether this unit can mine ores from floors/walls, respectively */
    public boolean mineWalls = false, mineFloor = true;
    /** if true, harder materials will take longer to mine */
    public boolean mineHardnessScaling = true;
    /** continuous sound emitted when mining. */
    public Sound mineSound = Sounds.minebeam;
    /** volume of mining sound. */
    public float mineSoundVolume = 0.6f;
    /** Target items to mine. Used in MinerAI */
    public Seq<Item> mineItems = Seq.with(Items.copper, Items.lead, Items.titanium, Items.thorium);

    //LEG UNITS

    /** number of legs this unit has (must have the correct type to function!) */
    public int legCount = 4;
    /** size of groups in which legs move. for example, insects (6 legs) usually move legs in groups of 3. */
    public int legGroupSize = 2;

    /** total length of a leg (both segments) */
    public float legLength = 10f,
    /** how fast individual legs move towards their destination (non-linear) */
    legSpeed = 0.1f,
    /** scale for how far in front (relative to unit velocity) legs try to place themselves; if legs lag behind a unit, increase this number */
    legForwardScl = 1f,
    /** leg offset from the center of the unit */
    legBaseOffset = 0f,
    /** scaling for space between leg movements */
    legMoveSpace = 1f,
    /** for legs without "joints", this is how much the second leg sprite is moved "back" by, so it covers the joint region (it's hard to explain without an image) */
    legExtension = 0,
    /** Higher values of this field make groups of legs move less in-sync with each other. */
    legPairOffset = 0,
    /** scaling for how far away legs *try* to be from the body (not their actual length); e.g. if set to 0.5, legs will appear somewhat folded */
    legLengthScl = 1f,
    /** if legStraightness > 0, this is the scale for how far away legs are from the body horizontally */
    legStraightLength = 1f,
    /** maximum length of an individual leg as fraction of real length */
    legMaxLength = 1.75f,
    /** minimum length of an individual leg as fraction of real length */
    legMinLength = 0f,
    /** splash damage dealt when a leg touches the ground */
    legSplashDamage = 0f,
    /** splash damage radius of legs */
    legSplashRange = 5,
    /** how straight the leg base/origin is (0 = circular, 1 = line) */
    baseLegStraightness = 0f,
    /** how straight the leg outward angles are (0 = circular, 1 = horizontal line) */
    legStraightness = 0f;

    /** If true, the base (further away) leg region is drawn under instead of over. */
    public boolean legBaseUnder = false;
    /** If true, legs are locked to the base of the unit instead of being on an implicit rotating "mount". */
    public boolean lockLegBase = false;
    /** If true, legs always try to move around even when the unit is not moving (leads to more natural behavior) */
    public boolean legContinuousMove;
    /** TODO neither of these appear to do much */
    public boolean flipBackLegs = true, flipLegSide = false;
    /** Whether to emit a splashing noise in water. */
    public boolean emitWalkSound = true;
    /** Whether to emit a splashing effect in water (fasle implies emitWalkSound false). */
    public boolean emitWalkEffect = true;

    //MECH UNITS

    /** screen shake amount for when this mech lands after boosting */
    public float mechLandShake = 0f;
    /** parameters for mech swaying animation */
    public float mechSideSway = 0.54f, mechFrontSway = 0.1f, mechStride = -1f;
    /** whether particles are created when this mech takes a step */
    public boolean mechStepParticles = false;
    /** color that legs change to when moving, to simulate depth */
    public Color mechLegColor = Pal.darkMetal;

    //TANK UNITS

    /** list of treads as rectangles in IMAGE COORDINATES, relative to the center. these are mirrored. */
    public Rect[] treadRects = {};
    /** number of frames of movement in a tread */
    public int treadFrames = 18;
    /** how much of a top part of a tread sprite is "cut off" relative to the pattern; this is corrected for */
    public int treadPullOffset = 0;

    //SEGMENTED / CRAWL UNITS (this is WIP content!)

    /** number of independent segments */
    public int segments = 0;
    /** TODO wave support - for multi-unit segmented units, this is the number of independent units that are spawned */
    public int segmentUnits = 1;
    /** unit spawned in segments; if null, the same unit is used */
    public @Nullable UnitType segmentUnit;
    /** unit spawned at the end; if null, the segment unit is used */
    public @Nullable UnitType segmentEndUnit;
    /** true - parent segments are on higher layers; false - parent segments are on lower layers than head*/
    public boolean segmentLayerOrder = true;
    /** magnitude of sine offset between segments */
    public float segmentMag = 2f,
    /** scale of sine offset between segments */
    segmentScl = 4f,
    /** index multiplier of sine offset between segments */
    segmentPhase = 5f,
    /** how fast each segment moves towards the next one */
    segmentRotSpeed = 1f,
    /** maximum difference between segment angles */
    segmentMaxRot = 30f,
    /** spacing between separate unit segments (only used for multi-unit worms) */
    segmentSpacing = -1f,
    /** rotation between segments is clamped to this range */
    segmentRotationRange = 80f,
    /** speed multiplier this unit will have when crawlSlowdownFrac is met. */
    crawlSlowdown = 0.5f,
    /** damage dealt to blocks under this tank/crawler every frame. */
    crushDamage = 0f,
    /** the fraction of solids under this block necessary for it to reach crawlSlowdown. */
    crawlSlowdownFrac = 0.55f;

    //MISSILE UNITS

    /** lifetime of this missile. */
    public float lifetime = 60f * 5f;
    /** ticks that must pass before this missile starts homing. */
    public float homingDelay = 10f;

    //REGIONS

    //(undocumented, you shouldn't need to use these, and if you do just check how they're drawn and copy that)
    public TextureRegion baseRegion, legRegion, region, previewRegion, shadowRegion, cellRegion, itemCircleRegion,
        softShadowRegion, jointRegion, footRegion, legBaseRegion, baseJointRegion, outlineRegion, treadRegion,
        mineLaserRegion, mineLaserEndRegion;
    public TextureRegion[] wreckRegions, segmentRegions, segmentCellRegions, segmentOutlineRegions;
    public TextureRegion[][] treadRegions;

    //INTERNAL REQUIREMENTS

    protected float buildTime = -1f;
    protected @Nullable ItemStack[] totalRequirements, cachedRequirements, firstRequirements;

    public UnitType(String name){
        super(name);

        // Try to immediately resolve the Unit constructor based on EntityMapping entry, if it is set.
        // This is the default Vanilla behavior - it won't work properly for mods (see comment in `init()`)!
        constructor = EntityMapping.map(this.name);
        selectionSize = 30f;
    }

    public UnitController createController(Unit unit){
        return controller.get(unit);
    }

    public Unit create(Team team){
        Unit unit = constructor.get();
        unit.team = team;
        unit.setType(this);
        if(unit.controller() instanceof CommandAI command && defaultCommand != null){
            command.command = defaultCommand;
        }
        for(var ability : unit.abilities){
            ability.created(unit);
        }
        unit.ammo = ammoCapacity; //fill up on ammo upon creation
        unit.elevation = flying ? 1f : 0;
        unit.heal();
        if(unit instanceof TimedKillc u){
            u.lifetime(lifetime);
        }
        return unit;
    }

    /** @param cons Callback that gets called with every unit that is spawned. This is used for multi-unit segmented units. */
    public Unit spawn(Team team, float x, float y, float rotation, @Nullable Cons<Unit> cons){
        float offsetX = 0f, offsetY = 0f;
        if(segmentUnits > 1 && sample instanceof Segmentc){
            Tmp.v1.trns(rotation, segmentSpacing * segmentUnits / 2f);
            offsetX = Tmp.v1.x;
            offsetY = Tmp.v1.y;
        }

        Unit out = create(team);
        out.rotation = rotation;
        out.set(x + offsetX, y + offsetY);
        out.add();
        if(cons != null) cons.get(out);

        if(segmentUnits > 1 && out instanceof Segmentc){
            Unit last = out;
            UnitType segType = segmentUnit == null ? this : segmentUnit;
            for(int i = 0; i < segmentUnits; i++){
                UnitType type = i == segmentUnits - 1 && segmentEndUnit != null ? segmentEndUnit : segType;

                Unit next = type.create(team);
                Tmp.v1.trns(rotation, segmentSpacing * (i + 1));
                next.set(x - Tmp.v1.x + offsetX, y - Tmp.v1.y + offsetY);
                next.rotation = rotation;
                next.add();
                ((Segmentc)last).addChild(next);

                if(cons != null) cons.get(next);
                last = next;
            }
        }
        return out;
    }

    public Unit spawn(Team team, float x, float y, float rotation){
        return spawn(team, x, y, rotation, null);
    }

    public Unit spawn(Team team, float x, float y){
        return spawn(team, x, y, 0f);
    }

    public Unit spawn(float x, float y){
        return spawn(state.rules.defaultTeam, x, y);
    }

    public Unit spawn(Team team, Position pos){
        return spawn(team, pos.getX(), pos.getY());
    }

    public Unit spawn(Position pos){
        return spawn(state.rules.defaultTeam, pos);
    }

    public Unit spawn(Position pos, Team team){
        return spawn(team, pos);
    }

    public boolean hasWeapons(){
        return weapons.size > 0;
    }

    public boolean targetable(Unit unit, Team targeter){
        return targetable || (vulnerableWithPayloads && unit instanceof Payloadc p && p.hasPayload());
    }

    public boolean killable(Unit unit){
        return killable;
    }

    public boolean hittable(Unit unit){
        return hittable || (vulnerableWithPayloads && unit instanceof Payloadc p && p.hasPayload());
    }

    /** Adds all available unit stances based on the unit's current state. This can change based on the command of the unit. */
    public void getUnitStances(Unit unit, Seq<UnitStance> out){
        //return mining stances based on present items
        if(unit.controller() instanceof CommandAI ai && ai.currentCommand() == UnitCommand.mineCommand){
            out.add(UnitStance.mineAuto);
            for(Item item : indexer.getAllPresentOres()){
                if(unit.canMine(item) && ((mineFloor && indexer.hasOre(item)) || (mineWalls && indexer.hasWallOre(item)))){
                    var itemStance = ItemUnitStance.getByItem(item);
                    if(itemStance != null){
                        out.add(itemStance);
                    }
                }
            }
        }else{
            out.addAll(stances);
        }
    }

    public boolean allowStance(Unit unit, UnitStance stance){
        if(stance == UnitStance.stop) return true;
        tmpStances.clear();
        getUnitStances(unit, tmpStances);
        return tmpStances.contains(stance);
    }

    public boolean allowCommand(Unit unit, UnitCommand command){
        return commands.contains(command);
    }

    public void update(Unit unit){

    }

    public void updatePayload(Unit unit, @Nullable Unit unitHolder, @Nullable Building buildingHolder){

    }

    public void killed(Unit unit){}

    public void landed(Unit unit){}

    public void display(Unit unit, Table table){
        table.table(t -> {
            t.left();
            t.add(new Image(uiIcon)).size(iconMed).scaling(Scaling.fit);
            t.labelWrap(unit.isPlayer() ? unit.getPlayer().coloredName() + "\n[lightgray]" + localizedName : localizedName).left().width(190f).padLeft(5);
        }).growX().left();
        table.row();

        table.table(bars -> {
            bars.defaults().growX().height(20f).pad(4);

            //TODO overlay shields
            bars.add(new Bar("stat.health", Pal.health, unit::healthf).blink(Color.white));
            bars.row();

            if(state.rules.unitAmmo){
                bars.add(new Bar(ammoType.icon() + " " + Core.bundle.get("stat.ammo"), ammoType.barColor(), () -> unit.ammo / ammoCapacity));
                bars.row();
            }

            for(Ability ability : unit.abilities){
                ability.displayBars(unit, bars);
            }

            if(payloadCapacity > 0 && unit instanceof Payloadc payload){
                bars.add(new Bar("stat.payloadcapacity", Pal.items, () -> payload.payloadUsed() / payloadCapacity));
                bars.row();

                var count = new float[]{-1};
                bars.table().update(t -> {
                    if(count[0] != payload.payloadUsed()){
                        payload.contentInfo(t, 8 * 2, 270);
                        count[0] = payload.payloadUsed();
                    }
                }).growX().left().height(0f).pad(0f);
            }
        }).growX();

        if(unit.controller() instanceof LogicAI ai){
            table.row();
            table.add(Blocks.microProcessor.emoji() + " " + Core.bundle.get("units.processorcontrol")).growX().wrap().left();
            if(ai.controller != null && (Core.settings.getBool("mouseposition") || Core.settings.getBool("position"))){
                table.row();
                table.add("[lightgray](" + ai.controller.tileX() + ", " + ai.controller.tileY() + ")").growX().wrap().left();
            }
            table.row();
            table.label(() -> Iconc.settings + " " + (long)unit.flag + "").color(Color.lightGray).growX().wrap().left();
            if(net.active() && ai.controller != null && ai.controller.lastAccessed != null){
                table.row();
                table.add(Core.bundle.format("lastaccessed", ai.controller.lastAccessed)).growX().wrap().left();
            }
        }else if(net.active() && unit.lastCommanded != null){
            table.row();
            table.add(Core.bundle.format("lastcommanded", unit.lastCommanded)).growX().wrap().left();
        }

        table.row();
    }

    /** @return whether this block supports a specific environment. */
    public boolean supportsEnv(int env){
        return (envEnabled & env) != 0 && (envDisabled & env) == 0 && (envRequired == 0 || (envRequired & env) == envRequired);
    }

    @Override
    public boolean isBanned(){
        return state.rules.isBanned(this);
    }

    @Override
    public void getDependencies(Cons<UnlockableContent> cons){
        //units require reconstructors being researched
        for(Block block : content.blocks()){
            if(block instanceof Reconstructor r){
                for(UnitType[] recipe : r.upgrades){
                    //result of reconstruction is this, so it must be a dependency
                    if(recipe[1] == this){
                        cons.get(block);
                    }
                }
            }
        }

        for(ItemStack stack : researchRequirements()){
            cons.get(stack.item);
        }
    }

    @Override
    public boolean isHidden(){
        return hidden;
    }

    @Override
    public void setStats(){
        stats.add(Stat.health, health);
        stats.add(Stat.armor, armor);
        stats.add(Stat.speed, speed * 60f / tilesize, StatUnit.tilesSecond);
        stats.add(Stat.size, StatValues.squared(hitSize / tilesize, StatUnit.blocks));
        stats.add(Stat.itemCapacity, itemCapacity);
        stats.add(Stat.range, Strings.autoFixed(maxRange / tilesize, 1), StatUnit.blocks);

        if(crushDamage > 0){
            stats.add(Stat.crushDamage, crushDamage * 60f * 5f, StatUnit.perSecond);
        }

        if(legSplashDamage > 0 && legSplashRange > 0){
            stats.add(Stat.legSplashDamage, legSplashDamage, StatUnit.perLeg);
            stats.add(Stat.legSplashRange, Strings.autoFixed(legSplashRange / tilesize, 1), StatUnit.blocks);
        }

        stats.add(Stat.targetsAir, targetAir);
        stats.add(Stat.targetsGround, targetGround);

        if(abilities.any()){
            stats.add(Stat.abilities, StatValues.abilities(abilities));
        }

        stats.add(Stat.flying, flying);

        if(!flying){
            stats.add(Stat.canBoost, canBoost);
        }

        if(mineTier >= 1){
            stats.addPercent(Stat.mineSpeed, mineSpeed);
            stats.add(Stat.mineTier, StatValues.drillables(mineSpeed, 1f, 1, null, b ->
                b.itemDrop != null &&
                (b instanceof Floor f && (((f.wallOre && mineWalls) || (!f.wallOre && mineFloor))) ||
                (!(b instanceof Floor) && mineWalls)) &&
                b.itemDrop.hardness <= mineTier && (!b.playerUnmineable || Core.settings.getBool("doubletapmine"))));
        }
        if(buildSpeed > 0){
            stats.addPercent(Stat.buildSpeed, buildSpeed);
        }
        if(sample instanceof Payloadc){
            stats.add(Stat.payloadCapacity, StatValues.squared(Mathf.sqrt(payloadCapacity / (tilesize * tilesize)), StatUnit.blocks));
        }

        var reqs = getFirstRequirements();

        if(reqs != null){
            stats.add(Stat.buildCost, StatValues.items(reqs));
        }

        if(weapons.any()){
            stats.add(Stat.weapons, StatValues.weapons(this, weapons));
        }

        if(immunities.size > 0){
            stats.add(Stat.immunities, StatValues.statusEffects(immunities.toSeq().sort()));
        }
    }

    protected void checkEntityMapping(Unit example){
        if(constructor == null) throw new IllegalArgumentException(Strings.format("""
            No constructor set up for unit '@': Assign `constructor = [your unit constructor]`. Vanilla defaults are:
              "flying": UnitEntity::create
              "mech": MechUnit::create
              "legs": LegsUnit::create
              "naval": UnitWaterMove::create
              "payload": PayloadUnit::create
              "missile": TimedKillUnit::create
              "tank": TankUnit::create
              "hover": ElevationMoveUnit::create
              "tether": BuildingTetherPayloadUnit::create
              "crawl": CrawlUnit::create
            """, name));

        // Often modders improperly only sets `constructor = ...` without mapping. Try to mitigate that.
        // In most cases, if the constructor is a Vanilla class, things should work just fine.
        if(EntityMapping.map(name) == null) EntityMapping.nameMap.put(name, constructor);

        // Sanity checks; this is an EXTREMELY COMMON pitfalls Java modders fall into.
        int classId = example.classId();
        if(
            // Check if `classId()` even points to a valid constructor...
        EntityMapping.map(classId) == null ||
        // ...or if the class doesn't register itself and uses the ID of its base class.
        classId != ((Entityc)EntityMapping.map(classId).get()).classId()
        ){
            String type = example.getClass().getSimpleName();
            throw new IllegalArgumentException(Strings.format("""
                Invalid class ID for `@` detected (found: @). Potential fixes:
                - Register with `EntityMapping.register("some-unique-name", @::new)` to get an ID, and store it somewhere.
                - Override `@#classId()` to return that ID.
                """, type, classId, type, type));
        }
    }

    void initPathType(){
        if(flowfieldPathType == -1){
            flowfieldPathType =
            naval ? Pathfinder.costNaval :
            allowLegStep ? Pathfinder.costLegs :
            flying ? Pathfinder.costNone :
            hovering ? Pathfinder.costHover :
            Pathfinder.costGround;
        }

        if(pathCost == null){
            pathCost =
            naval ? ControlPathfinder.costNaval :
            allowLegStep ? ControlPathfinder.costLegs :
            hovering ? ControlPathfinder.costHover :
            ControlPathfinder.costGround;
        }

        pathCostId = ControlPathfinder.costTypes.indexOf(pathCost);
        if(pathCostId == -1) pathCostId = 0;
    }

    @CallSuper
    @Override
    public void init(){
        super.init();

        Unit example = constructor.get();

        checkEntityMapping(example);

        allowLegStep = example instanceof Legsc || example instanceof Crawlc;

        //water preset
        if(example instanceof WaterMovec || example instanceof WaterCrawlc){
            naval = true;
            canDrown = false;
            emitWalkSound = false;
            omniMovement = false;
            immunities.add(StatusEffects.wet);
            if(shadowElevation < 0f){
                shadowElevation = 0.11f;
            }
        }

        initPathType();

        if(flying){
            envEnabled |= Env.space;
        }

        if(lightRadius == -1){
            lightRadius = Math.max(60f, hitSize * 2.3f);
        }

        //if a status effects slows a unit when firing, don't shoot while moving.
        if(autoFindTarget){
            autoFindTarget = !weapons.contains(w -> w.shootStatus.speedMultiplier < 0.99f) || alwaysShootWhenMoving;
        }

        if(flyingLayer < 0) flyingLayer = lowAltitude ? Layer.flyingUnitLow : Layer.flyingUnit;
        clipSize = Math.max(clipSize, lightRadius * 1.1f);
        singleTarget |= weapons.size <= 1 && !forceMultiTarget;

        if(itemCapacity < 0){
            itemCapacity = Math.max(Mathf.round((int)(hitSize * 4f), 10), 10);
        }

        //assume slight range margin
        float margin = 4f;

        //set up default range
        if(range < 0){
            range = Float.MAX_VALUE;
            for(Weapon weapon : weapons){
                if(!weapon.useAttackRange) continue;

                range = Math.min(range, weapon.range() - margin);
                maxRange = Math.max(maxRange, weapon.range() - margin);
            }
        }

        if(maxRange < 0){
            maxRange = Math.max(0f, range);

            for(Weapon weapon : weapons){
                if(!weapon.useAttackRange) continue;

                maxRange = Math.max(maxRange, weapon.range() - margin);
            }
        }

        if(fogRadius < 0){
            //TODO depend on range?
            fogRadius = Math.max(58f * 3f, hitSize * 2f) / 8f;
        }

        if(!weapons.contains(w -> w.useAttackRange)){
            if(range < 0 || range == Float.MAX_VALUE) range = mineRange;
            if(maxRange < 0 || maxRange == Float.MAX_VALUE) maxRange = mineRange;
        }

        if(mechStride < 0){
            mechStride = 4f + (hitSize -8f)/2.1f;
        }

        if(segmentSpacing < 0){
            segmentSpacing = hitSize;
        }

        if(aimDst < 0){
            aimDst = weapons.contains(w -> !w.rotate) ? hitSize * 2f : hitSize / 2f;
        }

        if(stepShake < 0){
            stepShake = Mathf.round((hitSize - 11f) / 9f);
            mechStepParticles = hitSize > 15f;
        }

        if(engineSize > 0){
            engines.add(new UnitEngine(0f, -engineOffset, engineSize, -90f));
        }

        if(treadEffect == null){
            treadEffect = new Effect(50, e -> {
                color(Tmp.c1.set(e.color).mul(1.5f));
                Fx.rand.setSeed(e.id);
                for(int i = 0; i < 3; i++){
                    Fx.v.trns(e.rotation + Fx.rand.range(40f), Fx.rand.random(6f * e.finpow()));
                    Fill.circle(e.x + Fx.v.x + Fx.rand.range(4f), e.y + Fx.v.y + Fx.rand.range(4f), Math.min(e.fout(), e.fin() * e.lifetime / 8f) * hitSize / 28f * 3f * Fx.rand.random(0.8f, 1.1f) + 0.3f);
                }
            }).layer(Layer.debris);
        }

        if(mineBeamOffset == Float.NEGATIVE_INFINITY) mineBeamOffset = hitSize / 2;

        for(Ability ab : abilities){
            ab.init(this);
        }

        //add mirrored weapon variants
        Seq<Weapon> mapped = new Seq<>();
        for(Weapon w : weapons){
            if(w.recoilTime < 0) w.recoilTime = w.reload;
            mapped.add(w);

            //mirrors are copies with X values negated
            if(w.mirror){
                Weapon copy = w.copy();
                copy.flip();
                mapped.add(copy);

                //since there are now two weapons, the reload and recoil time must be doubled
                w.recoilTime *= 2f;
                copy.recoilTime *= 2f;
                w.reload *= 2f;
                copy.reload *= 2f;

                w.otherSide = mapped.size - 1;
                copy.otherSide = mapped.size - 2;
            }
        }
        this.weapons = mapped;

        weapons.each(Weapon::init);

        canHeal = weapons.contains(w -> w.bullet.heals());

        canAttack = weapons.contains(w -> !w.noAttack);

        //assign default commands.
        if(commands.size == 0){

            commands.add(UnitCommand.moveCommand, UnitCommand.enterPayloadCommand);

            if(canBoost){
                commands.add(UnitCommand.boostCommand);

                if(buildSpeed > 0f){
                    commands.add(UnitCommand.rebuildCommand, UnitCommand.assistCommand);
                }
                if(mineTier > 0){
                    commands.add(UnitCommand.mineCommand);
                }
            }

            //healing, mining and building is only supported for flying units; pathfinding to ambiguously reachable locations is hard.
            if(flying){
                if(canHeal){
                    commands.add(UnitCommand.repairCommand);
                }

                if(buildSpeed > 0){
                    commands.add(UnitCommand.rebuildCommand, UnitCommand.assistCommand);
                }

                if(mineTier > 0){
                    commands.add(UnitCommand.mineCommand);
                }
                if(example instanceof Payloadc){
                    commands.addAll(UnitCommand.loadUnitsCommand, UnitCommand.loadBlocksCommand, UnitCommand.unloadPayloadCommand, UnitCommand.loopPayloadCommand);
                }
            }
        }

        if(defaultCommand == null && commands.size > 0){
            defaultCommand = commands.first();
        }

        if(stances.size == 0){
            if(canAttack){
                stances.addAll(UnitStance.stop, UnitStance.holdFire, UnitStance.pursueTarget, UnitStance.patrol);
                if(!flying){
                    stances.add(UnitStance.ram);
                }
            }else{
                stances.addAll(UnitStance.stop, UnitStance.patrol);
            }
        }

        //dynamically create ammo capacity based on firing rate
        if(ammoCapacity < 0){
            float shotsPerSecond = weapons.sumf(w -> w.useAmmo ? 60f / w.reload : 0f);
            //duration of continuous fire without reload
            float targetSeconds = 35;

            ammoCapacity = Math.max(1, (int)(shotsPerSecond * targetSeconds));
        }

        estimateDps();

        //only do this after everything else was initialized
        sample = constructor.get();
    }

    public float estimateDps(){
        //calculate estimated DPS for one target based on weapons
        if(dpsEstimate < 0){
            dpsEstimate = weapons.sumf(Weapon::dps);

            //suicide enemy
            if(weapons.contains(w -> w.bullet.killShooter)){
                //scale down DPS to be insignificant
                dpsEstimate /= 15f;
            }
        }

        return dpsEstimate;
    }

    @CallSuper
    @Override
    public void load(){
        super.load();

        for(var part : parts){
            part.load(name);
        }
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        previewRegion = Core.atlas.find(name + "-preview", name);
        legRegion = Core.atlas.find(name + "-leg");
        jointRegion = Core.atlas.find(name + "-joint");
        baseJointRegion = Core.atlas.find(name + "-joint-base");
        footRegion = Core.atlas.find(name + "-foot");
        treadRegion = Core.atlas.find(name + "-treads");
        itemCircleRegion = Core.atlas.find("ring-item");

        if(treadRegion.found()){
            treadRegions = new TextureRegion[treadRects.length][treadFrames];
            for(int r = 0; r < treadRects.length; r++){
                for(int i = 0; i < treadFrames; i++){
                    treadRegions[r][i] = Core.atlas.find(name + "-treads" + r + "-" + i);
                }
            }
        }
        legBaseRegion = Core.atlas.find(name + "-leg-base", name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));

        mineLaserRegion = Core.atlas.find("minelaser");
        mineLaserEndRegion = Core.atlas.find("minelaser-end");
        //when linear filtering is on, it's acceptable to use the relatively low-res 'particle' region
        softShadowRegion =
            squareShape ? Core.atlas.find("square-shadow") :
            hitSize <= 10f || (Core.settings != null && Core.settings.getBool("linear", true)) ?
                Core.atlas.find("particle") :
                Core.atlas.find("circle-shadow");

        outlineRegion = Core.atlas.find(name + "-outline");
        shadowRegion = fullIcon;

        wreckRegions = new TextureRegion[3];
        for(int i = 0; i < wreckRegions.length; i++){
            wreckRegions[i] = Core.atlas.find(name + "-wreck" + i);
        }

        segmentRegions = new TextureRegion[segments];
        segmentOutlineRegions = new TextureRegion[segments];
        segmentCellRegions = new TextureRegion[segments];
        for(int i = 0; i < segments; i++){
            segmentRegions[i] = Core.atlas.find(name + "-segment" + i);
            segmentOutlineRegions[i] = Core.atlas.find(name + "-segment-outline" + i);
            segmentCellRegions[i] = Core.atlas.find(name + "-segment-cell" + i);
        }

        clipSize = Math.max(region.width * 2f, clipSize);
    }

    public void getRegionsToOutline(Seq<TextureRegion> out){
        for(Weapon weapon : weapons){
            for(var part : weapon.parts){
                part.getOutlines(out);
            }
        }
        for(var part : parts){
            part.getOutlines(out);
        }
    }

    public boolean needsBodyOutline(){
        return alwaysCreateOutline;
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        if(constructor == null) throw new IllegalArgumentException("No constructor set up for unit '" + name + "', add this argument to your units field: `constructor = UnitEntity::create`");

        sample = constructor.get();

        var toOutline = new Seq<TextureRegion>();
        getRegionsToOutline(toOutline);

        for(var region : toOutline){
            if(region instanceof AtlasRegion atlas && !Core.atlas.has(atlas.name + "-outline")){
                String regionName = atlas.name;
                Pixmap outlined = Pixmaps.outline(Core.atlas.getPixmap(region), outlineColor, outlineRadius);

                Drawf.checkBleed(outlined);

                packer.add(PageType.main, regionName + "-outline", outlined);
                outlined.dispose();
            }
        }

        if(outlines){
            Seq<TextureRegion> outlineSeq = Seq.with(region, jointRegion, footRegion, baseJointRegion, legRegion, treadRegion);
            if(Core.atlas.has(name + "-leg-base")){
                outlineSeq.add(legBaseRegion);
            }

            //note that mods with these regions already outlined will have *two* outlines made, which is... undesirable
            for(var outlineTarget : outlineSeq){
                if(!outlineTarget.found()) continue;

                makeOutline(PageType.main, packer, outlineTarget, alwaysCreateOutline && region == outlineTarget, outlineColor, outlineRadius);
            }

            if(sample instanceof Crawlc){
                for(int i = 0; i < segments; i++){
                    makeOutline(packer, segmentRegions[i], name + "-segment-outline" + i, outlineColor, outlineRadius);
                }
            }

            for(Weapon weapon : weapons){
                if(!weapon.name.isEmpty() && (minfo.mod == null || weapon.name.startsWith(minfo.mod.name)) && (weapon.top || !packer.isOutlined(weapon.name) || weapon.parts.contains(p -> p.under))){
                    makeOutline(PageType.main, packer, weapon.region, !weapon.top || weapon.parts.contains(p -> p.under), outlineColor, outlineRadius);
                }
            }
        }

        if(sample instanceof Tankc){
            PixmapRegion pix = Core.atlas.getPixmap(treadRegion);

            for(int r = 0; r < treadRects.length; r++){
                Rect treadRect = treadRects[r];
                //slice is always 1 pixel wide
                Pixmap slice = pix.crop((int)(treadRect.x + pix.width/2f), (int)(treadRect.y + pix.height/2f), 1, (int)treadRect.height);
                int frames = treadFrames;
                for(int i = 0; i < frames; i++){
                    int pullOffset = treadPullOffset;
                    Pixmap frame = new Pixmap(slice.width, slice.height);
                    for(int y = 0; y < slice.height; y++){
                        int idx = y + i;
                        if(idx >= slice.height){
                            idx -= slice.height;
                            idx += pullOffset;
                            idx = Mathf.mod(idx, slice.height);
                        }

                        frame.setRaw(0, y, slice.getRaw(0, idx));
                    }

                    packer.add(PageType.main, name + "-treads" + r + "-" + i, frame);
                    frame.dispose();
                }
                slice.dispose();
            }
        }
    }

    @Override
    public void afterPatch(){
        super.afterPatch();
        totalRequirements = cachedRequirements = firstRequirements = null;

        //this will technically reset any assigned values, but in vanilla, they're not reassigned anyway
        flowfieldPathType = -1;
        pathCost = null;
        pathCostId = -1;
        initPathType();
    }

    /** @return the time required to build this unit, as a value that takes into account reconstructors */
    public float getBuildTime(){
        getTotalRequirements();
        return buildTime;
    }

    /** @return all items needed to build this unit, including reconstructor steps. */
    public ItemStack[] getTotalRequirements(){
        if(totalRequirements == null){
            UnitType[] ret = {null};
            float[] timeret = {0f};
            ItemStack[] result = getRequirements(ret, timeret);

            //prevents stack overflow if requirements are circular and result != null
            totalRequirements = ItemStack.empty;

            if(result != null){
                ItemSeq total = new ItemSeq();

                total.add(result);
                if(ret[0] != null){
                    total.add(ret[0].getTotalRequirements());
                }
                totalRequirements = total.toArray();
            }

            for(var stack : totalRequirements){
                buildTime += stack.item.cost * stack.amount;
            }
        }
        return totalRequirements;
    }

    /** @return item requirements based on reconstructors or factories found; returns previous unit in array if provided */
    public @Nullable ItemStack[] getRequirements(@Nullable UnitType[] prevReturn, @Nullable float[] timeReturn){
        //find reconstructor
        var rec = (Reconstructor)content.blocks().find(b -> b instanceof Reconstructor re && re.upgrades.contains(u -> u[1] == this));

        if(rec != null && rec.findConsumer(i -> i instanceof ConsumeItems) instanceof ConsumeItems ci){
            if(prevReturn != null){
                prevReturn[0] = rec.upgrades.find(u -> u[1] == this)[0];
            }
            if(timeReturn != null){
                timeReturn[0] = rec.constructTime;
            }
            return ci.items;
        }else{
            //find a factory
            var factory = (UnitFactory)content.blocks().find(u -> u instanceof UnitFactory uf && uf.plans.contains(p -> p.unit == this));
            if(factory != null){

                var plan = factory.plans.find(p -> p.unit == this);
                if(timeReturn != null){
                    timeReturn[0] = plan.time;
                }
                return plan.requirements;
            }else{
                //find an assembler
                var assembler = (UnitAssembler)content.blocks().find(u -> u instanceof UnitAssembler a && a.plans.contains(p -> p.unit == this));
                if(assembler != null){
                    var plan = assembler.plans.find(p -> p.unit == this);

                    if(timeReturn != null){
                        timeReturn[0] = plan.time;
                    }
                    ItemSeq reqs = new ItemSeq();
                    for(var bstack : plan.requirements){
                        if(bstack.item instanceof Block block){
                            for(var stack : block.requirements){
                                reqs.add(stack.item, stack.amount * bstack.amount);
                            }
                        }else if(bstack.item instanceof UnitType unit){
                            for(var stack : unit.getTotalRequirements()){
                                reqs.add(stack.item, stack.amount * bstack.amount);
                            }
                        }
                    }
                    return reqs.toArray();
                }
            }
        }
        return null;
    }

    public @Nullable ItemStack[] getFirstRequirements(){
        if(firstRequirements == null){
            firstRequirements = getRequirements(null, null);
        }
        return firstRequirements;
    }

    @Override
    public ItemStack[] researchRequirements(){
        if(cachedRequirements != null){
            return cachedRequirements;
        }

        ItemStack[] stacks = getRequirements(null, null);

        if(stacks != null){
            ItemStack[] out = new ItemStack[stacks.length];
            for(int i = 0; i < out.length; i++){
                out[i] = new ItemStack(stacks[i].item, UI.roundAmount((int)(stacks[i].amount * researchCostMultiplier)));
            }

            //remove zero-requirements for automatic unlocks
            out = Structs.filter(ItemStack.class, out, stack -> stack.amount > 0);

            cachedRequirements = out;

            return out;
        }

        return super.researchRequirements();
    }

    @Override
    public double sense(LAccess sensor){
        return switch(sensor){
            case health, maxHealth -> health;
            case size -> hitSize / tilesize;
            case itemCapacity -> itemCapacity;
            case speed -> speed * 60f / tilesize;
            case payloadCapacity -> sample instanceof Payloadc ? payloadCapacity / tilePayload : 0f;
            case id -> getLogicId();
            default -> Double.NaN;
        };
    }

    @Override
    public Object senseObject(LAccess sensor){
        if(sensor == LAccess.name) return name;
        return noSensed;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    /** Sets up engines, mirroring the contents of the specified array. */
    public void setEnginesMirror(UnitEngine... array){
        for(var base : array){
            engines.add(base);

            var engine = base.copy();
            engine.x *= -1;
            engine.rotation = 180f - engine.rotation;
            if(engine.rotation < 0) engine.rotation += 360f;
            engines.add(engine);
        }
    }

    //region drawing

    public void draw(Unit unit){
        float scl = xscl;
        if(unit.inFogTo(Vars.player.team())) return;

        if(buildSpeed > 0f){
            unit.drawBuilding();
        }

        if(unit.mining()){
            drawMining(unit);
        }

        boolean isPayload = !unit.isAdded();

        Mechc mech = unit instanceof Mechc m ? m : null;
        Segmentc seg = unit instanceof Segmentc c ? c : null;
        float z =
            isPayload ? Draw.z() :
            //dead flying units are assumed to be falling, and to prevent weird clipping issue with the dark "fog", they always draw above it
            unit.elevation > 0.5f || (flying && unit.dead) ? (flyingLayer) :
            seg != null ? groundLayer + seg.segmentIndex() / 4000f * Mathf.sign(segmentLayerOrder) + (!segmentLayerOrder ? 0.01f : 0f) :
            groundLayer + Mathf.clamp(hitSize / 4000f, 0, 0.01f);

        if(!isPayload && (unit.isFlying() || shadowElevation > 0)){
            Draw.z(Math.min(Layer.darkness, z - 1f));
            drawShadow(unit);
        }

        Draw.z(z - 0.02f);

        if(mech != null){
            drawMech(mech);

            //side
            legOffset.trns(mech.baseRotation(), 0f, Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f/Mathf.PI, 1) * mechSideSway, 0f, unit.elevation));

            //front
            legOffset.add(Tmp.v1.trns(mech.baseRotation() + 90, 0f, Mathf.lerp(Mathf.sin(mech.walkExtend(true), 1f/Mathf.PI, 1) * mechFrontSway, 0f, unit.elevation)));

            unit.trns(legOffset.x, legOffset.y);
        }

        if(unit instanceof Tankc){
            drawTank((Unit & Tankc)unit);
        }

        if(unit instanceof Legsc && !isPayload){
            drawLegs((Unit & Legsc)unit);
        }

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));

        if(unit instanceof Payloadc){
            drawPayload((Unit & Payloadc)unit);
        }

        if(drawSoftShadow) drawSoftShadow(unit);

        Draw.z(z);

        if(unit instanceof Crawlc c){
            drawCrawl(c);
        }

        if(drawBody) drawOutline(unit);
        drawWeaponOutlines(unit);
        if(engineLayer > 0) Draw.z(engineLayer);
        if(trailLength > 0 && !naval && (unit.isFlying() || !useEngineElevation)){
            drawTrail(unit);
        }
        if(engines.size > 0) drawEngines(unit);
        Draw.z(z);
        if(drawBody) drawBody(unit);
        if(drawCell && !(unit instanceof Crawlc)) drawCell(unit);
        Draw.scl(scl); //TODO this is a hack for neoplasm turrets
        drawWeapons(unit);
        if(drawItems) drawItems(unit);
        if(!isPayload){
            drawLight(unit);
        }

        if(unit.shieldAlpha > 0 && drawShields){
            drawShield(unit);
        }

        //TODO how/where do I draw under?
        if(parts.size > 0){
            for(int i = 0; i < parts.size; i++){
                var part = parts.get(i);

                WeaponMount mount = unit.mounts.length > part.weaponIndex ? unit.mounts[part.weaponIndex] : null;
                if(mount != null){
                    DrawPart.params.set(mount.warmup, mount.reload / mount.weapon.reload, mount.smoothReload, mount.heat, mount.recoil, mount.charge, unit.x, unit.y, unit.rotation);
                }else{
                    DrawPart.params.set(0f, 0f, 0f, 0f, 0f, 0f, unit.x, unit.y, unit.rotation);
                }

                if(unit instanceof Scaled s){
                    DrawPart.params.life = s.fin();
                }

                applyColor(unit);
                part.draw(DrawPart.params);
            }
        }

        if(!isPayload){
            for(Ability a : unit.abilities){
                Draw.reset();
                a.draw(unit);
            }
        }

        if(mech != null){
            unit.trns(-legOffset.x, -legOffset.y);
        }

        Draw.reset();
    }

    //...where do I put this
    public Color shieldColor(Unit unit){
        return shieldColor == null ? unit.team.color : shieldColor;
    }

    public void drawMining(Unit unit){
        if(drawMineBeam){
            float focusLen = mineBeamOffset + Mathf.absin(Time.time, 1.1f, 0.5f);
            float px = unit.x + Angles.trnsx(unit.rotation, focusLen);
            float py = unit.y + Angles.trnsy(unit.rotation, focusLen);

            drawMiningBeam(unit, px, py);
        }
    }

    public void drawMiningBeam(Unit unit, float px, float py){
        if(!unit.mining()) return;
        float swingScl = 12f, swingMag = tilesize / 8f;
        float flashScl = 0.3f;

        float ex = unit.mineTile.worldx() + Mathf.sin(Time.time + 48, swingScl, swingMag);
        float ey = unit.mineTile.worldy() + Mathf.sin(Time.time + 48, swingScl + 2f, swingMag);

        Draw.z(Layer.flyingUnit + 0.1f);

        Draw.color(Color.lightGray, Color.white, 1f - flashScl + Mathf.absin(Time.time, 0.5f, flashScl));

        Draw.alpha(Renderer.unitLaserOpacity);
        Drawf.laser(mineLaserRegion, mineLaserEndRegion, px, py, ex, ey, 0.75f);

        if(unit.isLocal()){
            Lines.stroke(1f, Pal.accent);
            Lines.poly(unit.mineTile.worldx(), unit.mineTile.worldy(), 4, tilesize / 2f * Mathf.sqrt2, Time.time);
        }

        Draw.color();
    }

    public <T extends Unit & Payloadc> void drawPayload(T unit){
        if(unit.hasPayload()){
            float prev = Draw.z();
            Draw.z(prev - 0.02f);
            Payload pay = unit.payloads().first();
            pay.set(unit.x, unit.y, unit.rotation);
            pay.draw();
            Draw.z(prev);
        }
    }

    public void drawShield(Unit unit){
        float alpha = unit.shieldAlpha();
        float radius = unit.hitSize() * 1.3f;
        Fill.light(unit.x, unit.y, Lines.circleVertices(radius), radius,
            Color.clear,
            Tmp.c2.set(unit.type.shieldColor(unit)).lerp(Color.white, Mathf.clamp(unit.hitTime() / 2f)).a(0.7f * alpha)
        );
    }

    public void drawShadow(Unit unit){
        float e = Mathf.clamp(unit.elevation, shadowElevation, 1f) * shadowElevationScl * (1f - unit.drownTime);
        float x = unit.x + shadowTX * e, y = unit.y + shadowTY * e;
        Floor floor = world.floorWorld(x, y);

        float dest = floor.canShadow ? 1f : 0f;
        //yes, this updates state in draw()... which isn't a problem, because I don't want it to be obvious anyway
        unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

        Draw.rect(shadowRegion, unit.x + shadowTX * e, unit.y + shadowTY * e, unit.rotation - 90);
        Draw.color();
    }

    public void drawSoftShadow(Unit unit){
        drawSoftShadow(unit, 1f);
    }

    public void drawSoftShadow(Unit unit, float alpha){
        drawSoftShadow(unit.x, unit.y, unit.rotation, alpha);
    }

    public void drawSoftShadow(float x, float y, float rotation, float alpha){
        Draw.color(0, 0, 0, 0.4f * alpha);
        float rad = 1.6f;
        float size = Math.max(region.width, region.height) * region.scl() * softShadowScl;
        Draw.rect(softShadowRegion, x, y, size * rad * Draw.xscl, size * rad * Draw.yscl, rotation - 90);
        Draw.color();
    }

    public void drawItems(Unit unit){
        applyColor(unit);

        //draw back items
        if(unit.item() != null && unit.itemTime > 0.01f){
            float sin = Mathf.absin(Time.time, 5f, 1f);
            float size = (itemSize + sin) * unit.itemTime;

            Draw.mixcol(Pal.accent, sin * 0.1f);
            Draw.rect(unit.item().fullIcon,
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            size, size, unit.rotation);
            Draw.mixcol();

            size = ((3f + sin) * unit.itemTime + 0.5f) * 2;
            Draw.color(Pal.accent);
            Draw.rect(itemCircleRegion,
            unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
            unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY),
            size, size);

            if(unit.isLocal() && !renderer.pixelate){
                Fonts.outline.draw(unit.stack.amount + "",
                unit.x + Angles.trnsx(unit.rotation + 180f, itemOffsetY),
                unit.y + Angles.trnsy(unit.rotation + 180f, itemOffsetY) - 3,
                Pal.accent, 0.25f * unit.itemTime / Scl.scl(1f), false, Align.center
                );
            }

            Draw.reset();
        }
    }

    public void drawTrail(Unit unit){
        if(unit.trail == null){
            unit.trail = new Trail(trailLength);
        }
        Trail trail = unit.trail;
        trail.draw(trailColor == null ? unit.team.color : trailColor, (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f) * (useEngineElevation ? unit.elevation : 1f)) * trailScl);
    }

    public void drawEngines(Unit unit){
        if((useEngineElevation ? unit.elevation : 1f) <= 0.0001f) return;

        for(var engine : engines){
            engine.draw(unit);
        }

        Draw.color();
    }

    public void drawWeapons(Unit unit){
        applyColor(unit);

        for(WeaponMount mount : unit.mounts){
            mount.weapon.draw(unit, mount);
        }

        Draw.reset();
    }

    public void drawWeaponOutlines(Unit unit){
        applyColor(unit);
        applyOutlineColor(unit);

        for(WeaponMount mount : unit.mounts){
            if(!mount.weapon.top){
                //apply layer offset, roll it back at the end
                float z = Draw.z();
                Draw.z(z + mount.weapon.layerOffset);

                mount.weapon.drawOutline(unit, mount);

                Draw.z(z);
            }
        }

        Draw.reset();
    }

    public void drawOutline(Unit unit){
        Draw.reset();

        if(Core.atlas.isFound(outlineRegion)){
            applyColor(unit);
            applyOutlineColor(unit);
            Draw.rect(outlineRegion, unit.x, unit.y, unit.rotation - 90);
            Draw.reset();
        }
    }

    public void drawBody(Unit unit){
        applyColor(unit);

        if(unit instanceof UnderwaterMovec){
            Draw.alpha(1f);
            Draw.mixcol(unit.floorOn().mapColor.write(Tmp.c1).mul(0.9f), 1f);
        }

        Draw.rect(region, unit.x, unit.y, unit.rotation - 90);

        Draw.reset();
    }

    public void drawCell(Unit unit){
        applyColor(unit);

        Draw.color(cellColor(unit));
        Draw.rect(cellRegion, unit.x, unit.y, unit.rotation - 90);
        Draw.reset();
    }

    public Color cellColor(Unit unit){
        float f = Mathf.clamp(unit.healthf());
        return Tmp.c1.set(Color.black).lerp(unit.team.color, f + Mathf.absin(Time.time, Math.max(f * 5f, 1f), 1f - f));
    }

    public void drawLight(Unit unit){
        if(lightRadius > 0){
            Drawf.light(unit.x, unit.y, lightRadius, lightColor, lightOpacity);
        }
    }

    public <T extends Unit & Tankc> void drawTank(T unit){
        applyColor(unit);
        Draw.rect(treadRegion, unit.x, unit.y, unit.rotation - 90);

        if(treadRegion.found()){
            int frame = (int)(unit.treadTime()) % treadFrames;
            for(int i = 0; i < treadRects.length; i ++){
                var region = treadRegions[i][frame];
                var treadRect = treadRects[i];
                float xOffset = -(treadRect.x + treadRect.width/2f);
                float yOffset = -(treadRect.y + treadRect.height/2f);

                for(int side : Mathf.signs){
                    Tmp.v1.set(xOffset * side, yOffset).rotate(unit.rotation - 90);
                    Draw.rect(region, unit.x + Tmp.v1.x / 4f, unit.y + Tmp.v1.y / 4f, treadRect.width / 4f, region.height * region.scale / 4f, unit.rotation - 90);
                }
            }
        }
    }

    public <T extends Unit & Legsc> void drawLegs(T unit){
        applyColor(unit);
        Tmp.c3.set(Draw.getMixColor());

        Leg[] legs = unit.legs();

        float ssize = footRegion.width * footRegion.scl() * 1.5f;
        float rotation = unit.baseRotation();
        float invDrown = 1f - unit.drownTime;

        if(footRegion.found()){
            for(Leg leg : legs){
                Drawf.shadow(leg.base.x, leg.base.y, ssize, invDrown);
            }
        }

        //legs are drawn front first
        for(int j = legs.length - 1; j >= 0; j--){
            int i = (j % 2 == 0 ? j/2 : legs.length - 1 - j/2);
            Leg leg = legs[i];
            boolean flip = i >= legs.length/2f;
            int flips = Mathf.sign(flip);

            Vec2 position = unit.legOffset(legOffset, i).add(unit);

            Tmp.v1.set(leg.base).sub(leg.joint).inv().setLength(legExtension);

            if(footRegion.found() && leg.moving && shadowElevation > 0){
                float scl = shadowElevation * invDrown;
                float elev = Mathf.slope(1f - leg.stage) * scl;
                Draw.color(Pal.shadow);
                Draw.rect(footRegion, leg.base.x + shadowTX * elev, leg.base.y + shadowTY * elev, position.angleTo(leg.base));
                Draw.color();
            }

            Draw.mixcol(Tmp.c3, Tmp.c3.a);

            if(footRegion.found()){
                Draw.rect(footRegion, leg.base.x, leg.base.y, position.angleTo(leg.base));
            }

            if(legBaseUnder){
                Lines.stroke(legBaseRegion.height * legRegion.scl() * flips);
                Lines.line(legBaseRegion, leg.joint.x + Tmp.v1.x, leg.joint.y + Tmp.v1.y, leg.base.x, leg.base.y, false);

                Lines.stroke(legRegion.height * legRegion.scl() * flips);
                Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);
            }else{
                Lines.stroke(legRegion.height * legRegion.scl() * flips);
                Lines.line(legRegion, position.x, position.y, leg.joint.x, leg.joint.y, false);

                Lines.stroke(legBaseRegion.height * legRegion.scl() * flips);
                Lines.line(legBaseRegion, leg.joint.x + Tmp.v1.x, leg.joint.y + Tmp.v1.y, leg.base.x, leg.base.y, false);
            }

            if(jointRegion.found()){
                Draw.rect(jointRegion, leg.joint.x, leg.joint.y);
            }
        }

        //base joints are drawn after everything else
        if(baseJointRegion.found()){
            for(int j = legs.length - 1; j >= 0; j--){
                //TODO does the index / draw order really matter?
                Vec2 position = unit.legOffset(legOffset, (j % 2 == 0 ? j/2 : legs.length - 1 - j/2)).add(unit);
                Draw.rect(baseJointRegion, position.x, position.y, rotation);
            }
        }

        if(baseRegion.found()){
            Draw.rect(baseRegion, unit.x, unit.y, rotation - 90);
        }

        Draw.reset();
    }

    public void drawCrawl(Crawlc crawl){
        Unit unit = (Unit)crawl;
        applyColor(unit);

        float crawlTime =
            crawl instanceof Segmentc seg && seg.headSegment() instanceof Crawlc head ? head.crawlTime() + seg.segmentIndex() * segmentPhase * segments :
            crawl.crawlTime();

        for(int p = 0; p < 2; p++){
            TextureRegion[] regions = p == 0 ? segmentOutlineRegions : segmentRegions;

            for(int i = 0; i < segments; i++){
                float trns = Mathf.sin(crawlTime + i * segmentPhase, segmentScl, segmentMag);

                //at segment 0, rotation = segmentRot, but at the last segment it is rotation
                float rot = Mathf.slerp(crawl.segmentRot(), unit.rotation, i / (float)(segments - 1));
                float tx = Angles.trnsx(rot, trns), ty = Angles.trnsy(rot, trns);

                //shadow
                //Draw.color(0f, 0f, 0f, 0.2f);
                //Draw.rect(regions[i], unit.x + tx + 2f, unit.y + ty - 2f, rot - 90);

                //applyColor(unit);

                //TODO merge outlines?
                Draw.rect(regions[i], unit.x + tx, unit.y + ty, rot - 90);

                // Draws the cells
                if(drawCell && p != 0 && segmentCellRegions[i].found()){
                    Draw.color(cellColor(unit));
                    Draw.rect(segmentCellRegions[i], unit.x + tx, unit.y + ty, rot - 90);
                    Draw.reset();
                }
            }
        }
    }

    public void drawMech(Mechc mech){
        Unit unit = (Unit)mech;

        Draw.reset();

        float e = unit.elevation;

        float sin = Mathf.lerp(Mathf.sin(mech.walkExtend(true), 2f / Mathf.PI, 1f), 0f, e);
        float extension = Mathf.lerp(mech.walkExtend(false), 0, e);
        float boostTrns = e * 2f;

        Floor floor = unit.isFlying() ? Blocks.air.asFloor() : unit.floorOn();

        if(floor.isLiquid){
            Draw.color(Color.white, floor.mapColor, 0.5f);
        }

        for(int i : Mathf.signs){
            Draw.mixcol(Tmp.c1.set(mechLegColor).lerp(Color.white, Mathf.clamp(unit.hitTime)), Math.max(Math.max(0, i * extension / mechStride), unit.hitTime));

            Draw.rect(legRegion,
            unit.x + Angles.trnsx(mech.baseRotation(), extension * i - boostTrns, -boostTrns*i),
            unit.y + Angles.trnsy(mech.baseRotation(), extension * i - boostTrns, -boostTrns*i),
            legRegion.width * legRegion.scl() * i,
            legRegion.height * legRegion.scl() * (1 - Math.max(-sin * i, 0) * 0.5f),
            mech.baseRotation() - 90 + 35f*i*e);
        }

        Draw.mixcol(Color.white, unit.hitTime);

        if(unit.lastDrownFloor != null){
            Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        }else{
            Draw.color(Color.white);
        }

        Draw.rect(baseRegion, unit, mech.baseRotation() - 90);

        Draw.mixcol();
    }

    public void applyOutlineColor(Unit unit){
        if(unit.drownTime > 0 && unit.lastDrownFloor != null){
            Draw.color(Color.white, Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.8f), unit.drownTime * 0.9f);
        }
    }

    public void applyColor(Unit unit){
        Draw.color();
        if(healFlash){
            Tmp.c1.set(Color.white).lerp(healColor, Mathf.clamp(unit.healTime - unit.hitTime));
        }
        Draw.mixcol(Tmp.c1, Math.max(unit.hitTime, !healFlash ? 0f : Mathf.clamp(unit.healTime)));

        if(unit.drownTime > 0 && unit.lastDrownFloor != null){
            Draw.mixcol(Tmp.c1.set(unit.lastDrownFloor.mapColor).mul(0.83f), unit.drownTime * 0.9f);
        }
        //this is horribly scuffed.
        if(renderer != null && renderer.overlays != null){
            renderer.overlays.checkApplySelection(unit);
        }
    }

    //endregion

    public static class UnitEngine implements Cloneable{
        public float x, y, radius, rotation;

        public UnitEngine(float x, float y, float radius, float rotation){
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.rotation = rotation;
        }

        public UnitEngine(){
        }

        public void draw(Unit unit){
            UnitType type = unit.type;
            float scale = type.useEngineElevation ? unit.elevation : 1f;

            if(scale <= 0.0001f) return;

            float rot = unit.rotation - 90;
            Color color = type.engineColor == null ? unit.team.color : type.engineColor;

            Tmp.v1.set(x, y).rotate(rot);
            float ex = Tmp.v1.x, ey = Tmp.v1.y;
            float rad = (radius + Mathf.absin(Time.time, 2f, radius / 4f)) * scale;

            //engine outlines (cursed?)
            /*float z = Draw.z();
            Draw.z(z - 0.0001f);
            Draw.color(type.outlineColor);
            Fill.circle(
                unit.x + ex,
                unit.y + ey,
                (type.outlineRadius * Draw.scl + radius + Mathf.absin(Time.time, 2f, radius / 4f)) * scale
            );
            Draw.z(z);*/

            Draw.color(color);
            Fill.circle(
                unit.x + ex,
                unit.y + ey,
                rad
            );
            Draw.color(type.engineColorInner);
            Fill.circle(
                unit.x + ex - Angles.trnsx(rot + rotation, rad / 4f),
                unit.y + ey - Angles.trnsy(rot + rotation, rad / 4f),
                rad / 2f
            );
        }

        public UnitEngine copy(){
            try{
                return (UnitEngine)clone();
            }catch(CloneNotSupportedException awful){
                throw new RuntimeException("fantastic", awful);
            }
        }
    }

}
