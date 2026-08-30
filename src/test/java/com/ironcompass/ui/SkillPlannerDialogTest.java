package com.ironcompass.ui;

import com.google.gson.Gson;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.planner.InMemoryPlannerPreferenceStore;
import com.ironcompass.requirement.ConditionEvaluator;
import com.ironcompass.state.AccountState;
import com.ironcompass.state.QuestProgress;
import com.ironcompass.training.IronmanMethodCatalog;
import com.ironcompass.training.IronmanMethodLoader;
import com.ironcompass.training.MethodPlannerService;
import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class SkillPlannerDialogTest
{
    @Test
    public void hunterTargetRendersCompactPlanMilestoneAndDisclosure() throws Exception
    {
        IronmanMethodCatalog catalog = new IronmanMethodLoader(new Gson())
            .loadResource("/methods/ironman-methods-2026.json");
        AccountState state = AccountState.builder().skill("Hunter", 68)
            .quest("Children of the Sun", QuestProgress.FINISHED).build();
        SwingUtilities.invokeAndWait(() ->
        {
            SkillPlannerDialog dialog = new SkillPlannerDialog(new JPanel(), catalog,
                new MethodPlannerService(new ConditionEvaluator()), state,
                new InMemoryPlannerPreferenceStore(), Collections.emptyList(), new WikiBridge(),
                "Hunter", 75, false);
            JPanel content = dialog.contentForTesting();
            assertNotNull(findText(content, "IRONMAN SKILL PLANNER"));
            assertNotNull(findText(content, "HUNTER  68 → 75"));
            assertNotNull(findText(content, "Adept Hunter Rumours"));
            assertNotNull(findText(content, "Moonlight moths"));
            assertNotNull(findButton(content, "VIEW METHOD"));
            assertNotNull(findButton(content, "FULL 1–99 GUIDE"));
            assertNotNull(findButton(content, "MORE DETAILS"));
            JScrollPane scroll = find(content, JScrollPane.class);
            assertNotNull(scroll);
            assertTrue(scroll.getHorizontalScrollBarPolicy() == ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        });
    }

    private static Component findText(Component root, String text)
    {
        if (root instanceof JLabel && ((JLabel) root).getText() != null
            && plain(((JLabel) root).getText()).contains(text)) return root;
        if (root.getAccessibleContext() != null && root.getAccessibleContext().getAccessibleName() != null
            && plain(root.getAccessibleContext().getAccessibleName()).contains(text)) return root;
        if (root instanceof Container)
            for (Component child : ((Container) root).getComponents())
            {
                Component match = findText(child, text);
                if (match != null) return match;
            }
        return null;
    }

    private static JButton findButton(Component root, String text)
    {
        if (root instanceof JButton && text.equals(((JButton) root).getText())) return (JButton) root;
        if (root instanceof Container)
            for (Component child : ((Container) root).getComponents())
            {
                JButton match = findButton(child, text);
                if (match != null) return match;
            }
        return null;
    }

    private static <T> T find(Component root, Class<T> type)
    {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Container)
            for (Component child : ((Container) root).getComponents())
            {
                T match = find(child, type);
                if (match != null) return match;
            }
        return null;
    }

    private static String plain(String text)
    {
        return text.replaceAll("<[^>]+>", "").replace("&rarr;", "→").replace("&ndash;", "–")
            .replace("&amp;", "&");
    }
}
