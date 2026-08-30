package com.ironcompass.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

/** Lightweight flat button painter with explicit hover, pressed, focus, and selected states. */
final class PremiumButtonUI extends BasicButtonUI
{
    static final String STYLE_PROPERTY = "ironcompass.buttonStyle";
    static final String SELECTED_PROPERTY = "ironcompass.selected";

    @Override
    public void paint(Graphics graphics, JComponent component)
    {
        AbstractButton button = (AbstractButton) component;
        UiComponents.ButtonStyle style = style(button);
        boolean selected = Boolean.TRUE.equals(button.getClientProperty(SELECTED_PROPERTY));
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background = background(button, style, selected);
        if (background != null)
        {
            g.setColor(background);
            g.fillRect(0, 0, button.getWidth(), button.getHeight());
        }
        Color border = border(style, selected);
        if (border != null)
        {
            g.setColor(border);
            g.drawRect(0, 0, Math.max(0, button.getWidth() - 1), Math.max(0, button.getHeight() - 1));
        }
        if (style == UiComponents.ButtonStyle.NAVIGATION && selected)
        {
            g.setColor(UiTokens.ACCENT);
            g.fillRect(1, Math.max(0, button.getHeight() - 2), Math.max(0, button.getWidth() - 2), 2);
        }
        if (button.isFocusPainted() && button.hasFocus())
        {
            g.setColor(UiTokens.ACCENT_HOVER);
            g.drawRect(2, 2, Math.max(0, button.getWidth() - 5), Math.max(0, button.getHeight() - 5));
        }
        g.dispose();
        button.setForeground(foreground(style, selected));
        super.paint(graphics, component);
    }

    private static UiComponents.ButtonStyle style(AbstractButton button)
    {
        Object value = button.getClientProperty(STYLE_PROPERTY);
        return value instanceof UiComponents.ButtonStyle
            ? (UiComponents.ButtonStyle) value : UiComponents.ButtonStyle.SECONDARY;
    }

    private static Color background(AbstractButton button, UiComponents.ButtonStyle style, boolean selected)
    {
        if (button.getModel().isPressed()) return UiTokens.BACKGROUND;
        if (button.getModel().isRollover())
            return style == UiComponents.ButtonStyle.PRIMARY ? UiTokens.ACCENT_DARK.brighter()
                : UiTokens.SURFACE_HOVER;
        switch (style)
        {
            case PRIMARY: return UiTokens.ACCENT_DARK;
            case DANGER: return new Color(67, 38, 37);
            case GHOST: return null;
            case NAVIGATION: return selected ? UiTokens.SURFACE_SELECTED : null;
            default: return UiTokens.SURFACE_RAISED;
        }
    }

    private static Color border(UiComponents.ButtonStyle style, boolean selected)
    {
        if (style == UiComponents.ButtonStyle.GHOST) return null;
        if (style == UiComponents.ButtonStyle.PRIMARY || selected) return UiTokens.ACCENT_DARK.brighter();
        if (style == UiComponents.ButtonStyle.DANGER) return UiTokens.DANGER.darker();
        return UiTokens.BORDER_STRONG;
    }

    private static Color foreground(UiComponents.ButtonStyle style, boolean selected)
    {
        if (style == UiComponents.ButtonStyle.DANGER) return UiTokens.DANGER.brighter();
        if (style == UiComponents.ButtonStyle.PRIMARY || selected) return UiTokens.ACCENT_HOVER;
        if (style == UiComponents.ButtonStyle.GHOST) return UiTokens.TEXT_SECONDARY;
        return UiTokens.TEXT_PRIMARY;
    }
}
