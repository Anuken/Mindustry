package arc.scene.ui;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;

import static arc.Core.*;

/**
 * A tree widget where each node has an icon, element, and child nodes.
 * <p>
 * The preferred size of the tree is determined by the preferred size of the elements for the expanded nodes.
 * <p>
 * {@link ChangeEvent} is fired when the selected node changes.
 * @author Nathan Sweet
 */
public class TreeElement extends WidgetGroup{
    final Seq<TreeElementNode> rootNodes = new Seq<>();
    final Selection<TreeElementNode> selection;
    TreeStyle style;
    float ySpacing = 4, iconSpacingLeft = 2, iconSpacingRight = 2, padding = 0, indentSpacing;
    TreeElementNode overNode, rangeStart;
    private float leftColumnWidth, prefWidth, prefHeight;
    private boolean sizeInvalid = true;
    private TreeElementNode foundNode;
    private ClickListener clickListener;

    public TreeElement(){
        this(scene.getStyle(TreeStyle.class));
    }

    public TreeElement(TreeStyle style){
        selection = new Selection<TreeElementNode>(){
            @Override
            protected void changed(){
                switch(size()){
                    case 0:
                        rangeStart = null;
                        break;
                    case 1:
                        rangeStart = first();
                        break;
                }
            }
        };
        selection.setActor(this);
        selection.setMultiple(true);
        setStyle(style);
        initialize();
    }

