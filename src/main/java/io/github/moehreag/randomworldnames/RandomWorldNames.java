package io.github.moehreag.randomworldnames;

import javax.naming.LimitExceededException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomWorldNames implements ClientModInitializer {
	private static final Logger log = LoggerFactory.getLogger("RandomWorldNames");
	private static final Identifier NAME_LOCATION = Identifier.of("random-world-names", "names.json");
	private static final Gson GSON = new GsonBuilder().create();
	public static final Random random = Random.create();
	private static final List<String> worldNames = new ArrayList<>();
	private static final int nameLength = 3;
	private static double maxCombinations;
	private static final String delimiter = " ";

	@Getter
	private static RandomWorldNames instance;

	@Override
	public void onInitializeClient() {
		instance = this;
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
				.registerReloadListener(new SimpleResourceReloadListener<List<String>>() {
					@Override
					public Identifier getFabricId() {
						return Identifier.of("random-world-names", "name-reloader");
					}

					@Override
					public CompletableFuture<List<String>> load(ResourceManager resourceManager, Executor executor) {
						return CompletableFuture.supplyAsync(() -> resourceManager.getAllResources(NAME_LOCATION)
								.stream().map(resource -> {
									try {
										return GSON.fromJson(resource.getReader(), String[].class);
									} catch (IOException e) {
										log.warn("Failed to load world names from {}: ", resource.getPackId(), e);
										return null;
									}
								}).filter(Objects::nonNull)
								.flatMap(Arrays::stream)
								.toList(), executor);
					}

					@Override
					public CompletableFuture<Void> apply(List<String> o, ResourceManager resourceManager, Executor executor) {
						return CompletableFuture.runAsync(() -> {
							worldNames.addAll(o);
							maxCombinations = Math.pow(nameLength, o.size());
							log.info("Loaded {} names for random world names!", o.size());
						}, executor);
					}
				});
	}

	public <T> T getRandomWorldName(Supplier<T> fallback, Function<String, T> converter) {
		try {
			return converter.apply(generateRandomName());
		} catch (TimeoutException e) {
			log.info("Using default world name as generation could not determine an unused name within the time limit!");
		} catch (Exception e) {
			log.warn("Failed to generate random world name: ", e);
		}
		return fallback.get();
	}

	public String getRandomWorldName(Supplier<String> fallback) {
		return getRandomWorldName(fallback, Function.identity());
	}

	private String generateRandomName() throws TimeoutException, LimitExceededException {
		String[] names = new String[nameLength];
		AtomicBoolean timeout = new AtomicBoolean();
		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException ignored) {
			}
			timeout.set(true);
		});
		int limit = worldNames.size() - 1;
		for (int total = 0; total < maxCombinations; total++) {
			if (timeout.get()) {
				throw new TimeoutException("Generation timeout reached.");
			}
			for (int i = 0; i < names.length; i++) {
				String name;
				do {
					name = worldNames.get(random.nextBetween(0, limit));
				} while (ArrayUtils.contains(names, name));
				names[i] = name;
			}
			for (int i = 0, length = names.length; i < length; i++) {
				names[i] = StringUtils.capitalize(names[i]);
			}

			String name = String.join(delimiter, names);
			if (Files.isDirectory(MinecraftClient.getInstance().getLevelStorage().getSavesDirectory().resolve(name))) {
				continue;
			}
			return name;
		}
		throw new LimitExceededException("Do you really need " + maxCombinations + " worlds? You should reconsider your life choices.");
	}
}
