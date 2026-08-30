package com.ironcompass.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Shared visual language for the compact 242 px RuneLite sidebar. */
final class UiTokens
{
    static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    static final Color SURFACE = ColorScheme.DARKER_GRAY_COLOR;
    static final Color SURFACE_RAISED = new Color(35, 35, 35);
    static final Color SURFACE_HOVER = new Color(47, 47, 47);
    static final Color SURFACE_SELECTED = new Color(48, 44, 34);
    static final Color BORDER_SUBTLE = new Color(52, 52, 52);
    static final Color BORDER_STRONG = new Color(73, 73, 73);
    static final Color TEXT_PRIMARY = new Color(224, 224, 220);
    static final Color TEXT_SECONDARY = new Color(184, 184, 180);
    static final Color TEXT_MUTED = new Color(139, 139, 136);
    static final Color ACCENT = new Color(214, 183, 101);
    static final Color ACCENT_HOVER = new Color(229, 201, 126);
    static final Color ACCENT_DARK = new Color(82, 69, 38);
    static final Color SUCCESS = new Color(108, 187, 122);
    static final Color WARNING = new Color(205, 166, 86);
    static final Color DANGER = new Color(207, 102, 93);
    static final Color UNKNOWN = WARNING;

    // Compatibility names retained while the views migrate to semantic tokens.
    static final Color CARD = SURFACE;
    static final Color BORDER = BORDER_SUBTLE;
    static final Color TEXT = TEXT_PRIMARY;
    static final Color MUTED = TEXT_MUTED;

    static final Font APP_TITLE = FontManager.getRunescapeBoldFont().deriveFont(16f);
    static final Font CARD_TITLE = FontManager.getDefaultBoldFont().deriveFont(12f);
    static final Font BODY = FontManager.getDefaultFont().deriveFont(11f);
    static final Font META = FontManager.getDefaultFont().deriveFont(10f);
    static final Font EYEBROW = FontManager.getDefaultBoldFont().deriveFont(9f);
    static final Font SMALL = FontManager.getDefaultFont().deriveFont(9f);
    static final Font TITLE = APP_TITLE;
    static final Font STEP_TITLE = CARD_TITLE;
    static final Font LABEL = EYEBROW;

    static final int XS = 3;
    static final int SM = 5;
    static final int MD = 8;
    static final int LG = 12;
    static final int XL = 16;
    static final int CONTROL_HEIGHT = 26;
    static final int SCROLLBAR_WIDTH = 7;

    private UiTokens()
    {
    }

    static Border cardBorder()
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE),
            BorderFactory.createEmptyBorder(MD, MD, MD, MD));
    }
}
