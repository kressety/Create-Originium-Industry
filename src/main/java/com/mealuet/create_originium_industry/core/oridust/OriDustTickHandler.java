package com.mealuet.create_originium_industry.core.oridust;

import com.mealuet.create_originium_industry.index.COIAttachments;
import com.mealuet.create_originium_industry.index.COIEffects;
import com.mealuet.create_originium_industry.CreateOriginiumIndustry;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class OriDustTickHandler {

    private static final int DIFFUSION_INTERVAL = 20; // Ticks between diffusion updates (1 second)
    private static final int EFFECT_CHECK_INTERVAL = 20; // Ticks between player effect checks (1 second)
    private static final int DUST_EFFECT_THRESHOLD = 2000; // Dust level to start applying effects
    private static final int DUST_PER_EFFECT_LEVEL = 1500; // Dust increase needed for next effect level
    private static final int MAX_EFFECT_AMPLIFIER = 4; // Max amplifier (level 5)
    private static final int INIT_CHUNK_RADIUS = 8; // Radius around players for initial cache population

    // In-memory cache of dust levels for currently loaded chunks (Thread-safe)
    private static final Map<ChunkPos, Integer> currentDustLevels = new ConcurrentHashMap<>();

    // --- Cache Management via Chunk Events ---

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Ensure we are on the server side and dealing with a LevelChunk
        if (event.getLevel() instanceof ServerLevel && event.getChunk() instanceof LevelChunk chunk) {
            // Read dust level from attachment and populate the cache
            COIAttachments.getChunkDustData(chunk).ifPresent(data -> {
                int dustLevel = data.getDustLevel();
                currentDustLevels.put(chunk.getPos(), dustLevel);
                // CreateOriginiumIndustry.LOGGER.debug("Loaded chunk {} with dust level {}, cache updated.", chunk.getPos(), dustLevel);
            });
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        // Ensure we are on the server side
        if (event.getLevel() instanceof ServerLevel) {
            ChunkAccess chunk = event.getChunk();
            // Remove chunk from cache upon unload
            if (currentDustLevels.remove(chunk.getPos()) != null) {
                 CreateOriginiumIndustry.LOGGER.debug("Unloaded chunk {}, removed from dust cache.", chunk.getPos());
            }
        }
    }

    // --- Initialization on Server Start ---
    // Needed to populate cache for chunks already loaded when the server starts
    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event) {
        // Clear any previous cache state
        currentDustLevels.clear();
        CreateOriginiumIndustry.LOGGER.info("Server starting, clearing and initializing dust cache based on player proximity.");

        // Iterate over all server levels
        event.getServer().getAllLevels().forEach(serverLevel -> {
            CreateOriginiumIndustry.LOGGER.debug("Initializing dust cache for level: {}", serverLevel.dimension().location());
            ServerChunkCache chunkSource = serverLevel.getChunkSource();

            // Iterate through online players (if any) and cache chunks around them
            serverLevel.getPlayers(player -> true).forEach(player -> {
                ChunkPos playerPos = player.chunkPosition(); // Get player's chunk position
                CreateOriginiumIndustry.LOGGER.debug("Initializing cache around player {} at {}", player.getName().getString(), playerPos);
                for (int x = -INIT_CHUNK_RADIUS; x <= INIT_CHUNK_RADIUS; x++) {
                    for (int z = -INIT_CHUNK_RADIUS; z <= INIT_CHUNK_RADIUS; z++) {
                        ChunkPos pos = new ChunkPos(playerPos.x + x, playerPos.z + z);
                        // Use getChunkNow to avoid loading chunks, only check already loaded ones
                        LevelChunk chunk = chunkSource.getChunkNow(pos.x, pos.z);
                        if (chunk != null) {
                            // Only add to cache if not already present (avoid redundant reads)
                            currentDustLevels.computeIfAbsent(pos, cp -> {
                                Optional<OriDustData> dataOpt = COIAttachments.getChunkDustData(chunk);
                                if (dataOpt.isPresent()) {
                                    int level = dataOpt.get().getDustLevel();
                                    CreateOriginiumIndustry.LOGGER.debug("Initialized chunk {} with dust level {}", cp, level);
                                    return level;
                                }
                                return 0; // Default to 0 if attachment somehow missing
                            });
                        }
                    }
                }
            });
            // Note: Spawn chunks or chunks loaded by other means might not be initialized here
            // if no players are nearby. They will be added via onChunkLoad when accessed.
            CreateOriginiumIndustry.LOGGER.debug("Finished initializing dust cache for level: {}. Cache size: {}", serverLevel.dimension().location(), currentDustLevels.size());
        });
    }


    // --- Diffusion Logic (Uses the Cache) ---

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel serverLevel && level.dimension() == Level.OVERWORLD) {
            // Execute diffusion logic periodically
            if (serverLevel.getGameTime() % DIFFUSION_INTERVAL == 0) {
                // Pass the current cache to the diffusion logic
                handleDustDiffusion(serverLevel);
            }
        }
    }

    // Diffusion logic now operates on the provided cache map
    private static void handleDustDiffusion(ServerLevel level) {
        if (OriDustTickHandler.currentDustLevels.isEmpty()) return; // Nothing to diffuse

        Map<ChunkPos, Integer> pendingUpdates = new ConcurrentHashMap<>(); // Use ConcurrentHashMap for updates too
        ServerChunkCache chunkSource = level.getChunkSource();

        // 1. Calculate diffusion based *only* on chunks in the cache
        OriDustTickHandler.currentDustLevels.forEach((pos, currentDust) -> {
            if (currentDust <= 0) return; // Skip chunks with no dust

            int neighborTotalDust = 0;
            int neighborCount = 0;
            int selfDust = currentDust;

            // Check horizontal neighbors
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                ChunkPos neighborPos = new ChunkPos(pos.x + dir.getStepX(), pos.z + dir.getStepZ());
                // Check if neighbor is also in the cache (i.e., loaded)
                int neighborDust = OriDustTickHandler.currentDustLevels.getOrDefault(neighborPos, 0);
                // We only consider loaded neighbors for diffusion calculation
                if (OriDustTickHandler.currentDustLevels.containsKey(neighborPos)) {
                    neighborTotalDust += neighborDust;
                    neighborCount++;
                }
            }

            if (neighborCount > 0) {
                int totalSystemDust = selfDust + neighborTotalDust;
                int averageDust = totalSystemDust / (neighborCount + 1);
                int dustToMovePerNeighbor = Math.max(0, (selfDust - averageDust) / (neighborCount * 2 + 2));

                int totalDustLeaving = 0;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    ChunkPos neighborPos = new ChunkPos(pos.x + dir.getStepX(), pos.z + dir.getStepZ());
                    // Only add update if neighbor is loaded (in cache)
                    if (OriDustTickHandler.currentDustLevels.containsKey(neighborPos)) {
                        int neighborDust = OriDustTickHandler.currentDustLevels.getOrDefault(neighborPos, 0);
                        if (selfDust > neighborDust && dustToMovePerNeighbor > 0) {
                            pendingUpdates.merge(neighborPos, dustToMovePerNeighbor, Integer::sum);
                            totalDustLeaving += dustToMovePerNeighbor;
                        }
                    }
                }
                if (totalDustLeaving > 0) {
                    pendingUpdates.merge(pos, -totalDustLeaving, Integer::sum);
                }
            }
        });

        // 2. Apply updates to cache AND persistent attachment data
        pendingUpdates.forEach((pos, dustChange) -> {
            // Get the chunk (it *should* be loaded if it's receiving updates, but check anyway)
            LevelChunk chunk = chunkSource.getChunkNow(pos.x, pos.z);
            if (chunk != null) {
                // Get the original level from the cache for accurate calculation
                int originalLevel = OriDustTickHandler.currentDustLevels.getOrDefault(pos, 0); // Default to 0 if somehow missing
                int newLevel = originalLevel + dustChange;
                if (newLevel < 0) newLevel = 0; // Clamp at zero

                // Update the cache *first*
                OriDustTickHandler.currentDustLevels.put(pos, newLevel); // Update cache directly

                // Update the persistent attachment data
                Optional<OriDustData> dataOpt = COIAttachments.getChunkDustData(chunk);
                if(dataOpt.isPresent()) {
                    OriDustData data = dataOpt.get();
                    if (data.getDustLevel() != newLevel) { // Check if persistent data needs update
                        data.setDustLevel(newLevel);
                        chunk.setUnsaved(true); // Mark chunk dirty *only* if persistent data changed
                        // CreateOriginiumIndustry.LOGGER.debug("Applied diffusion to chunk {}. Change: {}. New level: {}", pos, dustChange, newLevel);
                    }
                } else {
                    CreateOriginiumIndustry.LOGGER.warn("Failed to get OriDustData attachment for chunk {} during diffusion update.", pos);
                }
            } else {
                CreateOriginiumIndustry.LOGGER.warn("Chunk {} became unloaded during diffusion update processing.", pos);
                // Remove from cache if it became unloaded mid-tick? Or rely on Unload event?
                // For now, we just won't update the attachment. The cache might be slightly out of sync until unload.
                OriDustTickHandler.currentDustLevels.remove(pos); // Remove from cache if unloaded now
            }
        });
    }

    // --- Player Effect Logic (Reads directly from Attachment) ---
    // Player effect logic can still read directly from the chunk attachment,
    // as it only needs the value for the player's *current* chunk, which must be loaded.
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && !serverPlayer.level().isClientSide) {
            if (serverPlayer.tickCount % EFFECT_CHECK_INTERVAL == 0) {
                LevelChunk chunk = serverPlayer.level().getChunkAt(serverPlayer.blockPosition());
                // Read directly from the attachment for the player's current chunk
                COIAttachments.getChunkDustData(chunk).ifPresent(data -> {
                    int dustLevel = data.getDustLevel();
                    if (dustLevel > DUST_EFFECT_THRESHOLD) {
                        int amplifier = Math.min(MAX_EFFECT_AMPLIFIER, (dustLevel - DUST_EFFECT_THRESHOLD) / DUST_PER_EFFECT_LEVEL);
                        serverPlayer.addEffect(new MobEffectInstance(COIEffects.ORI_DUST_SICKNESS_EFFECT, EFFECT_CHECK_INTERVAL + 5, amplifier, true, false, true));
                    }
                });
            }
        }
    }
}
