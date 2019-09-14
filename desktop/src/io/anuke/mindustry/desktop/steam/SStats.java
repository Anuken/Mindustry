package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Stats.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.mindustry.desktop.steam.SAchievement.*;

@SuppressWarnings("unchecked")
public class SStats implements SteamUserStatsCallback{
    public final SteamUserStats stats = new SteamUserStats(this);

    //todo store stats periodically
    private boolean updated = false;
    private ObjectSet<String> mechs = new ObjectSet<>();

    public SStats(){
        stats.requestCurrentStats();

        Events.on(ClientLoadEvent.class, e -> {
            mechs = Core.settings.getObject("mechs", ObjectSet.class, ObjectSet::new);

            Core.app.addListener(new ApplicationListener(){
                Interval i = new Interval();
                @Override
                public void update(){
                    if(i.get(60f / 4f)){
                        checkUpdate();
                    }
                }
            });
        });
    }

    public void onUpdate(){
        this.updated = true;
    }

    private void checkUpdate(){
        if(campaign()){
            SStat.maxUnitActive.max(unitGroups[player.getTeam().ordinal()].size());

            if(unitGroups[player.getTeam().ordinal()].count(u -> u.getType() == UnitTypes.phantom) >= 10){
                active10Phantoms.achieved();
            }

            if(unitGroups[player.getTeam().ordinal()].count(u -> u.getType() == UnitTypes.crawler) >= 50){
                active50Crawlers.achieved();
            }

            for(Tile tile : state.teams.get(player.getTeam()).cores){
                if(!content.items().contains(i -> i.type == ItemType.material && tile.entity.items.get(i) < tile.block().itemCapacity)){
                    fillCoreAllCampaign.achieved();
                    break;
                }
            }
        }
    }

