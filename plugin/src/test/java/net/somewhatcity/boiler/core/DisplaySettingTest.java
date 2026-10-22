package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.core.smarttv.DisplaySetting;
import net.somewhatcity.boiler.core.smarttv.SmartTvMenu;
import net.somewhatcity.boiler.core.smarttv.SmartTvSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DisplaySettingTest {
    private SourceTestEnvironment env;
    @BeforeEach void setup() throws Exception { env = new SourceTestEnvironment(); }
    @AfterEach void teardown() throws Exception { env.close(); }

    @Test void allCommandOptionsCanBeChangedAndUnknownPropertiesSurvive() {
        env.display.settings().addProperty("extensionSetting", "preserved");
        for (DisplaySetting option : DisplaySetting.values()) {
            String value = switch (option.kind) {
                case TOGGLE -> "true";
                case ANGLE -> "12.5";
                case DIRECTION -> "WEST";
                case INTEGER -> option == DisplaySetting.ITEM_ROTATION ? "7" : "42";
            };
            option.apply(env.display, value);
            assertEquals(value, option.raw(env.display));
            assertFalse(option.value(env.display).text().isBlank());
        }
        assertEquals("preserved", env.display.settings().get("extensionSetting").getAsString());
        verify(env.display, times(DisplaySetting.values().length)).settings(any(JsonObject.class));
        verify(env.display, never()).saveSourceData(any());
        verify(env.display, never()).source(anyString(), any());
    }

    @Test void validatesBeforeApplyingAnythingAndClampsArrowAdjustments() {
        JsonObject before = env.display.settings().deepCopy();
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.RENDER_PERIOD.apply(env.display, "0"));
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.VIEW_DISTANCE.apply(env.display, "-1"));
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.SOUND_DISTANCE.apply(env.display, "2147483648"));
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.ITEM_ROTATION.apply(env.display, "8"));
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.YAW.apply(env.display, "NaN"));
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.PITCH.apply(env.display, "Infinity"));
        assertThrows(IllegalArgumentException.class, () -> DisplaySetting.DIRECTION.apply(env.display, "INVALID"));
        assertEquals(before, env.display.settings());
        verify(env.display, never()).settings(any(JsonObject.class));
        DisplaySetting.RENDER_PERIOD.apply(env.display, "1");
        DisplaySetting.RENDER_PERIOD.adjust(env.display, -1);
        assertEquals("1", DisplaySetting.RENDER_PERIOD.raw(env.display));
        DisplaySetting.VIEW_DISTANCE.apply(env.display, Integer.toString(Integer.MAX_VALUE));
        DisplaySetting.VIEW_DISTANCE.adjust(env.display, 1);
        assertEquals(Integer.toString(Integer.MAX_VALUE), DisplaySetting.VIEW_DISTANCE.raw(env.display));
    }

    @Test void rotationAlwaysContainsBothAnglesAndResetRestoresUsableDefaults() {
        DisplaySetting.YAW.apply(env.display, "45");
        assertEquals(0, env.display.settings().getAsJsonObject("rotation").get("pitch").getAsInt());
        DisplaySetting.PITCH.apply(env.display, "-30");
        DisplaySetting.YAW.reset(env.display);
        assertEquals(0, env.display.settings().getAsJsonObject("rotation").get("yaw").getAsInt());
        assertEquals(-30, env.display.settings().getAsJsonObject("rotation").get("pitch").getAsInt());
        DisplaySetting.BUFFER.reset(env.display);
        assertTrue(env.display.settings().get("buffer").getAsBoolean());
        DisplaySetting.ITEM_ROTATION.reset(env.display);
        assertEquals("0", DisplaySetting.ITEM_ROTATION.raw(env.display));
        DisplaySetting.DIRECTION.reset(env.display);
        assertEquals("NORTH", DisplaySetting.DIRECTION.raw(env.display));
    }

    @Test void everySettingsRowAndCategoryIsReachableAtSmallLandscapeAndPortraitSizes() {
        for (int[] size : new int[][]{{128,128}, {384,256}, {256,384}, {768,512}}) {
            for (DisplaySetting option : DisplaySetting.values()) {
                SmartTvSettings.Layout layout = new SmartTvSettings.Layout(size[0], size[1], option.ordinal());
                Rectangle row = layout.row(option.ordinal());
                assertTrue(new Rectangle(0, 0, size[0], size[1]).contains(row));
                assertEquals(option.ordinal(), layout.hit(row.x + 1, row.y + 1));
                assertFalse(row.intersects(layout.back()));
            }
        }
        assertFalse(SmartTvMenu.layout(384,128).large());
        assertFalse(SmartTvMenu.layout(256,256).large());
        assertTrue(SmartTvMenu.layout(384,256).large());
        assertTrue(SmartTvMenu.layout(256,384).large());
    }
}
