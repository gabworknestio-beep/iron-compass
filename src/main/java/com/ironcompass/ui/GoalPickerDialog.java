package com.ironcompass.ui;

import com.ironcompass.gear.GearPreferenceStore;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.goal.GoalCompletionService;
import com.ironcompass.goal.GoalDefinition;
import com.ironcompass.goal.GoalRequirementResolver;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.planner.GoalPlanProjection;
import com.ironcompass.requirement.ConditionSpec;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.route.RouteProjection;
import com.ironcompass.state.AccountState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static com.ironcompass.ui.UiComponents.*;

/** Purpose-built, dark Goal Picker with goal-first actions and secondary manual controls. */
final class GoalPickerDialog
{
    private final Component owner;
    private final GoalPickerModel model;
    private final GoalPlanProjection plan;
    private final AccountState state;
    private final GearProjection gear;
    private final RouteProjection route;
    private final GearPreferenceStore preferences;
    private final ManualOverrideStore overrides;
    private final GoalCompletionService completion;
    private final Runnable reevaluate;
    private final JTextField search = textField("Search goals...");
    private final JComboBox<String> category;
    private final JComboBox<String> stage;
    private final DefaultListModel<GoalChoice> choices = new DefaultListModel<>();
    private final JList<GoalChoice> list = new JList<>(choices);
    private final JPanel detailHost = scrollableVerticalPanel();
    private final JPanel root = new JPanel(new BorderLayout(UiTokens.MD, UiTokens.MD));
    private JDialog dialog;
    private boolean expanded;

    GoalPickerDialog(Component owner, GoalPickerModel model, GoalPlanProjection plan, AccountState state,
                     GearProjection gear, RouteProjection route, GearPreferenceStore preferences,
                     ManualOverrideStore overrides, GoalCompletionService completion, Runnable reevaluate)
    {
        this.owner = owner;
        this.model = model;
        this.plan = plan;
        this.state = state;
        this.gear = gear;
        this.route = route;
        this.preferences = preferences;
        this.overrides = overrides;
        this.completion = completion;
        this.reevaluate = reevaluate;
        category = new JComboBox<>(model.categories(plan.getCatalog()).toArray(new String[0]));
        stage = new JComboBox<>(model.stages().toArray(new String[0]));
        build();
    }

