package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamRemoteStorage.*;
import com.codedisaster.steamworks.SteamUGC.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.scene.ui.*;
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
    private Array<FileHandle> modFiles;
    private ObjectMap<SteamUGCQuery, BiConsumer<Array<SteamUGCDetails>, SteamResult>> detailHandlers = new ObjectMap<>();

    public SWorkshop(){
        int items = ugc.getNumSubscribedItems();
        SteamPublishedFileID[] ids = new SteamPublishedFileID[items];
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getSubscribedItems(ids);

        Array<FileHandle> folders = Array.with(ids).map(f -> {
            ugc.getItemInstallInfo(f, info);
            return new FileHandle(info.getFolder());
        }).select(f -> f != null && f.list().length > 0);

        mapFiles = folders.select(f -> f.list().length == 1 && f.list()[0].extension().equals(mapExtension)).map(f -> f.list()[0]);
        modFiles = folders.select(f -> f.child("mod.json").exists());

        if(!mapFiles.isEmpty()){
            SAchievement.downloadMapWorkshop.complete();
        }

        Log.info("Fetching {0} subscribed maps.", mapFiles.size);
        Log.info("Fetching {0} subscribed mods.", modFiles.size);
    }

    public Array<FileHandle> getMapFiles(){
        return mapFiles;
    }

    public Array<FileHandle> getModFiles(){
        return modFiles;
    }

    public void publishMap(Map map){
        if(map.tags.containsKey("steamid")){
            Log.info("Map already published, redirecting to ID.");
            SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + map.tags.get("steamid"));
            return;
        }

        //update author name when publishing
        map.tags.put("author", SVars.net.friends.getPersonaName());
        ui.editor.editor.getTags().put("author", map.tags.get("author"));
        ui.editor.save();

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

    public void viewMapListingInfo(Map map){
        String id = map.tags.get("steamid");
        long handle = Strings.parseLong(id, -1);
        SteamPublishedFileID fid = new SteamPublishedFileID(handle);

        Log.info("Requesting map listing view; id = " + id);

        ui.loadfrag.show();
        SteamUGCQuery query = ugc.createQueryUGCDetailsRequest(fid);
        Log.info("POST " + query);

        detailHandlers.put(query, (detailsList, result) -> {
            ui.loadfrag.hide();

            Log.info("Map listing result: " + result + " " + detailsList);

            if(result == SteamResult.OK){
                SteamUGCDetails details = detailsList.first();
                if(details.getResult() == SteamResult.OK){
                    if(details.getOwnerID().equals(SVars.user.user.getSteamID())){

                        FloatingDialog dialog = new FloatingDialog("$editor.mapinfo");
                        dialog.setFillParent(false);
                        dialog.cont.add("$map.menu").pad(20f);
                        dialog.addCloseButton();

                        dialog.buttons.addImageTextButton("$view.workshop", Icon.linkSmall, () -> {
                            platform.viewListing(id);
                            dialog.hide();
                        }).size(210f, 64f);

                        dialog.buttons.addImageTextButton("$map.update", Icon.upgradeSmall, () -> {
                            new FloatingDialog("$map.update"){{
                                setFillParent(false);
                                cont.margin(10).add("$map.changelog").padRight(6f);
                                cont.row();
                                TextArea field = cont.addArea("", t -> {}).size(500f, 160f).get();
                                field.setMaxLength(400);
                                buttons.defaults().size(120, 54).pad(4);
                                buttons.addButton("$ok", () -> {
                                    ui.loadfrag.show("$map.publishing");
                                    lastMap = map;
                                    updateMap(map, details.getPublishedFileID(), field.getText().replace("\r", "\n"));
                                    dialog.hide();
                                    hide();

                                    Log.info("Update map " + map.name());
                                });
                                buttons.addButton("$cancel", this::hide);
                            }}.show();

                        }).size(210f, 64f);
                        dialog.show();

                    }else{
                        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + SteamNativeHandle.getNativeHandle(details.getPublishedFileID()));
                    }
                }else if(details.getResult() == SteamResult.FileNotFound){
                    //force-remove tags
                    ui.editor.editor.getTags().remove("steamid");
                    map.tags.remove("steamid");
                    ui.editor.save();

                    ui.showErrorMessage("$map.missing");
                }else{
                    ui.showErrorMessage(Core.bundle.format("map.load.error", result.name()));
                }
            }else{
                ui.showErrorMessage(Core.bundle.format("map.load.error", result.name()));
            }
        });

        ugc.sendQueryUGCRequest(query);
    }

    @Override
    public void onRequestUGCDetails(SteamUGCDetails details, SteamResult result){

    }

    @Override
    public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result){
        Log.info("GET " + query);

        if(detailHandlers.containsKey(query)){
            if(numResultsReturned > 0){
                Array<SteamUGCDetails> details = new Array<>();
                for(int i = 0; i < numResultsReturned; i++){
                    details.set(i, new SteamUGCDetails());
                    ugc.getQueryUGCResult(query, i, details.get(i));
                }
                detailHandlers.get(query).accept(details, result);
            }else{
                detailHandlers.get(query).accept(new Array<>(), SteamResult.FileNotFound);
            }

            detailHandlers.remove(query);
        }
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
    public void onCreateItem(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result){
        if(lastMap == null){
            Log.err("No map to publish?");
            return;
        }

        //SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + publishedFileID.toString());

        Map map = lastMap;
        Log.info("Create item {0} result {1} {2}", SteamNativeHandle.getNativeHandle(publishedFileID), result, needsToAcceptWLA);

        if(result == SteamResult.OK){
            updateMap(map, publishedFileID, "<Map Created>");
        }else{
            ui.showErrorMessage(Core.bundle.format("map.publish.error ", result.name()));
        }

        lastMap = null;
    }

    void updateMap(Map map, SteamPublishedFileID publishedFileID, String changelog){
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
        ugc.submitItemUpdate(h, changelog);

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
