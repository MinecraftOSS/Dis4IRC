/*
 * This file is part of Dis4IRC.
 *
 * Copyright (c) 2018-2020 Dis4IRC contributors
 *
 * MIT License
 */

package io.zachbr.dis4irc.bridge.pier.discord

import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.listener.message.MessageCreateListener

/**
 * Responsible for listening to incoming discord messages and filtering garbage
 */
class DiscordMsgListener(private val pier: DiscordPier) : MessageCreateListener {
    private val logger = pier.logger

    override fun onMessageCreate(event: MessageCreateEvent) {
        // dont bridge itself
        val source = event.channel.asBridgeSource()
        if (pier.isThisBot(source, event.messageAuthor.id)) {
            return
        }

        // don't bridge empty messages (discord does this on join)
        if (event.message.readableContent.isEmpty() && event.message.attachments.isEmpty()) {
            return
        }

        val receiveTimestamp = System.nanoTime()
        logger.debug("DISCORD MSG ${event.channel.asServerChannel().get().name} ${event.messageAuthor.displayName}: ${event.message.readableContent}")

        val message = event.message.toBridgeMsg(logger, receiveTimestamp)
        pier.sendToBridge(message)
    }
}
