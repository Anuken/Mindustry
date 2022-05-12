package mindustry.type;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.logic.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Item extends UnlockableContent implements Senseable{
    public Color color;

    /** how explosive this item is. */
    public float explosiveness = 0f;
    /** flammability above 0.3 makes this eligible for item burners. */
    public float flammability = 0f;
    /** how radioactive this item is. */
    public float radioactivity;
    /** how electrically potent this item is. */
    public float charge = 0f;
    /** drill hardness of the item */
    public int hardness = 0;
    /**
     * base material cost of this item, used for calculating place times
     * 1 cost = 1 tick added to build time
     */
    public float cost = 1f;
    /** When this item is present in the build cost, a block's <b>default</b> health is multiplied by 1 + scaling, where 'scaling' is summed together for all item requirement types. */
    public float healthScaling = 0f;
    /** if true, this item is of the lowest priority to drills. */
    public boolean lowPriority;

    /** If >0, this item is animated. */
    public int frames = 0;
    /** Number of generated transition frames between each frame */
    public int transitionFrames = 0;
    /** Ticks in-between animation frames. */
    public float frameTime = 5f;
    /** If true, this material is used by buildings. If false, this material will be incinerated in certain cores. */
    public boolean buildable = true;
    public boolean hidden = false;

    public Item(String name, Color color){
        super(name);
        this.color = color;
    }

    public Item(String name){
        this(name, new Color(Color.black));
    }

    @Override
    public boolean isHidden(){
        return hidden;
    }

    @Override
    public void loadIcon(){
        super.loadIcon();

        //animation code ""borrowed"" from Project Unity - original implementation by GlennFolker and sk7725
        if(frames > 0){
            TextureRegion[] regions = new TextureRegion[frames * (transitionFrames + 1)];

            if(transitionFrames <= 0){
                for(int i = 1; i <= frames; i++){
                    regions[i - 1] = Core.atlas.find(name + i);
                }
            }else{
                for(int i = 0; i < frames; i++){
                    regions[i * (transitionFrames + 1)] = Core.atlas.find(name + (i + 1));
                    for(int j = 1; j <= transitionFrames; j++){
                        int index = i * (transitionFrames + 1) + j;
                        regions[index] = Core.atlas.find(name + "-t" + index);
                    }
                }
            }

            fullIcon = new TextureRegion(fullIcon);
            uiIcon = new TextureRegion(uiIcon);

            Events.run(Trigger.update, () -> {
                int frame = (int)(Time.globalTime / frameTime) % regions.length;

                fullIcon.set(regions[frame]);
                uiIcon.set(regions[frame]);
            });
        }
    }

    @Override
    public void setStats(){
        stats.addPercent(Stat.explosiveness, explosiveness);
        stats.addPercent(Stat.flammability, flammability);
        stats.addPercent(Stat.radioactivity, radioactivity);
        stats.addPercent(Stat.charge, charge);
    }

    @Override
    public String toString(){
        return localizedName;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.item;
    }

    @Override
    public void createIcons(MultiPacker packer){
        super.createIcons(packer);

        //create transitions
        if(frames > 0 && transitionFrames > 0){
            var pixmaps = new PixmapRegion[frames];

            for(int i = 0; i < frames; i++){
                pixmaps[i] = Core.atlas.getPixmap(name + (i + 1));
            }

            for(int i = 0; i < frames; i++){
                for(int j = 1; j <= transitionFrames; j++){
                    float f = (float)j / (transitionFrames + 1);
                    int index = i * (transitionFrames + 1) + j;

                    Pixmap res = Pixmaps.blend(pixmaps[i], pixmaps[(i + 1) % frames], f);
                    packer.add(PageType.main, name + "-t" + index, res);
                }
            }
        }
    }

    @Override
    public double sense(LAccess sensor){
        if(sensor == LAccess.color) return color.toFloatBits();
        return 0;
    }

    @Override
    public Object senseObject(LAccess sensor){
        if(sensor == LAccess.name) return name;
        return noSensed;
    }

    /** Allocates a new array containing all items that generate ores. */
    public static Seq<Item> getAllOres(){
        return content.blocks().select(b -> b instanceof OreBlock).map(b -> b.itemDrop);
    }
}
