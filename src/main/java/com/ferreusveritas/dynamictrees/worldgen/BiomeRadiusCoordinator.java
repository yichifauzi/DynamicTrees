package com.ferreusveritas.dynamictrees.worldgen;

import java.util.Random;

import com.ferreusveritas.dynamictrees.api.worldgen.IRadiusCoordinator;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

public class BiomeRadiusCoordinator implements IRadiusCoordinator {
	
	public NoiseGeneratorPerlin noiseGenerator;
	protected final TreeGenerator treeGenerator;
	protected final World world;
	protected int packing;
	
	public BiomeRadiusCoordinator(TreeGenerator treeGenerator, World world) {
		noiseGenerator = new NoiseGeneratorPerlin(new Random(96), 1);
		this.world = world;
		this.treeGenerator = treeGenerator;
	}
	
	@Override
	public int getRadiusAtCoords(int x, int z) {
		
		switch(packing) {
			case 1: return 5;
			case 2: return 3;
		}
		
		double scale = 128;//Effectively scales up the noisemap
		Biome biome = world.getBiome(new BlockPos(x + 8, 0, z + 8));//Placement is offset by +8,+8
		double noiseDensity = (noiseGenerator.getValue(x / scale, z / scale) + 1D) / 2.0D;//Gives 0.0 to 1.0
		double density = treeGenerator.getBiomeDataBase(world).getDensity(biome).getDensity(world.rand, noiseDensity);
		double size = ((1.0 - density) * 9);//Size is the inverse of density(Gives 0 to 9)
		
		//Oh Joy. Random can potentially start with the same number for each chunk. Let's just 
		//throw this large prime xor hack in there to get it to at least look like it's random.
		int kindaRandom = ((x * 674365771) ^ (z * 254326997)) >> 4;
		int shakelow =  (kindaRandom & 0x3) % 3;//Produces 0,0,1 or 2
		int shakehigh = (kindaRandom & 0xc) % 3;//Produces 0,0,1 or 2
		
		return MathHelper.clamp((int) size, 2 + shakelow, 8 - shakehigh);//Clamp to tree volume radius range
	}
	
	@Override
	public boolean setPacking(int chunkX, int chunkZ, int iteration) {
		packing = iteration;
		
		Biome biome = world.getBiome(new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8));//Aim at center of chunk

		if(treeGenerator.getBiomeDataBase(world).isOverpacked(biome)) {
			return packing < 3;
		}
		
		return packing == 0;
	}
	
}
