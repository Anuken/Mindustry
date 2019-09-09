package io.anuke.mindustry.ui;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.Button.*;
import io.anuke.arc.scene.ui.CheckBox.*;
import io.anuke.arc.scene.ui.Dialog.*;
import io.anuke.arc.scene.ui.ImageButton.*;
import io.anuke.arc.scene.ui.KeybindDialog.*;
import io.anuke.arc.scene.ui.Label.*;
import io.anuke.arc.scene.ui.ScrollPane.*;
import io.anuke.arc.scene.ui.Slider.*;
import io.anuke.arc.scene.ui.TextButton.*;
import io.anuke.arc.scene.ui.TextField.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;

import static io.anuke.mindustry.gen.Tex.*;

@StyleDefaults
public class Style{
    public static Drawable 
    dialogDim = whiteui.tint(0f, 0f, 0f, 0.9f),
    loadDim = whiteui.tint(0f, 0f, 0f, 0.8f),
    guideDim = whiteui.tint(0f, 0f, 0f, 0.3f),
    chatfield = whiteui.tint(0f, 0f, 0f, 0.2f),
    dark = whiteui.tint(0f, 0f, 0f, 1f),
    none = whiteui.tint(0f, 0f, 0f, 0.1f),
    flatTrans = whiteui.tint(0f, 0f, 0f, 0.6f),
    flat = whiteui.tint(0f, 0f, 0f, 1f),
    flatDown = createFlatDown(),
    flatOver = whiteui.tint(Color.valueOf("454545"));

    public static ButtonStyle
    defaultButton = new ButtonStyle(){{
        down = buttonDown;
        up = button;
        over = buttonOver;
        disabled = buttonDisabled;
    }},
    squareButton = new ButtonStyle(){{
        over = buttonSquareOver;
        disabled = buttonDisabled;
        down = buttonSquareDown;
        up = buttonSquare;
    }},
    toggleButton = new ButtonStyle(){{
        checked = buttonDown;
        down = buttonDown;
        up = button;
    }},
    waveButton = new ButtonStyle(){{
        up = buttonEdge4;
        over = buttonEdgeOver4;
        disabled = buttonEdge4;
    }};

