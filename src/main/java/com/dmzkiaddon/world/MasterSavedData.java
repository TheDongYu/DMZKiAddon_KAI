package com.dmzkiaddon.world;

import com.dmzkiaddon.DMZKiAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class MasterSavedData extends SavedData {

    private static final String KEY = DMZKiAddon.MOD_ID + "_masters";

    private boolean spawned    = false;
    private BlockPos vegetaPos  = null;
    private BlockPos piccoloPos = null;
    private BlockPos friezaPos  = null;

    // ── Factory ────────────────────────────────────────────────────────────

    public static MasterSavedData getOrCreate(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                MasterSavedData::load,
                MasterSavedData::new,
                KEY
        );
    }

    // ── Serialization ──────────────────────────────────────────────────────

    public static MasterSavedData load(CompoundTag tag) {
        MasterSavedData d = new MasterSavedData();
        d.spawned = tag.getBoolean("spawned");
        if (tag.contains("vegetaX"))  d.vegetaPos  = new BlockPos(tag.getInt("vegetaX"),  tag.getInt("vegetaY"),  tag.getInt("vegetaZ"));
        if (tag.contains("piccoloX")) d.piccoloPos = new BlockPos(tag.getInt("piccoloX"), tag.getInt("piccoloY"), tag.getInt("piccoloZ"));
        if (tag.contains("friezaX"))  d.friezaPos  = new BlockPos(tag.getInt("friezaX"),  tag.getInt("friezaY"),  tag.getInt("friezaZ"));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("spawned", spawned);
        if (vegetaPos  != null) { tag.putInt("vegetaX",  vegetaPos.getX());  tag.putInt("vegetaY",  vegetaPos.getY());  tag.putInt("vegetaZ",  vegetaPos.getZ()); }
        if (piccoloPos != null) { tag.putInt("piccoloX", piccoloPos.getX()); tag.putInt("piccoloY", piccoloPos.getY()); tag.putInt("piccoloZ", piccoloPos.getZ()); }
        if (friezaPos  != null) { tag.putInt("friezaX",  friezaPos.getX());  tag.putInt("friezaY",  friezaPos.getY());  tag.putInt("friezaZ",  friezaPos.getZ()); }
        return tag;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public boolean isSpawned()               { return spawned; }
    public void    setSpawned(boolean v)     { this.spawned = v; }

    public BlockPos getVegetaPos()           { return vegetaPos; }
    public void     setVegetaPos(BlockPos p) { this.vegetaPos = p; }

    public BlockPos getPiccoloPos()           { return piccoloPos; }
    public void     setPiccoloPos(BlockPos p) { this.piccoloPos = p; }

    public BlockPos getFriezaPos()           { return friezaPos; }
    public void     setFriezaPos(BlockPos p) { this.friezaPos = p; }
}