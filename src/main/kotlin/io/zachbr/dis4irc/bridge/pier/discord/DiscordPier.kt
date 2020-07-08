/*
 * This file is part of Dis4IRC.
 *
 * Copyright (c) 2018-2020 Dis4IRC contributors
 *
 * MIT License
 */

package io.zachbr.dis4irc.bridge.pier.discord

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import club.minnced.discord.webhook.send.AllowedMentions
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import io.zachbr.dis4irc.bridge.Bridge
import io.zachbr.dis4irc.bridge.message.BOT_SENDER
import io.zachbr.dis4irc.bridge.message.Message
import io.zachbr.dis4irc.bridge.message.Source
import io.zachbr.dis4irc.bridge.pier.Pier
import io.zachbr.dis4irc.util.replaceTarget
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.server.Server
import org.slf4j.Logger
import java.util.*

private const val ZERO_WIDTH_SPACE = 0x200B.toChar()

class DiscordPier(private val bridge: Bridge) : Pier {
    internal val logger: Logger = bridge.logger
    private val webhookMap = HashMap<String, WebhookClient>()
    private var botAvatarUrl: String? = null
    private lateinit var discordApi: DiscordApi

    override fun start() {
        logger.info("Connecting to Discord API...")

        val discordApiBuilder = DiscordApiBuilder().setToken(bridge.config.discord.apiKey).login().join()
        discordApiBuilder.updateActivity("IRC")
        if (bridge.config.announceJoinsQuits) {
            discordApiBuilder.addListener(DiscordJoinQuitListener(this))
        }
        discordApiBuilder.addListener(DiscordMsgListener(this))

        discordApi = discordApiBuilder

        // init webhooks
        if (bridge.config.discord.webHooks.isNotEmpty()) {
            logger.info("Initializing Discord webhooks")

            for (hook in bridge.config.discord.webHooks) {
                val webhook: WebhookClient
                try {
                    webhook = WebhookClientBuilder(hook.webhookUrl).build()
                } catch (ex: IllegalArgumentException) {
                    logger.error("Webhook for ${hook.discordChannel} with url ${hook.webhookUrl} is not valid!")
                    ex.printStackTrace()
                    continue
                }

                webhookMap[hook.discordChannel] = webhook
                logger.info("Webhook for ${hook.discordChannel} registered")
            }
        }

        botAvatarUrl = discordApi.yourself.avatar.url.toString()

        logger.info("Discord Bot Invite URL: ${discordApi.createBotInvite()}")
        logger.info("Connected to Discord!")
    }

    override fun onShutdown() {
        discordApi.disconnect()

        for (client in webhookMap.values) {
            client.close()
        }
    }

    override fun sendMessage(targetChan: String, msg: Message) {
        if (!this::discordApi.isInitialized) {
            logger.error("Discord Connection has not been initialized yet!")
            return
        }

        val channel = getTextChannelBy(targetChan)
        if (channel == null) {
            logger.error("Unable to get a discord channel for: $targetChan | Is bot present?")
            return
        }

        val webhook = webhookMap[targetChan]
        val guild = channel.asServerChannel().get().server

        // convert name use to proper mentions
        for (member in guild.members) {
            val mentionTrigger = "@${member.getNickname(guild)}" // require @ prefix
            msg.contents = replaceTarget(msg.contents, mentionTrigger, member.nicknameMentionTag)
        }

        // convert emotes to show properly
        for (emote in guild.customEmojis) {
            val mentionTrigger = ":${emote.name}:"
            msg.contents = replaceTarget(msg.contents, mentionTrigger, emote.mentionTag, requireSeparation = false)
        }

        // Discord won't broadcast messages that are just whitespace
        if (msg.contents.trim() == "") {
            msg.contents = "$ZERO_WIDTH_SPACE"
        }

        if (webhook != null) {
            sendMessageWebhook(guild, webhook, msg)
        } else {
            sendMessageOldStyle(channel, msg)
        }

        val outTimestamp = System.nanoTime()
        bridge.updateStatistics(msg, outTimestamp)
    }

    private fun sendMessageOldStyle(discordChannel: TextChannel, msg: Message) {
        if (!discordChannel.canWrite(discordApi.yourself)) {
            logger.warn("Bridge cannot speak in ${discordChannel.asServerTextChannel().get().name} to send message: $msg")
            return
        }

        val senderName = enforceSenderName(msg.sender.displayName)
        val prefix = if (msg.originatesFromBridgeItself()) "" else "<$senderName> "

        discordChannel.sendMessage("$prefix${msg.contents}")
    }

    private fun sendMessageWebhook(guild: Server, webhook: WebhookClient, msg: Message) {
        // try and get avatar for matching user
        var avatarUrl: String? = null
        val matchingUsers = guild.getMembersByName(msg.sender.displayName)
        if (matchingUsers.isNotEmpty()) {
            avatarUrl = matchingUsers.first().avatar.url.toString()
        }

        var senderName = enforceSenderName(msg.sender.displayName)
        // if sender is command, use bot's actual name and avatar if possible
        if (msg.sender == BOT_SENDER) {
            senderName = discordApi.yourself?.name ?: senderName
            avatarUrl = botAvatarUrl ?: avatarUrl
        }

        val message = WebhookMessageBuilder()
            .setContent(msg.contents)
            .setUsername(senderName)
            .setAvatarUrl(avatarUrl)
            .setAllowedMentions(AllowedMentions().withParseUsers(true))
            .build()

        webhook.send(message)
    }

    /**
     * Checks if the message came from this bot
     */
    fun isThisBot(source: Source, snowflake: Long): Boolean {
        // check against bot user directly
        if (snowflake == discordApi.yourself.id) {
            return true
        }

        // check against webclients
        val webhook = webhookMap[source.discordId.toString()] ?: webhookMap[source.channelName]
        if (webhook != null) {
            return snowflake == webhook.id
        }

        // nope
        return false
    }

    /**
     * Sends a message to the bridge for processing
     */
    fun sendToBridge(message: Message) {
        bridge.submitMessage(message)
    }

    /**
     * Gets the pinned messages from the specified discord channel or null if the channel cannot be found
     */
    fun getPinnedMessages(channelId: String): List<Message>? {
        val channel = getTextChannelBy(channelId) ?: return null
        val messages = channel.pins.get()

        return messages.map { it.toBridgeMsg(logger) }.toList()
    }

    /**
     * Gets a text channel by snowflake ID or string
     */
    private fun getTextChannelBy(string: String): TextChannel? {
        val byId = discordApi.getTextChannelById(string)
        if (byId != null) {
            return byId.get()
        }

        val byName = discordApi.getTextChannelsByName(string)
        return if (byName.isNotEmpty()) byName.first() else null
    }
}

private const val NICK_ENFORCEMENT_CHAR = "-"

/**
 * Ensures name is within Discord's requirements
 */
fun enforceSenderName(name: String): String {
    if (name.length < 2) {
        return NICK_ENFORCEMENT_CHAR + name + NICK_ENFORCEMENT_CHAR
    }

    if (name.length > 32) {
        return name.substring(0, 32)
    }

    return name
}

