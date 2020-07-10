/*
 * This file is part of Dis4IRC.
 *
 * Copyright (c) 2018-2020 Dis4IRC contributors
 *
 * MIT License
 */

package io.zachbr.dis4irc.bridge.pier.discord

import io.zachbr.dis4irc.bridge.message.*
import org.javacord.api.event.server.member.ServerMemberJoinEvent
import org.javacord.api.event.server.member.ServerMemberLeaveEvent
import org.javacord.api.listener.server.member.ServerMemberJoinListener
import org.javacord.api.listener.server.member.ServerMemberLeaveListener

class DiscordJoinQuitListener(private val pier: DiscordPier) : ServerMemberJoinListener, ServerMemberLeaveListener {

    private val logger = pier.logger

    override fun onServerMemberJoin(event: ServerMemberJoinEvent) {
        val channel = event.server.systemChannel.get()
        val source = channel.asBridgeSource()

        // don't bridge itself
        if (pier.isThisBot(source, event.user.id)) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("DISCORD JOIN ${event.user.name}")

        val sender = BOT_SENDER
        val message = Message("${event.user.name} has joined the Discord", sender, source, receiveTimestamp)
        pier.sendToBridge(message)
    }

    override fun onServerMemberLeave(event: ServerMemberLeaveEvent) {
        val channel = event.server.systemChannel.get()
        val source = channel.asBridgeSource()

        // don't bridge itself
        if (pier.isThisBot(source, event.user.id)) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("DISCORD PART ${event.user.name}")

        val sender = BOT_SENDER
        val message = Message("${event.user.name} has left the Discord", sender, source, receiveTimestamp)
        pier.sendToBridge(message)
    }
}
