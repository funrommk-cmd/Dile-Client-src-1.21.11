package dile.ru.api.module;

public enum ModuleCategory {
    COMBAT("Combat", "N"),
    MOVEMENT("Movement", "M"),
    VISUAL("Visual", "O"),
    PLAYER("Player", "P"),
    MISC("Misc", "Q"),
    AUTO_BUY("Auto Buy", "B");

    private final String displayName;
    private final String icon;

    ModuleCategory(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String icon() {
        return icon;
    }
}