    private void registerEvents(){
        Events.on(UnitDestroyEvent.class, e -> {
            if(ncustom()){
                if(e.unit.getTeam() != Vars.player.getTeam()){
                    SStat.enemiesDestroyed.add();

                    if(e.unit instanceof BaseUnit && ((BaseUnit)e.unit).isBoss()){
                        SStat.bossesDefeated.add();
                    }
                }
            }
        });

        Events.on(ZoneConfigureCompleteEvent.class, e -> {
            if(!content.zones().contains(z -> !z.canConfigure())){
                configAllZones.achieved();
            }
        });

        Events.on(Trigger.newGame, () -> Core.app.post(() -> {
            if(campaign() && player.getClosestCore() != null && player.getClosestCore().items.total() >= 10 * 1000){
                drop10kitems.achieved();
            }
        }));

        Events.on(CommandIssueEvent.class, e -> {
            if(campaign() && e.command == UnitCommand.attack){
                issueAttackcommand.achieved();
            }
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            if(campaign() && e.player == player && !e.breaking){
                SStat.blocksBuilt.add();

                if(e.tile.block() == Blocks.router && e.tile.entity.proximity().contains(t -> t.block() == Blocks.router)){
                    chainRouters.achieved();
                }

                if(e.tile.block() == Blocks.daggerFactory){
                    buildDaggerFactory.achieved();
                }

                if(e.tile.block() == Blocks.meltdown || e.tile.block() == Blocks.spectre){
                    if(e.tile.block() == Blocks.meltdown && !Core.settings.getBool("meltdownp", false)){
                        Core.settings.putSave("meltdownp", true);
                    }

                    if(e.tile.block() == Blocks.spectre && !Core.settings.getBool("spectrep", false)){
                        Core.settings.putSave("spectrep", true);
                    }

                    if(Core.settings.getBool("meltdownp", false) && Core.settings.getBool("spectrep", false)){
                        buildMeltdownSpectre.achieved();
                    }
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(campaign() && e.tile.getTeam() != player.getTeam()){
                SStat.blocksDestroyed.add();
            }
        });

        Events.on(MapMakeEvent.class, e -> SStat.mapsMade.add());

        Events.on(MapPublishEvent.class, e -> SStat.mapsPublished.add());

        Events.on(UnlockEvent.class, e -> {
            if(e.content == Items.thorium) obtainThorium.achieved();
            if(e.content == Items.titanium) obtainTitanium.achieved();
        });

        Events.on(Trigger.exclusionDeath, dieExclusion::achieved);

        Events.on(Trigger.drown, drown::achieved);

        Events.on(Trigger.impactPower, powerupImpactReactor::achieved);

        Events.on(Trigger.flameAmmo, useFlameAmmo::achieved);

        Events.on(Trigger.turretCool, coolTurret::achieved);

        Events.on(Trigger.enablePixelation, enablePixelation::achieved);

        Events.on(Trigger.suicideBomb, suicideBomb::achieved);

        Events.on(Trigger.thoriumReactorOverheat, SStat.reactorsOverheated::add);

        Events.on(Trigger.tutorialComplete, completeTutorial::achieved);

        Events.on(Trigger.shock, shockWetEnemy::achieved);

        Events.on(Trigger.phaseDeflectHit, killEnemyPhaseWall::achieved);

        Events.on(Trigger.itemLaunch, launchItemPad::achieved);

        Events.on(UnitCreateEvent.class, e -> {
            if(campaign() && e.unit.getTeam() == player.getTeam()){
                SStat.unitsBuilt.add();
            }
        });

        Events.on(LoseEvent.class, e -> {
            if(campaign()){
                if(world.getZone().metCondition() && (state.wave - world.getZone().conditionWave) / world.getZone().launchPeriod >= 1){
                    skipLaunching2Death.achieved();
                }
            }
        });

        Events.on(LaunchEvent.class, e -> {
            int total = 0;
            for(Item item : Vars.content.items()){
                total += Vars.state.stats.itemsDelivered.get(item, 0);
            }

            SStat.timesLaunched.add();
            SStat.itemsLaunched.add(total);
        });

        Events.on(WaveEvent.class, e -> {
            if(ncustom()){
                SStat.maxWavesSurvived.max(Vars.state.wave);

                if(state.stats.buildingsBuilt == 0 && state.wave >= 10){
                    survive10WavesNoBlocks.achieved();
                }
            }
        });

        Events.on(PlayerJoin.class, e -> {
            if(Vars.net.server()){
                SStat.maxPlayersServer.max(Vars.playerGroup.size());
            }
        });

        Events.on(ResearchEvent.class, e -> {
            if(e.content == Blocks.router) researchRouter.achieved();
            if(e.content == Blocks.launchPad) researchLaunchPad.achieved();

            if(!TechTree.all.contains(t -> t.block.locked())){
                researchAll.achieved();
            }
        });

        Events.on(WinEvent.class, e -> {
            if(campaign()){
                if(Vars.state.wave <= 5){
                    defeatAttack5Waves.achieved();
                }

                if(Vars.state.rules.attackMode){
                    SStat.attacksWon.add();
                }

                RankResult result = state.stats.calculateRank(world.getZone(), state.launched);
                if(result.rank == Rank.S) earnSRank.achieved();
                if(result.rank == Rank.SS) earnSSRank.achieved();
            }

            if(state.rules.pvp){
                SStat.pvpsWon.add();
            }
        });

        Events.on(MechChangeEvent.class, e -> {
            if(campaign()){
                if(mechs.add(e.mech.name)){
                    SStat.zoneMechsUsed.max(mechs.size);
                    Core.settings.putObject("mechs", mechs);
                    Core.settings.save();
                }
            }
        });
    }

    private boolean ncustom(){
        return campaign();
    }

    private boolean campaign(){
        return Vars.world.isZone();
    }

    @Override
    public void onUserStatsReceived(long gameID, SteamID steamID, SteamResult result){
        if(result == SteamResult.OK){
            registerEvents();
        }else{
            Log.err("Failed to recieve steam stats: {0}", result);
        }
    }

    @Override
    public void onUserStatsStored(long l, SteamResult steamResult){

    }

    @Override
    public void onUserStatsUnloaded(SteamID steamID){

    }

    @Override
    public void onUserAchievementStored(long l, boolean b, String s, int i, int i1){

    }

    @Override
    public void onLeaderboardFindResult(SteamLeaderboardHandle steamLeaderboardHandle, boolean b){

    }

    @Override
    public void onLeaderboardScoresDownloaded(SteamLeaderboardHandle steamLeaderboardHandle, SteamLeaderboardEntriesHandle steamLeaderboardEntriesHandle, int i){

    }

    @Override
    public void onLeaderboardScoreUploaded(boolean b, SteamLeaderboardHandle steamLeaderboardHandle, int i, boolean b1, int i1, int i2){

    }

    @Override
    public void onGlobalStatsReceived(long l, SteamResult steamResult){

    }
}