    public static TextButtonStyle
    defaultTbutton = new TextButtonStyle(){{
        over = buttonOver;
        disabled = buttonDisabled;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        down = buttonDown;
        up = button;
    }},
    squareTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        over = buttonSquareOver;
        disabled = buttonDisabled;
        down = buttonSquareDown;
        up = buttonSquare;
    }},
    nodeTbutton = new TextButtonStyle(){{
        disabled = button;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        up = buttonOver;
        over = buttonDown;
    }},
    rightTbutton = new TextButtonStyle(){{
        over = buttonRightOver;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        down = buttonRightDown;
        up = buttonRight;
    }},
    waveTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        up = buttonEdge4;
    }},
    clearTbutton = new TextButtonStyle(){{
        over = flatOver;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        down = flatOver;
        up = flat;
    }},
    discordTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        up = discordBanner;
    }},
    infoTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        up = infoBanner;
    }},
    clearPartialTbutton = new TextButtonStyle(){{
        down = whiteui;
        up = pane;
        over = flatDown;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
    }},
    clearPartial2Tbutton = new TextButtonStyle(){{
        down = flatOver;
        up = none;
        over = flatOver;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
    }},
    emptyTbutton = new TextButtonStyle(){{
        font = Fonts.def;
    }},
    clearToggleTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        checked = flatDown;
        down = flatDown;
        up = flat;
        over = flatOver;
        disabled = flat;
        disabledFontColor = Color.GRAY;
    }},
    clearToggleMenuTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        checked = flatDown;
        down = flatDown;
        up = clear;
        over = flatOver;
        disabled = flat;
        disabledFontColor = Color.GRAY;
    }},
    toggleTbutton = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        checked = buttonDown;
        down = buttonDown;
        up = button;
        over = buttonOver;
        disabled = buttonDisabled;
        disabledFontColor = Color.GRAY;
    }};

    public static ImageButtonStyle
    defaultIbutton = new ImageButtonStyle(){{
        down = buttonDown;
        up = button;
        over = buttonOver;
        imageDisabledColor = Color.GRAY;
        imageUpColor = Color.WHITE;
        disabled = buttonDisabled;
    }},
    nodeIbutton = new ImageButtonStyle(){{
        up = buttonOver;
        over = buttonDown;
    }},
    rightIbutton = new ImageButtonStyle(){{
        over = buttonRightOver;
        down = buttonRightDown;
        up = buttonRight;
    }},
    emptyIbutton = new ImageButtonStyle(){{
        imageDownColor = Pal.accent;
        imageUpColor = Color.WHITE;
    }},
    emptytoggleIbutton = new ImageButtonStyle(){{
        imageCheckedColor = Color.WHITE;
        imageDownColor = Color.WHITE;
        imageUpColor = Color.GRAY;
    }},
    staticIbutton = new ImageButtonStyle(){{
        up = button;
    }},
    staticDownIbutton = new ImageButtonStyle(){{
        up = buttonDown;
    }},
    toggleIbutton = new ImageButtonStyle(){{
        checked = buttonDown;
        down = buttonDown;
        up = button;
        imageDisabledColor = Color.GRAY;
        imageUpColor = Color.WHITE;
    }},
    selectIbutton = new ImageButtonStyle(){{
        checked = buttonSelect;
        up = none;
    }},
    clearIbutton = new ImageButtonStyle(){{
        down = flatOver;
        up = flat;
        over = flatOver;
    }},
    clearFullIbutton = new ImageButtonStyle(){{
        down = whiteui;
        up = pane;
        over = flatDown;
    }},
    clearPartialIbutton = new ImageButtonStyle(){{
        down = flatDown;
        up = none;
        over = flatOver;
    }},
    clearToggleIbutton = new ImageButtonStyle(){{
        down = flatDown;
        checked = flatDown;
        up = flat;
        over = flatOver;
    }},
    clearTransIbutton = new ImageButtonStyle(){{
        down = flatDown;
        up = flatTrans;
        over = flatOver;
    }},
    clearToggleTransIbutton = new ImageButtonStyle(){{
        down = flatDown;
        checked = flatDown;
        up = flatTrans;
        over = flatOver;
    }},
    clearTogglePartialIbutton = new ImageButtonStyle(){{
        down = flatDown;
        checked = flatDown;
        up = none;
        over = flatOver;
    }};

    public static ScrollPaneStyle
    defaultPane = new ScrollPaneStyle(){{
        vScroll = scroll;
        vScrollKnob = scrollKnobVerticalBlack;
    }},
    horizontalPane = new ScrollPaneStyle(){{
        vScroll = scroll;
        vScrollKnob = scrollKnobVerticalBlack;
        hScroll = scrollHorizontal;
        hScrollKnob = scrollKnobHorizontalBlack;
    }};
    
    public static KeybindDialogStyle
    defaultKeybindDialog = new KeybindDialogStyle(){{
        keyColor = Pal.accent;
        keyNameColor = Color.WHITE;
        controllerColor = Color.LIGHT_GRAY;
    }};
    
    public static SliderStyle
    defaultHorizontalSlider = new SliderStyle(){{
        background = slider;
        knob = sliderKnob;
        knobOver = sliderKnobOver;
        knobDown = sliderKnobDown;
    }},
    defaultVerticalSlider = new SliderStyle(){{
        background = sliderVertical;
        knob = sliderKnob;
        knobOver = sliderKnobOver;
        knobDown = sliderKnobDown;
    }};
    
    public static LabelStyle
    defaultLabel = new LabelStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
    }},
    outlineLabel = new LabelStyle(){{
        font = Fonts.outline;
        fontColor = Color.WHITE;
    }};
    
    public static TextFieldStyle
    defaultField = new TextFieldStyle(){{
        font = Fonts.chat;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        disabledBackground = underlineDisabled;
        selection = Tex.selection;
        background = underline;
        invalidBackground = underlineRed;
        cursor = Tex.cursor;
        messageFont = Fonts.def;
        messageFontColor = Color.GRAY;
    }},
    textareaField = new TextFieldStyle(){{
        font = Fonts.chat;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        selection = Tex.selection;
        background = underline;
        cursor = Tex.cursor;
        messageFont = Fonts.def;
        messageFontColor = Color.GRAY;
    }};
    public static CheckBoxStyle
    defaultCheck = new CheckBoxStyle(){{
        checkboxOn = checkOn;
        checkboxOff = checkOff;
        checkboxOnOver = checkOnOver;
        checkboxOver = checkOver;
        checkboxOnDisabled = checkOnDisabled;
        checkboxOffDisabled = checkDisabled;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
    }};
    public static DialogStyle
    defaultWindow = new DialogStyle(){{
        stageBackground = dialogDim;
        titleFont = Fonts.def;
        background = windowEmpty;
        titleFontColor = Pal.accent;
    }},
    fulldialogWindow = new DialogStyle(){{
        stageBackground = dark;
        titleFont = Fonts.def;
        background = windowEmpty;
        titleFontColor = Pal.accent;
    }};


    private static Drawable createFlatDown(){
        AtlasRegion region = Core.atlas.find("flat-down-base");
        int[] splits = region.splits;

        ScaledNinePatchDrawable copy = new ScaledNinePatchDrawable(new NinePatch(region, splits[0], splits[1], splits[2], splits[3])){
            public float getLeftWidth(){ return 0; }
            public float getRightWidth(){ return 0; }
            public float getTopHeight(){ return 0; }
            public float getBottomHeight(){ return 0; }
        };
        copy.setMinWidth(0);
        copy.setMinHeight(0);
        copy.setTopHeight(0);
        copy.setRightWidth(0);
        copy.setBottomHeight(0);
        copy.setLeftWidth(0);
        return copy;
    }
}
