package com.ironcompass.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.JEditorPane;
import javax.swing.text.html.HTMLEditorKit;

/** Rich text that follows the card width instead of forcing a hard-coded HTML table width. */
final class WrappingText extends JEditorPane
{
    private static final int FALLBACK_WIDTH = 188;

    WrappingText(String body, Color color, Font font)
    {
        setEditorKit(new HTMLEditorKit());
        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        setContentType("text/html");
        setEditable(false);
        setFocusable(false);
        setOpaque(false);
        setBorder(null);
        setMargin(new Insets(0, 0, 0, 0));
        setFont(font);
        setForeground(color);
        setText("<html><body style='margin:0;padding:0;color:" + hex(color)
            + ";font-family:" + font.getFamily() + ";font-size:" + font.getSize()
            + "pt'>" + body + "</body></html>");
        setAlignmentX(LEFT_ALIGNMENT);
        getAccessibleContext().setAccessibleName(plain(body));
    }

    @Override
    public Dimension getPreferredSize()
    {
        int width = availableWidth();
        setSize(new Dimension(width, Short.MAX_VALUE));
        Dimension preferred = super.getPreferredSize();
        return new Dimension(width, Math.max(1, preferred.height));
    }

    @Override
    public Dimension getMaximumSize()
    {
        Dimension preferred = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferred.height);
    }

    private int availableWidth()
    {
        Container parent = getParent();
        if (parent != null && parent.getWidth() > 0)
        {
            Insets insets = parent.getInsets();
            return Math.max(80, parent.getWidth() - insets.left - insets.right);
        }
        return FALLBACK_WIDTH;
    }

    private static String hex(Color color)
    {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String plain(String html)
    {
        return html.replaceAll("<br\\s*/?>", " ").replaceAll("<[^>]+>", "")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").trim();
    }
}
