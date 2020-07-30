package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.layout.*;
import mindustry.ui.layout.TreeLayout.*;

import java.util.*;

import static mindustry.Vars.*;

public class ResearchDialog extends BaseDialog{
    private final float nodeSize = Scl.scl(60f);
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    private TechTreeNode root = new TechTreeNode(TechTree.root, null);
    private Rect bounds = new Rect();
    private ItemsDisplay itemDisplay;
    private View view;

    private ItemSeq items;

    public ResearchDialog(){
        super("");

        titleTable.remove();
        margin(0f).marginBottom(8);
        cont.stack(view = new View(), itemDisplay = new ItemsDisplay()).grow();

        shouldPause = true;

        shown(() -> {

            items = new ItemSeq(){
                //store sector item amounts for modifications
                ObjectMap<Sector, ItemSeq> cache = new ObjectMap<>();

                {
                    //add global counts of each sector
                    for(Planet planet : content.planets()){
                        for(Sector sector : planet.sectors){
                            if(sector.hasSave()){
                                ItemSeq cached = sector.calculateItems();
                                add(cached);
                                cache.put(sector, cached);
                            }
                        }
                    }
                }

                //this is the only method that actually modifies the sequence itself.
                @Override
                public void add(Item item, int amount){
                    //only have custom removal logic for when the sequence gets items taken out of it (e.g. research)
                    if(amount < 0){
                        //remove items from each sector's storage, one by one

                        //negate amount since it's being *removed* - this makes it positive
                        amount = -amount;

                        //% that gets removed from each sector
                        double percentage = (double)amount / get(item);
                        int[] counter = {amount};
                        cache.each((sector, seq) -> {
                            if(counter[0] == 0) return;

                            //amount that will be removed
                            int toRemove = Math.min((int)Math.ceil(percentage * seq.get(item)), counter[0]);

                            //actually remove it from the sector
                            sector.removeItem(item, toRemove);
                            seq.remove(item, toRemove);

                            counter[0] -= toRemove;
                        });

                        //negate again to display correct number
                        amount = -amount;
                    }

                    super.add(item, amount);
                }
            };

            checkNodes(root);
            treeLayout();

        });

        hidden(ui.planet::setup);

        addCloseButton();

        buttons.button("$database", Icon.book, () -> {
            hide();
            ui.database.show();
        }).size(210f, 64f);

        //scaling/drag input
        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                view.setScale(Mathf.clamp(view.scaleX - amountY / 10f * view.scaleX, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                view.requestScroll();
                return super.mouseMoved(event, x, y);
            }
        });

        addListener(new ElementGestureListener(){
            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(view.lastZoom < 0){
                    view.lastZoom = view.scaleX;
                }

                view.setScale(Mathf.clamp(distance / initialDistance * view.lastZoom, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                view.lastZoom = view.scaleX;
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                view.panX += deltaX / view.scaleX;
                view.panY += deltaY / view.scaleY;
                view.moved = true;
                view.clamp();
            }
        });
    }

    void treeLayout(){
        float spacing = 20f;
        LayoutNode node = new LayoutNode(root, null);
        LayoutNode[] children = node.children;
        LayoutNode[] leftHalf = Arrays.copyOfRange(node.children, 0, Mathf.ceil(node.children.length/2f));
        LayoutNode[] rightHalf = Arrays.copyOfRange(node.children, Mathf.ceil(node.children.length/2f), node.children.length);

        node.children = leftHalf;
        new BranchTreeLayout(){{
            gapBetweenLevels = gapBetweenNodes = spacing;
            rootLocation = TreeLocation.top;
        }}.layout(node);

        float lastY = node.y;

        if(rightHalf.length > 0){

            node.children = rightHalf;
            new BranchTreeLayout(){{
                gapBetweenLevels = gapBetweenNodes = spacing;
                rootLocation = TreeLocation.bottom;
            }}.layout(node);

            shift(leftHalf, node.y - lastY);
        }

        node.children = children;

        float minx = 0f, miny = 0f, maxx = 0f, maxy = 0f;
        copyInfo(node);

        for(TechTreeNode n : nodes){
            if(!n.visible) continue;
            minx = Math.min(n.x - n.width/2f, minx);
            maxx = Math.max(n.x + n.width/2f, maxx);
            miny = Math.min(n.y - n.height/2f, miny);
            maxy = Math.max(n.y + n.height/2f, maxy);
        }
        bounds = new Rect(minx, miny, maxx - minx, maxy - miny);
        bounds.y += nodeSize*1.5f;
    }

