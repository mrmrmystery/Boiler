/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Assets {

    public static BufferedImage SETTINGS_BG;
    public static BufferedImage SETTINGS_ON;
    public static BufferedImage SETTINGS_OFF;

    public static Font MINECRAFTIA;

    public static void load() {
        try {
            SETTINGS_BG = ImageIO.read(Assets.class.getResourceAsStream("/assets/boiler_settings.png"));
            SETTINGS_ON = ImageIO.read(Assets.class.getResourceAsStream("/assets/boiler_settings_on.png"));
            SETTINGS_OFF = ImageIO.read(Assets.class.getResourceAsStream("/assets/boiler_settings_off.png"));

            Font font = Font.createFont(Font.TRUETYPE_FONT, Assets.class.getResource("/assets/Minecraftia.ttf").openStream()).deriveFont(8F);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            MINECRAFTIA = font;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
