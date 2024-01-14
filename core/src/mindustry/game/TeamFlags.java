package mindustry.game;

/** A list of strictly-positive flags for team interactions. */
public class TeamFlags{
    //These flags control whether the source team:
    public static final int

    //will not default targeting the other team
    isNeutral = 1,
    //is unable to target and damage the other team (excluding reactors)
    cannotDamage = 1 << 1,
    //can replace and/or upgrade buildings
    canPlaceOver = 1 << 2,
    //can deconstruct the other team's buildings
    canDeconstruct = 1 << 3,
    //can help (de)construct unfinished buildings of the other team
    canAssistBuilding = 1 << 4,
    //can use the other team's buildings as targets (including connecting them in cursed cross-team power graphs)
    canAttach = 1 << 5,
    //can configure and see data of the other team's buildings
    canInteract = 1 << 6,
    //will ignore the no-build radius of the other team
    ignoresBuildRadius = 1 << 7,
    //can logic control the other team's blocks
    canLogicBlocks = 1 << 8,
    //can logic control (but not naturally bind) the other team's units
    canLogicUnits = 1 << 9,
    //can give items and payloads to the other team, including via blocks
    canGiveItems = 1 << 10,
    //can take items and payloads from the other team, primarily with units
    canTakeItems = 1 << 11,
    //can see through fog with the other team
    canSeeExplored = 1 << 12,

    //enemies don't get any positive attributes
    genericEnemy = 0,
    //derelict buildings aren't owned by anyone
    derelictTarget = canTakeItems | canGiveItems | canLogicBlocks | ignoresBuildRadius |
                     canInteract | canDeconstruct | isNeutral,
    //true neutrals don't interact
    genericNeutral = isNeutral,
    //player-controlled neutrals usually interact differently
    greedyNeutral = canGiveItems | isNeutral,
    //"allies" are considered more cooperative than neutral teams
    genericAlly = canSeeExplored | canGiveItems | canAttach | canAssistBuilding |
                  canPlaceOver | cannotDamage | isNeutral,
    //lenient check for if a team is considered allied or not
    allyCheck = cannotDamage | isNeutral,
    //normally attributed to own teams or trusted teams, full access
    genericSelf = 0x1fff;
}