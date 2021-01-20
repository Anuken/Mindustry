package mindustry.world.blocks.logic;

import arc.*;
import arc.Input.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MessageBlock extends Block{
    //don't change this too much unless you want to run into issues with packet sizes
    public int maxTextLength = 220;
    public int maxNewlines = 24;

    public MessageBlock(String name){
        super(name);
        configurable = true;
        solid = true;
        destructible = true;
        group = BlockGroup.logic;
        drawDisabled = false;

        config(String.class, (MessageBuild tile, String text) -> {
            if(text.length() > maxTextLength){
                return; //no.
            }

            tile.message.ensureCapacity(text.length());
            tile.message.setLength(0);

            text = text.trim();
            int count = 0;
            for(int i = 0; i < text.length(); i++){
                char c = text.charAt(i);
                if(c == '\n'){
                    count ++;
                    if(count <= maxNewlines){
                        tile.message.append('\n');
                    }
                }else{
                    tile.message.append(c);
                }
            }
        });
    }

    public class MessageBuild extends Building{
        public StringBuilder message = new StringBuilder();

        @Override
        public void drawSelect(){
            if(renderer.pixelator.enabled()) return;

            Font font = Fonts.outline;
            GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
            boolean ints = font.usesIntegerPositions();
            font.getData().setScale(1 / 4f / Scl.scl(1f));
            font.setUseIntegerPositions(false);

            CharSequence text = message == null || message.length() == 0 ? "[lightgray]" + Core.bundle.get("empty") : message;

            l.setText(font, text, Color.white, 90f, Align.left, true);
            float offset = 1f;

            Draw.color(0f, 0f, 0f, 0.2f);
            Fill.rect(x, y - tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
            Draw.color();
            font.setColor(Color.white);
            font.draw(text, x - l.width/2f, y - tilesize/2f - offset, 90f, Align.left, true);
            font.setUseIntegerPositions(ints);

            font.getData().setScale(1f);

            Pools.free(l);
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.pencil, () -> {
                if(mobile){
                    Core.input.getTextInput(new TextInput(){{
                        text = message.toString();
                        multiline = true;
                        maxLength = maxTextLength;
                        accepted = str -> configure(str);
                    }});
                }else{
                    BaseDialog dialog = new BaseDialog("@editmessage");
                    dialog.setFillParent(false);
                    TextArea a = dialog.cont.add(new TextArea(message.toString().replace("\r", "\n"))).size(380f, 160f).get();
                    a.setFilter((textField, c) -> {
                        if(c == '\n'){
                            int count = 0;
                            for(int i = 0; i < textField.getText().length(); i++){
                                if(textField.getText().charAt(i) == '\n'){
                                    count++;
                                }
                            }
                            return count < maxNewlines;
                        }
                        return true;
                    });
                    a.setMaxLength(maxTextLength);
                    dialog.buttons.button("@ok", () -> {
                        configure(a.getText());
                        dialog.hide();
                    }).size(130f, 60f);
                    dialog.update(() -> {
                        if(tile.block() != MessageBlock.this){
                            dialog.hide();
                        }
                    });
                    dialog.show();
                }
                deselect();
            }).size(40f);
        }

        @Override
        public void handleString(Object value){
            message.setLength(0);
            message.append(value);
        }

        @Override
        public void updateTableAlign(Table table){
            Vec2 pos = Core.input.mouseScreen(x, y + size * tilesize / 2f + 1);
            table.setPosition(pos.x, pos.y, Align.bottom);
        }

        @Override
        public String config(){
            return message.toString();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.str(message.toString());
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            message = new StringBuilder(read.str());
        }
    }
}
