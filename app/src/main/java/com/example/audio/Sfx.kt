package com.example.audio

/**
 * Every interactive action maps to a unique soft sound fingerprint.
 * Recipes are subtle, sweet, slow, and low — pure sine, warm midrange.
 */
enum class Sfx {
    /** Soft pillow tap — generic light press */
    TAP_SOFT,

    /** Gentle glass click — chips, filters, secondary toggles */
    TAP_CRISP,

    /** Warm confirm hum — primary buttons / save */
    TAP_CONFIRM,

    /** Slow airy open breath — dialogs open */
    DIALOG_OPEN,

    /** Soft settle close — dialogs dismiss */
    DIALOG_CLOSE,

    /** Sweet rising third — create / add task */
    ADD_TASK,

    /** Warm two-tone — create budget transaction */
    ADD_BUDGET,

    /** Gentle arpeggio — create calendar event */
    ADD_EVENT,

    /** Soft sparkle triad — create goal */
    ADD_GOAL,

    /** Low bank-soft blip — create account */
    ADD_ACCOUNT,

    /** Satisfying soft major resolve — task marked done */
    TASK_COMPLETE,

    /** Soft undo sigh — task unchecked */
    TASK_UNDO,

    /** Tiny subtask tick — checkbox sub-item */
    SUBTASK_TOGGLE,

    /** Soft paper hush — delete anything */
    DELETE,

    /** Filter select murmur */
    FILTER_SELECT,

    /** Soft FAB bloom */
    FAB,

    /** Tab pad tone — pitch offset applied at play */
    TAB_SWITCH,

    /** Profile sheet lift */
    PROFILE_OPEN,

    /** Profile save soft chime */
    PROFILE_SAVE,

    /** Theme cascade */
    THEME_CHANGE,

    /** Settings soft tick */
    SETTINGS_CHANGE,

    /** Focus start breath */
    FOCUS_START,

    /** Focus pause hold */
    FOCUS_PAUSE,

    /** Focus reset settle */
    FOCUS_RESET,

    /** Focus complete warm resolve */
    FOCUS_COMPLETE,

    /** Goal progress bump */
    GOAL_PROGRESS,

    /** Goal fully completed */
    GOAL_COMPLETE,

    /** Calendar day select */
    DAY_SELECT,

    /** Event toggled complete */
    EVENT_TOGGLE,

    /** Quick note saved */
    NOTE_SAVE,

    /** Search field focus */
    SEARCH_FOCUS,

    /** Success micro chime */
    SUCCESS,

    /** Soft error murmur */
    ERROR,

    /** Slow sweet startup chime — splash intro */
    SPLASH_INTRO
}
