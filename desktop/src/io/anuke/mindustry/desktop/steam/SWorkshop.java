package io.anuke.mindustry.desktop.steam;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamRemoteStorage.*;
import io.anuke.arc.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.maps.*;

import static io.anuke.mindustry.Vars.*;

public class SWorkshop implements SteamUGCCallback{
    public final SteamUGC ugc = new SteamUGC(this);

    private Map lastMap;

    public void publishMap(Map map){
        this.lastMap = map;
        ugc.createItem(SVars.steamID, WorkshopFileType.GameManagedItem);
    }

    @Override
    public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result){

    }

    @Override
    public void onSubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result){

    }

    @Override
    public void onUnsubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result){

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

        Map map = lastMap;

        if(result == SteamResult.OK){
            SteamUGCUpdateHandle h = ugc.startItemUpdate(SVars.steamID, publishedFileID);

            Gamemode mode = Gamemode.attack.valid(map) ? Gamemode.attack : Gamemode.survival;
            FileHandle mapFile = tmpDirectory.child("map_" + publishedFileID.toString()).child("preview.png");
            lastMap.file.copyTo(mapFile);

            ugc.setItemTitle(h, map.name());
            ugc.setItemDescription(h, map.description());
            ugc.setItemTags(h, new String[]{"map", mode.name()});
            ugc.setItemVisibility(h, PublishedFileVisibility.Public);
            ugc.setItemPreview(h, map.previewFile().absolutePath());
            ugc.setItemContent(h, mapFile.parent().absolutePath());
            ugc.addItemKeyValueTag(h, "mode", mode.name());
            ugc.submitItemUpdate(h, "Map created");
        }else{
            ui.showErrorMessage(Core.bundle.format("map.publish.error ", result.name()));
        }

        lastMap = null;
    }

    @Override
    public void onSubmitItemUpdate(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result){
        if(result == SteamResult.OK){
            //redirect user to page for further updates
            SVars.net.friends.activateGameOverlayToWebPage("steam://url/CommunityFilePage/" + publishedFileID.toString());
        }else{
            ui.showErrorMessage(Core.bundle.format("map.publish.error ", result.name()));
        }
    }

    @Override
    public void onDownloadItemResult(int appID, SteamPublishedFileID publishedFileID, SteamResult result){

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

    }
}
