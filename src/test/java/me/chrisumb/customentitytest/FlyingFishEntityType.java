package me.chrisumb.customentitytest;

import me.chrisumb.customentity.CustomEntityType;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

public class FlyingFishEntityType extends CustomEntityType<Bat> {

    public FlyingFishEntityType(@NotNull String id) {
        super(id, Bat.class, Salmon.class);
    }

    @Override
    public void onSpawn(@NotNull Bat entity) {
        entity.setSilent(true);
    }
}
