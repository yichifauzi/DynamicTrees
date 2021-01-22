package com.ferreusveritas.dynamictrees.event;

import com.ferreusveritas.dynamictrees.DynamicTrees;
import com.ferreusveritas.dynamictrees.systems.poissondisc.PoissonDiscProviderUniversal;
import com.ferreusveritas.dynamictrees.worldgen.TreeGenerator;
import net.minecraft.nbt.ByteArrayNBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PoissonDiscEventHandler {

	public static final String CIRCLE_DATA_ID = "GTCD"; // ID for "Growing Trees Circle Data" NBT tag.

	/** This piece of crap event will not fire until after PLENTY of chunks have already generated when creating a new world.  WHY!? */
	/*@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {}*/

	/** We'll use this instead because at least new chunks aren't created after the world is unloaded. I hope. >:( */
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event) {
        IWorld world = event.getWorld();
		if(!world.isRemote()) {
			TreeGenerator.getTreeGenerator().getCircleProvider().unloadWorld(world);//clears the circles
		}
	}

	@SubscribeEvent
	public void onChunkDataLoad(ChunkDataEvent.Load event) {
        IWorld world = event.getWorld();
		if (world != null && !world.isRemote()) {
			byte[] circleData = event.getData().getByteArray(CIRCLE_DATA_ID);
			PoissonDiscProviderUniversal cp = TreeGenerator.getTreeGenerator().getCircleProvider();

			final ChunkPos chunkPos = event.getChunk().getPos();
			cp.setChunkPoissonData(world, chunkPos.x, 0, chunkPos.z, circleData);
		}
	}

	@SubscribeEvent
	public void onChunkDataSave(ChunkDataEvent.Save event) {
		IWorld world = event.getWorld();
		PoissonDiscProviderUniversal cp = TreeGenerator.getTreeGenerator().getCircleProvider();
        final ChunkPos chunkPos = event.getChunk().getPos();
        byte[] circleData = cp.getChunkPoissonData(world, chunkPos.x, 0, chunkPos.z);
		event.getData().putByteArray(CIRCLE_DATA_ID, circleData);//Growing Trees Circle Data
	}

	@SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        final ChunkPos chunkPos = event.getChunk().getPos();
        TreeGenerator.getTreeGenerator().getCircleProvider().unloadChunkPoissonData(event.getWorld(), chunkPos.x, 0, chunkPos.z);
    }

}
