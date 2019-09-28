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

    private boolean updated = false;
    private ObjectSet<String> mechs = new ObjectSet<>();
    private int statSavePeriod = 4; //in minutes

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

            Timer.schedule(() -> {
                if(updated){
                    stats.storeStats();
                }
            }, statSavePeriod * 60, statSavePeriod * 60);
        });
    }

    public void onUpdate(){
        this.updated = true;
    }

    private void checkUpdate(){
        if(campaign()){
            SStat.maxUnitActive.max(unitGroups[player.getTeam().ordinal()].size());

            if(unitGroups[player.getTeam().ordinal()].count(u -> u.getType() == UnitTypes.phantom) >= 10){
                active10Phantoms.complete();
            }

            if(unitGroups[player.getTeam().ordinal()].count(u -> u.getType() == UnitTypes.crawler) >= 50){
                active50Crawlers.complete();
            }

            for(Tile tile : state.teams.get(player.getTeam()).cores){
                if(!content.items().contains(i -> i.type == ItemType.material && tile.entity.items.get(i) < tile.block().itemCapacity)){
                    fillCoreAllCampaign.complete();
                    break;
                }
            }
        }
    }

    private void registerEvents(){
        Events.on(UnitDestroyEvent.class, e -> {
            if(ncustom()){
                if(e.unit.getTeam() != Vars.player.getTeam()){
                    SStat.unitsDestroyed.add();

                    if(e.unit instanceof BaseUnit && ((BaseUnit)e.unit).isBoss()){
                        SStat.bossesDefeated.add();
                    }
                }
            }
        });

        Events.on(ZoneConfigureCompleteEvent.class, e -> {
            if(!content.zones().contains(z -> !z.canConfigure())){
                configAllZones.complete();
            }
        });

        Events.on(Trigger.newGame, () -> Core.app.post(() -> {
            if(campaign() && player.getClosestCore() != null && player.getClosestCore().items.total() >= 10 * 1000){
                drop10kitems.complete();
            }
        }));

        Events.on(CommandIssueEvent.class, e -> {
            if(campaign() && e.command == UnitCommand.attack){
                issueAttackCommand.complete();
            }
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            if(campaign() && e.player == player && !e.breaking){
                SStat.blocksBuilt.add();

                if(e.tile.block() == Blocks.router && e.tile.entity.proximity().contains(t -> t.block() == Blocks.router)){
                    chainRouters.complete();
                }

                if(e.tile.block() == Blocks.daggerFactory){
                    buildDaggerFactory.complete();
                }

                if(e.tile.block() == Blocks.meltdown || e.tile.block() == Blocks.spectre){
                    if(e.tile.block() == Blocks.meltdown && !Core.settings.getBool("meltdownp", false)){
                        Core.settings.putSave("meltdownp", true);
                    }

                    if(e.tile.block() == Blocks.spectre && !Core.settings.getBool("spectrep", false)){
                        Core.settings.putSave("spectrep", true);
                    }

                    if(Core.settings.getBool("meltdownp", false) && Core.settings.getBool("spectrep", false)){
                        buildMeltdownSpectre.complete();
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
            if(e.content == Items.thorium) obtainThorium.complete();
            if(e.content == Items.titanium) obtainTitanium.complete();

            if(!content.zones().contains(Zone::locked)){
                unlockAllZones.complete();
            }
        });

        Events.on(Trigger.openWiki, openWiki::complete);

        Events.on(Trigger.exclusionDeath, dieExclusion::complete);

        Events.on(Trigger.drown, drown::complete);

        trigger(Trigger.impactPower, powerupImpactReactor);

        trigger(Trigger.flameAmmo, useFlameAmmo);

        trigger(Trigger.turretCool, coolTurret);

        trigger(Trigger.suicideBomb, suicideBomb);

        Events.on(Trigger.enablePixelation, enablePixelation::complete);

        Events.on(Trigger.thoriumReactorOverheat, () -> {
            if(campaign()){
                SStat.reactorsOverheated.add();
            }
        });

        trigger(Trigger.shock, shockWetEnemy);

        trigger(Trigger.phaseDeflectHit, killEnemyPhaseWall);

        trigger(Trigger.itemLaunch, launchItemPad);

        Events.on(UnitCreateEvent.class, e -> {
            if(campaign() && e.unit.getTeam() == player.getTeam()){
                SStat.unitsBuilt.add();
            }
        });

        Events.on(LoseEvent.class, e -> {
            if(campaign()){
                if(world.getZone().metCondition() && (state.wave - world.getZone().conditionWave) / world.getZone().launchPeriod >= 1){
                    skipLaunching2Death.complete();
                }
            }
        });

        Events.on(LaunchEvent.class, e -> {
            int total = 0;
            for(Item item : Vars.content.items()){
                total += Vars.state.stats.itemsDelivered.get(item, 0);
            }

            if(state.rules.tutorial){
                completeTutorial.complete();
            }

            SStat.timesLaunched.add();
            SStat.itemsLaunched.add(total);
        });

        Events.on(WaveEvent.class, e -> {
            if(ncustom()){
                SStat.maxWavesSurvived.max(Vars.state.wave);

                if(state.stats.buildingsBuilt == 0 && state.wave >= 10){
                    survive10WavesNoBlocks.complete();
                }
            }
        });

        Events.on(PlayerJoin.class, e -> {
            if(Vars.net.server()){
                SStat.maxPlayersServer.max(Vars.playerGroup.size());
            }
        });

        Events.on(ResearchEvent.class, e -> {
            if(e.content == Blocks.router) researchRouter.complete();

            if(!TechTree.all.contains(t -> t.block.locked())){
                researchAll.complete();
            }
        });

        Events.on(WinEvent.class, e -> {
            if(campaign()){
                if(Vars.state.wave <= 5 && state.rules.attackMode){
                    defeatAttack5Waves.complete();
                }

                if(Vars.state.rules.attackMode){
                    SStat.attacksWon.add();
                }

                RankResult result = state.stats.calculateRank(world.getZone(), state.launched);
                if(result.rank == Rank.S) earnSRank.complete();
                if(result.rank == Rank.SS) earnSSRank.complete();
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

    private void trigger(Trigger trigger, SAchievement ach){
        Events.on(trigger, () -> {
            if(campaign()){
                ach.complete();
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
        registerEvents();

        if(result != SteamResult.OK){
            Log.err("Failed to recieve steam stats: {0}", result);
        }else{
            Log.err("Recieved steam stats.");
        }
    }

    @Override
    public void onUserStatsStored(long gameID, SteamResult result){
        Log.info("Stored stats: {0}", result);

        if(result == SteamResult.OK){
            updated = true;
        }
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
