package dile.ru.mixin.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
    @Accessor("level")
    void dile$setLevel(ClientLevel level);

    @Accessor("levelData")
    void dile$setLevelData(ClientLevel.ClientLevelData levelData);

    @Accessor("levelData")
    ClientLevel.ClientLevelData dile$getLevelData();

    @Accessor("serverChunkRadius")
    void dile$setServerChunkRadius(int serverChunkRadius);

    @Accessor("serverChunkRadius")
    int dile$getServerChunkRadius();

    @Accessor("serverSimulationDistance")
    void dile$setServerSimulationDistance(int serverSimulationDistance);

    @Accessor("serverSimulationDistance")
    int dile$getServerSimulationDistance();

    @Accessor("levels")
    void dile$setLevels(Set<ResourceKey<Level>> levels);
}
