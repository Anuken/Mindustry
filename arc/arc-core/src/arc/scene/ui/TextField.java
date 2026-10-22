package arc.scene.ui;

import arc.*;
import arc.Input.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.font.*;
import arc.graphics.g2d.*;
import arc.graphics.font.Font.*;
import arc.graphics.g2d.GlyphLayout.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.ChangeListener.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Timer.*;
import arc.util.pooling.*;

import static arc.Core.*;

/**
 * A single-line text input field.
 * <p>
 * The preferred height of a text field is the height of the {@link TextFieldStyle#font} and {@link TextFieldStyle#background}.
 * The preferred width of a text field is 150, a relatively arbitrary size.
 * <p>
 * @author mzechner
 * @author Nathan Sweet
 */
public class TextField extends Element implements Disableable{
    static protected final char BACKSPACE = 8;
    static protected final char TAB = '\t';
    static protected final char DELETE = 127;
    static protected final char BULLET = 149;

    private static final Vec2 tmp1 = new Vec2();
    private static final Vec2 tmp2 = new Vec2();
    private static final Vec2 tmp3 = new Vec2();

    public static float keyRepeatInitialTime = 0.4f;
    public static float keyRepeatTime = 0.1f;

    /** Special field containing implementation-specific data for IME support. */
    public Object imeData;

    protected final GlyphLayout layout = new GlyphLayout(true);
    protected final FloatSeq glyphPositions = new FloatSeq();
    protected String text;
    protected int cursor, selectionStart;
    protected boolean hasSelection;
    protected boolean writeEnters;
    protected CharSequence displayText;
    protected float fontOffset, textHeight, textOffset;
    protected InputListener inputDialogListener;
    protected TextFieldStyle style;
    protected InputListener inputListener;
    protected TextFieldListener listener;
    protected TextFieldValidator validator;
    protected TextFieldFilter filter;
    protected boolean focusTraversal = true, onlyFontChars = true, disabled;
    protected String undoText = "";
    protected long lastChangeTime;
    protected boolean passwordMode;
    protected float renderOffset;
    protected boolean cursorOn = true;
    protected boolean hasInputDialog = false;
    protected long lastBlink;
    protected KeyRepeatTask keyRepeatTask = new KeyRepeatTask();
    protected boolean programmaticChangeEvents;
    protected String messageText;
    protected int textHAlign = Align.left;
    protected float selectionX, selectionWidth;
    protected StringBuilder passwordBuffer;
    protected char passwordCharacter = BULLET;
    protected int visibleTextStart, visibleTextEnd;
    protected int maxLength = 0;
    protected float blinkTime = 0.32f;

    public TextField(){
        this("");
    }

    public TextField(String text){
        this(text, scene.getStyle(TextFieldStyle.class));
    }

    public TextField(String text, @Nullable Cons<String> listener){
        this(text);

        if(listener != null){
            changed(() -> {
                if(isValid()){
                    listener.get(getText());
                }
            });
        }
    }

    public TextField(String text, TextFieldStyle style){
        setStyle(style);
        initialize();
        setText(text);
        setSize(getPrefWidth(), getPrefHeight());
    }

    protected void initialize(){
        addListener(inputListener = createInputListener());
        addListener(new IbeamCursorListener());
        addInputDialog();
    }

    protected InputListener createInputListener(){
        return new TextFieldClickListener();
    }

    protected int letterUnderCursor(float x){

        x -= textOffset + fontOffset - style.font.getData().cursorX - glyphPositions.get(visibleTextStart);
        Drawable background = getBackgroundDrawable();
        if(background != null) x -= style.background.getLeftWidth();
        int n = this.glyphPositions.size;
        float[] glyphPositions = this.glyphPositions.items;
        for(int i = 1; i < n; i++){
            if(glyphPositions[i] > x){
                if(glyphPositions[i] - x <= x - glyphPositions[i - 1]) return i;
                return i - 1;
            }
        }

        return n - 1;
    }

    protected boolean isWordCharacter(char c){
        return Character.isLetterOrDigit(c);
    }

    protected int[] wordUnderCursor(int at){
        String text = this.text;
        int right = text.length(), left = 0, index = at;
        if(at >= text.length()){
            left = text.length();
            right = 0;
        }else{
            for(; index < right; index++){
                if(!isWordCharacter(text.charAt(index))){
                    right = index;
                    break;
                }
            }
            for(index = at - 1; index > -1; index--){
                if(!isWordCharacter(text.charAt(index))){
                    left = index + 1;
                    break;
                }
            }
        }
        return new int[]{left, right};
    }

