package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.Label.LabelStyle;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.core.Core.scene;
import static io.anuke.ucore.core.Core.skin;

public class ChatFragment extends Table{
    private final static int messagesShown = 10;
    private Array<ChatMessage> messages = new Array<>();
    private float fadetime;
    private boolean chatOpen = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = Unit.dp.scl(4), offsety = Unit.dp.scl(4), fontoffsetx = Unit.dp.scl(2), chatspace = Unit.dp.scl(50);
    private float textWidth = Unit.dp.scl(600);
    private Color shadowColor = new Color(0, 0, 0, 0.4f);
    private float textspacing = Unit.dp.scl(10);
    private Array<String> history = new Array<>();
    private int historyPos = 0;
    private int scrollPos = 0;
    private Fragment container = new Fragment(){
        @Override
        public void build(Group parent){
            scene.add(ChatFragment.this);
        }
    };

    public ChatFragment(){
        super();

        setFillParent(true);
        font = Core.skin.getFont("default-font");

        visible(() -> {
            if(!Net.active() && messages.size > 0){
                clearMessages();

                if(chatOpen){
                    hide();
                }
            }

            return !state.is(State.menu) && Net.active();
        });

        update(() -> {

            if(Net.active() && Inputs.keyTap("chat")){
                toggle();
            }

            if(chatOpen){
                if(Inputs.keyTap("chat_history_prev") && historyPos < history.size - 1){
                    if(historyPos == 0) history.set(0, chatfield.getText());
                    historyPos++;
                    updateChat();
                }
                if(Inputs.keyTap("chat_history_next") && historyPos > 0){
                    historyPos--;
                    updateChat();
                }
                scrollPos = (int) Mathf.clamp(scrollPos + Inputs.getAxis("chat_scroll"), 0, Math.max(0, messages.size - messagesShown));
            }
        });

        history.insert(0, "");
        setup();
    }

    public Fragment container(){
        return container;
    }

    public void clearMessages(){
        messages.clear();
        history.clear();
        history.insert(0, "");
    }

    private void setup(){
        fieldlabel.setStyle(new LabelStyle(fieldlabel.getStyle()));
        fieldlabel.getStyle().font = font;
        fieldlabel.setStyle(fieldlabel.getStyle());

        chatfield = new TextField("", new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class)));
        chatfield.setTextFieldFilter((field, c) -> field.getText().length() < Vars.maxTextLength);
        chatfield.getStyle().background = null;
        chatfield.getStyle().font = skin.getFont("default-font-chat");
        chatfield.getStyle().fontColor = Color.WHITE;
        chatfield.setStyle(chatfield.getStyle());

        if(mobile){
            chatfield.tapped(() -> {
                Dialog dialog = new Dialog("", "dialog");
                dialog.setFillParent(true);
                dialog.content().top();
                dialog.content().defaults().height(65f);
                TextField to = dialog.content().addField("", t-> {}).pad(15).width(250f).get();
                to.setMaxLength(maxTextLength);
                to.keyDown(Keys.ENTER, () -> dialog.content().find("okb").fireClick());
                dialog.content().addButton("$text.ok", () -> {
                    chatfield.clearText();
                    chatfield.appendText(to.getText());
                    chatfield.change();
                    dialog.hide();
                    Gdx.input.setOnscreenKeyboardVisible(false);
                    toggle();
                }).width(90f).name("okb");

                dialog.show();
                Timers.runTask(1f, () -> {
                    to.setCursorPosition(to.getText().length());
                    Core.scene.setKeyboardFocus(to);
                    Gdx.input.setOnscreenKeyboardVisible(true);
                });
            });
        }

        bottom().left().marginBottom(offsety).marginLeft(offsetx * 2).add(fieldlabel).padBottom(6f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);

        if(Vars.mobile){
            marginBottom(105f);
            marginRight(240f);
        }
    }

    @Override
    public void draw(Batch batch, float alpha){

        batch.setColor(shadowColor);

        if(chatOpen){
            batch.draw(skin.getRegion("white"), offsetx, chatfield.getY(), chatfield.getWidth() + 15f, chatfield.getHeight() - 1);
        }

        super.draw(batch, alpha);

        float spacing = chatspace;

        chatfield.setVisible(chatOpen);
        fieldlabel.setVisible(chatOpen);

        batch.setColor(shadowColor);

        float theight = offsety + spacing + getMarginBottom();
        for(int i = scrollPos; i < messages.size && i < messagesShown + scrollPos && (i < fadetime || chatOpen); i++){

            layout.setText(font, messages.get(i).formattedMessage, Color.WHITE, textWidth, Align.bottomLeft, true);
            theight += layout.height + textspacing;
            if(i - scrollPos == 0) theight -= textspacing + 1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i).formattedMessage, fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(!chatOpen && fadetime - i < 1f && fadetime - i >= 0f){
                font.getCache().setAlphas(fadetime - i);
                batch.setColor(0, 0, 0, shadowColor.a * (fadetime - i));
            }

            batch.draw(skin.getRegion("white"), offsetx, theight - layout.height - 2, textWidth + Unit.dp.scl(4f), layout.height + textspacing);
            batch.setColor(shadowColor);

            font.getCache().draw(batch);
        }

        batch.setColor(Color.WHITE);

        if(fadetime > 0 && !chatOpen)
            fadetime -= Timers.delta() / 180f;
    }

    private void sendMessage(){
        String message = chatfield.getText();
        clearChatInput();

        if(message.replaceAll(" ", "").isEmpty()) return;

        history.insert(1, message);

        Call.sendMessage(players[0], message);
    }

    public void toggle(){

        if(!chatOpen){
            scene.setKeyboardFocus(chatfield);
            chatfield.fireClick();
            chatOpen = !chatOpen;
        }else{
            scene.setKeyboardFocus(null);
            chatOpen = !chatOpen;
            scrollPos = 0;
            sendMessage();
        }
    }

    public void hide(){
        scene.setKeyboardFocus(null);
        chatOpen = false;
        clearChatInput();
    }

    public void updateChat(){
        chatfield.setText(history.get(historyPos));
        chatfield.setCursorPosition(chatfield.getText().length());
    }

    public void clearChatInput(){
        historyPos = 0;
        history.set(0, "");
        chatfield.setText("");
    }

    public boolean chatOpen(){
        return chatOpen;
    }

    public int getMessagesSize(){
        return messages.size;
    }

    public void addMessage(String message, String sender){
        messages.insert(0, new ChatMessage(message, sender));

        fadetime += 1f;
        fadetime = Math.min(fadetime, messagesShown) + 1f;
    }

    private static class ChatMessage{
        public final String sender;
        public final String message;
        public final String formattedMessage;

        public ChatMessage(String message, String sender){
            this.message = message;
            this.sender = sender;
            if(sender == null){ //no sender, this is a server message?
                formattedMessage = message;
            }else{
                formattedMessage = "[CORAL][[" + sender + "[CORAL]]:[WHITE] " + message;
            }
        }
    }

}
