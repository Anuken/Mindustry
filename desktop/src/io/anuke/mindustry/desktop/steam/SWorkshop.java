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
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.mod.Mods.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.*;

public class SWorkshop implements SteamUGCCallback{
    public final SteamUGC ugc = new SteamUGC(this);

    private ObjectMap<Class<? extends Publishable>, Array<FileHandle>> workshopFiles = new ObjectMap<>();
    private ObjectMap<SteamUGCQuery, BiConsumer<Array<SteamUGCDetails>, SteamResult>> detailHandlers = new ObjectMap<>();
    private Array<Consumer<SteamPublishedFileID>> itemHandlers = new Array<>();
    private ObjectMap<SteamPublishedFileID, Runnable> updatedHandlers = new ObjectMap<>();

    public SWorkshop(){
        int items = ugc.getNumSubscribedItems();
        SteamPublishedFileID[] ids = new SteamPublishedFileID[items];
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getSubscribedItems(ids);

        Array<FileHandle> folders = Array.with(ids).map(f -> {
            ugc.getItemInstallInfo(f, info);
            return new FileHandle(info.getFolder());
        }).select(f -> f != null && f.list().length > 0);

        workshopFiles.put(Map.class, folders.select(f -> f.list().length == 1 && f.list()[0].extension().equals(mapExtension)).map(f -> f.list()[0]));
        workshopFiles.put(Schematic.class, folders.select(f -> f.list().length == 1 && f.list()[0].extension().equals(schematicExtension)).map(f -> f.list()[0]));
        workshopFiles.put(LoadedMod.class, folders.select(f -> f.child("mod.json").exists()));

        if(!workshopFiles.get(Map.class).isEmpty()){
            SAchievement.downloadMapWorkshop.complete();
        }

        workshopFiles.each((type, list) -> {
            Log.info("Fetched content ({0}): {1}", type.getSimpleName(), list.size);
        });
    }

    public Array<FileHandle> getWorkshopFiles(Class<? extends Publishable> type){
        return workshopFiles.getOr(type, () -> new Array<>(0));
    }

    /** Publish a new item and submit an update for it.
     * If it is already published, redirects to its page.*/
    public void publish(Publishable p){
        Log.info("publish(): " + p.steamTitle());
        if(p.hasSteamID()){
            Log.info("Content already published, redirecting to ID.");
            viewListing(p);
            return;
        }

        if(!p.prePublish()){
            Log.info("Rejecting due to pre-publish.");
            return;
        }

        showPublish(id -> update(p, id, null));
    }

    /** Update an existing item with a changelog. */
    public void updateItem(Publishable p, String changelog){
        String id = p.getSteamID();
        long handle = Strings.parseLong(id, -1);
        SteamPublishedFileID fid = new SteamPublishedFileID(handle);
        update(p, fid, changelog);
    }

