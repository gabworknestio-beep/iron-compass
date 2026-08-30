package com.ironcompass.ui;

import com.ironcompass.requirement.TruthValue;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

/** Small shared UI kit used by every Iron Compass sidebar view and dialog. */
final class UiComponents
{
    enum ButtonStyle { PRIMARY, SECONDARY, GHOST, DANGER, NAVIGATION, ICON }
    enum CardStyle { STANDARD, HERO, SUBTLE, WARNING, SUCCESS }

    private static final ImageIcon COMPASS_ICON = loadCompassIcon();

    private UiComponents()
    {
    }

    static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    static JPanel scrollableVerticalPanel()
    {
        return new WidthTrackingPanel();
    }

    static JPanel card(Component content)
    {
        return card(content, CardStyle.STANDARD);
    }

    static JPanel card(Component content, CardStyle style)
    {
        JPanel panel = new JPanel(new BorderLayout())
        {
            @Override
            public Dimension getMaximumSize()
            {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        Color background = style == CardStyle.HERO ? UiTokens.SURFACE_RAISED : UiTokens.SURFACE;
        panel.setBackground(background);
        if (content instanceof JComponent) ((JComponent) content).setOpaque(false);
        Color accent = style == CardStyle.WARNING ? UiTokens.WARNING
            : style == CardStyle.SUCCESS ? UiTokens.SUCCESS : UiTokens.ACCENT;
        javax.swing.border.Border edge = style == CardStyle.HERO || style == CardStyle.WARNING
            || style == CardStyle.SUCCESS
            ? BorderFactory.createMatteBorder(0, 2, 0, 0, accent)
            : BorderFactory.createLineBorder(UiTokens.BORDER_SUBTLE);
        panel.setBorder(BorderFactory.createCompoundBorder(edge,
            BorderFactory.createEmptyBorder(UiTokens.MD, UiTokens.MD, UiTokens.MD, UiTokens.MD)));
        panel.add(content, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.putClientProperty("ironcompass.cardStyle", style);
        return panel;
    }

    static JLabel sectionLabel(String text)
    {
        JLabel label = new JLabel(text.toUpperCase(java.util.Locale.ENGLISH));
        label.setForeground(UiTokens.TEXT_MUTED);
        label.setFont(UiTokens.EYEBROW);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    static JLabel cardTitle(String text, Color color)
    {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(UiTokens.CARD_TITLE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    static WrappingText labelHtml(String body, Color color)
    {
        return new WrappingText(body, color, UiTokens.BODY);
    }

    static JPanel statusLine(TruthValue value, String text)
    {
        String glyph = value == TruthValue.TRUE ? "✓" : value == TruthValue.FALSE ? "×" : "?";
        Color color = value == TruthValue.TRUE ? UiTokens.SUCCESS
            : value == TruthValue.FALSE ? UiTokens.DANGER : UiTokens.UNKNOWN;
        JPanel row = new JPanel(new BorderLayout(UiTokens.SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel marker = new JLabel(glyph);
        marker.setForeground(color);
        marker.setFont(UiTokens.META.deriveFont(java.awt.Font.BOLD));
        marker.setVerticalAlignment(SwingConstants.TOP);
        WrappingText copy = labelHtml(text, UiTokens.TEXT_SECONDARY);
        row.add(marker, BorderLayout.WEST);
        row.add(copy, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.putClientProperty("ironcompass.truthValue", value);
        return row;
    }

    static JLabel badge(String text, Color color)
    {
        JLabel badge = new JLabel(text.toUpperCase(java.util.Locale.ENGLISH));
        badge.setForeground(color);
        badge.setFont(UiTokens.EYEBROW);
        badge.setOpaque(true);
        badge.setBackground(UiTokens.SURFACE_RAISED);
        badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color.darker()),
            BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        return badge;
    }

    static JButton button(String text, ButtonStyle style)
    {
        JButton button = new JButton(text);
        button.putClientProperty(PremiumButtonUI.STYLE_PROPERTY, style);
        button.setUI(new PremiumButtonUI());
        button.setFocusable(true);
        button.setFocusPainted(true);
        button.setRolloverEnabled(true);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFont(UiTokens.EYEBROW);
        button.setMargin(new Insets(5, style == ButtonStyle.ICON ? 5 : 8, 5,
            style == ButtonStyle.ICON ? 5 : 8));
        button.setMinimumSize(new Dimension(style == ButtonStyle.ICON ? 24 : 42, UiTokens.CONTROL_HEIGHT));
        button.setPreferredSize(new Dimension(button.getPreferredSize().width, UiTokens.CONTROL_HEIGHT));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    static JButton smallButton(String text) { return button(text, ButtonStyle.SECONDARY); }
    static JButton primaryButton(String text) { return button(text, ButtonStyle.PRIMARY); }
    static JButton ghostButton(String text) { return button(text, ButtonStyle.GHOST); }

    static void stylePopupMenu(JPopupMenu menu)
    {
        menu.setOpaque(true);
        menu.setBackground(UiTokens.SURFACE_RAISED);
        menu.setForeground(UiTokens.TEXT_PRIMARY);
        menu.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTokens.BORDER_STRONG),
            BorderFactory.createEmptyBorder(UiTokens.XS, UiTokens.XS, UiTokens.XS, UiTokens.XS)));
    }

    static <T extends JMenuItem> T styleMenuItem(T item)
    {
        item.setOpaque(true);
        item.setBackground(UiTokens.SURFACE_RAISED);
        item.setForeground(UiTokens.TEXT_PRIMARY);
        item.setFont(UiTokens.BODY);
        item.setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        return item;
    }

    static JButton iconButton(String glyph, String description)
    {
        JButton button = button(glyph, ButtonStyle.ICON);
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleName(description);
        return button;
    }

    static JTextField textField(String placeholder)
    {
        PromptTextField field = new PromptTextField(placeholder);
        styleTextField(field);
        return field;
    }

    static void styleTextField(JTextField field)
    {
        field.setOpaque(true);
        field.setBackground(UiTokens.SURFACE_RAISED);
        field.setForeground(UiTokens.TEXT_PRIMARY);
        field.setCaretColor(UiTokens.ACCENT_HOVER);
        field.setSelectionColor(UiTokens.ACCENT_DARK);
        field.setSelectedTextColor(UiTokens.TEXT_PRIMARY);
        field.setFont(UiTokens.BODY);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CONTROL_HEIGHT));
        field.setPreferredSize(new Dimension(120, UiTokens.CONTROL_HEIGHT));
        setFieldBorder(field, false);
        if (!Boolean.TRUE.equals(field.getClientProperty("ironcompass.focusStyled")))
        {
            field.putClientProperty("ironcompass.focusStyled", Boolean.TRUE);
            field.addFocusListener(new FocusAdapter()
            {
                @Override public void focusGained(FocusEvent event) { setFieldBorder(field, true); }
                @Override public void focusLost(FocusEvent event) { setFieldBorder(field, false); }
            });
        }
    }

    static <T> void styleComboBox(JComboBox<T> combo)
    {
        combo.setUI(new DarkComboBoxUI());
        combo.setOpaque(true);
        combo.setBackground(UiTokens.SURFACE_RAISED);
        combo.setForeground(UiTokens.TEXT_PRIMARY);
        combo.setFont(UiTokens.BODY);
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setFocusable(true);
        combo.setMaximumRowCount(10);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CONTROL_HEIGHT));
        combo.setPreferredSize(new Dimension(120, UiTokens.CONTROL_HEIGHT));
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UiTokens.BORDER_STRONG),
            BorderFactory.createEmptyBorder(1, UiTokens.XS, 1, UiTokens.XS)));
        combo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean selected, boolean focus)
            {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setOpaque(true);
                setBackground(selected ? UiTokens.SURFACE_SELECTED : UiTokens.SURFACE_RAISED);
                setForeground(selected ? UiTokens.ACCENT_HOVER : UiTokens.TEXT_PRIMARY);
                setFont(UiTokens.BODY);
                setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
                return this;
            }
        });
    }

    static JScrollPane scrollPane(Component view)
    {
        JScrollPane scroll = new JScrollPane(view);
        styleScrollPane(scroll);
        return scroll;
    }

    static void styleScrollPane(JScrollPane scroll)
    {
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(UiTokens.BACKGROUND);
        styleScrollBar(scroll.getVerticalScrollBar());
        styleScrollBar(scroll.getHorizontalScrollBar());
    }

    static Component gap(int height)
    {
        Box.Filler filler = new Box.Filler(new Dimension(0, height), new Dimension(0, height),
            new Dimension(Integer.MAX_VALUE, height));
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    static Component verticalGlue()
    {
        Box.Filler filler = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    static ImageIcon compassIcon() { return COMPASS_ICON; }

    static void installHover(JComponent component, Color normal)
    {
        component.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent event)
            {
                component.setBackground(UiTokens.SURFACE_HOVER);
                component.repaint();
            }

            @Override public void mouseExited(MouseEvent event)
            {
                component.setBackground(normal);
                component.repaint();
            }
        });
    }

    private static void setFieldBorder(JTextField field, boolean focused)
    {
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(focused ? UiTokens.ACCENT : UiTokens.BORDER_STRONG),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
    }

    private static void styleScrollBar(JScrollBar bar)
    {
        if (bar == null) return;
        bar.setUI(new DarkScrollBarUI());
        bar.setUnitIncrement(16);
        bar.setPreferredSize(new Dimension(UiTokens.SCROLLBAR_WIDTH, UiTokens.SCROLLBAR_WIDTH));
        bar.setOpaque(true);
        bar.setBackground(UiTokens.BACKGROUND);
    }

    private static void styleNestedScrollPanes(Container container)
    {
        for (Component child : container.getComponents())
        {
            if (child instanceof JScrollPane) styleScrollPane((JScrollPane) child);
            else if (child instanceof Container) styleNestedScrollPanes((Container) child);
        }
    }

    private static ImageIcon loadCompassIcon()
    {
        java.net.URL resource = UiComponents.class.getResource("/icon.png");
        if (resource == null) return null;
        ImageIcon source = new ImageIcon(resource);
        java.awt.Image image = source.getImage().getScaledInstance(22, 22, java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private static final class PromptTextField extends JTextField
    {
        private final String placeholder;

        private PromptTextField(String placeholder) { this.placeholder = placeholder; }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            if (!getText().isEmpty() || isFocusOwner() || placeholder == null) return;
            graphics.setColor(UiTokens.TEXT_MUTED);
            graphics.setFont(UiTokens.BODY);
            Insets insets = getInsets();
            graphics.drawString(placeholder, insets.left,
                (getHeight() - graphics.getFontMetrics().getHeight()) / 2 + graphics.getFontMetrics().getAscent());
        }
    }

    /** Box-layout stack that follows a viewport width while retaining content-driven height. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable
    {
        private WidthTrackingPanel()
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
        {
            return 16;
        }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
        {
            return Math.max(32, visible.height - 32);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class DarkComboBoxUI extends BasicComboBoxUI
    {
        @Override
        public void paint(Graphics graphics, JComponent component)
        {
            super.paint(graphics, component);
            int centerX = Math.max(8, component.getWidth() - 10);
            int centerY = component.getHeight() / 2;
            graphics.setColor(UiTokens.BORDER_STRONG);
            graphics.drawLine(Math.max(1, component.getWidth() - 20), 3,
                Math.max(1, component.getWidth() - 20), Math.max(3, component.getHeight() - 4));
            graphics.setColor(UiTokens.TEXT_SECONDARY);
            graphics.fillPolygon(new int[] {centerX - 3, centerX + 3, centerX},
                new int[] {centerY - 2, centerY - 2, centerY + 2}, 3);
        }

        @Override
        public void paintCurrentValueBackground(Graphics graphics, java.awt.Rectangle bounds, boolean hasFocus)
        {
            graphics.setColor(UiTokens.SURFACE_RAISED);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected JButton createArrowButton()
        {
            JButton arrow = new JButton()
            {
                @Override
                public void paint(Graphics graphics)
                {
                    graphics.setColor(getModel().isRollover() ? UiTokens.SURFACE_HOVER : UiTokens.SURFACE_RAISED);
                    graphics.fillRect(0, 0, getWidth(), getHeight());
                    graphics.setColor(UiTokens.BORDER_STRONG);
                    graphics.drawLine(0, 3, 0, Math.max(3, getHeight() - 4));
                    int centerX = getWidth() / 2;
                    int centerY = getHeight() / 2;
                    graphics.setColor(UiTokens.TEXT_SECONDARY);
                    graphics.fillPolygon(new int[] {centerX - 3, centerX + 3, centerX},
                        new int[] {centerY - 2, centerY - 2, centerY + 2}, 3);
                }
            };
            arrow.setFocusable(false);
            arrow.setRolloverEnabled(true);
            arrow.setOpaque(false);
            arrow.setContentAreaFilled(false);
            arrow.setBorderPainted(false);
            arrow.setPreferredSize(new Dimension(20, UiTokens.CONTROL_HEIGHT));
            arrow.getAccessibleContext().setAccessibleName("Open choices");
            return arrow;
        }

        @Override
        protected ComboPopup createPopup()
        {
            BasicComboPopup popup = new BasicComboPopup(comboBox);
            popup.getList().setBackground(UiTokens.SURFACE_RAISED);
            popup.getList().setForeground(UiTokens.TEXT_PRIMARY);
            popup.setBorder(BorderFactory.createLineBorder(UiTokens.BORDER_STRONG));
            styleNestedScrollPanes(popup);
            return popup;
        }
    }
}
