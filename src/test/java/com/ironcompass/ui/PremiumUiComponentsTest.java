package com.ironcompass.ui;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class PremiumUiComponentsTest
{
    @Test
    public void premiumButtonsShareOneFocusableDarkStyle() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            JButton primary = UiComponents.primaryButton("CONTINUE");
            JButton navigation = UiComponents.button("OVERVIEW", UiComponents.ButtonStyle.NAVIGATION);
            assertTrue(primary.getUI() instanceof PremiumButtonUI);
            assertTrue(navigation.getUI() instanceof PremiumButtonUI);
            assertTrue(primary.isFocusable());
            assertTrue(primary.isFocusPainted());
            assertTrue(primary.isRolloverEnabled());
            assertEquals(UiComponents.ButtonStyle.PRIMARY,
                primary.getClientProperty(PremiumButtonUI.STYLE_PROPERTY));
            assertFalse("Premium controls must not expose the native white content area",
                primary.isContentAreaFilled());
            assertFalse(Color.WHITE.equals(primary.getBackground()));
        });
    }

    @Test
    public void sharedScrollPaneUsesDarkCompactScrollbarAndNeverScrollsHorizontally() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            JScrollPane scroll = UiComponents.scrollPane(new JPanel());
            assertTrue(scroll.getVerticalScrollBar().getUI() instanceof DarkScrollBarUI);
            assertTrue(scroll.getHorizontalScrollBar().getUI() instanceof DarkScrollBarUI);
            assertEquals(UiTokens.SCROLLBAR_WIDTH, scroll.getVerticalScrollBar().getPreferredSize().width);
            assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
                scroll.getHorizontalScrollBarPolicy());
        });
    }

    @Test
    public void wrappingTextUsesTheAvailableCardWidthInsteadOfLegacy155Pixels() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            JPanel parent = new JPanel();
            parent.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 8, 0, 8));
            parent.setSize(new Dimension(220, 200));
            WrappingText copy = UiComponents.labelHtml(
                "A longer explanation that should use the useful width of a RuneLite sidebar card.",
                UiTokens.TEXT_PRIMARY);
            parent.add(copy);
            assertEquals(204, copy.getPreferredSize().width);
            assertTrue(copy.getPreferredSize().width > 155);
            assertEquals(Integer.MAX_VALUE, copy.getMaximumSize().width);
            assertTrue(copy.getAccessibleContext().getAccessibleName().startsWith("A longer explanation"));
        });
    }

    @Test
    public void semanticCardStyleIsRetainedForStructuralQa() throws Exception
    {
        SwingUtilities.invokeAndWait(() ->
        {
            JPanel card = UiComponents.card(new JPanel(), UiComponents.CardStyle.HERO);
            assertSame(UiComponents.CardStyle.HERO, card.getClientProperty("ironcompass.cardStyle"));
            assertEquals(UiTokens.SURFACE_RAISED, card.getBackground());
        });
    }
}