    void shift(LayoutNode[] children, float amount){
        for(LayoutNode node : children){
            node.y += amount;
            if(node.children != null && node.children.length > 0) shift(node.children, amount);
        }
    }

    void copyInfo(LayoutNode node){
        node.node.x = node.x;
        node.node.y = node.y;
        if(node.children != null){
            for(LayoutNode child : node.children){
                copyInfo(child);
            }
        }
    }

    void checkNodes(TechTreeNode node){
        boolean locked = locked(node.node);
        if(!locked) node.visible = true;
        node.selectable = selectable(node.node);
        for(TechTreeNode l : node.children){
            l.visible = !locked;
            checkNodes(l);
        }

        itemDisplay.rebuild(items);
    }

    boolean selectable(TechNode node){
        return !node.objectives.contains(i -> !i.complete());
    }

    void showToast(String info){
        Table table = new Table();
        table.actions(Actions.fadeOut(0.5f, Interp.fade), Actions.remove());
        table.top().add(info);
        table.name = "toast";
        table.update(() -> {
            table.toFront();
            table.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() - 21, Align.top);
        });
        Core.scene.add(table);
    }

    boolean locked(TechNode node){
        return node.content.locked();
    }

    class LayoutNode extends TreeNode<LayoutNode>{
        final TechTreeNode node;

        LayoutNode(TechTreeNode node, LayoutNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = nodeSize;
            if(node.children != null){
                children = Seq.with(node.children).map(t -> new LayoutNode(t, this)).toArray(LayoutNode.class);
            }
        }
    }

    class TechTreeNode extends TreeNode<TechTreeNode>{
        final TechNode node;
        boolean visible = true, selectable = true;

        TechTreeNode(TechNode node, TechTreeNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = nodeSize;
            nodes.add(this);
            if(node.children != null){
                children = new TechTreeNode[node.children.size];
                for(int i = 0; i < children.length; i++){
                    children[i] = new TechTreeNode(node.children.get(i), this);
                }
            }
        }
    }

    class View extends Group{
        float panX = 0, panY = -200, lastZoom = -1;
        boolean moved = false;
        ImageButton hoverNode;
        Table infoTable = new Table();

        {
            infoTable.touchable = Touchable.enabled;

            for(TechTreeNode node : nodes){
                ImageButton button = new ImageButton(node.node.content.icon(Cicon.medium), Styles.nodei);
                button.visible(() -> node.visible);
                button.clicked(() -> {
                    if(moved) return;

                    if(mobile){
                        hoverNode = button;
                        rebuild();
                        float right = infoTable.getRight();
                        if(right > Core.graphics.getWidth()){
                            float moveBy = right - Core.graphics.getWidth();
                            addAction(new RelativeTemporalAction(){
                                {
                                    setDuration(0.1f);
                                    setInterpolation(Interp.fade);
                                }

                                @Override
                                protected void updateRelative(float percentDelta){
                                    panX -= moveBy * percentDelta;
                                }
                            });
                        }
                    }else if(canSpend(node.node) && locked(node.node)){
                        spend(node.node);
                    }
                });
                button.hovered(() -> {
                    if(!mobile && hoverNode != button && node.visible){
                        hoverNode = button;
                        rebuild();
                    }
                });
                button.exited(() -> {
                    if(!mobile && hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                        hoverNode = null;
                        rebuild();
                    }
                });
                button.touchable(() -> !node.visible ? Touchable.disabled : Touchable.enabled);
                button.userObject = node.node;
                button.setSize(nodeSize);
                button.update(() -> {
                    float offset = (Core.graphics.getHeight() % 2) / 2f;
                    button.setPosition(node.x + panX + width / 2f, node.y + panY + height / 2f + offset, Align.center);
                    button.getStyle().up = !locked(node.node) ? Tex.buttonOver : selectable(node.node) && !canSpend(node.node) ? Tex.buttonRed : Tex.button;

                    ((TextureRegionDrawable)button.getStyle().imageUp).setRegion(node.selectable ? node.node.content.icon(Cicon.medium) : Icon.lock.getRegion());
                    button.getImage().setColor(!locked(node.node) ? Color.white : node.selectable ? Color.gray : Pal.gray);
                });
                addChild(button);
            }

            if(mobile){
                tapped(() -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e == this){
                        hoverNode = null;
                        rebuild();
                    }
                });
            }

            setOrigin(Align.center);
            setTransform(true);
            released(() -> moved = false);
        }

        void clamp(){
            float pad = nodeSize;

            float ox = width/2f, oy = height/2f;
            float rx = bounds.x + panX + ox, ry = panY + oy + bounds.y;
            float rw = bounds.width, rh = bounds.height;
            rx = Mathf.clamp(rx, -rw + pad, Core.graphics.getWidth() - pad);
            ry = Mathf.clamp(ry, -rh + pad, Core.graphics.getHeight() - pad);
            panX = rx - bounds.x - ox;
            panY = ry - bounds.y - oy;
        }

        boolean canSpend(TechNode node){
            //can spend when there's at least 1 item that can be spent
            return selectable(node) && (node.requirements.length == 0 || Structs.contains(node.requirements, i -> items.has(i.item)));
        }

        void spend(TechNode node){
            boolean complete = true;

            boolean[] shine = new boolean[node.requirements.length];
            boolean[] usedShine = new boolean[content.items().size];

            for(int i = 0; i < node.requirements.length; i++){
                ItemStack req = node.requirements[i];
                ItemStack completed = node.finishedRequirements[i];

                //amount actually taken from inventory
                int used = Math.min(req.amount - completed.amount, items.get(req.item));
                items.remove(req.item, used);
                completed.amount += used;

                if(used > 0){
                    shine[i] = true;
                    usedShine[req.item.id] = true;
                }

                //disable completion if the completed amount has not reached requirements
                if(completed.amount < req.amount){
                    complete = false;
                }
            }

            if(complete){
                unlock(node);
            }

            node.save();

            //??????
            Core.scene.act();
            rebuild(shine);
            itemDisplay.rebuild(items, usedShine);
        }

        void unlock(TechNode node){
            node.content.unlock();
            showToast(Core.bundle.format("researched", node.content.localizedName));
            checkNodes(root);
            hoverNode = null;
            treeLayout();
            rebuild();
            Core.scene.act();
            Sounds.unlock.play();
            Events.fire(new ResearchEvent(node.content));
        }

        void rebuild(){
            rebuild(null);
        }

        //pass an array of stack indexes that should shine here
        void rebuild(@Nullable boolean[] shine){
            ImageButton button = hoverNode;

            infoTable.remove();
            infoTable.clear();
            infoTable.update(null);

            if(button == null) return;

            TechNode node = (TechNode)button.userObject;

            infoTable.exited(() -> {
                if(hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                    hoverNode = null;
                    rebuild();
                }
            });

            infoTable.update(() -> infoTable.setPosition(button.getX() + button.getWidth(), button.getY() + button.getHeight(), Align.topLeft));

            infoTable.left();
            infoTable.background(Tex.button).margin(8f);

            boolean selectable = selectable(node);

            infoTable.table(b -> {
                b.margin(0).left().defaults().left();

                if(selectable){
                    b.button(Icon.info, Styles.cleari, () -> ui.content.show(node.content)).growY().width(50f);
                }
                b.add().grow();
                b.table(desc -> {
                    desc.left().defaults().left();
                    desc.add(selectable ? node.content.localizedName : "[accent]???");
                    desc.row();
                    if(locked(node)){

                        desc.table(t -> {
                            t.left();
                            if(selectable){

                                //check if there is any progress, add research progress text
                                if(Structs.contains(node.finishedRequirements, s -> s.amount > 0)){
                                    float sum = 0f, used = 0f;
                                    boolean shiny = false;

                                    for(int i = 0; i < node.requirements.length; i++){
                                        sum += node.requirements[i].item.cost * node.requirements[i].amount;
                                        used += node.finishedRequirements[i].item.cost * node.finishedRequirements[i].amount;
                                        if(shine != null) shiny |= shine[i];
                                    }

                                    int percent = (int)(used / sum * 100);
                                    Label label = t.add(Core.bundle.format("research.progress", percent)).left().get();

                                    if(shiny){
                                        label.setColor(Pal.accent);
                                        label.actions(Actions.color(Color.lightGray, 0.75f, Interp.fade));
                                    }else{
                                        label.setColor(Color.lightGray);
                                    }

                                    t.row();
                                }

                                for(int i = 0; i < node.requirements.length; i++){
                                    ItemStack req = node.requirements[i];
                                    ItemStack completed = node.finishedRequirements[i];

                                    //skip finished stacks
                                    if(req.amount <= completed.amount) continue;
                                    boolean shiny = shine != null && shine[i];

                                    t.table(list -> {
                                        int reqAmount = req.amount - completed.amount;

                                        list.left();
                                        list.image(req.item.icon(Cicon.small)).size(8 * 3).padRight(3);
                                        list.add(req.item.localizedName).color(Color.lightGray);
                                        Label label = list.label(() -> " " +
                                                UI.formatAmount(Math.min(items.get(req.item), reqAmount)) + " / "
                                            + UI.formatAmount(reqAmount)).get();

                                        Color targetColor = items.has(req.item) ? Color.lightGray : Color.scarlet;

                                        if(shiny){
                                            label.setColor(Pal.accent);
                                            label.actions(Actions.color(targetColor, 0.75f, Interp.fade));
                                        }else{
                                            label.setColor(targetColor);
                                        }

                                    }).fillX().left();
                                    t.row();
                                }
                            }else if(node.objectives.size > 0){
                                t.table(r -> {
                                    r.add("$complete").colspan(2).left();
                                    r.row();
                                    for(Objective o : node.objectives){
                                        if(o.complete()) continue;

                                        r.add("> " + o.display()).color(Color.lightGray).left();
                                        r.image(o.complete() ? Icon.ok : Icon.cancel, o.complete() ? Color.lightGray : Color.scarlet).padLeft(3);
                                        r.row();
                                    }
                                });
                                t.row();
                            }
                        });
                    }else{
                        desc.add("$completed");
                    }
                }).pad(9);

                if(mobile && locked(node)){
                    b.row();
                    b.button("$research", Icon.ok, Styles.nodet, () -> spend(node))
                    .disabled(i -> !canSpend(node)).growX().height(44f).colspan(3);
                }
            });

            infoTable.row();
            if(node.content.description != null && selectable){
                infoTable.table(t -> t.margin(3f).left().labelWrap(node.content.displayDescription()).color(Color.lightGray).growX()).fillX();
            }

            addChild(infoTable);
            infoTable.pack();
            infoTable.act(Core.graphics.getDeltaTime());
        }

        @Override
        public void drawChildren(){
            clamp();
            float offsetX = panX + width / 2f, offsetY = panY + height / 2f;
            Draw.sort(true);

            for(TechTreeNode node : nodes){
                if(!node.visible) continue;
                for(TechTreeNode child : node.children){
                    if(!child.visible) continue;
                    boolean lock = locked(node.node) || locked(child.node);
                    Draw.z(lock ? 1f : 2f);

                    Lines.stroke(Scl.scl(4f), lock ? Pal.gray : Pal.accent);
                    Draw.alpha(parentAlpha);
                    if(Mathf.equal(Math.abs(node.y - child.y), Math.abs(node.x - child.x), 1f) && Mathf.dstm(node.x, node.y, child.x, child.y) <= node.width*3){
                        Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                    }else{
                        Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, node.y + offsetY);
                        Lines.line(child.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                    }
                }
            }

            Draw.sort(false);
            Draw.reset();
            super.drawChildren();
        }
    }
}
