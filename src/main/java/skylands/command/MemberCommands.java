package skylands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import skylands.logic.Skylands;
import skylands.util.Texts;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MemberCommands {

	static void init(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("sl").then(literal("members").then(literal("invite").then(argument("player", player()).executes(context -> {
			var player = context.getSource().getPlayer();
			var newcomer = EntityArgumentType.getPlayer(context, "player");
			if(player != null && newcomer != null) {
				MemberCommands.invite(player, newcomer);
			}
			return 1;
		})))));
		dispatcher.register(literal("sl").then(literal("members").then(literal("remove").then(argument("player", word()).suggests((context, builder) -> {
			var player = context.getSource().getPlayer();

			if(player != null) {
				var island = Skylands.instance.islands.get(player);
				if(island.isPresent()) {
					var members = island.get().members;

					String remains = builder.getRemaining();

					for(var member : members) {
						if(member.name.contains(remains)) {
							builder.suggest(member.name);
						}
					}
					return builder.buildFuture();
				}
			}
			return builder.buildFuture();
		}).executes(context -> {
			String memberToRemove = StringArgumentType.getString(context, "player");
			var player = context.getSource().getPlayer();
			if(player != null) {
				MemberCommands.remove(player, memberToRemove);
			}
			return 1;
		})))));
	}

	static void invite(ServerPlayerEntity inviter, ServerPlayerEntity newcomer) {
		Skylands.instance.islands.get(inviter).ifPresentOrElse(island -> {
			if(island.isMember(newcomer)) {
				inviter.sendMessage(Texts.prefixed("message.skylands.invite_member.already_member"));
			}
			else {
				if(Skylands.instance.invites.hasInvite(island, newcomer)) {
					inviter.sendMessage(Texts.prefixed("message.skylands.invite_member.already_invited"));
				}
				else {
					inviter.sendMessage(Texts.prefixed("message.skylands.invite_member.success", (map) -> map.put("%newcomer%", newcomer.getName().getString())));

					var hoverText = Texts.prefixed("hover_event.skylands.invite_member.accept", (map) -> map.put("%inviter%", inviter.getName().getString()));
					Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sl accept " + inviter.getName().getString()));
					style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));

					var inviteText = Texts.prefixed("message.skylands.invite_member.invite", (map) -> map.put("%inviter%", inviter.getName().getString()));

					newcomer.sendMessage(inviteText.getWithStyle(style).get(0));
					newcomer.sendMessage(Texts.prefixed("message.skylands.invite_member.accept").getWithStyle(style).get(0));
					Skylands.instance.invites.create(island, newcomer);
				}
			}
		}, () -> inviter.sendMessage(Texts.prefixed("message.skylands.invite_member.no_island")));
	}

	static void remove(ServerPlayerEntity player, String removed) {
		Skylands.instance.islands.get(player).ifPresentOrElse(island -> {
			if(player.getName().getString().equals(removed)) {
				player.sendMessage(Texts.prefixed("message.skylands.remove_member.yourself"));
			}
			else {
				if(island.isMember(removed)) {
					island.members.removeIf(member -> member.name.equals(removed));
					player.sendMessage(Texts.prefixed("message.skylands.remove_member.success", (map) -> map.put("%member%", removed)));
				}
				else {
					player.sendMessage(Texts.prefixed("message.skylands.remove_member.not_member"));
				}
			}
		}, () -> player.sendMessage(Texts.prefixed("message.skylands.remove_member.no_island")));
	}
}
