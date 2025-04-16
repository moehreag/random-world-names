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
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.axolotlclient.AxolotlClientConfig.api.AxolotlClientConfig;
import io.github.axolotlclient.AxolotlClientConfig.api.options.OptionCategory;
import io.github.axolotlclient.AxolotlClientConfig.impl.managers.JsonConfigManager;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.IntegerOption;
import io.github.axolotlclient.AxolotlClientConfig.impl.options.StringOption;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomWorldNames implements ClientModInitializer {
	private static final Logger log = LoggerFactory.getLogger("RandomWorldNames");
	private static final String MODID = "random-world-names";
	private static final ResourceLocation BLACKLIST_LOCATION = rl("blacklist.json");
	private static final ResourceLocation NAME_LOCATION = rl("names.json");
	private static final Gson GSON = new GsonBuilder().create();
	public static final RandomSource random = RandomSource.create();
	private static final List<String> worldNames = new ArrayList<>();
	private static final IntegerOption nameLength = new IntegerOption(optionName("name_length"), 3, 1, 6);
	private static final StringOption delimiter = new StringOption(optionName("delimiter"), " ");
	private static final IntegerOption timeout = new IntegerOption(optionName("timeout"), 2, 1, 10);

	@Getter
	private static RandomWorldNames instance;

	public static ResourceLocation rl(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	private static String optionName(String name) {
		return MODID.replace("-", "_") + "." + name;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		var category = OptionCategory.create(MODID);
		category.add(nameLength, delimiter, timeout);
		delimiter.setMaxLength(10);
		var configManager = new JsonConfigManager(FabricLoader.getInstance().getConfigDir().resolve(MODID + ".json"), category);
		AxolotlClientConfig.getInstance().register(configManager);
		configManager.load();
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
				.registerReloadListener(new SimpleResourceReloadListener<List<String>>() {
					@Override
					public ResourceLocation getFabricId() {
						return rl("name-reloader");
					}

					@Override
					public CompletableFuture<List<String>> load(ResourceManager resourceManager, Executor executor) {
						return CompletableFuture.supplyAsync(() -> {
							var blacklist = resourceManager.getResourceStack(BLACKLIST_LOCATION)
									.stream().map(resource -> {
										try {
											return GSON.fromJson(resource.openAsReader(), String[].class);
										} catch (IOException e) {
											log.warn("Failed to load world names from {}: ", resource.sourcePackId(), e);
											return null;
										}
									}).filter(Objects::nonNull)
									.flatMap(Arrays::stream)
									.toList();
							return resourceManager.getResourceStack(NAME_LOCATION)
									.stream().map(resource -> {
										if (blacklist.contains(resource.sourcePackId())) {
											log.info("Skipping names from blacklisted pack: {}", resource.sourcePackId());
											return null;
										}
										try {
											return GSON.fromJson(resource.openAsReader(), String[].class);
										} catch (IOException e) {
											log.warn("Failed to load world names from {}: ", resource.sourcePackId(), e);
											return null;
										}
									}).filter(Objects::nonNull)
									.flatMap(Arrays::stream)
									.toList();
						}, executor);
					}

					@Override
					public CompletableFuture<Void> apply(List<String> o, ResourceManager resourceManager, Executor executor) {
						return CompletableFuture.runAsync(() -> {
							worldNames.addAll(o);
							log.info("Loaded {} names for random world names!", o.size());
						}, executor);
					}
				});
	}

	public <T> T getRandomWorldName(Supplier<T> fallback, Function<String, T> converter) {
		try {
			String name = generateRandomName();
			if (name != null) {
				return converter.apply(name);
			}
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
		String[] names = new String[nameLength.get()];
		AtomicBoolean timeoutReached = new AtomicBoolean();
		int limit = worldNames.size() - 1;
		if (limit < 0) {
			return null;
		}
		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(timeout.get()*1000);
			} catch (InterruptedException ignored) {
			}
			timeoutReached.set(true);
		});
		double maxCombinations = Math.pow(nameLength.get(), limit+1);
		for (int total = 0; total < maxCombinations; total++) {
			if (timeoutReached.get()) {
				throw new TimeoutException("Generation timeout reached.");
			}
			for (int i = 0; i < names.length; i++) {
				String name;
				do {
					name = worldNames.get(random.nextInt(0, limit));
				} while (ArrayUtils.contains(names, name));
				names[i] = name;
				// The world name text field has a (default) limit of 32 characters
				if (Arrays.stream(names).filter(Objects::nonNull).collect(Collectors.joining(delimiter.get())).length() > 32) {
					log.info("Got {} but the string would be too long, stopping at {} out of {}", name, i, names.length);
					Arrays.fill(names, i, names.length, "");
					break;
				}
			}
			for (int i = 0, length = names.length; i < length; i++) {
				names[i] = StringUtils.capitalize(names[i]);
			}

			String name = Arrays.stream(names).filter(Objects::nonNull).collect(Collectors.joining(delimiter.get())).trim();
			if (Files.isDirectory(Minecraft.getInstance().getLevelSource().getBaseDir().resolve(name))) {
				continue;
			}
			return name;
		}
		throw new LimitExceededException("Do you really need " + maxCombinations + " worlds? You should reconsider your life choices.");
	}
}
