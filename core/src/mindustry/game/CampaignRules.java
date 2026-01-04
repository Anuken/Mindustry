package mindustry.game;

import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;

public class CampaignRules{
    public Difficulty difficulty = Difficulty.normal;
    public boolean fog;
    public boolean showSpawns;
    public boolean sectorInvasion;
    public boolean randomWaveAI;
    public boolean legacyLaunchPads;
    public boolean rtsAI;
    public boolean clearSectorOnLose;

    public void apply(Planet planet, Rules rules){
        rules.staticFog = rules.fog = fog;
        rules.showSpawns = showSpawns;
        rules.randomWaveAI = randomWaveAI;
        rules.objectiveTimerMultiplier = difficulty.waveTimeMultiplier;
        if(planet.showRtsAIRule){
            boolean enabled = rtsAI && rules.attackMode; //if attackMode becomes false (waves enabled), turn off RTS AI
            boolean swapped = rules.teams.get(rules.waveTeam).rtsAi != enabled;

            rules.teams.get(rules.waveTeam).rtsAi = enabled;
            rules.teams.get(rules.waveTeam).rtsMaxSquad = 15;

            if(swapped && Vars.state.isGame()){
                Groups.unit.each(u -> {
                    if(u.team == rules.waveTeam && !u.isPlayer()){
                        u.resetController();
                    }
                });
            }
        }
        rules.teams.get(rules.waveTeam).blockHealthMultiplier = difficulty.enemyHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitHealthMultiplier = difficulty.enemyHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitCostMultiplier = 1f / difficulty.enemySpawnMultiplier;
        rules.teams.get(rules.waveTeam).unitBuildSpeedMultiplier = difficulty.enemySpawnMultiplier;
    }
}
