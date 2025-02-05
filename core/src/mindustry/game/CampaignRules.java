package mindustry.game;

import mindustry.type.*;

public class CampaignRules{
    public Difficulty difficulty = Difficulty.normal;
    public boolean fog;
    public boolean showSpawns;
    public boolean sectorInvasion;
    public boolean randomWaveAI;
    public boolean legacyLaunchPads;

    public void apply(Planet planet, Rules rules){
        rules.staticFog = rules.fog = fog;
        rules.showSpawns = showSpawns;
        rules.randomWaveAI = randomWaveAI;
        rules.objectiveTimerMultiplier = difficulty.waveTimeMultiplier;
        rules.teams.get(rules.waveTeam).blockHealthMultiplier = difficulty.enemyHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitHealthMultiplier = difficulty.enemyHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitCostMultiplier = 1f / difficulty.enemySpawnMultiplier;
        rules.teams.get(rules.waveTeam).unitBuildSpeedMultiplier = difficulty.enemySpawnMultiplier;
    }
}
