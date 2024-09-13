package mindustry.game;

public class CampaignRules{
    public Difficulty difficulty = Difficulty.normal;
    public boolean fog;
    public boolean showSpawns;
    public boolean sectorInvasion;

    public void apply(Rules rules){
        rules.staticFog = rules.fog = fog;
        rules.showSpawns = showSpawns;
        rules.teams.get(rules.waveTeam).blockHealthMultiplier = difficulty.enemyHealthMultiplier;
        rules.teams.get(rules.waveTeam).unitHealthMultiplier = difficulty.enemyHealthMultiplier;
    }
}
