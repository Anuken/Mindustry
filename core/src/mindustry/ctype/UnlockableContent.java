package mindustry.ctype;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.TechTree.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Base interface for an unlockable content type. */
public abstract class UnlockableContent extends MappableContent{
    /** Stat storage for this content. Initialized on demand. */
    public Stats stats = new Stats();
    /** Localized, formal name. Never null. Set to internal name if not found in bundle. */
    public String localizedName;
    /** Localized description & details. May be null. */
    public @Nullable String description, details;
    /** Whether this content is always unlocked in the tech tree. */
    public boolean alwaysUnlocked = false;
    /** Whether to show the description in the research dialog preview. */
    public boolean inlineDescription = true;
    /** Whether details of blocks are hidden in custom games if they haven't been unlocked in campaign mode. */
    public boolean hideDetails = true;
    /** If false, all icon generation is disabled for this content; createIcons is not called. */
    public boolean generateIcons = true;
    /** Special logic icon ID. */
    public int iconId = 0;
    /** Icon of the content to use in UI. */
    public TextureRegion uiIcon;
    /** Icon of the full content. Unscaled.*/
    public TextureRegion fullIcon;
    /** The tech tree node for this content, if applicable. Null if not part of a tech tree. */
    public @Nullable TechNode techNode;
    /** Unlock state. Loaded from settings. Do not modify outside of the constructor. */
    protected boolean unlocked;

    public UnlockableContent(String name){
        super(name);

        this.localizedName = Core.bundle.get(getContentType() + "." + this.name + ".name", this.name);
        this.description = Core.bundle.getOrNull(getContentType() + "." + this.name + ".description");
        this.details = Core.bundle.getOrNull(getContentType() + "." + this.name + ".details");
        this.unlocked = Core.settings != null && Core.settings.getBool(this.name + "-unlocked", false);
    }

    @Override
    public void loadIcon(){
        fullIcon =
            Core.atlas.find(getContentType().name() + "-" + name + "-full",
            Core.atlas.find(name + "-full",
            Core.atlas.find(name,
            Core.atlas.find(getContentType().name() + "-" + name,
            Core.atlas.find(name + "1")))));

        uiIcon = Core.atlas.find(getContentType().name() + "-" + name + "-ui", fullIcon);
    }

    public String displayDescription(){
        return minfo.mod == null ? description : description + "\n" + Core.bundle.format("mod.display", minfo.mod.meta.displayName());
    }

    /** Checks stat initialization state. Call before displaying stats. */
    public void checkStats(){
        if(!stats.intialized){
            setStats();
            stats.intialized = true;
        }
    }

    /** Initializes stats on demand. Should only be called once. Only called before something is displayed. */
    public void setStats(){
    }

    /** Display any extra info after details. */
    public void displayExtra(Table table){

    }

    /**
     * Generate any special icons for this content. Called synchronously.
     * No regions are loaded at this point; grab pixmaps from the packer.
     * */
    @CallSuper
    public void createIcons(MultiPacker packer){

    }

    protected void makeOutline(PageType page, MultiPacker packer, TextureRegion region, boolean makeNew, Color outlineColor, int outlineRadius){
        if(region instanceof AtlasRegion at && region.found()){
            String name = at.name;
            if(!makeNew || !packer.has(name + "-outline")){
                String regName = name + (makeNew ? "-outline" : "");
                if(packer.registerOutlined(regName)){
                    PixmapRegion base = Core.atlas.getPixmap(region);
                    var result = Pixmaps.outline(base, outlineColor, outlineRadius);
                    Drawf.checkBleed(result);
                    packer.add(page, regName, result);
                }
            }
        }
    }

    protected void makeOutline(MultiPacker packer, TextureRegion region, String name, Color outlineColor, int outlineRadius){
        if(region.found() && packer.registerOutlined(name)){
            PixmapRegion base = Core.atlas.getPixmap(region);
            var result = Pixmaps.outline(base, outlineColor, outlineRadius);
            Drawf.checkBleed(result);
            packer.add(PageType.main, name, result);
        }
    }

    protected void makeOutline(MultiPacker packer, TextureRegion region, String name, Color outlineColor){
        makeOutline(packer, region, name, outlineColor, 4);
    }

    /** @return items needed to research this content */
    public ItemStack[] researchRequirements(){
        return ItemStack.empty;
    }

    public String emoji(){
        return Fonts.getUnicodeStr(name);
    }

    public boolean hasEmoji(){
        return Fonts.hasUnicodeStr(name);
    }

    /** Iterates through any implicit dependencies of this content.
     * For blocks, this would be the items required to build it. */
    public void getDependencies(Cons<UnlockableContent> cons){

    }

    /** Called when this content is unlocked. Use this to unlock other related content. */
    public void onUnlock(){
    }

    /** Whether this content is always hidden in the content database dialog. */
    public boolean isHidden(){
        return false;
    }

    /** @return whether to show a notification toast when this is unlocked */
    public boolean showUnlock(){
        return true;
    }

    public boolean logicVisible(){
        return !isHidden();
    }

    /** Makes this piece of content unlocked; if it already unlocked, nothing happens. */
    public void unlock(){
        if(!unlocked && !alwaysUnlocked){
            unlocked = true;
            Core.settings.put(name + "-unlocked", true);

            onUnlock();
            Events.fire(new UnlockEvent(this));
        }
    }

    /** Unlocks this content, but does not fire any events. */
    public void quietUnlock(){
        if(!unlocked()){
            unlocked = true;
            Core.settings.put(name + "-unlocked", true);
        }
    }

    public boolean unlockedNowHost(){
        if(!state.isCampaign()) return true;
        return net != null && net.client() ?
            alwaysUnlocked || state.rules.researched.contains(name) :
            unlocked || alwaysUnlocked;
    }

    public boolean unlocked(){
        return net != null && net.client() ?
            alwaysUnlocked || unlocked || state.rules.researched.contains(name) :
            unlocked || alwaysUnlocked;
    }

    /** Locks this content again. */
    public void clearUnlock(){
        if(unlocked){
            unlocked = false;
            Core.settings.put(name + "-unlocked", false);
        }
    }

    /** @return whether this content is unlocked, or the player is in a custom (non-campaign) game. */
    public boolean unlockedNow(){
        return unlocked() || !state.isCampaign();
    }

    public boolean locked(){
        return !unlocked();
    }
}
