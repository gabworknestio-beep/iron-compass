package com.ironcompass.ui;

import com.ironcompass.gear.CombatStyle;
import com.ironcompass.gear.GearEvaluation;
import com.ironcompass.gear.GearPreferenceStore;
import com.ironcompass.gear.GearProjection;
import com.ironcompass.gear.GearStatus;
import com.ironcompass.gear.GearUpgrade;
import com.ironcompass.integration.WikiBridge;
import com.ironcompass.persistence.ManualOverride;
import com.ironcompass.persistence.ManualOverrideStore;
import com.ironcompass.requirement.RequirementResult;
import com.ironcompass.requirement.TruthValue;
import com.ironcompass.state.AccountState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public final class GearPathPanel extends JPanel
{
    private static final String[] STYLE_FILTERS = {"ALL", "MELEE", "RANGED", "MAGIC", "PRAYER", "UTILITY", "SKILLING"};
    private static final String[] STATUS_FILTERS = {"ALL", "RECOMMENDED", "AVAILABLE", "UNCONFIRMED", "LOCKED", "OWNED", "OPTIONAL", "SKIPPED"};

    private final WikiBridge wiki;
    private final GearPreferenceStore preferences;
    private final ManualOverrideStore overrides;
    private final Runnable reevaluate;
    private final JPanel content = verticalPanel();
    private final JTextField search = new JTextField();
    private final JComboBox<String> styleFilter = new JComboBox<>(STYLE_FILTERS);
    private final JComboBox<String> statusFilter = new JComboBox<>(STATUS_FILTERS);
    private AccountState state = AccountState.loggedOut();
    private GearProjection projection;
    private String detailId;
    private boolean updatingFilters;
    private final java.util.Set<JComboBox<?>> pendingFilterRefresh =
        java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final java.util.Set<JComboBox<?>> openFilterPopups =
        java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public GearPathPanel(WikiBridge wiki, GearPreferenceStore preferences, ManualOverrideStore overrides,
                         Runnable reevaluate)
    {
        this.wiki = wiki;
        this.preferences = preferences;
        this.overrides = overrides;
        this.reevaluate = reevaluate;
        setLayout(new BorderLayout());
        setBackground(UiTokens.BACKGROUND);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UiTokens.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        search.setToolTipText("Search gear, source, tags, or region");
        search.getAccessibleContext().setAccessibleName("Search gear objectives");
        search.getAccessibleContext().setAccessibleDescription("Filter gear by name, source, tag, or region");
        styleFilter.getAccessibleContext().setAccessibleName("Gear combat style filter");
        statusFilter.getAccessibleContext().setAccessibleName("Gear status filter");
        UiTokens.styleComboBox(styleFilter);
        UiTokens.styleComboBox(statusFilter);
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent event) { rebuild(); }
            @Override public void removeUpdate(DocumentEvent event) { rebuild(); }
            @Override public void changedUpdate(DocumentEvent event) { rebuild(); }
        });
        configureFilter(styleFilter,true);
        configureFilter(statusFilter,false);
    }

    public void update(AccountState newState, GearProjection newProjection)
    {
        state = newState;
        projection = newProjection;
        updatingFilters = true;
        styleFilter.setSelectedItem(preferences.getGearStyleFilter());
        statusFilter.setSelectedItem(preferences.getGearStatusFilter());
        updatingFilters = false;
        rebuild();
    }

    public void showObjective(String objectiveId)
    {
        detailId = objectiveId;
        rebuild();
    }

    void selectStyleForTesting(String style)
    {
        styleFilter.setSelectedItem(style);
    }

    void setSearchForTesting(String query)
    {
        search.setText(query);
    }

    JComboBox<String> styleFilterForTesting() { return styleFilter; }
    JComboBox<String> statusFilterForTesting() { return statusFilter; }

    private void filterChanged(boolean style)
    {
        if (updatingFilters) return;
        if (style) preferences.setGearStyleFilter(String.valueOf(styleFilter.getSelectedItem()));
        else preferences.setGearStatusFilter(String.valueOf(statusFilter.getSelectedItem()));
        JComboBox<?> combo = style ? styleFilter : statusFilter;
        if (combo.isPopupVisible() || openFilterPopups.contains(combo)) pendingFilterRefresh.add(combo);
        else rebuild();
    }

    private void configureFilter(JComboBox<?> combo, boolean style)
    {
        combo.addActionListener(event -> filterChanged(style));
        combo.addPopupMenuListener(new PopupMenuListener()
        {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent event) { openFilterPopups.add(combo); }
            @Override public void popupMenuCanceled(PopupMenuEvent event)
            {
                openFilterPopups.remove(combo);
                pendingFilterRefresh.remove(combo);
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent event)
            {
                openFilterPopups.remove(combo);
                if (pendingFilterRefresh.remove(combo)) SwingUtilities.invokeLater(GearPathPanel.this::rebuild);
            }
        });
    }

    private void rebuild()
    {
        content.removeAll();
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(topRow());
        content.add(gap(9));
        if (projection == null || !state.isLoggedIn())
        {
            content.add(card(labelHtml("Log in to evaluate the 2026 gear path.", UiTokens.MUTED)));
        }
        else if (detailId != null && projection.find(detailId) != null)
        {
            content.add(buildDetail(projection.find(detailId)));
        }
        else
        {
            content.add(buildSummary());
            content.add(gap(9));
            content.add(buildFilters());
            if (projection.getRecommended() != null)
            {
                content.add(gap(9));
                content.add(buildRecommendation(projection.getRecommended()));
            }
            content.add(gap(9));
            content.add(buildList());
        }
        content.add(verticalGlue());
        content.revalidate();
        content.repaint();
    }

    private JPanel topRow()
    {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton back = smallButton("BACK");
        back.setVisible(detailId != null);
        back.addActionListener(event -> { detailId = null; rebuild(); });
        JLabel title = new JLabel("GEAR PATH");
        title.setForeground(UiTokens.ACCENT);
        title.setFont(UiTokens.TITLE);
        row.add(back, BorderLayout.WEST);
        row.add(title, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            Math.max(back.getPreferredSize().height, title.getPreferredSize().height)));
        return row;
    }

    private JPanel buildSummary()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("GEAR OWNERSHIP"));
        body.add(gap(4));
        if (state.getBank().isObserved())
        {
            body.add(labelHtml("<b>" + projection.getOwnedCount() + " / " + projection.getTotalCount()
                + " confirmed</b>", UiTokens.TEXT));
            body.add(gap(5));
            JProgressBar progress = new JProgressBar(0, Math.max(1, projection.getTotalCount()));
            progress.setValue(projection.getOwnedCount());
            progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            progress.setForeground(UiTokens.ACCENT);
            progress.setBackground(UiTokens.BACKGROUND);
            progress.setBorderPainted(false);
            progress.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(progress);
            body.add(gap(6));
            long age = Math.max(0L, System.currentTimeMillis() - state.getBank().getObservedAtEpochMillis());
            long minutes = age / 60000L;
            body.add(statusLine(TruthValue.TRUE, "Bank scanned this session "
                + (minutes == 0 ? "less than a minute ago" : minutes + " min ago")));
        }
        else
        {
            body.add(labelHtml("<b>Not scanned</b>", UiTokens.UNKNOWN));
            body.add(gap(3));
            body.add(labelHtml("Open your bank once to detect stored gear. All "
                + projection.getTotalCount() + " objectives are loaded.", UiTokens.MUTED));
        }
        return card(body);
    }

    private JPanel buildFilters()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("FILTERS"));
        body.add(gap(4));
        body.add(sectionLabel("SEARCH GEAR"));
        body.add(gap(2));
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        body.add(search);
        body.add(gap(5));
        body.add(sectionLabel("STYLE"));
        body.add(gap(2));
        styleFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        styleFilter.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(styleFilter);
        body.add(gap(5));
        body.add(sectionLabel("STATUS"));
        body.add(gap(2));
        statusFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        statusFilter.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(statusFilter);
        return card(body);
    }

    private JPanel buildRecommendation(GearEvaluation evaluation)
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("NEXT GEAR OPPORTUNITY"));
        body.add(gap(5));
        body.add(labelHtml("<b>" + escape(evaluation.getUpgrade().getName()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(priorityLabel(evaluation) + "  ·  " + styleText(evaluation.getUpgrade())
            + "  ·  tier " + evaluation.getUpgrade().getTier(), UiTokens.MUTED));
        body.add(gap(6));
        body.add(labelHtml(escape(evaluation.getUpgrade().getWhy()), UiTokens.TEXT));
        body.add(gap(7));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton details = smallButton("DETAILS");
        details.addActionListener(event -> { detailId = evaluation.getUpgrade().getId(); rebuild(); });
        JButton goal = smallButton(evaluation.isSelectedGoal() ? "GOAL SET" : "DO THIS NOW");
        goal.addActionListener(event -> setGoal(evaluation));
        row.add(details);
        row.add(goal);
        body.add(row);
        return card(body);
    }

    private JPanel buildList()
    {
        JPanel body = verticalPanel();
        body.add(sectionLabel("GEAR JOURNEY"));
        body.add(gap(4));
        String query = search.getText().trim().toLowerCase(Locale.ENGLISH);
        String style = String.valueOf(styleFilter.getSelectedItem());
        String status = String.valueOf(statusFilter.getSelectedItem());
        List<GearEvaluation> filtered = new ArrayList<>();
        for (GearEvaluation evaluation : projection.getEvaluations())
        {
            if (matches(evaluation, query, style, status)) filtered.add(evaluation);
        }
        filtered.sort(Comparator
            .comparingInt((GearEvaluation evaluation) -> primaryStyle(evaluation.getUpgrade()).ordinal())
            .thenComparingInt(evaluation -> evaluation.getUpgrade().getTier())
            .thenComparing(evaluation -> evaluation.getUpgrade().getSlot().name())
            .thenComparing(evaluation -> evaluation.getUpgrade().getName()));
        int shown = 0;
        String previousGroup = null;
        for (GearEvaluation evaluation : filtered)
        {
            String group = journeyGroup(evaluation.getUpgrade());
            if (!group.equals(previousGroup))
            {
                if (previousGroup != null) body.add(gap(7));
                body.add(sectionLabel(group));
                body.add(gap(3));
                previousGroup = group;
            }
            JPanel row = new JPanel(new BorderLayout(5, 0));
            row.setBackground(UiTokens.CARD);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.BORDER));
            JLabel glyph = new JLabel(statusGlyph(evaluation.getStatus()));
            glyph.setForeground(statusColor(evaluation.getStatus()));
            JLabel name = labelHtml("<b>" + escape(evaluation.getUpgrade().getName()) + "</b>"
                + (evaluation.isSelectedGoal() ? " <span style='color:#d9a441'>[goal]</span>" : "")
                + "<br><span style='color:#8f8f8f'>T" + evaluation.getUpgrade().getTier() + " · "
                + escape(humanize(evaluation.getUpgrade().getSlot().name())) + " · "
                + escape(statusText(evaluation.getStatus())) + "</span>", UiTokens.TEXT);
            JButton open = smallButton("›");
            open.setToolTipText("Open objective details");
            open.addActionListener(event -> { detailId = evaluation.getUpgrade().getId(); rebuild(); });
            row.add(glyph, BorderLayout.WEST);
            row.add(name, BorderLayout.CENTER);
            row.add(open, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            body.add(row);
            shown++;
        }
        if (shown == 0) body.add(labelHtml("No gear objective matches these filters.", UiTokens.MUTED));
        return card(body);
    }

    private JPanel buildDetail(GearEvaluation evaluation)
    {
        GearUpgrade upgrade = evaluation.getUpgrade();
        JPanel body = verticalPanel();
        body.add(sectionLabel(statusText(evaluation.getStatus())));
        body.add(gap(5));
        body.add(labelHtml("<b>" + escape(upgrade.getName()) + "</b>", UiTokens.ACCENT));
        body.add(labelHtml(styleText(upgrade) + "  ·  " + humanize(upgrade.getSlot().name())
            + "  ·  tier " + upgrade.getTier(), UiTokens.MUTED));
        body.add(gap(7));
        body.add(labelHtml(escape(upgrade.getWhy()), UiTokens.TEXT));
        if (upgrade.getNotes() != null)
        {
            body.add(gap(5));
            body.add(labelHtml(escape(upgrade.getNotes()), UiTokens.MUTED));
        }
        body.add(gap(9));
        body.add(sectionLabel("WHY THIS NOW"));
        body.add(gap(3));
        body.add(labelHtml(priorityReason(evaluation), evaluation.getStatus() == GearStatus.LOCKED
            ? UiTokens.UNKNOWN : UiTokens.TEXT));
        body.add(gap(4));
        body.add(statusLine(evaluation.getReadiness(), evaluation.getReadiness() == TruthValue.TRUE
            ? "Direct account requirements are met" : evaluation.getReadiness() == TruthValue.UNKNOWN
                ? "Some readiness facts are not known yet" : evaluation.getMissingReasons().size()
                    + " readiness condition(s) remain"));
        body.add(statusLine(TruthValue.TRUE, upgrade.getUsefulness() >= 5
            ? "Broad value across future progression" : "Useful upgrade for its authored role"));
        if (upgrade.getSource() != null)
        {
            body.add(gap(9));
            body.add(sectionLabel("UNLOCK SOURCE"));
            body.add(gap(3));
            body.add(labelHtml(escape(upgrade.getSource().getMethod()) + " — "
                + escape(upgrade.getSource().getActivity()) + " (" + escape(upgrade.getSource().getRegion()) + ")", UiTokens.TEXT));
        }
        body.add(gap(9));
        body.add(sectionLabel("DETECTION"));
        body.add(gap(3));
        String detection = evaluation.getCompletion() == TruthValue.TRUE ? "Owned equivalent detected"
            : evaluation.getCompletion() == TruthValue.FALSE ? "No owned equivalent detected"
            : "Ownership unknown until the bank is scanned";
        body.add(statusLine(evaluation.getCompletion(), detection));
        body.add(gap(9));
        body.add(sectionLabel("READINESS"));
        body.add(gap(3));
        if (evaluation.getReadinessDetails().isEmpty())
        {
            body.add(statusLine(evaluation.getReadiness(), "No direct account requirement"));
        }
        else
        {
            for (RequirementResult requirement : evaluation.getReadinessDetails())
            {
                body.add(statusLine(requirement.getValue(), requirement.getLabel() + detailSuffix(requirement)));
            }
        }
        for (String missing : evaluation.getMissingReasons())
        {
            if (missing.startsWith("Prerequisite:")) body.add(statusLine(TruthValue.FALSE, missing));
        }
        body.add(gap(9));
        body.add(sectionLabel("WHAT THIS UNLOCKS"));
        body.add(gap(3));
        int unlocked = 0;
        for (GearEvaluation candidate : projection.getEvaluations())
        {
            if (candidate.getUpgrade().getPreviousIds().contains(upgrade.getId())
                || candidate.getUpgrade().getPrerequisiteIds().contains(upgrade.getId()))
            {
                body.add(labelHtml("○  " + escape(candidate.getUpgrade().getName()), UiTokens.TEXT));
                if (++unlocked == 3) break;
            }
        }
        if (unlocked == 0)
        {
            body.add(labelHtml("Improves the account directly without gating a later catalogued objective.", UiTokens.MUTED));
        }
        body.add(gap(9));
        body.add(detailActions(evaluation));
        if (!upgrade.getAlternativeIds().isEmpty())
        {
            body.add(gap(9));
            body.add(sectionLabel("ALTERNATIVES"));
            body.add(gap(4));
            for (String alternativeId : upgrade.getAlternativeIds())
            {
                GearEvaluation alternative = projection.find(alternativeId);
                if (alternative == null) continue;
                JButton choose = smallButton("USE " + alternative.getUpgrade().getName().toUpperCase(Locale.ENGLISH));
                choose.setAlignmentX(Component.LEFT_ALIGNMENT);
                choose.addActionListener(event ->
                {
                    preferences.chooseAlternative(upgrade.getId(), alternativeId);
                    preferences.setSelectedGoalId(alternativeId);
                    detailId = alternativeId;
                    reevaluate.run();
                });
                body.add(choose);
                body.add(gap(3));
            }
        }
        return card(body);
    }

    private JPanel detailActions(GearEvaluation evaluation)
    {
        JPanel body = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        body.setOpaque(false);
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton goal = smallButton(evaluation.isSelectedGoal() ? "CLEAR GOAL" : "SET AS GOAL");
        goal.addActionListener(event -> setGoal(evaluation));
        body.add(goal);
        if (evaluation.getUpgrade().getWikiPage() != null)
        {
            JButton wikiButton = smallButton("WIKI");
            wikiButton.addActionListener(event -> wiki.open(evaluation.getUpgrade().getWikiPage()));
            body.add(wikiButton);
        }
        JButton manage = smallButton("MANAGE");
        manage.setToolTipText("Skip, priority, manual ownership, and reset controls");
        manage.addActionListener(event -> showManageMenu(manage, evaluation));
        body.add(manage);
        return body;
    }

    private void showManageMenu(Component anchor, GearEvaluation evaluation)
    {
        JPopupMenu menu = new JPopupMenu();
        addMenuItem(menu, evaluation.getStatus() == GearStatus.SKIPPED ? "Unskip" : "Skip", () ->
        {
            preferences.setSkipped(evaluation.getUpgrade().getId(), evaluation.getStatus() != GearStatus.SKIPPED);
            reevaluate.run();
        });
        addMenuItem(menu, preferences.isMarkedOptional(evaluation.getUpgrade().getId())
            ? "Restore priority" : "Mark optional", () ->
        {
            String id = evaluation.getUpgrade().getId();
            preferences.setMarkedOptional(id, !preferences.isMarkedOptional(id));
            reevaluate.run();
        });
        boolean manuallyOwned = overrides.get(evaluation.getUpgrade().getId()) == ManualOverride.FORCE_COMPLETE;
        addMenuItem(menu, manuallyOwned ? "Return to automatic detection" : "Mark owned manually", () ->
        {
            String id = evaluation.getUpgrade().getId();
            if (overrides.get(id) == ManualOverride.FORCE_COMPLETE) overrides.remove(id);
            else overrides.put(id, ManualOverride.FORCE_COMPLETE);
            reevaluate.run();
        });
        menu.addSeparator();
        addMenuItem(menu, "Reset all gear choices…", this::confirmReset);
        menu.show(anchor, 0, anchor.getHeight());
    }

    private static void addMenuItem(JPopupMenu menu, String text, Runnable action)
    {
        javax.swing.JMenuItem item = new javax.swing.JMenuItem(text);
        item.addActionListener(event -> action.run());
        menu.add(item);
    }

    private void setGoal(GearEvaluation evaluation)
    {
        preferences.setSelectedGoalId(evaluation.isSelectedGoal() ? null : evaluation.getUpgrade().getId());
        reevaluate.run();
    }

    private void confirmReset()
    {
        int answer = JOptionPane.showConfirmDialog(this,
            "Reset the selected goal, gear skips, optional marks, alternatives, and filters for this character?",
            "Reset Iron Compass gear choices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.OK_OPTION)
        {
            preferences.resetGearPreferences();
            detailId = null;
            reevaluate.run();
        }
    }

    private static boolean matches(GearEvaluation evaluation, String query, String style, String status)
    {
        if (!"ALL".equals(style))
        {
            boolean found = false;
            for (CombatStyle combatStyle : evaluation.getUpgrade().getStyles())
            {
                if (combatStyle.name().equals(style)) { found = true; break; }
            }
            if (!found) return false;
        }
        if (!"ALL".equals(status) && !evaluation.getStatus().name().equals(status)) return false;
        if (query.isEmpty()) return true;
        GearUpgrade upgrade = evaluation.getUpgrade();
        String source = upgrade.getSource() == null ? "" : upgrade.getSource().getActivity() + " " + upgrade.getSource().getRegion();
        String searchable = (upgrade.getName() + " " + source + " " + String.join(" ", upgrade.getTags())).toLowerCase(Locale.ENGLISH);
        return searchable.contains(query);
    }

    private static String styleText(GearUpgrade upgrade)
    {
        StringBuilder value = new StringBuilder();
        for (CombatStyle style : upgrade.getStyles())
        {
            if (value.length() > 0) value.append(" / ");
            value.append(humanize(style.name()));
        }
        return value.toString();
    }

    private static CombatStyle primaryStyle(GearUpgrade upgrade)
    {
        return upgrade.getStyles().isEmpty() ? CombatStyle.UTILITY : upgrade.getStyles().get(0);
    }

    private static String journeyGroup(GearUpgrade upgrade)
    {
        String stage = upgrade.getTier() <= 2 ? "EARLY  ·  T1–2"
            : upgrade.getTier() <= 4 ? "MIDGAME  ·  T3–4" : "LATE  ·  T5–6";
        return primaryStyle(upgrade).name() + "  ·  " + stage;
    }

    private static String priorityLabel(GearEvaluation evaluation)
    {
        if (evaluation.isSelectedGoal()) return "Selected goal";
        if (evaluation.getReadiness() == TruthValue.TRUE && evaluation.getUpgrade().getUsefulness() >= 5)
            return "High priority now";
        if (evaluation.getReadiness() == TruthValue.TRUE) return "Ready now";
        if (evaluation.getStatus() == GearStatus.OPTIONAL) return "Optional detour";
        return "Build toward this";
    }

    private static String priorityReason(GearEvaluation evaluation)
    {
        if (evaluation.getStatus() == GearStatus.OWNED)
            return "Already owned; follow the unlock chain below for the next upgrade.";
        if (evaluation.getStatus() == GearStatus.UNCONFIRMED)
            return "Ownership is unconfirmed because the bank has not been scanned. You may still select this goal manually, but Iron Compass will not recommend it automatically.";
        if (evaluation.getStatus() == GearStatus.LOCKED || evaluation.getReadiness() == TruthValue.FALSE)
        {
            if (!evaluation.getMissingReasons().isEmpty())
                return "Not now: " + escape(evaluation.getMissingReasons().get(0)) + ".";
            return "Not now: a direct requirement or prior objective is still missing.";
        }
        if (evaluation.getStatus() == GearStatus.OPTIONAL)
            return "Optional detour: useful, but it should not block the main account path.";
        if (evaluation.isSelectedGoal())
            return "Selected by you; Iron Compass will route the first unfinished dependency to Overview.";
        return "Ready and useful at this account stage; choose it to turn the objective into concrete route steps.";
    }

    private static JPanel verticalPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UiTokens.BACKGROUND);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel card(Component component)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTokens.CARD);
        if (component instanceof JPanel) component.setBackground(UiTokens.CARD);
        panel.setBorder(UiTokens.cardBorder());
        panel.add(component, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private static JLabel sectionLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(UiTokens.MUTED);
        label.setFont(UiTokens.LABEL);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel labelHtml(String body, Color color)
    {
        JLabel label = new JLabel("<html><table width='155' cellspacing='0' cellpadding='0'><tr><td>"
            + body + "</td></tr></table></html>");
        label.setForeground(color);
        label.setFont(UiTokens.BODY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel statusLine(TruthValue value, String text)
    {
        String glyph = value == TruthValue.TRUE ? "✓" : value == TruthValue.FALSE ? "×" : "?";
        Color color = value == TruthValue.TRUE ? UiTokens.SUCCESS : value == TruthValue.FALSE ? UiTokens.DANGER : UiTokens.UNKNOWN;
        return labelHtml(glyph + "  " + escape(text), color);
    }

    private static JButton smallButton(String text)
    {
        JButton button = new JButton(text);
        button.setFocusable(true);
        button.setFont(UiTokens.LABEL);
        button.setMargin(new java.awt.Insets(4, 7, 4, 7));
        return button;
    }

    private static Component gap(int height)
    {
        Box.Filler filler = new Box.Filler(new Dimension(0, height), new Dimension(0, height),
            new Dimension(Integer.MAX_VALUE, height));
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    private static Component verticalGlue()
    {
        Box.Filler filler = new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
            new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        filler.setAlignmentX(Component.LEFT_ALIGNMENT);
        return filler;
    }

    private static String statusGlyph(GearStatus status)
    {
        switch (status)
        {
            case OWNED: return "✓";
            case RECOMMENDED: return "→";
            case LOCKED: return "×";
            case UNCONFIRMED: return "?";
            case OPTIONAL: return "◇";
            case SKIPPED: return "−";
            default: return "○";
        }
    }

    private static Color statusColor(GearStatus status)
    {
        switch (status)
        {
            case OWNED: return UiTokens.SUCCESS;
            case RECOMMENDED: return UiTokens.ACCENT;
            case LOCKED: return UiTokens.DANGER;
            case UNCONFIRMED: return UiTokens.UNKNOWN;
            case OPTIONAL: return UiTokens.UNKNOWN;
            case SKIPPED: return UiTokens.MUTED;
            default: return UiTokens.TEXT;
        }
    }

    private static String statusText(GearStatus status)
    {
        return humanize(status.name()).toUpperCase(Locale.ENGLISH);
    }

    private static String detailSuffix(RequirementResult result)
    {
        return result.getDetail() == null || result.getDetail().isEmpty() ? "" : " — " + result.getDetail();
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
}