    void showDialog()
    {
        if (java.awt.GraphicsEnvironment.isHeadless()) return;
        Window window = SwingUtilities.getWindowAncestor(owner);
        dialog = new JDialog(window, "Iron Compass · Choose goals", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(430, 580));
        dialog.setPreferredSize(new Dimension(470, 650));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    JPanel contentForTesting()
    {
        return root;
    }

    private void build()
    {
        root.setBackground(UiTokens.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(UiTokens.LG, UiTokens.LG, UiTokens.LG, UiTokens.LG));
        root.getAccessibleContext().setAccessibleName("Iron Compass Goal Picker");

        JPanel top = verticalPanel();
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (compassIcon() != null)
        {
            JLabel icon = new JLabel(compassIcon());
            icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UiTokens.SM));
            brand.add(icon);
        }
        JLabel title = new JLabel("CHOOSE A GOAL");
        title.setFont(UiTokens.APP_TITLE);
        title.setForeground(UiTokens.ACCENT);
        brand.add(title);
        top.add(brand);
        top.add(labelHtml(plan.getCatalog().getGoals().size() + " researched Ironman goals", UiTokens.TEXT_MUTED));
        top.add(gap(UiTokens.MD));
        search.getAccessibleContext().setAccessibleName("Search goals");
        top.add(search);
        top.add(gap(UiTokens.SM));
        JPanel filters = new JPanel(new GridLayout(1, 2, UiTokens.SM, 0));
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        styleComboBox(category);
        styleComboBox(stage);
        category.getAccessibleContext().setAccessibleName("Goal category filter");
        stage.getAccessibleContext().setAccessibleName("Goal stage filter");
        filters.add(category);
        filters.add(stage);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiTokens.CONTROL_HEIGHT));
        top.add(filters);
        root.add(top, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(8);
        list.setFixedCellHeight(52);
        list.setBackground(UiTokens.SURFACE);
        list.setForeground(UiTokens.TEXT_PRIMARY);
        list.setSelectionBackground(UiTokens.SURFACE_SELECTED);
        list.setSelectionForeground(UiTokens.TEXT_PRIMARY);
        list.setCellRenderer(new GoalCellRenderer());
        list.getAccessibleContext().setAccessibleName("Goal results");
        JScrollPane listScroll = scrollPane(list);
        listScroll.setPreferredSize(new Dimension(420, 260));
        root.add(card(listScroll, CardStyle.SUBTLE), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, UiTokens.MD));
        south.setOpaque(false);
        JScrollPane detailScroll = scrollPane(detailHost);
        detailScroll.setPreferredSize(new Dimension(420, 210));
        south.add(card(detailScroll, CardStyle.HERO), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.SM, 0));
        actions.setOpaque(false);
        JButton advanced = ghostButton("MANUAL ···");
        advanced.setToolTipText("Manual completion overrides");
        advanced.addActionListener(event -> showAdvanced(advanced));
        JButton secondary = smallButton("ADD SECONDARY");
        secondary.addActionListener(event -> addSecondary());
        JButton primary = primaryButton("SET PRIMARY");
        primary.addActionListener(event -> setPrimary());
        actions.add(advanced);
        actions.add(secondary);
        actions.add(primary);
        south.add(actions, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { refresh(); }
            @Override public void removeUpdate(DocumentEvent event) { refresh(); }
            @Override public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        category.addActionListener(event -> refresh());
        stage.addActionListener(event -> refresh());
        list.addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) rebuildDetail(); });
        refresh();
    }

    private void refresh()
    {
        String selectedId = selected() == null ? null : selected().id;
        choices.clear();
        Set<String> active = new HashSet<>();
        for (GoalPlanProjection activePlan : plan.getActiveGoals()) active.add(activePlan.getGoalId());
        for (GoalSuggestion suggestion : model.suggestions(plan.getCatalog(), search.getText(),
            (String) category.getSelectedItem(), (String) stage.getSelectedItem(), active, state, gear,
            route == null ? -1.0 : route.getProgressPercent()))
            choices.addElement(new GoalChoice(suggestion));
        int selection = 0;
        if (selectedId != null)
            for (int i = 0; i < choices.size(); i++) if (selectedId.equals(choices.get(i).id)) selection = i;
        if (!choices.isEmpty()) list.setSelectedIndex(selection);
        rebuildDetail();
    }

    private void rebuildDetail()
    {
        detailHost.removeAll();
        detailHost.setBorder(BorderFactory.createEmptyBorder(UiTokens.XS, UiTokens.XS, UiTokens.XS, UiTokens.XS));
        GoalChoice choice = selected();
        if (choice == null)
        {
            detailHost.add(labelHtml("Choose a result to inspect its value and current status.", UiTokens.TEXT_MUTED));
        }
        else
        {
            GoalDefinition goal = choice.suggestion.getGoal();
            JPanel heading = new JPanel(new BorderLayout(UiTokens.SM, 0));
            heading.setOpaque(false);
            heading.setAlignmentX(Component.LEFT_ALIGNMENT);
            heading.add(cardTitle(goal.getTitle(), UiTokens.ACCENT_HOVER), BorderLayout.CENTER);
            JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTokens.XS, 0));
            badges.setOpaque(false);
            if (goal.getId().equals(preferences.getPrimaryGoalId())) badges.add(badge("PRIMARY", UiTokens.ACCENT));
            else if (preferences.getSecondaryGoalIds().contains(goal.getId()))
                badges.add(badge("SECONDARY", UiTokens.TEXT_SECONDARY));
            if (goal.isRng()) badges.add(badge("RNG", UiTokens.WARNING));
            heading.add(badges, BorderLayout.EAST);
            detailHost.add(heading);
            detailHost.add(labelHtml(goal.getStage().getLabel() + " · " + goal.getCategory() + " · "
                + humanize(goal.getEffort().name()), UiTokens.TEXT_MUTED));
            detailHost.add(gap(UiTokens.MD));
            detailHost.add(sectionLabel("WHY THIS GOAL"));
            detailHost.add(labelHtml(escape(goal.getWhyItMatters()), UiTokens.TEXT_PRIMARY));
            detailHost.add(gap(UiTokens.MD));
            detailHost.add(sectionLabel("CURRENT STATUS"));
            detailHost.add(statusLine(choice.suggestion.getEvaluation().getReadiness(),
                choice.suggestion.getEvaluation().getStatus().getLabel() + " · "
                    + choice.suggestion.getEvaluation().getExplanation()));
            detailHost.add(gap(UiTokens.MD));
            detailHost.add(sectionLabel("WHAT REMAINS"));
            ConditionSpec requirement = GoalRequirementResolver.effectiveRequirements(goal, gear);
            detailHost.add(labelHtml(requirement == null ? "Manual confirmation in game"
                : escape(requirement.getLabel()), UiTokens.TEXT_SECONDARY));
            detailHost.add(gap(UiTokens.MD));
            detailHost.add(sectionLabel("WHAT IT UNLOCKS"));
            int limit = expanded ? goal.getUnlocks().size() : Math.min(2, goal.getUnlocks().size());
            for (int i = 0; i < limit; i++)
                detailHost.add(labelHtml("•  " + escape(goal.getUnlocks().get(i)), UiTokens.TEXT_SECONDARY));
            if (expanded)
            {
                detailHost.add(gap(UiTokens.MD));
                detailHost.add(sectionLabel("MORE DETAILS"));
                detailHost.add(labelHtml(escape(goal.getDescription()), UiTokens.TEXT_SECONDARY));
                for (String reason : choice.suggestion.getReasons())
                    detailHost.add(labelHtml("•  " + escape(reason), UiTokens.TEXT_MUTED));
            }
            JButton disclosure = ghostButton(expanded ? "LESS DETAIL" : "MORE DETAILS");
            disclosure.addActionListener(event -> { expanded = !expanded; rebuildDetail(); });
            detailHost.add(gap(UiTokens.SM));
            detailHost.add(disclosure);
        }
        detailHost.revalidate();
        detailHost.repaint();
    }

    private void setPrimary()
    {
        GoalChoice choice = selected();
        if (choice == null) return;
        preferences.setPrimaryGoalId(choice.id);
        reevaluate.run();
        close();
    }

    private void addSecondary()
    {
        GoalChoice choice = selected();
        if (choice == null) return;
        if (preferences.addSecondaryGoalId(choice.id))
        {
            reevaluate.run();
            close();
        }
        else
        {
            detailHost.add(gap(UiTokens.SM));
            detailHost.add(labelHtml("Secondary goals are limited to three and cannot duplicate the primary goal.",
                UiTokens.WARNING));
            detailHost.revalidate();
        }
    }

    private void showAdvanced(Component anchor)
    {
        GoalChoice choice = selected();
        if (choice == null) return;
        JPopupMenu menu = new JPopupMenu();
        stylePopupMenu(menu);
        addMenu(menu, "Mark complete", () -> completion.markComplete(choice.id, overrides));
        addMenu(menu, "Mark incomplete", () -> completion.markIncomplete(choice.id, overrides));
        addMenu(menu, "Clear manual override", () -> completion.clear(choice.id, overrides));
        if (choice.id.equals(preferences.getPrimaryGoalId())
            || preferences.getSecondaryGoalIds().contains(choice.id))
            addMenu(menu, "Remove from active goals", () ->
            {
                if (choice.id.equals(preferences.getPrimaryGoalId())) preferences.setPrimaryGoalId(null);
                preferences.removeSecondaryGoalId(choice.id);
            });
        menu.show(anchor, 0, anchor.getHeight());
    }

    private void addMenu(JPopupMenu menu, String text, Runnable action)
    {
        JMenuItem item = styleMenuItem(new JMenuItem(text));
        item.addActionListener(event ->
        {
            action.run();
            reevaluate.run();
            refresh();
        });
        menu.add(item);
    }

    private GoalChoice selected() { return list.getSelectedValue(); }
    private void close() { if (dialog != null) dialog.dispose(); }

    private final class GoalCellRenderer implements ListCellRenderer<GoalChoice>
    {
        private final GoalCell cell = new GoalCell();

        @Override
        public Component getListCellRendererComponent(JList<? extends GoalChoice> source, GoalChoice choice,
                                                       int index, boolean selected, boolean focus)
        {
            String role = choice.id.equals(preferences.getPrimaryGoalId()) ? "PRIMARY"
                : preferences.getSecondaryGoalIds().contains(choice.id) ? "SECONDARY" : null;
            cell.configure(choice, selected, role);
            return cell;
        }
    }

    /** A lightweight renderer avoids nested Swing layout inconsistencies in headless screenshot runs. */
    private static final class GoalCell extends JPanel
    {
        private GoalChoice choice;
        private boolean selected;
        private String role;

        private GoalCell()
        {
            setOpaque(true);
            setPreferredSize(new Dimension(360, 52));
        }

        private void configure(GoalChoice choice, boolean selected, String role)
        {
            this.choice = choice;
            this.selected = selected;
            this.role = role;
            setBackground(selected ? UiTokens.SURFACE_SELECTED : UiTokens.SURFACE);
            getAccessibleContext().setAccessibleName(choice.suggestion.getGoal().getTitle() + " "
                + choice.suggestion.getEvaluation().getStatus().getLabel()
                + (role == null ? "" : " " + role));
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            if (choice == null) return;
            int left = selected ? 9 : 7;
            if (selected)
            {
                graphics.setColor(UiTokens.ACCENT);
                graphics.fillRect(0, 0, 2, getHeight());
            }
            graphics.setColor(UiTokens.BORDER_SUBTLE);
            graphics.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            int roleWidth = 0;
            if (role != null)
            {
                graphics.setFont(UiTokens.EYEBROW);
                roleWidth = graphics.getFontMetrics().stringWidth(role) + 12;
                int x = Math.max(left, getWidth() - roleWidth - 7);
                graphics.setColor(UiTokens.SURFACE_RAISED);
                graphics.fillRect(x, 7, roleWidth, 16);
                graphics.setColor("PRIMARY".equals(role) ? UiTokens.ACCENT : UiTokens.TEXT_SECONDARY);
                graphics.drawRect(x, 7, roleWidth, 16);
                graphics.drawString(role, x + 6, 19);
            }

            graphics.setFont(UiTokens.CARD_TITLE);
            graphics.setColor(selected ? UiTokens.ACCENT_HOVER : UiTokens.TEXT_PRIMARY);
            FontMetrics titleMetrics = graphics.getFontMetrics();
            int available = Math.max(40, getWidth() - left - roleWidth - 18);
            graphics.drawString(elide(choice.suggestion.getGoal().getTitle(), titleMetrics, available), left, 19);
            graphics.setFont(UiTokens.META);
            graphics.setColor(UiTokens.TEXT_MUTED);
            String meta = choice.suggestion.getGoal().getStage().getLabel() + " · "
                + choice.suggestion.getGoal().getCategory() + " · "
                + choice.suggestion.getEvaluation().getStatus().getLabel();
            graphics.drawString(elide(meta, graphics.getFontMetrics(), getWidth() - left - 8), left, 38);
        }

        private static String elide(String text, FontMetrics metrics, int width)
        {
            if (metrics.stringWidth(text) <= width) return text;
            String suffix = "…";
            int end = text.length();
            while (end > 1 && metrics.stringWidth(text.substring(0, end) + suffix) > width) end--;
            return text.substring(0, end) + suffix;
        }
    }

    private static String humanize(String value)
    {
        String lower = value.toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;").replace("\n", "<br>");
    }

    private static final class GoalChoice
    {
        private final String id;
        private final GoalSuggestion suggestion;
        private GoalChoice(GoalSuggestion suggestion)
        {
            this.id = suggestion.getGoal().getId();
            this.suggestion = suggestion;
        }
    }
}
