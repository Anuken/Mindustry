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
public class Styles{
    public static Drawable
    black = whiteui.tint(0f, 0f, 0f, 1f),
    black9 = whiteui.tint(0f, 0f, 0f, 0.9f),
    black8 = whiteui.tint(0f, 0f, 0f, 0.8f),
    black6 = whiteui.tint(0f, 0f, 0f, 0.6f),
    black3 = whiteui.tint(0f, 0f, 0f, 0.3f),
    none = whiteui.tint(0f, 0f, 0f, 0f),
    flatDown = createFlatDown(),
    flatOver = whiteui.tint(Color.valueOf("454545"));

    public static ButtonStyle
    defaultb = new ButtonStyle(){{
        down = buttonDown;
        up = button;
        over = buttonOver;
        disabled = buttonDisabled;
    }},
    waveb = new ButtonStyle(){{
        up = buttonEdge4;
        over = buttonEdgeOver4;
        disabled = buttonEdge4;
    }};

    public static TextButtonStyle
    defaultt = new TextButtonStyle(){{
        over = buttonOver;
        disabled = buttonDisabled;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        down = buttonDown;
        up = button;
    }},
    squaret = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        over = buttonSquareOver;
        disabled = buttonDisabled;
        down = buttonSquareDown;
        up = buttonSquare;
    }},
    nodet = new TextButtonStyle(){{
        disabled = button;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        up = buttonOver;
        over = buttonDown;
    }},
    cleart = new TextButtonStyle(){{
        over = flatOver;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
        down = flatOver;
        up = black;
    }},
    discordt = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        up = discordBanner;
    }},
    infot = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        up = infoBanner;
    }},
    clearPartialt = new TextButtonStyle(){{
        down = whiteui;
        up = pane;
        over = flatDown;
        font = Fonts.def;
        fontColor = Color.WHITE;
        disabledFontColor = Color.GRAY;
    }},
    clearTogglet = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        checked = flatDown;
        down = flatDown;
        up = black;
        over = flatOver;
        disabled = black;
        disabledFontColor = Color.GRAY;
    }},
    clearToggleMenut = new TextButtonStyle(){{
        font = Fonts.def;
        fontColor = Color.WHITE;
        checked = flatDown;
        down = flatDown;
        up = clear;
        over = flatOver;
        disabled = black;
        disabledFontColor = Color.GRAY;
    }},
    togglet = new TextButtonStyle(){{
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
    defaulti = new ImageButtonStyle(){{
        down = buttonDown;
        up = button;
        over = buttonOver;
        imageDisabledColor = Color.GRAY;
        imageUpColor = Color.WHITE;
        disabled = buttonDisabled;
    }},
    nodei = new ImageButtonStyle(){{
        up = buttonOver;
        over = buttonDown;
    }},
    righti = new ImageButtonStyle(){{
        over = buttonRightOver;
        down = buttonRightDown;
        up = buttonRight;
    }},
    emptyi = new ImageButtonStyle(){{
        imageDownColor = Pal.accent;
        imageUpColor = Color.WHITE;
    }},
    emptytogglei = new ImageButtonStyle(){{
        imageCheckedColor = Color.WHITE;
        imageDownColor = Color.WHITE;
        imageUpColor = Color.GRAY;
    }},
    selecti = new ImageButtonStyle(){{
        checked = buttonSelect;
        up = none;
    }},
    cleari = new ImageButtonStyle(){{
        down = flatOver;
        up = black;
        over = flatOver;
    }},
    clearFulli = new ImageButtonStyle(){{
        down = whiteui;
        up = pane;
        over = flatDown;
    }},
    clearPartiali = new ImageButtonStyle(){{
        down = flatDown;
        up = none;
        over = flatOver;
    }},
    clearTogglei = new ImageButtonStyle(){{
        down = flatDown;
        checked = flatDown;
        up = black;
        over = flatOver;
    }},
    clearTransi = new ImageButtonStyle(){{
        down = flatDown;
        up = black6;
        over = flatOver;
    }},
    clearToggleTransi = new ImageButtonStyle(){{
        down = flatDown;
        checked = flatDown;
        up = black6;
        over = flatOver;
    }},
    clearTogglePartiali = new ImageButtonStyle(){{
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
    defaultSlider = new SliderStyle(){{
        background = slider;
        knob = sliderKnob;
        knobOver = sliderKnobOver;
        knobDown = sliderKnobDown;
    }},
    vSlider = new SliderStyle(){{
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
    areaField = new TextFieldStyle(){{
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
    defaultDialog = new DialogStyle(){{
        stageBackground = black9;
        titleFont = Fonts.def;
        background = windowEmpty;
        titleFontColor = Pal.accent;
    }},
    fullDialog = new DialogStyle(){{
        stageBackground = black;
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
