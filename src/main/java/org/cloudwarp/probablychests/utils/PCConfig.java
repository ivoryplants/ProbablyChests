package org.cloudwarp.probablychests.utils;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import org.cloudwarp.probablychests.ProbablyChests;
import static org.cloudwarp.probablychests.utils.MimicDifficulty.*;

@Config(name = ProbablyChests.MOD_ID)
public class PCConfig implements ConfigData {


	@ConfigEntry.Gui.CollapsibleObject
	public MimicSettings mimicSettings = new MimicSettings();
	@ConfigEntry.Gui.CollapsibleObject
	public WorldGen worldGen = new WorldGen();

	public static class MimicSettings {
		@ConfigEntry.Gui.Tooltip()
		public MimicDifficulty mimicDifficulty = MEDIUM;
		@ConfigEntry.Gui.Tooltip()
		public boolean spawnNaturalMimics = true;
		@ConfigEntry.Gui.Tooltip()
		public float naturalMimicSpawnRate = 0.95F;
		@ConfigEntry.Gui.Tooltip()
		public boolean allowPetMimics = true;
		@ConfigEntry.Gui.Tooltip()
		public boolean doPetMimicLimit = false;
		@ConfigEntry.Gui.Tooltip()
		public int petMimicLimit = 2;
		@ConfigEntry.Gui.Tooltip()
		public int abandonedMimicTimer = 5;
	}

	public static class WorldGen {
		@ConfigEntry.Gui.Tooltip()
		public float potSpawnChance = 0.40F;
		@ConfigEntry.Gui.Tooltip()
		public float chestSpawnChance = 0.37F;
		@ConfigEntry.Gui.Tooltip()
		public float surfaceChestSpawnChance = 0.02F;
		@ConfigEntry.Gui.Tooltip()
		public float secretMimicChance = 0.25F;
	}


}
