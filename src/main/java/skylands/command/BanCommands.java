package skylands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import skylands.logic.Member;
import skylands.logic.Skylands;
import skylands.util.SkylandsTexts;
import skylands.util.SkylandsWorlds;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BanCommands {

	static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("sl").then(literal("ban").requires(Permissions.require("skylands.ban", true)).then(argument("player", player()).executes(context -> {
			var player = context.getSource().getPlayer();
			var bannedPlayer = EntityArgumentType.getPlayer(context, "player");
			if(player != null && bannedPlayer != null) {
				BanCommands.ban(player, bannedPlayer);
			}
			return 1;
		}))));

		dispatcher.register(literal("sl").then(literal("unban").then(argument("player", word()).suggests((context, builder) -> {
			var player = context.getSource().getPlayer();

			if(player != null) {
				var island = Skylands.instance.islands.get(player);
				if(island.isPresent()) {
					var bans = island.get().bans;

					String remains = builder.getRemaining();

					for(var member : bans) {
						if(member.name.contains(remains)) {
							builder.suggest(member.name);
						}
					}
					return builder.buildFuture();
				}
			}
			return builder.buildFuture();
		}).executes(context -> {
			String unbanned = StringArgumentType.getString(context, "player");
			var player = context.getSource().getPlayer();

			if(player != null) {
				BanCommands.unban(player, unbanned);
			}
			return 1;
		}))));
	}

	static void ban(ServerPlayerEntity player, ServerPlayerEntity banned) {
		Skylands.instance.islands.get(player).ifPresentOrElse(island -> {
			if(player.getName().getString().equals(banned.getName().getString())) {
				player.sendMessage(SkylandsTexts.prefixed("message.skylands.ban_player.yourself"));
			}
			else {
				if(island.isMember(banned)) {
					player.sendMessage(SkylandsTexts.prefixed("message.skylands.ban_player.member"));
				}
				else {
					if(island.isBanned(banned)) {
						player.sendMessage(SkylandsTexts.prefixed("message.skylands.ban_player.fail"));
					}
					else {
						island.bans.add(new Member(banned));
						player.sendMessage(SkylandsTexts.prefixed("message.skylands.ban_player.success", map -> map.put("%player%", banned.getName().getString())));
						banned.sendMessage(SkylandsTexts.prefixed("message.skylands.ban_player.ban", map -> map.put("%owner%", island.owner.name)));

						SkylandsWorlds.getIsland(banned.getWorld()).ifPresent(isl -> {
							if(isl.owner.uuid.equals(island.owner.uuid)) {
								banned.sendMessage(SkylandsTexts.prefixed("message.skylands.hub_visit"));
								Skylands.instance.hub.visit(banned);
							}
						});
					}
				}
			}
		}, () -> player.sendMessage(SkylandsTexts.prefixed("message.skylands.ban_player.no_island")));
	}

	static void unban(ServerPlayerEntity player, String unbanned) {
		Skylands.instance.islands.get(player).ifPresentOrElse(island -> {
			if(!island.isBanned(unbanned)) {
				player.sendMessage(SkylandsTexts.prefixed("message.skylands.unban_player.fail"));
			}
			else {
				island.bans.removeIf(member -> member.name.equals(unbanned));
				player.sendMessage(SkylandsTexts.prefixed("message.skylands.unban_player.success", map -> map.put("%player%", unbanned)));
			}
		}, () -> player.sendMessage(SkylandsTexts.prefixed("message.skylands.unban_player.no_island")));
	}
}
