package dile.ru.mixin;

import dile.ru.UserDileAccess;
import net.minecraft.client.User;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(User.class)
public abstract class UserDileMixin implements UserDileAccess {
    @Shadow @Mutable
    private String name;

    @Override
    public void dile$setName(String name) {
        this.name = name;
    }
}
