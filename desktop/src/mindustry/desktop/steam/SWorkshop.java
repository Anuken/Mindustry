package mindustry.desktop.steam;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamRemoteStorage.*;
import com.codedisaster.steamworks.SteamUGC.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.mod.Mods.*;
import mindustry.service.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class SWorkshop implements SteamUGCCallback{
    public final SteamUGC ugc = new SteamUGC(this);

    private ObjectMap<Class<? extends Publishable>, Seq<Fi>> workshopFiles = new ObjectMap<>();
    private ObjectMap<SteamUGCQuery, Cons2<Seq<SteamUGCDetails>, SteamResult>> detailHandlers = new ObjectMap<>();
    private Seq<Cons<SteamPublishedFileID>> itemHandlers = new Seq<>();
    private ObjectMap<SteamPublishedFileID, Runnable> updatedHandlers = new ObjectMap<>();

    public SWorkshop(){
        int items = ugc.getNumSubscribedItems();
        SteamPublishedFileID[] ids = new SteamPublishedFileID[items];
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getSubscribedItems(ids);

        Seq<Fi> folders = Seq.with(ids)
            .map(f -> !ugc.getItemInstallInfo(f, info) || info.getFolder() == null ? null : new Fi(info.getFolder()))
            .select(f -> f != null && f.list().length > 0);

        workshopFiles.put(Map.class, folders.select(f -> f.list().length == 1 && f.list()[0].extension().equals(mapExtension)).map(f -> f.list()[0]));
        workshopFiles.put(Schematic.class, folders.select(f -> f.list().length == 1 && f.list()[0].extension().equals(schematicExtension)).map(f -> f.list()[0]));
        workshopFiles.put(LoadedMod.class, folders.select(f -> f.child("mod.json").exists() || f.child("mod.hjson").exists()));

        if(!workshopFiles.get(Map.class).isEmpty()){
            Achievement.downloadMapWorkshop.complete();
        }

        workshopFiles.each((type, list) -> {
            Log.info("Fetched content (@): @", type.getSimpleName(), list.size);
        });
    }

    public Seq<Fi> getWorkshopFiles(Class<? extends Publishable> type){
        return workshopFiles.get(type, () -> new Seq<>(0));
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

        showPublish(id -> update(p, id, null, true));
    }

    /** Fetches info for an item, checking to make sure that it exists.*/
    public void viewListing(Publishable p){
        long handle = Strings.parseLong(p.getSteamID(), -1);
        SteamPublishedFileID id = new SteamPublishedFileID(handle);
        Log.info("Handle = " + handle);

        ui.loadfrag.show();
        query(ugc.createQueryUGCDetailsRequest(id), (detailsList, result) -> {
            ui.loadfrag.hide();
            Log.info("Fetch result = " + result);

            if(result == SteamResult.OK){
                SteamUGCDetails details = detailsList.first();
                Log.info("Details result = " + details.getResult());
                if(details.getResult() == SteamResult.OK){
                    if(details.getOwnerID().equals(SVars.user.user.getSteamID())){

                        BaseDialog dialog = new BaseDialog("@workshop.info");
                        dialog.setFillParent(false);
                        dialog.cont.add("@workshop.menu").pad(20f);
                        dialog.addCloseButton();

                        dialog.buttons.button("@view.workshop", Icon.link, () -> {
                            viewListingID(id);
                            dialog.hide();
                        }).size(210f, 64f);

                        dialog.buttons.button("@workshop.update", Icon.up, () -> {
                            new BaseDialog("@workshop.update"){{
                                setFillParent(false);
                                cont.margin(10).add("@changelog").padRight(6f);
                                cont.row();
                                TextArea field = cont.area("", t -> {}).size(500f, 160f).get();
                                field.setMaxLength(400);
                                cont.row();

                                boolean[] updatedesc = {false};

                                cont.check("@updatedesc", b -> updatedesc[0] = b).pad(4);

                                buttons.defaults().size(120, 54).pad(4);
                                buttons.button("@ok", () -> {
                                    if(!p.prePublish()){
                                        Log.info("Rejecting due to pre-publish.");
                                        return;
                                    }

                                    ui.loadfrag.show("@publishing");
                                    SWorkshop.this.update(p, new SteamPublishedFileID(Strings.parseLong(p.getSteamID(), -1)), field.getText().replace("\r", "\n"), updatedesc[0]);
                                    dialog.hide();
                                    hide();
                                });
                                buttons.button("@cancel", this::hide);
                            }}.show();

                        }).size(210f, 64f);
                        dialog.show();
                    }else{
                        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + details.getPublishedFileID().handle());
                    }
                }else if(details.getResult() == SteamResult.FileNotFound){
                    p.removeSteamID();
                    ui.showErrorMessage("@missing");
                }else{
                    ui.showErrorMessage(Core.bundle.format("workshop.error", details.getResult().name()));
                }
            }else{
                ui.showErrorMessage(Core.bundle.format("workshop.error", result.name()));
            }
        });
    }

    void viewListingID(SteamPublishedFileID id){
        SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + id.handle());
    }

    void update(Publishable p, SteamPublishedFileID id, String changelog, boolean updateDescription){
        Log.info("Calling update(@) @", p.steamTitle(), id.handle());
        String sid = id.handle() + "";

        updateItem(id, h -> {
            if(updateDescription){
                ugc.setItemTitle(h, p.steamTitle());
                if(p.steamDescription() != null){
                    ugc.setItemDescription(h, p.steamDescription());
                }
            }

            Seq<String> tags = p.extraTags();
            tags.add(p.steamTag());

            ugc.setItemTags(h, tags.toArray(String.class));
            ugc.setItemPreview(h, p.createSteamPreview(sid).absolutePath());
            ugc.setItemContent(h, p.createSteamFolder(sid).absolutePath());
            if(changelog == null){
                ugc.setItemVisibility(h, PublishedFileVisibility.Private);
            }
            ugc.submitItemUpdate(h, changelog == null ? "<Created>" : changelog);

            if(p instanceof Map){
                Achievement.publishMap.complete();
            }
        }, () -> p.addSteamID(sid));
    }

    void showPublish(Cons<SteamPublishedFileID> published){
        BaseDialog dialog = new BaseDialog("@confirm");
        dialog.setFillParent(false);
        dialog.cont.add("@publish.confirm").width(600f).wrap();
        dialog.addCloseButton();
        dialog.buttons.button("@eula", Icon.link,
            () -> SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement"))
            .size(210f, 64f);

        dialog.buttons.button("@ok", Icon.ok, () -> {
            Log.info("Accepted, publishing item...");
            itemHandlers.add(published);
            ugc.createItem(SVars.steamID, WorkshopFileType.Community);
            ui.loadfrag.show("@publishing");
            dialog.hide();
        }).size(170f, 64f);
        dialog.show();
    }

    void query(SteamUGCQuery query, Cons2<Seq<SteamUGCDetails>, SteamResult> handler){
        Log.info("POST QUERY " + query);
        detailHandlers.put(query, handler);
        ugc.sendQueryUGCRequest(query);
    }

    void updateItem(SteamPublishedFileID publishedFileID, Cons<SteamUGCUpdateHandle> tagger, Runnable updated){
        try{
            SteamUGCUpdateHandle h = ugc.startItemUpdate(SVars.steamID, publishedFileID);
            Log.info("begin updateItem(@)", publishedFileID.handle());

            tagger.get(h);
            Log.info("Tagged.");

            ItemUpdateInfo info = new ItemUpdateInfo();

            ui.loadfrag.setProgress(() -> {
                ItemUpdateStatus status = ugc.getItemUpdateProgress(h, info);
                ui.loadfrag.setText("@" + status.name().toLowerCase());
                if(status == ItemUpdateStatus.Invalid){
                    ui.loadfrag.setText("@done");
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
    public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result){
        Log.info("GET QUERY " + query);

        if(detailHandlers.containsKey(query)){
            Log.info("Query being handled...");
            if(numResultsReturned > 0){
                Log.info("@ q results", numResultsReturned);
                Seq<SteamUGCDetails> details = new Seq<>();
                for(int i = 0; i < numResultsReturned; i++){
                    details.add(new SteamUGCDetails());
                    ugc.getQueryUGCResult(query, i, details.get(i));
                }
                detailHandlers.get(query).get(details, result);
            }else{
                Log.info("Nothing found.");
                detailHandlers.get(query).get(new Seq<>(), SteamResult.FileNotFound);
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
        Log.info("Item subscribed from @", info.getFolder());
        Achievement.downloadMapWorkshop.complete();
    }

    @Override
    public void onUnsubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result){
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item unsubscribed from @", info.getFolder());
    }

    @Override
    public void onCreateItem(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result){
        Log.info("onCreateItem(" + result + ")");
        if(!itemHandlers.isEmpty()){
            if(result == SteamResult.OK){
                Log.info("Passing to first handler.");
                itemHandlers.first().get(publishedFileID);
            }else{
                ui.showErrorMessage(Core.bundle.format("publish.error", result.name()));
            }

            itemHandlers.remove(0);
        }else{
            Log.err("No handlers for createItem()");
        }
    }

    @Override
    public void onSubmitItemUpdate(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result){
        ui.loadfrag.hide();
        Log.info("onsubmititemupdate @ @ @", publishedFileID.handle(), needsToAcceptWLA, result);
        if(result == SteamResult.OK){
            //redirect user to page for further updates
            SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + publishedFileID.handle());
            if(needsToAcceptWLA){
                SVars.net.friends.activateGameOverlayToWebPage("https://steamcommunity.com/sharedfiles/workshoplegalagreement");
            }

            if(updatedHandlers.containsKey(publishedFileID)){
                updatedHandlers.get(publishedFileID).run();
            }
        }else{
            ui.showErrorMessage(Core.bundle.format("publish.error", result.name()));
        }
    }

    @Override
    public void onDownloadItemResult(int appID, SteamPublishedFileID publishedFileID, SteamResult result){
        Achievement.downloadMapWorkshop.complete();
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item downloaded to @", info.getFolder());
    }

    @Override
    public void onDeleteItem(SteamPublishedFileID publishedFileID, SteamResult result){
        ItemInstallInfo info = new ItemInstallInfo();
        ugc.getItemInstallInfo(publishedFileID, info);
        Log.info("Item removed from @", info.getFolder());
    }
}
