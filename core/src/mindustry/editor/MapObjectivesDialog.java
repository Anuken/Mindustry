package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.editor.MapObjectivesDialog.ObjectiveContainer.ObjectiveTilemap.ObjectiveTile.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class MapObjectivesDialog extends BaseDialog{
    private static final int
    objWidth = 6, objHeight = 3,
    bounds = 100;

    private static final float unitSize = 48f;

    public ObjectiveContainer container;

    public MapObjectivesDialog(){
        super("@editor.objectives");
        cont.add(container = new ObjectiveContainer()).pad(12f).grow();

        buttons.defaults().size(170f, 64f).pad(2f);
        buttons.button("@back", Icon.left, this::hide);

        buttons.button("@add", Icon.add, () -> {
            var selection = new BaseDialog("@add");
            selection.cont.pane(p -> {
                p.background(Tex.button);
                p.marginRight(14f);
                p.defaults().size(195f, 56f);

                int i = 0;
                for(var gen : MapObjectives.allObjectiveTypes){
                    var obj = gen.get();

                    p.button(obj.typeName(), Styles.flatt, () -> {
                        container.query(obj);
                        selection.hide();
                    }).with(Table::left).get().getLabelCell().growX().left().padLeft(5f).labelAlign(Align.left);

                    if(++i % 3 == 0) p.row();
                }
            }).scrollX(false);

            selection.addCloseButton();
            selection.show();
        });
    }

    public void show(Seq<MapObjective> objectives){
        container.clearObjectives();
        if(
            objectives.any() && (
            // If the objectives were previously programmatically made...
            objectives.contains(obj -> obj.editorX == -1 || obj.editorY == -1) ||
            // ... or some idiot somehow made it not work...
            objectives.contains(obj -> !container.tilemap.createTile(obj))
        )){
            // ... then rebuild the structure.
            container.clearObjectives();

            // This is definitely NOT a good way to do it, but only insane people or people from the distant past would actually encounter this anyway.
            int w = objWidth + 2,
                len = objectives.size * w,
                columns = objectives.size,
                rows = 1;

            if(len > bounds){
                rows = len / bounds;
                columns = bounds / w;
            }

            int i = 0;
            loop:
            for(int y = 0; y < rows; y++){
                for(int x = 0; x < columns; x++){
                    container.tilemap.createTile(x * w, bounds - 1 - y * 2, objectives.get(i++));
                    if(i >= objectives.size) break loop;
                }
            }
        }

        container.objectives.set(objectives);
    }

    public static class ObjectiveContainer extends ScrollPane{
        public Seq<MapObjective> objectives = new Seq<>();
        public ObjectiveTilemap tilemap;

        protected boolean querying;
        protected MapObjective toQuery;

        protected final InputListener canceler;
        protected final ClickListener creator;

        public ObjectiveContainer(){
            super(null, Styles.noBarPane);

            getStyle().background = Styles.black5;
            setWidget(tilemap = new ObjectiveTilemap());
            setOverscroll(false, false);
            setCancelTouchFocus(false);

            addCaptureListener(canceler = new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    if(querying && button == KeyCode.mouseRight){
                        stopQuery();

                        event.stop();
                        return true;
                    }else{
                        return false;
                    }
                }
            });

            addCaptureListener(creator = new HandCursorListener(){
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Element fromActor){
                    if(querying) super.enter(event, x, y, pointer, fromActor);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Element toActor){
                    if(querying) super.exit(event, x, y, pointer, toActor);
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    cancel();
                }

                @Override
                public void clicked(InputEvent event, float x, float y){
                    if(!querying || toQuery == null) return;

                    Vec2 pos = localToDescendantCoordinates(tilemap, Tmp.v1.set(x, y));
                    if(tilemap.createTile(
                        Mathf.round((pos.x - objWidth * unitSize / 2f) / unitSize),
                        Mathf.floor((pos.y - unitSize) / unitSize),
                        toQuery
                    )){
                        objectives.add(toQuery);
                        stopQuery();
                    }
                }
            });
        }

        public void clearObjectives(){
            stopQuery();
            tilemap.clearTiles();
        }

        protected void stopQuery(){
            if(!querying) return;
            querying = false;

            Core.graphics.restoreCursor();
        }

        public void query(MapObjective obj){
            stopQuery();
            querying = true;
            toQuery = obj;
        }

        public boolean isQuerying(){
            return querying;
        }

        public boolean isVisualPressed(){
            return creator.isVisualPressed();
        }

        public class ObjectiveTilemap extends WidgetGroup{
            protected final GridBits grid = new GridBits(bounds, bounds);

            /** The connector button that is being pressed. */
            protected @Nullable Connector connecting;
            /** The current tile that is being moved. */
            protected @Nullable ObjectiveTile moving;
            /** True if {@link #connecting} is looking for a parent, false otherwise. */
            protected boolean findParent;

            public ObjectiveTilemap(){
                setTransform(false);
                setSize(getPrefWidth(), getPrefHeight());
                touchable(() -> isQuerying() ? Touchable.disabled : Touchable.childrenOnly);
            }

            @Override
            public void draw(){
                validate();
                int minX = Math.max(Mathf.floor((x - 1f) / unitSize), 0), minY = Math.max(Mathf.floor((y - 1f) / unitSize), 0),
                    maxX = Math.min(Mathf.ceil((x + width + 1f) / unitSize), bounds), maxY = Math.min(Mathf.ceil((y + height + 1f) / unitSize), bounds);
                float progX = x % unitSize, progY = y % unitSize;

                Lines.stroke(2f);
                Draw.color(Pal.gray, parentAlpha);

                for(int x = minX; x <= maxX; x++) Lines.line(progX + x * unitSize, minY * unitSize, progX + x * unitSize, maxY * unitSize);
                for(int y = minY; y <= maxY; y++) Lines.line(minX * unitSize, progY + y * unitSize, maxX * unitSize, progY + y * unitSize);

                if(isQuerying()){
                    int tx, ty;
                    Vec2 pos = screenToLocalCoordinates(Core.input.mouse());
                    pos.x = x + (tx = Mathf.round((pos.x - objWidth * unitSize / 2f) / unitSize)) * unitSize;
                    pos.y = y + (ty = Mathf.floor((pos.y - unitSize) / unitSize)) * unitSize;

                    Lines.stroke(4f);
                    Draw.color(
                        isVisualPressed() ? Pal.metalGrayDark : validPlace(tx, ty) ? Pal.accent : Pal.remove,
                        parentAlpha * (inPlaceBounds(tx, ty) ? 1f : Mathf.absin(6f, 1f))
                    );

                    Lines.rect(pos.x, pos.y, objWidth * unitSize, objHeight * unitSize);
                }

                if(moving != null){
                    int tx, ty;
                    float x = this.x + (tx = Mathf.round(moving.x / unitSize)) * unitSize;
                    float y = this.y + (ty = Mathf.round(moving.y / unitSize)) * unitSize;

                    Draw.color(
                        validMove(moving, tx, ty) ? Pal.accent : Pal.remove,
                        0.5f * parentAlpha * (inPlaceBounds(tx, ty) ? 1f : Mathf.absin(6f, 1f))
                    );

                    Fill.crect(x, y, objWidth * unitSize, objHeight * unitSize);
                }

                Draw.reset();
                super.draw();

                Draw.reset();
                Seq<ObjectiveTile> tiles = getChildren().as();

                Connector targetConnect = null;
                if(connecting != null){
                    Vec2 pos = screenToLocalCoordinates(Core.input.mouse());
                    if(hit(pos.x, pos.y, true) instanceof Connector con && connecting.canConnectTo(con)) targetConnect = con;
                }

                boolean removing = false;
                for(var tile : tiles){
                    for(var parent : tile.obj.parents){
                        var parentTile = tiles.find(t -> t.obj == parent);

                        Connector
                            parentConnect = parentTile.selectorChildren,
                            childConnect = tile.selectorParent;

                        if(targetConnect != null && (
                            (connecting.findParent && connecting == childConnect && targetConnect == parentConnect) ||
                            (!connecting.findParent && connecting == parentConnect && targetConnect == childConnect)
                        )){
                            removing = true;
                            continue;
                        }

                        Vec2
                            from = parentConnect.localToAscendantCoordinates(this, Tmp.v1.set(parentConnect.getWidth() / 2f, parentConnect.getHeight() / 2f)).add(x, y),
                            to = childConnect.localToAscendantCoordinates(this, Tmp.v2.set(childConnect.getWidth() / 2f, childConnect.getHeight() / 2f)).add(x, y);

                        drawCurve(false, from.x, from.y, to.x, to.y);
                    }
                }

                if(connecting != null){
                    Vec2
                        from = (targetConnect == null
                            ? screenToLocalCoordinates(Core.input.mouse())
                            : targetConnect.localToAscendantCoordinates(this, Tmp.v2.set(targetConnect.getWidth() / 2f, targetConnect.getHeight() / 2f))
                        ).add(x, y),
                        to = connecting.localToAscendantCoordinates(this, Tmp.v1.set(connecting.getWidth() / 2f, connecting.getHeight() / 2f)).add(x, y);

                    drawCurve(removing, to.x, to.y, from.x, from.y);
                }

                Draw.reset();
            }

            protected void drawCurve(boolean remove, float x1, float y1, float x2, float y2){
                Lines.stroke(4f);
                Draw.color(remove ? Pal.remove : Pal.accent, parentAlpha);

                float dist = Math.abs(x1 - x2) / 2f;
                Lines.curve(x1, y1, x1 + dist, y1, x2 - dist, y2, x2, y2, Math.max(4, (int)(Mathf.dst(x1, y1, x2, y2) / 4f)));

                Draw.reset();
            }

            public boolean inPlaceBounds(int x, int y){
                return Structs.inBounds(x, y, bounds - objWidth + 1, bounds - objHeight + 1);
            }

            public boolean validPlace(int x, int y){
                if(!inPlaceBounds(x, y)) return false;
                for(int tx = 0; tx < objWidth; tx++){
                    for(int ty = 0; ty < objHeight; ty++){
                        if(occupied(x + tx, y + ty)) return false;
                    }
                }

                return true;
            }

            public boolean validMove(ObjectiveTile tile, int newX, int newY){
                if(!inPlaceBounds(newX, newY)) return false;

                int x = tile.tx, y = tile.ty;
                for(int tx = 0; tx < objWidth; tx++){
                    for(int ty = 0; ty < objHeight; ty++){
                        grid.set(x + tx, y + ty, false);
                    }
                }

                boolean valid = validPlace(newX, newY);
                for(int tx = 0; tx < objWidth; tx++){
                    for(int ty = 0; ty < objHeight; ty++){
                        grid.set(x + tx, y + ty);
                    }
                }

                return valid;
            }

            public boolean occupied(int x, int y){
                return grid.get(x, y);
            }

            public boolean createTile(MapObjective obj){
                return createTile(obj.editorX, obj.editorY, obj);
            }

            public boolean createTile(int x, int y, MapObjective obj){
                if(!validPlace(x, y)) return false;

                ObjectiveTile tile = new ObjectiveTile(obj, x, y);
                tile.pack();

                addChild(tile);
                for(int tx = 0; tx < objWidth; tx++){
                    for(int ty = 0; ty < objHeight; ty++){
                        grid.set(x + tx, y + ty);
                    }
                }

                return true;
            }

            public boolean moveTile(ObjectiveTile tile, int newX, int newY){
                if(!validMove(tile, newX, newY)) return false;

                int x = tile.tx, y = tile.ty;
                for(int tx = 0; tx < objWidth; tx++){
                    for(int ty = 0; ty < objHeight; ty++){
                        grid.set(x + tx, y + ty, false);
                    }
                }

                tile.pos(newX, newY);

                x = newX;
                y = newY;
                for(int tx = 0; tx < objWidth; tx++){
                    for(int ty = 0; ty < objHeight; ty++){
                        grid.set(x + tx, y + ty);
                    }
                }

                return true;
            }

            public void clearTiles(){
                clearChildren();
                grid.clear();
            }

            @Override
            public float getPrefWidth(){
                return bounds * unitSize;
            }

            @Override
            public float getPrefHeight(){
                return bounds * unitSize;
            }

            public class ObjectiveTile extends Table{
                public final MapObjective obj;
                public int tx, ty;

                public final Connector selectorParent, selectorChildren;

                public ObjectiveTile(MapObjective obj, int x, int y){
                    this.obj = obj;
                    setTransform(false);
                    setClip(false);

                    var middle = new ImageButtonStyle(){{
                        down = Tex.buttonEdgeDown5;
                        up = Tex.buttonEdge5;
                        over = Tex.buttonEdgeOver5;
                        imageUpColor = Color.white;
                    }};

                    add(selectorParent = new Connector(true)).size(unitSize);
                    button(Icon.eraser, middle, obj.parents::clear).size(unitSize);

                    add(new ImageButton(Icon.move, new ImageButtonStyle(){{
                        up = Tex.whiteui;
                        imageUpColor = Color.black;
                    }}){{
                        var e = this;

                        setColor(Pal.accent);
                        addCaptureListener(new InputListener(){
                            int prevX, prevY;
                            float lastX, lastY;

                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                                if(moving != null) return false;
                                moving = ObjectiveTile.this;

                                prevX = moving.tx;
                                prevY = moving.ty;

                                // Convert to world pos first because the button gets dragged too.
                                Vec2 pos = e.localToStageCoordinates(Tmp.v1.set(x, y));
                                lastX = pos.x;
                                lastY = pos.y;

                                moving.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                                return true;
                            }

                            @Override
                            public void touchDragged(InputEvent event, float x, float y, int pointer){
                                Vec2 pos = e.localToStageCoordinates(Tmp.v1.set(x, y));

                                moving.moveBy(pos.x - lastX, pos.y - lastY);
                                lastX = pos.x;
                                lastY = pos.y;

                                moving.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                            }

                            @Override
                            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                                if(!moveTile(moving,
                                    Mathf.round(moving.x / unitSize),
                                    Mathf.round(moving.y / unitSize)
                                )) moving.pos(prevX, prevY);
                                moving = null;
                            }
                        });
                    }}).height(unitSize).growX();

                    button(Icon.eraser, middle, () -> objectives.each(o -> o.parents.remove(obj))).size(unitSize);
                    add(selectorChildren = new Connector(false)).size(unitSize);

                    row().table(t -> t.background(Tex.buttonSelectTrans)).grow().colspan(5);

                    setSize(getPrefWidth(), getPrefHeight());
                    pos(x, y);
                }

                public void pos(int x, int y){
                    tx = obj.editorX = x;
                    ty = obj.editorY = y;
                    this.x = x * unitSize;
                    this.y = y * unitSize;
                }

                @Override
                public float getPrefWidth(){
                    return objWidth * unitSize;
                }

                @Override
                public float getPrefHeight(){
                    return objHeight * unitSize;
                }

                public class Connector extends ImageButton{
                    public final boolean findParent;

                    public Connector(boolean findParent){
                        super(new ImageButtonStyle(){{
                            down = findParent ? Tex.buttonEdgeDown1 : Tex.buttonEdgeDown3;
                            up = findParent ? Tex.buttonEdge1 : Tex.buttonEdge3;
                            over = findParent ? Tex.buttonEdgeOver1 : Tex.buttonEdgeOver3;
                            imageUp = Tex.checkOn;
                        }});

                        this.findParent = findParent;

                        clearChildren();
                        addCaptureListener(new InputListener(){
                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                                if(connecting != null) return false;
                                connecting = Connector.this;
                                ObjectiveTilemap.this.findParent = true;

                                connecting.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                                return true;
                            }

                            @Override
                            public void touchDragged(InputEvent event, float x, float y, int pointer){
                                connecting.getScene().cancelTouchFocusExcept(this, event.listenerActor);
                            }

                            @Override
                            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                                if(connecting != Connector.this) return;

                                Vec2 pos = Connector.this.localToAscendantCoordinates(ObjectiveTilemap.this, Tmp.v1.set(x, y));
                                if(
                                    ObjectiveTilemap.this.hit(pos.x, pos.y, true) instanceof Connector con &&
                                        con.tile() != tile() &&
                                        con.findParent != findParent
                                ){
                                    if(findParent){
                                        if(!obj.parents.remove(con.tile().obj)) obj.parents.add(con.tile().obj);
                                    }else{
                                        if(!con.tile().obj.parents.remove(obj)) con.tile().obj.parents.add(obj);
                                    }
                                }

                                connecting = null;
                            }
                        });
                    }
                    
                    public boolean canConnectTo(Connector other){
                        return
                            findParent != other.findParent &&
                            tile() != other.tile();
                    }
                    
                    public boolean isConnectedTo(Connector other){
                        return (findParent ? other.tile() : tile()).obj.parents.contains(findParent ? obj : other.tile().obj);
                    }

                    public ObjectiveTile tile(){
                        return ObjectiveTile.this;
                    }

                    @Override
                    public boolean isPressed(){
                        return super.isPressed() || connecting == this;
                    }
                }
            }
        }
    }
}
