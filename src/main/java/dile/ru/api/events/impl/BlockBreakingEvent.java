package dile.ru.api.events.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dile.ru.api.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {
}