    /** Fetches info for an item, checking to make sure that it exists.*/
    public void viewListing(Publishable p){
        long handle = Strings.parseLong(p.getSteamID(), -1);
        SteamPublishedFileID id = new SteamPublishedFileID(handle);

        ui.loadfrag.show();
        query(ugc.createQueryUGCDetailsRequest(id), (detailsList, result) -> {
            ui.loadfrag.hide();

            if(result == SteamResult.OK){
                SteamUGCDetails details = detailsList.first();
                if(details.getResult() == SteamResult.OK){
                    if(details.getOwnerID().equals(SVars.user.user.getSteamID())){

                        FloatingDialog dialog = new FloatingDialog("$workshop.info");
                        dialog.setFillParent(false);
                        dialog.cont.add("$workshop.menu").pad(20f);
                        dialog.addCloseButton();

                        dialog.buttons.addImageTextButton("$view.workshop", Icon.linkSmall, () -> {
                            viewListingID(id);
                            dialog.hide();
                        }).size(210f, 64f);

                        dialog.buttons.addImageTextButton("$workshop.update", Icon.upgradeSmall, () -> {
                            new FloatingDialog("$workshop.update"){{
                                setFillParent(false);
                                cont.margin(10).add("$changelog").padRight(6f);
                                cont.row();
                                TextArea field = cont.addArea("", t -> {}).size(500f, 160f).get();
                                field.setMaxLength(400);
                                buttons.defaults().size(120, 54).pad(4);
                                buttons.addButton("$ok", () -> {
                                    ui.loadfrag.show("$publishing");
                                    updateItem(p, field.getText().replace("\r", "\n"));
                                    dialog.hide();
                                    hide();
                                });
                                buttons.addButton("$cancel", this::hide);
                            }}.show();

                        }).size(210f, 64f);
                        dialog.show();
                    }else{
                        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + SteamNativeHandle.getNativeHandle(details.getPublishedFileID()));
                    }
                }else if(details.getResult() == SteamResult.FileNotFound){
                    p.removeSteamID();
                    ui.showErrorMessage("$missing");
                }else{
                    ui.showErrorMessage(Core.bundle.format("workshop.error", details.getResult().name()));
                }
            }else{
                ui.showErrorMessage(Core.bundle.format("workshop.error", result.name()));
            }
        });
    }

    void viewListingID(SteamPublishedFileID id){
        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + SteamNativeHandle.getNativeHandle(id));
    }

    void update(Publishable p, SteamPublishedFileID id, String changelog){
        Log.info("Calling update({0})", p.steamTitle());
        String sid = SteamNativeHandle.getNativeHandle(id) + "";

        updateItem(id, h -> {
            if(p.steamDescription() != null){
                ugc.setItemDescription(h, p.steamDescription());
            }

            Array<String> tags = p.extraTags();
            tags.add(p.steamTag());

            ugc.setItemTitle(h, p.steamTitle());
            ugc.setItemTags(h, tags.toArray(String.class));
            ugc.setItemPreview(h, p.createSteamPreview(sid).absolutePath());
            ugc.setItemContent(h, p.createSteamFolder(sid).absolutePath());
            if(changelog == null){
                ugc.setItemVisibility(h, PublishedFileVisibility.Private);
            }
            ugc.submitItemUpdate(h, changelog == null ? "<Created>" : changelog);
        }, () -> p.addSteamID(sid));
    }

    void showPublish(Consumer<SteamPublishedFileID> published){
        FloatingDialog dialog = new FloatingDialog("$confirm");
        dialog.setFillParent(false);
        dialog.cont.add("$publish.confirm").width(600f).wrap();
        dialog.addCloseButton();
        dialog.buttons.addImageTextButton("$eula", Icon.linkSmall,
            () -> SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement"))
            .size(210f, 64f);

        dialog.buttons.addImageTextButton("$ok", Icon.checkSmall, () -> {
            Log.info("Accepted, publishing item...");
            itemHandlers.add(published);
            ugc.createItem(SVars.steamID, WorkshopFileType.Community);
            ui.loadfrag.show("$publishing");
            dialog.hide();
        }).size(170f, 64f);
        dialog.show();
    }

    void query(SteamUGCQuery query, BiConsumer<Array<SteamUGCDetails>, SteamResult> handler){
        Log.info("POST QUERY " + query);
        detailHandlers.put(query, handler);
        ugc.sendQueryUGCRequest(query);
    }

    void updateItem(SteamPublishedFileID publishedFileID, Consumer<SteamUGCUpdateHandle> tagger, Runnable updated){
        try{
            SteamUGCUpdateHandle h = ugc.startItemUpdate(SVars.steamID, publishedFileID);
            Log.info("begin updateItem({0})", publishedFileID.toString());

            tagger.accept(h);
            Log.info("Tagged.");

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

            updatedHandlers.put(publishedFileID, updated);
        }catch(Throwable t){
            ui.loadfrag.hide();
            Log.err(t);
        }
    }

    @Override
    public void onRequestUGCDetails(SteamUGCDetails details, SteamResult result){

    }

    @Override
    public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result){
        Log.info("GET QUERY " + query);

        if(detailHandlers.containsKey(query)){
            Log.info("Query being handled...");
            if(numResultsReturned > 0){
                Log.info("{0} q results", numResultsReturned);
                Array<SteamUGCDetails> details = new Array<>();
                for(int i = 0; i < numResultsReturned; i++){
                    details.add(new SteamUGCDetails());
                    ugc.getQueryUGCResult(query, i, details.get(i));
                }
                detailHandlers.get(query).accept(details, result);
            }else{
                Log.info("Nothing found.");
                detailHandlers.get(query).accept(new Array<>(), SteamResult.FileNotFound);
            }

            detailHandlers.remove(query);
        }else{
            Log.info("Query not handled.");
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
        Log.info("onCreateItem(" + result + ")");
        if(!itemHandlers.isEmpty()){
            if(result == SteamResult.OK){
                Log.info("Passing to first handler.");
                itemHandlers.first().accept(publishedFileID);
            }else{
                ui.showErrorMessage(Core.bundle.format("publish.error ", result.name()));
            }

            itemHandlers.remove(0);
        }else{
            Log.err("No handlers for createItem()");
        }
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

            if(updatedHandlers.containsKey(publishedFileID)){
                updatedHandlers.get(publishedFileID).run();
            }
        }else{
            ui.showErrorMessage(Core.bundle.format("publish.error ", result.name()));
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
