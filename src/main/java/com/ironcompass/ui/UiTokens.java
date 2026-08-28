package com.ironcompass.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;

final class UiTokens
{
    static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
    static final Color BORDER = new Color(58, 58, 58);
    static final Color TEXT = new Color(215, 215, 215);
    static final Color MUTED = new Color(155, 155, 155);
    static final Color ACCENT = new Color(214, 183, 101);
    static final Color SUCCESS = new Color(104, 190, 120);
    static final Color DANGER = new Color(215, 100, 92);
    static final Color UNKNOWN = new Color(190, 164, 105);
    static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    static final Font STEP_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private UiTokens()
    {
    }

    static Border cardBorder()
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(9, 9, 9, 9));
    }
}
