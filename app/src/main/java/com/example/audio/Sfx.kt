package com.example.audio

/**
 * Every interactive action maps to a unique sound fingerprint.
 * No two values share the same synthesis recipe.
 */
enum class Sfx {
    /** Soft wooden tick — generic light press */
    TAP_SOFT,

    /** Crisp UI click — chips, filters, secondary toggles */
    TAP_CRISP,

    /** Deeper confirm — primary buttons / save */
    TAP_CONFIRM,

    /** Airy open whoosh — dialogs open */
    DIALOG_OPEN,

    /** Soft reverse whoosh — dialogs dismiss */
    DIALOG_CLOSE,

    /** Rising chime — create / add task */
    ADD_TASK,

    /** Warm double-ping — create budget transaction */
    ADD_BUDGET,

    /** Bright arpeggio up — create calendar event */
    ADD_EVENT,

    /** Sparkly triad — create goal */
    ADD_GOAL,

    /** Bank-card blip — create account */
    ADD_ACCOUNT,

    /** Satisfying major chord — task marked done */
    TASK_COMPLETE,

    /** Soft undo thud — task unchecked */
    TASK_UNDO,

    /** Subtask tick — checkbox sub-item */
    SUBTASK_TOGGLE,

    /** Paper tear — delete anything */
    DELETE,

    /** Swipe filter select */
    FILTER_SELECT,

    /** Floating action button pop */
    FAB,

    /** Tab navigation — base; pitch offset applied at play */
    TAB_SWITCH,

    /** Profile sheet open */
    PROFILE_OPEN,

    /** Profile saved */
    PROFILE_SAVE,

    /** Theme swatch selected */
    THEME_CHANGE,

    /** Currency / settings change */
    SETTINGS_CHANGE,

    /** Focus timer start */
    FOCUS_START,

    /** Focus timer pause */
    FOCUS_PAUSE,

    /** Focus timer reset */
    FOCUS_RESET,

    /** Focus session finished */
    FOCUS_COMPLETE,

    /** Goal progress bump */
    GOAL_PROGRESS,

    /** Goal fully completed */
    GOAL_COMPLETE,

    /** Calendar day selected */
    DAY_SELECT,

    /** Event toggled complete */
    EVENT_TOGGLE,

    /** Quick note saved */
    NOTE_SAVE,

    /** Search field focus */
    SEARCH_FOCUS,

    /** Success fanfare micro */
    SUCCESS,

    /** Gentle error / invalid */
    ERROR
}
