package mindustry.content;

import arc.*;
import arc.func.*;
import arc.scene.style.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

/** Class for storing a list of TechNodes with some utility tree builder methods; context dependent. See {@link SerpuloTechTree#load} source for example usage. */
public class TechTree{
    private static TechNode context = null;

    public static Seq<TechNode> all = new Seq<>();
    public static Seq<TechNode> roots = new Seq<>();

    public static TechNode nodeRoot(String name, UnlockableContent content, Runnable children){
        return nodeRoot(name, content, false, children);
    }

    public static TechNode nodeRoot(String name, UnlockableContent content, boolean requireUnlock, Runnable children){
        var root = node(content, content.researchRequirements(), children);
        root.name = name;
        root.requiresUnlock = requireUnlock;
        roots.add(root);
        return root;
    }

    public static TechNode node(UnlockableContent content, Runnable children){
        return node(content, content.researchRequirements(), children);
    }

    public static TechNode node(UnlockableContent content, ItemStack[] requirements, Runnable children){
        return node(content, requirements, null, children);
    }

    public static TechNode node(UnlockableContent content, ItemStack[] requirements, Seq<Objective> objectives, Runnable children){
        TechNode node = new TechNode(context, content, requirements);
        if(objectives != null){
            node.objectives.addAll(objectives);
        }

        TechNode prev = context;
        context = node;
        children.run();
        context = prev;

        return node;
    }

    public static TechNode node(UnlockableContent content, Seq<Objective> objectives, Runnable children){
        return node(content, content.researchRequirements(), objectives, children);
    }

    public static TechNode node(UnlockableContent block){
        return node(block, () -> {});
    }

    public static TechNode nodeProduce(UnlockableContent content, Seq<Objective> objectives, Runnable children){
        return node(content, content.researchRequirements(), objectives.add(new Produce(content)), children);
    }

    public static TechNode nodeProduce(UnlockableContent content, Runnable children){
        return nodeProduce(content, new Seq<>(), children);
    }

    public static @Nullable TechNode context(){
        return context;
    }

    public static class TechNode{
        /** Depth in tech tree. */
        public int depth;
        /** Icon displayed in tech tree selector. */
        public @Nullable Drawable icon;
        /** Name for root node - used in tech tree selector. */
        public @Nullable String name;
        /** For roots only. If true, this needs to be unlocked before it is selectable in the research dialog. Does not apply when you are on the planet itself. */
        public boolean requiresUnlock = false;
        /** Requirement node. */
        public @Nullable TechNode parent;
        /** Multipliers for research costs on a per-item basis. Inherits from parent. */
        public @Nullable ObjectFloatMap<Item> researchCostMultipliers;
        /** Content to be researched. */
        public UnlockableContent content;
        /** Item requirements for this content. */
        public ItemStack[] requirements;
        /** Requirements that have been fulfilled. Always the same length as the requirement array. */
        public ItemStack[] finishedRequirements;
        /** Extra objectives needed to research this. */
        public Seq<Objective> objectives = new Seq<>();
        /** Nodes that depend on this node. */
        public final Seq<TechNode> children = new Seq<>();

        public TechNode(@Nullable TechNode parent, UnlockableContent content, ItemStack[] requirements){
            if(parent != null){
                parent.children.add(this);
                researchCostMultipliers = parent.researchCostMultipliers;
            }else if(researchCostMultipliers == null){
                researchCostMultipliers = new ObjectFloatMap<>();
            }

            this.parent = parent;
            this.content = content;
            this.depth = parent == null ? 0 : parent.depth + 1;

            if(researchCostMultipliers.size > 0){
                requirements = ItemStack.copy(requirements);
                for(ItemStack requirement : requirements){
                    requirement.amount = (int)(requirement.amount * researchCostMultipliers.get(requirement.item, 1));
                }
            }

            setupRequirements(requirements);

            var used = new ObjectSet<Content>();

            //add dependencies as objectives.
            content.getDependencies(d -> {
                if(used.add(d)){
                    objectives.add(new Research(d));
                }
            });

            content.techNode = this;
            all.add(this);
        }

        /** Recursively iterates through everything that is a child of this node. Includes itself. */
        public void each(Cons<TechNode> consumer){
            consumer.get(this);
            for(var child : children){
                child.each(consumer);
            }
        }

        public Drawable icon(){
            return icon == null ? new TextureRegionDrawable(content.uiIcon) : icon;
        }

        public String localizedName(){
            return Core.bundle.get("techtree." + name);
        }

        public void setupRequirements(ItemStack[] requirements){
            this.requirements = requirements;
            this.finishedRequirements = new ItemStack[requirements.length];

            //load up the requirements that have been finished if settings are available
            for(int i = 0; i < requirements.length; i++){
                finishedRequirements[i] = new ItemStack(requirements[i].item, Core.settings == null ? 0 : Core.settings.getInt("req-" + content.name + "-" + requirements[i].item.name));
            }
        }

        /** Resets finished requirements and saves. */
        public void reset(){
            for(ItemStack stack : finishedRequirements){
                stack.amount = 0;
            }
            save();
        }

        /** Removes this node from the tech tree. */
        public void remove(){
            all.remove(this);
            if(parent != null){
                parent.children.remove(this);
            }
        }

        /** Flushes research progress to settings. */
        public void save(){

            //save finished requirements by item type
            for(ItemStack stack : finishedRequirements){
                Core.settings.put("req-" + content.name + "-" + stack.item.name, stack.amount);
            }
        }
    }
}