    static boolean findExpandedObjects(Seq<TreeElementNode> nodes, Seq<Object> objects){
        boolean expanded = false;
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            if(node.expanded && !findExpandedObjects(node.children, objects)) objects.add(node.object);
        }
        return expanded;
    }

    static TreeElementNode findNode(Seq<TreeElementNode> nodes, Object object){
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            if(object.equals(node.object)) return node;
        }
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            TreeElementNode found = findNode(node.children, object);
            if(found != null) return found;
        }
        return null;
    }

    static void collapseAll(Seq<TreeElementNode> nodes){
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            node.setExpanded(false);
            collapseAll(node.children);
        }
    }

    static void expandAll(Seq<TreeElementNode> nodes){
        for(int i = 0, n = nodes.size; i < n; i++)
            nodes.get(i).expandAll();
    }

    private void initialize(){
        addListener(clickListener = new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                TreeElementNode node = getNodeAt(y);
                if(node == null) return;
                if(node != getNodeAt(getTouchDownY())) return;
                if(selection.getMultiple() && selection.hasItems() && Core.input.shift()){
                    // Select range (shift).
                    if(rangeStart == null) rangeStart = node;
                    TreeElementNode rangeStart = TreeElement.this.rangeStart;
                    if(!Core.input.ctrl()) selection.clear();
                    float start = rangeStart.element.y, end = node.element.y;
                    if(start > end)
                        selectNodes(rootNodes, end, start);
                    else{
                        selectNodes(rootNodes, start, end);
                        selection.items().orderedItems().reverse();
                    }

                    selection.fireChangeEvent();
                    TreeElement.this.rangeStart = rangeStart;
                    return;
                }
                if(node.hasChildren() && (!selection.getMultiple() || !Core.input.ctrl())){
                    // Toggle expanded.
                    float rowX = node.element.x;
                    if(node.icon != null) rowX -= iconSpacingRight + node.icon.getMinWidth();
                    if(x < rowX){
                        node.setExpanded(!node.expanded);
                        return;
                    }
                }
                if(!node.isSelectable()) return;
                selection.choose(node);
                if(!selection.isEmpty()) rangeStart = node;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                setOverNode(getNodeAt(y));
                return false;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                super.exit(event, x, y, pointer, toActor);
                if(toActor == null || !toActor.isDescendantOf(TreeElement.this)) setOverNode(null);
            }
        });
    }

    public void add(TreeElementNode node){
        insert(rootNodes.size, node);
    }

    public void insert(int index, TreeElementNode node){
        remove(node);
        node.parent = null;
        rootNodes.insert(index, node);
        node.addToTree(this);
        invalidateHierarchy();
    }

    public void remove(TreeElementNode node){
        if(node.parent != null){
            node.parent.remove(node);
            return;
        }
        rootNodes.remove(node, true);
        node.removeFromTree(this);
        invalidateHierarchy();
    }

    /** Removes all tree nodes. */
    @Override
    public void clearChildren(){
        super.clearChildren();
        setOverNode(null);
        rootNodes.clear();
        selection.clear();
    }

    public Seq<TreeElementNode> getNodes(){
        return rootNodes;
    }

    @Override
    public void invalidate(){
        super.invalidate();
        sizeInvalid = true;
    }

    private void computeSize(){
        sizeInvalid = false;
        prefWidth = style.plus.getMinWidth();
        prefWidth = Math.max(prefWidth, style.minus.getMinWidth());
        prefHeight = getHeight();
        leftColumnWidth = 0;
        computeSize(rootNodes, indentSpacing);
        leftColumnWidth += iconSpacingLeft + padding;
        prefWidth += leftColumnWidth + padding;
        prefHeight = getHeight() - prefHeight;
    }

    private void computeSize(Seq<TreeElementNode> nodes, float indent){
        float ySpacing = this.ySpacing;
        float spacing = iconSpacingLeft + iconSpacingRight;
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            float rowWidth = indent + iconSpacingRight;
            Element element = node.element;
            if(element != null){
                rowWidth += (element).getPrefWidth();
                node.height = (element).getPrefHeight();
                (element).pack();
            }else{
                rowWidth += element.getWidth();
                node.height = element.getHeight();
            }
            if(node.icon != null){
                rowWidth += spacing + node.icon.getMinWidth();
                node.height = Math.max(node.height, node.icon.getMinHeight());
            }
            prefWidth = Math.max(prefWidth, rowWidth);
            prefHeight -= node.height + ySpacing;
            if(node.expanded) computeSize(node.children, indent + indentSpacing);
        }
    }

    @Override
    public void layout(){
        if(sizeInvalid) computeSize();
        layout(rootNodes, leftColumnWidth + indentSpacing + iconSpacingRight, getHeight() - ySpacing / 2);
    }

    private float layout(Seq<TreeElementNode> nodes, float indent, float y){
        float ySpacing = this.ySpacing;
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            float x = indent;
            if(node.icon != null) x += node.icon.getMinWidth();
            y -= node.getHeight();
            node.element.setPosition(x, y);
            y -= ySpacing;
            if(node.expanded) y = layout(node.children, indent + indentSpacing, y);
        }
        return y;
    }

    @Override
    public void draw(){
        drawBackground();
        Color color = this.color;
        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
        draw(rootNodes, leftColumnWidth);
        super.draw(); // Draw elements.
    }

    /** Called to draw the background. Default implementation draws the style background drawable. */
    protected void drawBackground(){
        if(style.background != null){
            Color color = this.color;
            Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
            style.background.draw(x, y, getWidth(), getHeight());
        }
    }

    /** Draws selection, icons, and expand icons. */
    private void draw(Seq<TreeElementNode> nodes, float indent){
        Drawable plus = style.plus, minus = style.minus;
        float x = this.x, y = this.y;
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            Element element = node.element;

            if(selection.contains(node) && style.selection != null){
                style.selection.draw(x, y + element.y - ySpacing / 2, getWidth(), node.height + ySpacing);
            }else if(node == overNode && style.over != null && node.hoverable){
                style.over.draw(x, y + element.y - ySpacing / 2, getWidth(), node.height + ySpacing);
            }

            if(node.icon != null){
                float iconY = element.y + Math.round((node.height - node.icon.getMinHeight()) / 2);
                Draw.color(element.color);
                node.icon.draw(x + node.element.x - iconSpacingRight - node.icon.getMinWidth(), y + iconY,
                node.icon.getMinWidth(), node.icon.getMinHeight());
                Draw.color(Color.white);
            }

            if(!node.hasChildren()) continue;

            Draw.color(color);
            Drawable expandIcon = node.expanded ? minus : plus;
            float iconY = element.y + Math.round((node.height - expandIcon.getMinHeight()) / 2);
            expandIcon.draw(x + indent - iconSpacingLeft, y + iconY, expandIcon.getMinWidth(), expandIcon.getMinHeight());
            if(node.expanded) draw(node.children, indent + indentSpacing);
        }
    }

    /** @return May be null. */
    public TreeElementNode getNodeAt(float y){
        foundNode = null;
        getNodeAt(rootNodes, y, getHeight());
        return foundNode;
    }

    private float getNodeAt(Seq<TreeElementNode> nodes, float y, float rowY){
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            float height = node.height;
            rowY -= node.getHeight() - height; // Node subclass may increase getHeight.
            if(y >= rowY - height - ySpacing && y < rowY){
                foundNode = node;
                return -1;
            }
            rowY -= height + ySpacing;
            if(node.expanded){
                rowY = getNodeAt(node.children, y, rowY);
                if(rowY == -1) return -1;
            }
        }
        return rowY;
    }

    void selectNodes(Seq<TreeElementNode> nodes, float low, float high){
        for(int i = 0, n = nodes.size; i < n; i++){
            TreeElementNode node = nodes.get(i);
            if(node.element.y < low) break;
            if(!node.isSelectable()) continue;
            if(node.element.y <= high) selection.add(node);
            if(node.expanded) selectNodes(node.children, low, high);
        }
    }

    public Selection<TreeElementNode> getSelection(){
        return selection;
    }

    public TreeStyle getStyle(){
        return style;
    }

    public void setStyle(TreeStyle style){
        this.style = style;
        indentSpacing = Math.max(style.plus.getMinWidth(), style.minus.getMinWidth()) + iconSpacingLeft;
    }

    public Seq<TreeElementNode> getRootNodes(){
        return rootNodes;
    }

    /** @return May be null. */
    public TreeElementNode getOverNode(){
        return overNode;
    }

    /** @param overNode May be null. */
    public void setOverNode(TreeElementNode overNode){
        this.overNode = overNode;
    }

    /** @return May be null. */
    public Object getOverObject(){
        if(overNode == null) return null;
        return overNode.getObject();
    }

    /** Sets the amount of horizontal space between the nodes and the left/right edges of the tree. */
    public void setPadding(float padding){
        this.padding = padding;
    }

    /** Returns the amount of horizontal space for indentation level. */
    public float getIndentSpacing(){
        return indentSpacing;
    }

    public float getYSpacing(){
        return ySpacing;
    }

    /** Sets the amount of vertical space between nodes. */
    public void setYSpacing(float ySpacing){
        this.ySpacing = ySpacing;
    }

    /** Sets the amount of horizontal space between the node elements and icons. */
    public void setIconSpacing(float left, float right){
        this.iconSpacingLeft = left;
        this.iconSpacingRight = right;
    }

    @Override
    public float getPrefWidth(){
        if(sizeInvalid) computeSize();
        return prefWidth;
    }

    @Override
    public float getPrefHeight(){
        if(sizeInvalid) computeSize();
        return prefHeight;
    }

    public void findExpandedObjects(Seq objects){
        findExpandedObjects(rootNodes, objects);
    }

    public void restoreExpandedObjects(Seq objects){
        for(int i = 0, n = objects.size; i < n; i++){
            TreeElementNode node = findNode(objects.get(i));
            if(node != null){
                node.setExpanded(true);
                node.expandTo();
            }
        }
    }

    /** Returns the node with the specified object, or null. */
    public TreeElementNode findNode(Object object){
        if(object == null) throw new IllegalArgumentException("object cannot be null.");
        return findNode(rootNodes, object);
    }

    public void collapseAll(){
        collapseAll(rootNodes);
    }

    public void expandAll(){
        expandAll(rootNodes);
    }

    /** Returns the click listener the tree uses for clicking on nodes and the over node. */
    public ClickListener getClickListener(){
        return clickListener;
    }

    public static class TreeElementNode{
        final Seq<TreeElementNode> children = new Seq<>(0);
        @Nullable Cons<Cons<TreeElementNode>> childProvider;
        Element element;
        TreeElementNode parent;
        boolean selectable = true, hoverable = true;
        boolean expanded;
        Drawable icon;
        float height;
        Object object;

        public TreeElementNode(Element element){
            if(element == null) throw new IllegalArgumentException("element cannot be null.");
            this.element = element;
        }

        /**
         * Adds a child node provider that only gets called on-demand.
         * @return this
         * */
        public TreeElementNode children(Cons<Cons<TreeElementNode>> provider){
            childProvider = provider;
            return this;
        }

        public TreeElementNode hoverable(boolean hover){
            this.hoverable = hover;
            return this;
        }

        public boolean hasChildren(){
            return childProvider != null || children.size > 0;
        }

        /** Called to add the element to the tree when the node's parent is expanded. */
        protected void addToTree(TreeElement tree){
            tree.addChild(element);
            if(!expanded) return;

            if(childProvider != null){
                childProvider.get(this::add);
                childProvider = null;
            }

            for(int i = 0, n = children.size; i < n; i++){
                children.get(i).addToTree(tree);
            }
        }

        /** Called to remove the element from the tree when the node's parent is collapsed. */
        protected void removeFromTree(TreeElement tree){
            tree.removeChild(element);
            if(!expanded) return;
            Object[] children = this.children.items;
            for(int i = 0, n = this.children.size; i < n; i++)
                ((TreeElementNode)children[i]).removeFromTree(tree);
        }

        public TreeElementNode add(TreeElementNode node){
            insert(children.size, node);
            return this;
        }

        public TreeElementNode addAll(Seq<TreeElementNode> nodes){
            for(int i = 0, n = nodes.size; i < n; i++)
                insert(children.size, nodes.get(i));
            return this;
        }

        public void insert(int index, TreeElementNode node){
            node.parent = this;
            children.insert(index, node);
            updateChildren();
        }

        public void remove(){
            TreeElement tree = getTree();
            if(tree != null){
                tree.remove(this);
            }else if(parent != null){
                parent.remove(this);
            }
        }

        public void remove(TreeElementNode node){
            children.remove(node, true);
            if(!expanded) return;
            TreeElement tree = getTree();
            if(tree == null) return;
            node.removeFromTree(tree);
            if(children.size == 0) expanded = false;
        }

        public void removeAll(){
            TreeElement tree = getTree();
            if(tree != null){
                for(int i = 0, n = children.size; i < n; i++)
                    children.get(i).removeFromTree(tree);
            }
            children.clear();
        }

        /** Returns the tree this node is currently in, or null. */
        public TreeElement getTree(){
            Group parent = element.parent;
            if(!(parent instanceof TreeElement)) return null;
            return (TreeElement)parent;
        }

        public boolean isExpanded(){
            return expanded;
        }

        public void setExpanded(boolean expanded){
            if(expanded == this.expanded) return;
            if(expanded && childProvider != null){
                childProvider.get(this::add);
                childProvider = null;
            }
            this.expanded = expanded;
            if(children.size == 0) return;
            TreeElement tree = getTree();
            if(tree == null) return;
            if(expanded){
                for(int i = 0, n = children.size; i < n; i++)
                    children.get(i).addToTree(tree);
            }else{
                for(int i = 0, n = children.size; i < n; i++)
                    children.get(i).removeFromTree(tree);
            }
            tree.invalidateHierarchy();
        }

        /** If the children order is changed, {@link #updateChildren()} must be called. */
        public Seq<TreeElementNode> getChildren(){
            return children;
        }

        public void updateChildren(){
            if(!expanded) return;
            TreeElement tree = getTree();
            if(tree == null) return;
            for(int i = 0, n = children.size; i < n; i++)
                children.get(i).addToTree(tree);
        }

        /** @return May be null. */
        public TreeElementNode getParent(){
            return parent;
        }

        public Object getObject(){
            return object;
        }

        /** Sets an application specific object for this node. */
        public void setObject(Object object){
            this.object = object;
        }

        public Drawable getIcon(){
            return icon;
        }

        /** Sets an icon that will be drawn to the left of the element. */
        public void setIcon(Drawable icon){
            this.icon = icon;
        }

        public int getLevel(){
            int level = 0;
            TreeElementNode current = this;
            do{
                level++;
                current = current.getParent();
            }while(current != null);
            return level;
        }

        /** Returns this node or the child node with the specified object, or null. */
        public TreeElementNode findNode(Object object){
            if(object == null) throw new IllegalArgumentException("object cannot be null.");
            if(object.equals(this.object)) return this;
            return TreeElement.findNode(children, object);
        }

        /** Collapses all nodes under and including this node. */
        public void collapseAll(){
            setExpanded(false);
            TreeElement.collapseAll(children);
        }

        /** Expands all nodes under and including this node. */
        public void expandAll(){
            setExpanded(true);
            if(children.size > 0) TreeElement.expandAll(children);
        }

        /** Expands all parent nodes of this node. */
        public void expandTo(){
            TreeElementNode node = parent;
            while(node != null){
                node.setExpanded(true);
                node = node.parent;
            }
        }

        public boolean isSelectable(){
            return selectable;
        }

        public void setSelectable(boolean selectable){
            this.selectable = selectable;
        }

        public void findExpandedObjects(Seq<Object> objects){
            if(expanded && !TreeElement.findExpandedObjects(children, objects)) objects.add(object);
        }

        public void restoreExpandedObjects(Seq objects){
            for(int i = 0, n = objects.size; i < n; i++){
                TreeElementNode node = findNode(objects.get(i));
                if(node != null){
                    node.setExpanded(true);
                    node.expandTo();
                }
            }
        }

        /**
         * Returns the height of the node as calculated for layout. A subclass may override and increase the returned height to
         * create a blank space in the tree above the node, eg for a separator.
         */
        public float getHeight(){
            return height;
        }
    }

    /**
     * The style for a {@link TreeElement}.
     * @author Nathan Sweet
     */
    public static class TreeStyle{
        public Drawable plus, minus;
        /** Optional. */
        public Drawable over, selection, background;

        public TreeStyle(){
        }

        public TreeStyle(Drawable plus, Drawable minus, Drawable selection){
            this.plus = plus;
            this.minus = minus;
            this.selection = selection;
        }

        public TreeStyle(TreeStyle style){
            this.plus = style.plus;
            this.minus = style.minus;
            this.selection = style.selection;
        }
    }
}
