package skylands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import skylands.SkylandsMod;
import skylands.data.SkylandsComponents;
import skylands.logic.Skylands;
import skylands.util.SkylandsTexts;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HomeCommand {

	static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("sl").then(literal("home").requires(Permissions.require("skylands.home", true)).executes(context -> {
			var player = context.getSource().getPlayer();
			if(player != null) {
				HomeCommand.run(player);
			}
			return 1;
		})));
		dispatcher.register(literal("sl").then(literal("home").requires(Permissions.require("skylands.home", true)).then(argument("player", word()).suggests((context, builder) -> {
			var player = context.getSource().getPlayer();

			if(player != null) {
				var islands = SkylandsComponents.PLAYER_DATA.get(player).getIslands();

				String remains = builder.getRemaining();

				for(String ownerName : islands) {
					if(ownerName.contains(remains)) {
						builder.suggest(ownerName);
					}
				}
				return builder.buildFuture();
			}
			return builder.buildFuture();
		}).executes(context -> {
			var ownerName = StringArgumentType.getString(context, "player");
			var visitor = context.getSource().getPlayer();
			if(visitor != null) {
				HomeCommand.run(visitor, ownerName);
			}
			return 1;
		}))));
	}

	public static void run(ServerPlayerEntity player) {
		Skylands.instance.islands.get(player).ifPresentOrElse(island -> {
			if(player.getWorld().getRegistryKey().getValue().equals(SkylandsMod.id(player.getUuid().toString()))) {
				player.sendMessage(SkylandsTexts.prefixed("message.skylands.home.fail"));
			}
			else {
				player.sendMessage(SkylandsTexts.prefixed("message.skylands.home.success"));
				island.visitAsMember(player);
			}
		}, () -> player.sendMessage(SkylandsTexts.prefixed("message.skylands.home.no_island")));
	}

	public static void run(ServerPlayerEntity visitor, String islandOwner) {
		Skylands.instance.islands.get(islandOwner).ifPresentOrElse(island -> {
			if(visitor.getWorld().getRegistryKey().getValue().equals(SkylandsMod.id(island.owner.uuid.toString()))) {
				visitor.sendMessage(SkylandsTexts.prefixed("message.skylands.visit_home.fail", map -> map.put("%owner%", islandOwner)));
			}
			else {
				if(island.isMember(visitor)) {
					visitor.sendMessage(SkylandsTexts.prefixed("message.skylands.visit_home.success", map -> map.put("%owner%", islandOwner)));
					island.visitAsMember(visitor);
					SkylandsComponents.PLAYER_DATA.get(visitor).addIsland(islandOwner);
				}
				else {
					visitor.sendMessage(SkylandsTexts.prefixed("message.skylands.visit_home.not_member"));
					SkylandsComponents.PLAYER_DATA.get(visitor).removeIsland(islandOwner);
				}
			}
		}, () -> {
			visitor.sendMessage(SkylandsTexts.prefixed("message.skylands.visit_home.no_island"));
			SkylandsComponents.PLAYER_DATA.get(visitor).removeIsland(islandOwner);
		});
	}
}
