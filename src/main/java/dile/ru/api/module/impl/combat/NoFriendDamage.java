package dile.ru.api.module.impl.combat;

import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.InteractEntityEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.utils.repository.friend.FriendUtils;

public class NoFriendDamage extends Module {
    public NoFriendDamage() {
        super("No Friend Damage", "Blocks attacks on friends.", ModuleCategory.COMBAT);
    }

    @SubscribeEvent
    private void onInteract(InteractEntityEvent event) {
        if (FriendUtils.isFriend(event.getEntity())) {
            event.cancel();
        }
    }
}
