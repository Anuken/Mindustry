package io.anuke.mindustry.world.blocks.logic;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.Input.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.pooling.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class MessageBlock extends Block{
    protected static int maxTextLength = 150;

    public MessageBlock(String name){
        super(name);
        configurable = true;
        solid = true;
        destructible = true;
    }

    @Remote(targets = Loc.both, called = Loc.both, forward = true)
    public static void setMessageBlockText(Player player, Tile tile, String text){
        if(Net.server() && text.length() > maxTextLength){
            throw new ValidateException(player, "Player has gone above text limit.");
        }

        //can be broken while a player is typing
        if(!(tile.block() instanceof MessageBlock)){
            return;
        }

        MessageBlockEntity entity = tile.entity();
        if(entity != null){
            entity.message = text.replace("\r", "\n");
        }
    }

    @Override
    public void drawSelect(Tile tile){
        MessageBlockEntity entity = tile.entity();
        BitmapFont font = Core.scene.skin.getFont("outline");
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 4f / UnitScl.dp.scl(1f));
        font.setUseIntegerPositions(false);

        String text = entity.message == null || entity.message.isEmpty() ? "[lightgray]" + Core.bundle.get("empty") : entity.message;

        l.setText(font, text, Color.WHITE, 90f, Align.left, false);
        float offset = 1f;

        Draw.color(0f, 0f, 0f, 0.2f);
        Fill.rect(tile.drawx(), tile.drawy() - tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
        Draw.color();
        font.draw(text, tile.drawx() - l.width/2f, tile.drawy() - tilesize/2f - offset, 90f, Align.left, false);
        font.setUseIntegerPositions(ints);

        font.getData().setScale(1f);

        Pools.free(l);
    }

    @Override
    public void buildTable(Tile tile, Table table){
        MessageBlockEntity entity = tile.entity();

        table.addImageButton("icon-pencil-small", iconsizesmall, () -> {
            if(mobile){
                Core.input.getTextInput(new TextInput(){{
                    text = entity.message;
                    multiline = true;
                    maxLength = maxTextLength;
                    accepted = out -> {
                        Call.setMessageBlockText(player, tile, out);
                    };
                }});
            }else{
                FloatingDialog dialog = new FloatingDialog("$editmessage");
                dialog.setFillParent(false);
                TextArea a = dialog.cont.add(new TextArea(entity.message.replace("\n", "\r"))).size(340f, 160f).get();
                a.setMaxLength(maxTextLength);
                dialog.buttons.addButton("$ok", () -> {
                    Call.setMessageBlockText(player, tile, a.getText());
                    dialog.hide();
                }).size(130f, 60f);
                dialog.show();
            }
            control.input.frag.config.hideConfig();
        }).size(40f);
    }

    @Override
    public void updateTableAlign(Tile tile, Table table){
        Vector2 pos = Core.input.mouseScreen(tile.drawx(), tile.drawy() + tile.block().size * tilesize / 2f + 1);
        table.setPosition(pos.x, pos.y, Align.bottom);
    }

    @Override
    public TileEntity newEntity(){
        return new MessageBlockEntity();
    }

    public class MessageBlockEntity extends TileEntity{
        public String message = "";

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeUTF(message);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            message = stream.readUTF();
        }
    }
}
