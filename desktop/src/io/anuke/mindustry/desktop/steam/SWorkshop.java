package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamRemoteStorage.*;
import com.codedisaster.steamworks.SteamUGC.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.*;

public class SWorkshop implements SteamUGCCallback{
    public final SteamUGC ugc = new SteamUGC(this);

    private Map lastMap;
    private Array<FileHandle> mapFiles;

    public SWorkshop(){
        int items = ugc.getNumSubscribedItems();
        SteamPublishedFileID[] ids = new SteamPublishedFileID[items];
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getSubscribedItems(ids);
        mapFiles = Array.with(ids).map(f -> {
            ugc.getItemInstallInfo(f, info);
            return new FileHandle(info.getFolder());
        }).select(f -> f.list().length > 0).map(f -> f.list()[0]);

        if(items > 0){
            SAchievement.downloadMapWorkshop.complete();
        }

        Log.info("Fetching {0} subscribed maps.", items);
    }

    public Array<FileHandle> getMapFiles(){
        return mapFiles;
    }

    public void publishMap(Map map){
        if(map.tags.containsKey("steamid")){
            Log.info("Map already published, redirecting to ID.");
            SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + map.tags.get("steamid"));
            return;
        }

        //update author name when publishing
        map.tags.put("author", SVars.net.friends.getPersonaName());

        FloatingDialog dialog = new FloatingDialog("$confirm");
        dialog.setFillParent(false);
        dialog.cont.add("$map.publish.confirm").width(600f).wrap();
        dialog.addCloseButton();
        dialog.buttons.addImageTextButton("$eula", Icon.linkSmall, () -> {
            SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement");
        }).size(210f, 64f);

        dialog.buttons.addImageTextButton("$ok", Icon.checkSmall, () -> {
            this.lastMap = map;
            ugc.createItem(SVars.steamID, WorkshopFileType.Community);
            ui.loadfrag.show("$map.publishing");
            Log.info("Publish map " + map.name());
            dialog.hide();
        }).size(170f, 64f);
        dialog.show();
    }

    @Override
    public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result){

    }

    @Override
    public void onSubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result){
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item subscribed from {0}", info.getFolder());
        SAchievement.downloadMapWorkshop.complete();
    }

    @Override
    public void onUnsubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result){
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item unsubscribed from {0}", info.getFolder());
    }

    @Override
    public void onRequestUGCDetails(SteamUGCDetails details, SteamResult result){

    }

    @Override
    public void onCreateItem(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result){
        if(lastMap == null){
            Log.err("No map to publish?");
            return;
        }

        //SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + publishedFileID.toString());

        Map map = lastMap;
        Log.info("Create item {0} result {1} {2}", SteamNativeHandle.getNativeHandle(publishedFileID), result, needsToAcceptWLA);

        if(result == SteamResult.OK){
            SteamUGCUpdateHandle h = ugc.startItemUpdate(SVars.steamID, publishedFileID);

            Gamemode mode = Gamemode.attack.valid(map) ? Gamemode.attack : Gamemode.survival;
            FileHandle mapFile = tmpDirectory.child("map_" + publishedFileID.toString()).child("map.msav");
            lastMap.file.copyTo(mapFile);

            Log.info(mapFile.parent().absolutePath());
            Log.info(map.previewFile().absolutePath());

            ugc.setItemTitle(h, map.name());
            ugc.setItemDescription(h, map.description());
            ugc.setItemTags(h, new String[]{"map", mode.name()});
            ugc.setItemVisibility(h, PublishedFileVisibility.Private);
            ugc.setItemPreview(h, map.previewFile().absolutePath());
            ugc.setItemContent(h, mapFile.parent().absolutePath());
            ugc.addItemKeyValueTag(h, "mode", mode.name());
            ugc.submitItemUpdate(h, "Map created");

            ItemUpdateInfo info = new ItemUpdateInfo();

            ui.loadfrag.setProgress(() -> {
                ItemUpdateStatus status = ugc.getItemUpdateProgress(h, info);
                ui.loadfrag.setText("$" + status.name().toLowerCase());
                if(status == ItemUpdateStatus.Invalid){
                    ui.loadfrag.setText("$done");
                    return 1f;
                }
                return (float)status.ordinal() / (float)ItemUpdateStatus.values().length;
            });
        }else{
            ui.showErrorMessage(Core.bundle.format("map.publish.error ", result.name()));
        }

        lastMap = null;
    }

    @Override
    public void onSubmitItemUpdate(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result){
        ui.loadfrag.hide();
        Log.info("onsubmititemupdate {0} {1} {2}", publishedFileID, needsToAcceptWLA, result);
        if(result == SteamResult.OK){
            //redirect user to page for further updates
            SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + SteamNativeHandle.getNativeHandle(publishedFileID));
            if(needsToAcceptWLA){
                SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement");
            }
            ui.editor.editor.getTags().put("steamid", SteamNativeHandle.getNativeHandle(publishedFileID) + "");
            try{
                ui.editor.save();
            }catch(Exception e){
                Log.err(e);
            }
            Events.fire(new MapPublishEvent());
        }else{
            ui.showErrorMessage(Core.bundle.format("map.publish.error ", result.name()));
        }

    }

    @Override
    public void onDownloadItemResult(int appID, SteamPublishedFileID publishedFileID, SteamResult result){
        SAchievement.downloadMapWorkshop.complete();
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item downloaded to {0}", info.getFolder());
    }

    @Override
    public void onUserFavoriteItemsListChanged(SteamPublishedFileID publishedFileID, boolean wasAddRequest, SteamResult result){

    }

    @Override
    public void onSetUserItemVote(SteamPublishedFileID publishedFileID, boolean voteUp, SteamResult result){

    }

    @Override
    public void onGetUserItemVote(SteamPublishedFileID publishedFileID, boolean votedUp, boolean votedDown, boolean voteSkipped, SteamResult result){

    }

    @Override
    public void onStartPlaytimeTracking(SteamResult result){

    }

    @Override
    public void onStopPlaytimeTracking(SteamResult result){

    }

    @Override
    public void onStopPlaytimeTrackingForAllItems(SteamResult result){

    }

    @Override
    public void onDeleteItem(SteamPublishedFileID publishedFileID, SteamResult result){
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item removed from {0}", info.getFolder());
    }
}
