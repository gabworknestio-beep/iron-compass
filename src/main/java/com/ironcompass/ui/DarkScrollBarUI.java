package com.ironcompass.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Compact RuneLite-dark scrollbar without native Windows arrow buttons. */
final class DarkScrollBarUI extends BasicScrollBarUI
{
    @Override
    protected void configureScrollBarColors()
    {
        trackColor = UiTokens.BACKGROUND;
        thumbColor = UiTokens.BORDER_STRONG;
        thumbDarkShadowColor = UiTokens.BORDER_STRONG;
        thumbHighlightColor = UiTokens.SURFACE_HOVER;
        thumbLightShadowColor = UiTokens.BORDER_SUBTLE;
    }

    @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
    @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds)
    {
        graphics.setColor(UiTokens.BACKGROUND);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds)
    {
        if (!component.isEnabled() || bounds.isEmpty()) return;
        Color color = isDragging ? UiTokens.TEXT_MUTED
            : isThumbRollover() ? UiTokens.SURFACE_HOVER.brighter() : UiTokens.BORDER_STRONG;
        graphics.setColor(color);
        graphics.fillRoundRect(bounds.x + 1, bounds.y + 2, Math.max(3, bounds.width - 2),
            Math.max(6, bounds.height - 4), 5, 5);
    }

    private static JButton zeroButton()
    {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setMinimumSize(zero);
        button.setPreferredSize(zero);
        button.setMaximumSize(zero);
        button.setBorder(null);
        return button;
    }
}
