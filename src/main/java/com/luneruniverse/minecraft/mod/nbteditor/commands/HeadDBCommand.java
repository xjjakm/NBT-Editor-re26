package com.luneruniverse.minecraft.mod.nbteditor.commands;

import static com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.ClientCommandManager.literal;

import java.util.concurrent.TimeUnit;

import java.util.function.Consumer;

import com.luneruniverse.minecraft.mod.nbteditor.commands.get.GetHdbCommand;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.TextInst;
import com.luneruniverse.minecraft.mod.nbteditor.multiversion.commands.FabricClientCommandSource;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import tsp.headdb.ported.HeadAPI;
import tsp.headdb.ported.HeadDatabase;

public class HeadDBCommand extends ClientCommand {

	public static final HeadDBCommand INSTANCE = new HeadDBCommand();

	@Override
	public String getName() {
		return "headdb";
	}

	@Override
	public String getExtremeAlias() {
		return "hd";
	}

	@Override
	public void registerAll(Consumer<LiteralArgumentBuilder<FabricClientCommandSource>> commandHandler, String path) {
		super.registerAll(commandHandler, path);
		LiteralArgumentBuilder<FabricClientCommandSource> builder = literal("hdb");
		register(builder, path);
		commandHandler.accept(builder);
	}

	@Override
	public void register(LiteralArgumentBuilder<FabricClientCommandSource> builder, String path) {
		GetHdbCommand.registerHdbSubCommands(builder);
		builder.then(literal("help").executes(context -> {
					sendHelp(context.getSource());
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("gui").executes(context -> {
					if (HeadAPI.checkUpdated())
						HeadAPI.openDatabase();
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("info").executes(context -> sendInfo(context.getSource())))
				.executes(context -> {
					sendHelp(context.getSource());
					return Command.SINGLE_SUCCESS;
				});
	}

	private static void sendHelp(FabricClientCommandSource source) {
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_1"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_2"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_3"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_4"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_5"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_6"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_7"));
		source.sendFeedback(TextInst.translatable("nbteditor.hdb.help_8"));
	}

	private static int sendInfo(FabricClientCommandSource source) {
		HeadDatabase db = HeadAPI.getDatabase();

		if (db.isLoading()) {
			source.sendFeedback(TextInst.translatable("nbteditor.hdb.info.loading",
					db.getLoadedCategories(), db.getTotalCategories(), db.getCurrentCategory()));
		} else {
			if (db.getLoadedHeadCount() > 0) {
				source.sendFeedback(TextInst.translatable("nbteditor.hdb.info.loaded",
						db.getLoadedHeadCount(), db.getLoadedCategories(), db.getTotalCategories()));
				source.sendFeedback(TextInst.translatable("nbteditor.hdb.info.cache_left",
						TimeUnit.SECONDS.toMinutes(db.getTimeUntilLastUpdateOld()),
						db.getTimeUntilLastUpdateOld() % 60));
			} else if (db.wasLastLoadFailed()) {
				source.sendFeedback(TextInst.translatable("nbteditor.hdb.info.failed", db.getCurrentCategory()));
			} else {
				source.sendFeedback(TextInst.translatable("nbteditor.hdb.info.not_loaded"));
			}
		}

		long duration = db.getLastLoadDuration();
		if (duration >= 0)
			source.sendFeedback(TextInst.translatable("nbteditor.hdb.info.load_time", String.valueOf(duration / 1000.0)));

		return Command.SINGLE_SUCCESS;
	}

}