    int[] wordUnderCursor(float x){
        return wordUnderCursor(letterUnderCursor(x));
    }

    boolean withinMaxLength(int size){
        return maxLength <= 0 || size < maxLength;
    }

    public void removeInputDialog(){
        if(hasInputDialog && inputDialogListener != null){
            removeListener(inputDialogListener);
            hasInputDialog = false;
        }
    }

    public void addInputDialog(){
        //mobile only
        if(!app.isMobile() || hasInputDialog) return;

        hasInputDialog = true;

        inputDialogListener = tapped(() -> {
            if(input.useKeyboard()) return;

            TextInput input = new TextInput();
            input.text = getText();
            if(maxLength > 0) input.maxLength = maxLength;
            input.multiline = this instanceof TextArea;
            input.accepted = text -> {
                clearText();
                appendText(text);
                change();
                Core.input.setOnscreenKeyboardVisible(false);
            };
            input.canceled = () -> {
                if(hasKeyboard()){
                    scene.setKeyboardFocus(null);
                }
                Core.input.setOnscreenKeyboardVisible(false);
            };
            Core.input.getTextInput(input);
        });
    }

    public int getMaxLength(){
        return this.maxLength;
    }

    public void setMaxLength(int maxLength){
        this.maxLength = maxLength;
    }

    public void clearText(){
        setText("");
    }

    /**
     * When false, text set by {@link #setText(String)} may contain characters not in the font, a space will be displayed instead.
     * When true (the default), characters not in the font are stripped by setText. Characters not in the font are always stripped
     * when typed or pasted.
     */
    public void setOnlyFontChars(boolean onlyFontChars){
        this.onlyFontChars = onlyFontChars;
    }

    /**
     * Returns the text field's style. Modifying the returned style may not have an effect until {@link #setStyle(TextFieldStyle)}
     * is called.
     */
    public TextFieldStyle getStyle(){
        return style;
    }

    public void setStyle(TextFieldStyle style){
        if(style == null) throw new IllegalArgumentException("style cannot be null.");
        this.style = style;
        textHeight = style.font.getCapHeight() - style.font.getDescent() * 2;
        invalidateHierarchy();
    }

    protected void calculateOffsets(){
        float visibleWidth = getWidth();
        Drawable background = getBackgroundDrawable();
        if(background != null) visibleWidth -= background.getLeftWidth() + background.getRightWidth();

        int glyphCount = glyphPositions.size;
        float[] glyphPositions = this.glyphPositions.items;

        // Check if the cursor has gone out the left or right side of the visible area and adjust renderOffset.
        cursor = Mathf.clamp(cursor, 0, glyphPositions.length - 1);
        float distance = glyphPositions[Math.max(0, cursor - 1)] + renderOffset;
        if(distance <= 0)
            renderOffset -= distance;
        else{
            int index = Math.min(glyphCount - 1, cursor + 1);
            float minX = glyphPositions[index] - visibleWidth;
            if(-renderOffset < minX) renderOffset = -minX;
        }

        // Prevent renderOffset from starting too close to the end, eg after text was deleted.
        float maxOffset = 0;
        float width = glyphPositions[Mathf.clamp(glyphCount - 1, 0, glyphPositions.length - 1)];
        for(int i = glyphCount - 2; i >= 0; i--){
            float x = glyphPositions[i];
            if(width - x > visibleWidth) break;
            maxOffset = x;
        }
        if(-renderOffset > maxOffset) renderOffset = -maxOffset;

        // calculate first visible char based on render offset
        visibleTextStart = 0;
        float startX = 0;
        for(int i = 0; i < glyphCount; i++){
            if(glyphPositions[i] >= -renderOffset){
                visibleTextStart = Math.max(0, i);
                startX = glyphPositions[i];
                break;
            }
        }

        // calculate last visible char based on visible width and render offset
        int length = Math.min(displayText.length(), glyphPositions.length - 1);
        visibleTextEnd = Math.min(length, cursor + 1);
        for(; visibleTextEnd <= length; visibleTextEnd++)
            if(glyphPositions[visibleTextEnd] > startX + visibleWidth) break;
        visibleTextEnd = Math.max(0, visibleTextEnd - 1);

        if((textHAlign & Align.left) == 0){
            textOffset = visibleWidth - (glyphPositions[visibleTextEnd] - startX);
            if((textHAlign & Align.center) != 0) textOffset = Math.round(textOffset * 0.5f);
        }else
            textOffset = startX + renderOffset;

        // calculate selection x position and width
        if(hasSelection){
            int minIndex = Math.min(cursor, selectionStart);
            int maxIndex = Math.max(cursor, selectionStart);
            float minX = Math.max(glyphPositions[minIndex] - glyphPositions[visibleTextStart], -textOffset);
            float maxX = Math.min(glyphPositions[maxIndex] - glyphPositions[visibleTextStart], visibleWidth - textOffset);
            selectionX = minX;
            selectionWidth = maxX - minX - style.font.getData().cursorX;
        }
    }

