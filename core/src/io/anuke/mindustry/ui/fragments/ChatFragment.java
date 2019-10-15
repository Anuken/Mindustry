package io.anuke.mindustry.ui.fragments;

import io.anuke.arc.*;
import io.anuke.arc.Input.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.Label.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.arc.Core.*;
import static io.anuke.mindustry.Vars.net;
import static io.anuke.mindustry.Vars.*;

public class ChatFragment extends Table{
    private final static int messagesShown = 10;
    private Array<ChatMessage> messages = new Array<>();
    private float fadetime;
    private boolean chatOpen = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = Scl.scl(4), offsety = Scl.scl(4), fontoffsetx = Scl.scl(2), chatspace = Scl.scl(50);
    private Color shadowColor = new Color(0, 0, 0, 0.4f);
    private float textspacing = Scl.scl(10);
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
        font = Fonts.def;

        visible(() -> {
            if(!net.active() && messages.size > 0){
                clearMessages();

                if(chatOpen){
                    hide();
                }
            }

            return net.active();
        });

        update(() -> {

            if(net.active() && input.keyTap(Binding.chat) && (scene.getKeyboardFocus() == chatfield || scene.getKeyboardFocus() == null)){
                toggle();
            }

            if(chatOpen){
                if(input.keyTap(Binding.chat_history_prev) && historyPos < history.size - 1){
                    if(historyPos == 0) history.set(0, chatfield.getText());
                    historyPos++;
                    updateChat();
                }
                if(input.keyTap(Binding.chat_history_next) && historyPos > 0){
                    historyPos--;
                    updateChat();
                }
                scrollPos = (int)Mathf.clamp(scrollPos + input.axis(Binding.chat_scroll), 0, Math.max(0, messages.size - messagesShown));
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

        chatfield = new TextField("", new TextField.TextFieldStyle(scene.getStyle(TextField.TextFieldStyle.class)));
        chatfield.setMaxLength(Vars.maxTextLength);
        chatfield.getStyle().background = null;
        chatfield.getStyle().font = Fonts.chat;
        chatfield.getStyle().fontColor = Color.white;
        chatfield.setStyle(chatfield.getStyle());

        bottom().left().marginBottom(offsety).marginLeft(offsetx * 2).add(fieldlabel).padBottom(6f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);

        if(Vars.mobile){
            marginBottom(105f);
            marginRight(240f);
        }
    }

    @Override
    public void draw(){
        float opacity = Core.settings.getInt("chatopacity") / 100f;
        float textWidth = Math.min(Core.graphics.getWidth()/1.5f, Scl.scl(700f));

        Draw.color(shadowColor);

        if(chatOpen){
            Fill.crect(offsetx, chatfield.getY(), chatfield.getWidth() + 15f, chatfield.getHeight() - 1);
        }

        super.draw();

        float spacing = chatspace;

        chatfield.visible(chatOpen);
        fieldlabel.visible(chatOpen);

        Draw.color(shadowColor);
        Draw.alpha(shadowColor.a * opacity);

        float theight = offsety + spacing + getMarginBottom();
        for(int i = scrollPos; i < messages.size && i < messagesShown + scrollPos && (i < fadetime || chatOpen); i++){

            layout.setText(font, messages.get(i).formattedMessage, Color.white, textWidth, Align.bottomLeft, true);
            theight += layout.height + textspacing;
            if(i - scrollPos == 0) theight -= textspacing + 1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i).formattedMessage, fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(!chatOpen && fadetime - i < 1f && fadetime - i >= 0f){
                font.getCache().setAlphas((fadetime - i) * opacity);
                Draw.color(0, 0, 0, shadowColor.a * (fadetime - i) * opacity);
            }else{
                font.getCache().setAlphas(opacity);
            }

            Fill.crect(offsetx, theight - layout.height - 2, textWidth + Scl.scl(4f), layout.height + textspacing);
            Draw.color(shadowColor);
            Draw.alpha(opacity * shadowColor.a);

            font.getCache().draw();
        }

        Draw.color();

        if(fadetime > 0 && !chatOpen)
            fadetime -= Time.delta() / 180f;
    }

    private void sendMessage(){
        String message = chatfield.getText();
        clearChatInput();

        if(message.replaceAll(" ", "").isEmpty()) return;

        history.insert(1, message);

        Call.sendChatMessage(message);
    }

    public void toggle(){

        if(!chatOpen){
            scene.setKeyboardFocus(chatfield);
            chatOpen = !chatOpen;
            if(mobile){
                TextInput input = new TextInput();
                input.maxLength = maxTextLength;
                input.accepted = text -> {
                    chatfield.setText(text);
                    sendMessage();
                    hide();
                    Core.input.setOnscreenKeyboardVisible(false);
                };
                input.canceled = this::hide;
                Core.input.getTextInput(input);
            }else{
                chatfield.fireClick();
            }
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
