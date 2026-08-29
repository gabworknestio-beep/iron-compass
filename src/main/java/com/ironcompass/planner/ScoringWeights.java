package com.ironcompass.planner;

/** Central ranking constants. Scores remain internal and are never presented as account truth. */
public final class ScoringWeights
{
    public static final int ACTIVE_GOAL = 150;
    public static final int ACTIVE_INTENT_SYNERGY = 12;
    public static final int DIRECT_RELATIONSHIP = 16;
    public static final int ACCOUNT_NEED_WEAK = 28;
    public static final int ACCOUNT_NEED_DEVELOPING = 12;
    public static final int STAGE_MISMATCH = 24;
    public static final int HCIM_DANGEROUS = 45;
    public static final int HCIM_WILDERNESS = 140;
    public static final int UIM_BANK_HEAVY = 24;
    public static final int MAX_SHARED_GOAL_SYNERGY = 76;
    public static final int QUICK_WIN_WEAK_NEED = 24;
    public static final int QUICK_WIN_DEVELOPING_NEED = 10;
    public static final int QUICK_WIN_STAGE_MATCH = 12;
    public static final int QUICK_WIN_ACTIVE_SYNERGY = 18;

    private ScoringWeights() { }
}