    private Drawable getBackgroundDrawable(){
        Scene stage = getScene();
        boolean focused = stage != null && stage.getKeyboardFocus() == this;
        return (disabled && style.disabledBackground != null) ? style.disabledBackground
        : (style.invalidBackground != null) && !isValid() ? style.invalidBackground
        : ((focused && style.focusedBackground != null) ? style.focusedBackground : style.background);
    }

    @Override
    public void draw(){
        Scene stage = getScene();
        boolean focused = stage != null && stage.getKeyboardFocus() == this;
        if(!focused) keyRepeatTask.cancel();

        final Font font = style.font;
        final Color fontColor = (disabled && style.disabledFontColor != null) ? style.disabledFontColor
        : ((focused && style.focusedFontColor != null) ? style.focusedFontColor : style.fontColor);
        final Drawable selection = style.selection;
        final Drawable cursorPatch = style.cursor;
        final Drawable background = getBackgroundDrawable();

        Color color = this.color;
        float x = this.x;
        float y = this.y;
        float width = getWidth();
        float height = getHeight();

        Draw.color(color.r, color.g, color.b, color.a * parentAlpha);
        float bgLeftWidth = 0, bgRightWidth = 0;
        if(background != null){
            background.draw(x, y, width, height);
            bgLeftWidth = background.getLeftWidth();
            bgRightWidth = background.getRightWidth();
        }

        float textY = getTextY(font, background);
        calculateOffsets();

        if(focused && hasSelection && selection != null){
            drawSelection(selection, font, x + bgLeftWidth, y + textY);
        }

        float yOffset = font.isFlipped() ? -textHeight : 0;
        if(displayText.length() == 0){
            if(!focused && messageText != null){
                Font messageFont = style.messageFont != null ? style.messageFont : font;
                messageFont.getColor().write(Tmp.c1);

                if(style.messageFontColor != null){
                    messageFont.setColor(style.messageFontColor.r, style.messageFontColor.g, style.messageFontColor.b,
                    style.messageFontColor.a * color.a * parentAlpha);
                }else{
                    messageFont.setColor(0.7f, 0.7f, 0.7f, color.a * parentAlpha);
                }

                boolean had = messageFont.getData().markupEnabled;
                messageFont.getData().markupEnabled = false;
                messageFont.draw(messageText, x + bgLeftWidth, y + textY + yOffset, 0, messageText.length(),
                    width - bgLeftWidth - bgRightWidth, textHAlign, false, "...");
                messageFont.getData().markupEnabled = had;
                messageFont.setColor(Tmp.c1);
            }
        }else{
            font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * color.a * parentAlpha);
            drawText(font, x + bgLeftWidth, y + textY + yOffset);
        }
        if(focused && !disabled){
            blink();
            if(cursorOn && cursorPatch != null){
                drawCursor(cursorPatch, font, x + bgLeftWidth, y + textY);
            }
        }
    }

    public boolean isValid(){
        return validator == null || validator.valid(text);
    }

    protected float getTextY(Font font, Drawable background){
        float height = getHeight();
        float textY = textHeight / 2 + font.getDescent();
        if(background != null){
            float bottom = background.getBottomHeight();
            textY = textY + (height - background.getTopHeight() - bottom) / 2 + bottom;
        }else{
            textY = textY + height / 2;
        }
        if(font.usesIntegerPositions()) textY = (int)textY;
        return textY;
    }

    /** Draws selection rectangle **/
    protected void drawSelection(Drawable selection, Font font, float x, float y){
        selection.draw(x + textOffset + selectionX + fontOffset, y - textHeight - font.getDescent(), selectionWidth, textHeight);
    }

    protected void drawText(Font font, float x, float y){
        boolean had = font.getData().markupEnabled;
        font.getData().markupEnabled = false;
        font.draw(displayText, x + textOffset, y, visibleTextStart, visibleTextEnd, 0, Align.left, false);
        font.getData().markupEnabled = had;
    }

    protected void drawCursor(Drawable cursorPatch, Font font, float x, float y){
        cursorPatch.draw(
        x + textOffset + glyphPositions.get(cursor) - glyphPositions.get(visibleTextStart) + fontOffset + font.getData().cursorX,
        y - textHeight - font.getDescent(), cursorPatch.getMinWidth(), textHeight);
    }

    protected void updateDisplayText(){
        Font font = style.font;
        FontData data = font.getData();
        String text = this.text;
        int textLength = text.length();

        StringBuilder buffer = new StringBuilder();
        for(int i = 0; i < textLength; i++){
            char c = text.charAt(i);
            buffer.append(data.hasGlyph(c) ? c : ' ');
        }
        String newDisplayText = buffer.toString();

        if(passwordMode && data.hasGlyph(passwordCharacter)){
            if(passwordBuffer == null) passwordBuffer = new StringBuilder(newDisplayText.length());
            if(passwordBuffer.length() > textLength)
                passwordBuffer.setLength(textLength);
            else{
                for(int i = passwordBuffer.length(); i < textLength; i++)
                    passwordBuffer.append(passwordCharacter);
            }
            displayText = passwordBuffer;
        }else
            displayText = newDisplayText;

        layout.setText(font, displayText.toString().replace('\n', ' ').replace('\r', ' '));
        glyphPositions.clear();
        float x = 0;
        if(layout.runs.size > 0){
            GlyphRun run = layout.runs.first();
            FloatSeq xAdvances = run.xAdvances;
            fontOffset = xAdvances.first();
            for(int i = 1, n = xAdvances.size; i < n; i++){
                glyphPositions.add(x);
                x += xAdvances.get(i);
            }
        }else{
            fontOffset = 0;
        }
        glyphPositions.add(x);

        visibleTextStart = Math.min(visibleTextStart, glyphPositions.size);
        visibleTextEnd = Mathf.clamp(visibleTextEnd, visibleTextStart, glyphPositions.size);

        if(selectionStart > newDisplayText.length()) selectionStart = textLength;
    }

    private void blink(){
        if(!Core.graphics.isContinuousRendering()){
            cursorOn = true;
            return;
        }
        long time = Time.nanos();
        if((time - lastBlink) / 1000000000.0f > blinkTime){
            cursorOn = !cursorOn;
            lastBlink = time;
        }
    }

    /** Copies the contents of this TextField to the lipboard implementation set on this TextField. */
    public void copy(){
        if(hasSelection && !passwordMode){
            Core.app.setClipboardText(text.substring(Math.min(cursor, selectionStart), Math.max(cursor, selectionStart)));
        }
    }

    /**
     * Copies the selected contents of this TextField to the Clipboard implementation set on this TextField, then removes
     * it.
     */
    public void cut(){
        cut(programmaticChangeEvents);
    }

    void cut(boolean fireChangeEvent){
        if(hasSelection && !passwordMode){
            copy();
            cursor = delete(fireChangeEvent);
            updateDisplayText();
        }
    }

    public void paste(String content, boolean fireChangeEvent){
        if(content == null) return;
        StringBuilder buffer = new StringBuilder();
        int textLength = text.length();
        if(hasSelection) textLength -= Math.abs(cursor - selectionStart);
        FontData data = style.font.getData();
        for(int i = 0, n = content.length(); i < n; i++){
            if(!withinMaxLength(textLength + buffer.length())) break;
            char c = content.charAt(i);
            if(c == '\r') continue;
            if(!(writeEnters && (c == '\n'))){
                if(c == '\n') continue;
                if(onlyFontChars && !data.hasGlyph(c)) continue;
                if(filter != null && !filter.acceptChar(this, c)) continue;
            }
            buffer.append(c);
        }
        content = buffer.toString();

        if(hasSelection) cursor = delete(fireChangeEvent);
        if(fireChangeEvent)
            changeText(text, insert(cursor, content, text));
        else
            text = insert(cursor, content, text);
        updateDisplayText();
        cursor += content.length();
    }

    String insert(int position, CharSequence text, String to){
        if(to.length() == 0) return text.toString();
        return to.substring(0, position) + text + to.substring(position);
    }

    int delete(boolean fireChangeEvent){
        int from = selectionStart;
        int to = cursor;
        int minIndex = Math.min(from, to);
        int maxIndex = Math.max(from, to);
        String newText = (minIndex > 0 ? text.substring(0, minIndex) : "")
        + (maxIndex < text.length() ? text.substring(maxIndex) : "");
        if(fireChangeEvent)
            changeText(text, newText);
        else
            text = newText;
        clearSelection();
        return minIndex;
    }

    /**
     * Focuses the next TextField. If none is found, the keyboard is hidden. Does nothing if the text field is not in a stage.
     * @param up If true, the TextField with the same or next smallest y coordinate is found, else the next highest.
     */
    public void next(boolean up){
        Scene stage = getScene();
        if(stage == null) return;
        TextField current = this;
        while(true){
            current.parent.localToStageCoordinates(tmp1.set(x, y));
            TextField textField = current.findNextTextField(stage.getElements(), null, tmp2, tmp1, up);
            if(textField == null){ // Try to wrap around.
                if(up)
                    tmp1.set(Float.MIN_VALUE, Float.MIN_VALUE);
                else
                    tmp1.set(Float.MAX_VALUE, Float.MAX_VALUE);
                textField = current.findNextTextField(getScene().getElements(), null, tmp2, tmp1, up);
            }
            if(textField == null){
                Core.input.setOnscreenKeyboardVisible(false);
                break;
            }
            if(stage.setKeyboardFocus(textField)) break;
            current = textField;
        }
    }

    private TextField findNextTextField(Seq<Element> elements, TextField best, Vec2 bestCoords, Vec2 currentCoords, boolean up){
        for(int i = 0, n = elements.size; i < n; i++){
            Element element = elements.get(i);
            if(element == this || !element.visible) continue;
            if(element instanceof TextField){
                TextField textField = (TextField)element;
                if(textField.isDisabled() || !textField.focusTraversal) continue;
                Vec2 elementCoords = element.parent.localToStageCoordinates(tmp3.set(element.x, element.y));
                if((elementCoords.y < currentCoords.y || (elementCoords.y == currentCoords.y && elementCoords.x > currentCoords.x)) ^ up){
                    if(best == null
                    || (elementCoords.y > bestCoords.y || (elementCoords.y == bestCoords.y && elementCoords.x < bestCoords.x)) ^ up){
                        best = (TextField)element;
                        bestCoords.set(elementCoords);
                    }
                }
            }else if(element instanceof Group)
                best = findNextTextField(((Group)element).getChildren(), best, bestCoords, currentCoords, up);
        }
        return best;
    }

    public InputListener getDefaultInputListener(){
        return inputListener;
    }

    /** @param listener May be null. */
    public void setTextFieldListener(TextFieldListener listener){
        this.listener = listener;
    }

    public void typed(char ch, Runnable run){
        setTextFieldListener((textField, c) -> {
            if(c == ch){
                run.run();
            }
        });
    }

    public void typed(Cons<Character> cons){
        setTextFieldListener((textField, c) -> cons.get(c));
    }

    public TextFieldFilter getFilter(){
        return filter;
    }

    /** @param filter May be null. */
    public void setFilter(TextFieldFilter filter){
        this.filter = filter;
    }

    public void setValidator(TextFieldValidator validator){
        this.validator = validator;
    }

    public TextFieldValidator getValidator(){
        return validator;
    }

    /** If true (the default), tab/shift+tab will move to the next text field. */
    public void setFocusTraversal(boolean focusTraversal){
        this.focusTraversal = focusTraversal;
    }

    /** @return May be null. */
    public String getMessageText(){
        return messageText;
    }

    /**
     * Sets the text that will be drawn in the text field if no text has been entered.
     * @param messageText may be null.
     */
    public void setMessageText(String messageText){
        if(messageText != null && (messageText.startsWith("$") || messageText.startsWith("@")) && bundle != null && bundle.has(messageText.substring(1))){
            this.messageText = bundle.get(messageText.substring(1));
        }else{
            this.messageText = messageText;
        }
    }

    /** @param str If null, "" is used. */
    public void appendText(String str){
        if(str == null) str = "";

        clearSelection();
        cursor = text.length();
        paste(str, programmaticChangeEvents);
    }

    /** @return Never null, might be an empty string. */
    public String getText(){
        return text;
    }

    /** @param str If null, "" is used. */
    public void setText(String str){
        if(str == null) str = "";
        if(str.equals(text)) return;

        clearSelection();
        String oldText = text;
        text = "";
        paste(str, false);
        if(programmaticChangeEvents) changeText(oldText, text);
        cursor = 0;
    }

    /**
     * @param oldText May be null.
     * @return True if the text was changed.
     */
    boolean changeText(String oldText, String newText){
        if(newText.equals(oldText)) return false;
        text = newText;
        ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class, ChangeEvent::new);
        boolean cancelled = fire(changeEvent);
        text = cancelled ? oldText : newText;
        Pools.free(changeEvent);
        return !cancelled;
    }

    public boolean getProgrammaticChangeEvents(){
        return programmaticChangeEvents;
    }

    /**
     * If false, methods that change the text will not fire {@link ChangeEvent}, the event will be fired only when user changes
     * the text.
     */
    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents){
        this.programmaticChangeEvents = programmaticChangeEvents;
    }

    public int getSelectionStart(){
        return selectionStart;
    }

    public String getSelection(){
        return hasSelection ? text.substring(Math.min(selectionStart, cursor), Math.max(selectionStart, cursor)) : "";
    }

    /** Sets the selected text. */
    public void setSelection(int selectionStart, int selectionEnd){
        if(selectionStart < 0) throw new IllegalArgumentException("selectionStart must be >= 0");
        if(selectionEnd < 0) throw new IllegalArgumentException("selectionEnd must be >= 0");
        selectionStart = Math.min(text.length(), selectionStart);
        selectionEnd = Math.min(text.length(), selectionEnd);
        if(selectionEnd == selectionStart){
            clearSelection();
            return;
        }
        if(selectionEnd < selectionStart){
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        hasSelection = true;
        this.selectionStart = selectionStart;
        cursor = selectionEnd;
    }

    public void selectAll(){
        setSelection(0, text.length());
    }

    public void clearSelection(){
        hasSelection = false;
    }

    public int getCursorPosition(){
        return cursor;
    }

    /** Sets the cursor position and clears any selection. */
    public void setCursorPosition(int cursorPosition){
        if(cursorPosition < 0) throw new IllegalArgumentException("cursorPosition must be >= 0");
        clearSelection();
        cursor = Math.min(cursorPosition, text.length());
    }

    @Override
    public float getPrefWidth(){
        return 150;
    }

    @Override
    public float getPrefHeight(){
        float topAndBottom = 0, minHeight = 0;
        if(style.background != null){
            topAndBottom = Math.max(topAndBottom, style.background.getBottomHeight() + style.background.getTopHeight());
            minHeight = Math.max(minHeight, style.background.getMinHeight());
        }
        if(style.focusedBackground != null){
            topAndBottom = Math.max(topAndBottom,
            style.focusedBackground.getBottomHeight() + style.focusedBackground.getTopHeight());
            minHeight = Math.max(minHeight, style.focusedBackground.getMinHeight());
        }
        if(style.disabledBackground != null){
            topAndBottom = Math.max(topAndBottom,
            style.disabledBackground.getBottomHeight() + style.disabledBackground.getTopHeight());
            minHeight = Math.max(minHeight, style.disabledBackground.getMinHeight());
        }
        return Math.max(topAndBottom + textHeight, minHeight);
    }

    /**
     * Sets text horizontal alignment (left, center or right).
     * @see Align
     */
    public void setAlignment(int alignment){
        this.textHAlign = alignment;
    }

    public boolean isPasswordMode(){
        return passwordMode;
    }

    /**
     * If true, the text in this text field will be shown as bullet characters.
     * @see #setPasswordCharacter(char)
     */
    public void setPasswordMode(boolean passwordMode){
        this.passwordMode = passwordMode;
        updateDisplayText();
    }

    /**
     * Sets the password character for the text field. The character must be present in the {@link Font}. Default is 149
     * (bullet).
     */
    public void setPasswordCharacter(char passwordCharacter){
        this.passwordCharacter = passwordCharacter;
        if(passwordMode) updateDisplayText();
    }

    public void setBlinkTime(float blinkTime){
        this.blinkTime = blinkTime;
    }

    @Override
    public boolean isDisabled(){
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled){
        this.disabled = disabled;
    }

    protected void moveCursor(boolean forward, boolean jump){
        int limit = forward ? text.length() : 0;
        int charOffset = forward ? 0 : -1;
        while((forward ? ++cursor < limit : --cursor > limit) && jump){
            if(!continueCursor(cursor, charOffset)) break;
        }
    }

    protected boolean continueCursor(int index, int offset){
        char c = text.charAt(index + offset);
        return isWordCharacter(c);
    }

    /**
     * Interface for listening to typed characters.
     * @author mzechner
     */
    public interface TextFieldListener{
        void keyTyped(TextField textField, char c);
    }

    /**
     * Interface for filtering characters entered into the text field.
     * @author mzechner
     */
    public interface TextFieldFilter{
        TextFieldFilter digitsOnly = (field, c) -> Character.isDigit(c);
        TextFieldFilter floatsOnly = (field, c) -> Character.isDigit(c) || ((!field.getText().contains(".")) && c == '.');

        boolean acceptChar(TextField textField, char c);
    }

    public interface TextFieldValidator{
        boolean valid(String text);
    }

    /**
     * The style for a text field, see {@link TextField}.
     * @author mzechner
     * @author Nathan Sweet
     */
    public static class TextFieldStyle extends Style{
        public Font font;
        public Color fontColor;
        /** Optional. */
        public Color focusedFontColor, disabledFontColor;
        /** Optional. */
        public Drawable background, focusedBackground, disabledBackground, invalidBackground, cursor, selection;
        /** Optional. */
        public Font messageFont;
        /** Optional. */
        public Color messageFontColor;

        public TextFieldStyle(){
        }

        public TextFieldStyle(TextFieldStyle style){
            this.messageFont = style.messageFont;
            if(style.messageFontColor != null) this.messageFontColor = new Color(style.messageFontColor);
            this.background = style.background;
            this.focusedBackground = style.focusedBackground;
            this.disabledBackground = style.disabledBackground;
            this.cursor = style.cursor;
            this.font = style.font;
            if(style.fontColor != null) this.fontColor = new Color(style.fontColor);
            if(style.focusedFontColor != null) this.focusedFontColor = new Color(style.focusedFontColor);
            if(style.disabledFontColor != null) this.disabledFontColor = new Color(style.disabledFontColor);
            this.selection = style.selection;
        }
    }

    class KeyRepeatTask extends Task{
        KeyCode keycode;

        @Override
        public void run(){
            inputListener.keyDown(null, keycode);
        }
    }

    /** Basic input listener for the text field */
    public class TextFieldClickListener extends ClickListener{
        @Override
        public void clicked(InputEvent event, float x, float y){
            if(imeData != null) return;
            int count = getTapCount() % 4;
            if(count == 0) clearSelection();
            if(count == 2){
                int[] array = wordUnderCursor(x);
                setSelection(array[0], array[1]);
            }
            if(count == 3) selectAll();
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
            if(!super.touchDown(event, x, y, pointer, button)) return false;
            if(pointer == 0 && button != KeyCode.mouseLeft) return false;
            if(disabled || imeData != null) return true;
            setCursorPosition(x, y);
            selectionStart = cursor;
            Scene stage = getScene();
            if(stage != null) stage.setKeyboardFocus(TextField.this);
            if(!hasInputDialog){
                input.setOnscreenKeyboardVisible(true);
            }
            hasSelection = true;
            return true;
        }

        @Override
        public void touchDragged(InputEvent event, float x, float y, int pointer){
            super.touchDragged(event, x, y, pointer);
            setCursorPosition(x, y);
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
            if(selectionStart == cursor) hasSelection = false;
            super.touchUp(event, x, y, pointer, button);
        }

        protected void setCursorPosition(float x, float y){
            lastBlink = 0;
            cursorOn = false;
            cursor = letterUnderCursor(x);
        }

        protected void goHome(boolean jump){
            cursor = 0;
        }

        protected void goEnd(boolean jump){
            cursor = text.length();
        }

        @Override
        public boolean keyDown(InputEvent event, KeyCode keycode){
            if(disabled) return false;
            if(imeData != null) return true;

            lastBlink = 0;
            cursorOn = false;

            Scene stage = getScene();
            if(stage == null || stage.getKeyboardFocus() != TextField.this) return false;

            boolean repeat = false;
            boolean ctrl = Core.input.ctrl() && !Core.input.alt();
            boolean jump = ctrl && !passwordMode;

            if(ctrl){
                if(keycode == KeyCode.v){
                    paste(Core.app.getClipboardText(), true);
                    repeat = true;
                }
                if(keycode == KeyCode.c || keycode == KeyCode.insert){
                    copy();
                    return true;
                }
                if(keycode == KeyCode.x){
                    cut(true);
                    return true;
                }
                if(keycode == KeyCode.a){
                    selectAll();
                    return true;
                }
                if(keycode == KeyCode.z){
                    String oldText = text;
                    setText(undoText);
                    undoText = oldText;
                    updateDisplayText();
                    return true;
                }
            }

            if(Core.input.shift()){
                if(keycode == KeyCode.insert) paste(Core.app.getClipboardText(), true);
                if(keycode == KeyCode.forwardDel) cut(true);
                selection:
                {
                    int temp = cursor;
                    keys:
                    {
                        if(keycode == KeyCode.left){
                            moveCursor(false, jump);
                            repeat = true;
                            break keys;
                        }
                        if(keycode == KeyCode.right){
                            moveCursor(true, jump);
                            repeat = true;
                            break keys;
                        }
                        if(keycode == KeyCode.home){
                            goHome(jump);
                            break keys;
                        }
                        if(keycode == KeyCode.end){
                            goEnd(jump);
                            break keys;
                        }
                        break selection;
                    }
                    if(!hasSelection){
                        selectionStart = temp;
                        hasSelection = true;
                    }
                }
            }else{
                // Cursor movement or other keys (kills selection).
                if(keycode == KeyCode.left){
                    moveCursor(false, jump);
                    clearSelection();
                    repeat = true;
                }
                if(keycode == KeyCode.right){
                    moveCursor(true, jump);
                    clearSelection();
                    repeat = true;
                }
                if(keycode == KeyCode.home){
                    goHome(jump);
                    clearSelection();
                }
                if(keycode == KeyCode.end){
                    goEnd(jump);
                    clearSelection();
                }
            }
            cursor = Mathf.clamp(cursor, 0, text.length());

            if(repeat){
                scheduleKeyRepeatTask(keycode);
            }
            return true;
        }

        protected void scheduleKeyRepeatTask(KeyCode keycode){
            if(!keyRepeatTask.isScheduled() || keyRepeatTask.keycode != keycode){
                keyRepeatTask.keycode = keycode;
                keyRepeatTask.cancel();
                Timer.schedule(keyRepeatTask, keyRepeatInitialTime, keyRepeatTime);
            }
        }

        @Override
        public boolean keyUp(InputEvent event, KeyCode keycode){
            if(disabled) return false;
            if(imeData != null) return true;
            keyRepeatTask.cancel();
            return true;
        }

        protected boolean checkFocusTraverse(char character){
            return focusTraversal && (character == TAB || ((character == '\r' || character == '\n') && Core.app.isMobile()));
        }

        @Override
        public boolean keyTyped(InputEvent event, char character){
            if(disabled) return false;

            // Disallow "typing" most ASCII control characters, which would show up as a space when onlyFontChars is true.
            switch(character){
                case DELETE:
                case BACKSPACE:
                case TAB:
                case '\r':
                case '\n':
                    if(imeData != null) return true;
                    break;
                default:
                    if(character < 32) return false;
            }

            Scene stage = getScene();
            if(stage == null || stage.getKeyboardFocus() != TextField.this) return false;

            if(OS.isMac && Core.input.keyDown(KeyCode.sym)) return true;

            if(checkFocusTraverse(character)){
                next(Core.input.shift());
            }else{
                boolean delete = character == DELETE;
                boolean backspace = character == BACKSPACE;
                boolean enter = character == '\n' || character == '\r';
                boolean add = enter ? writeEnters : (!onlyFontChars || style.font.getData().hasGlyph(character));
                boolean remove = backspace || delete;
                if(add || remove){
                    String oldText = text;
                    int oldCursor = cursor;
                    if(hasSelection)
                        cursor = delete(false);
                    else{
                        if(backspace && cursor > 0){
                            text = text.substring(0, cursor - 1) + text.substring(cursor--);
                            renderOffset = 0;
                        }
                        if(delete && cursor < text.length()){
                            text = text.substring(0, cursor) + text.substring(cursor + 1);
                        }
                    }
                    if(add && !remove){
                        // Character may be added to the text.
                        if(filter != null && !filter.acceptChar(TextField.this, character)) return true;
                        if(!withinMaxLength(text.length())) return true;
                        String insertion = enter ? "\n" : String.valueOf(character);
                        text = insert(cursor++, insertion, text);
                    }
                    if(changeText(oldText, text)){
                        long time = System.currentTimeMillis();
                        if(time - 750 > lastChangeTime) undoText = oldText;
                        lastChangeTime = time;
                    }else
                        cursor = oldCursor;
                    updateDisplayText();
                }
            }
            if(listener != null) listener.keyTyped(TextField.this, character);
            return true;
        }
    }
}
