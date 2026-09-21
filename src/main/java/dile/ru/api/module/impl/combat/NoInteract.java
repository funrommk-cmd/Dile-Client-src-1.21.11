package dile.ru.api.module.impl.combat;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;

public class NoInteract extends Module {
    public NoInteract() {
        super("No Interact", "Blocks unwanted interactions.", ModuleCategory.COMBAT);
    }
}
