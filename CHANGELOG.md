# Changelog
User-relevant changes to the software, see the full commit log for all changes.  
[Downloads](https://github.com/zachbr/Dis4IRC/releases)

## 1.2.0 - `7766b34`
[Commits since 1.1.0](https://github.com/zachbr/Dis4IRC/compare/v1.1.0...v1.2.0)
* Discord Library Updates - **IMPORTANT**: You will be required to update to this version before November 7th 2020. That
  is when Discord will remove its old discordapp.com API endpoint.  
  
  As part of this update, Please note that Dis4IRC
  **REQUIRES** the `GUILD_MEMBERS` privileged intent in order to properly cache members at runtime.  
  **For instructions on adding the needed intent in the Discord Developer Console, please click [here](https://github.com/zachbr/Dis4IRC/blob/master/docs/Registering-A-Discord-Application.md#gateway-intents).**
* The webhook system now takes advantage of Discord API's _Allowed Mentions_ system, making it harder to abuse mentions.
* IRC users can now request all pinned messages from the bridged Discord channel using the `pinned` command.
* All bridge statistics are now persisted to disk, allowing you to restart the bridge without losing message statistics.
* Commands like `pinned` and `stats` can now be entirely disabled in the config file.
* The expiration time on pastes submitted by the paste service can now be configured.
* The IRC library was updated, fixing a reconnect issue with unstable connections.

## 1.1.0 - `5a3a45e`
[Commits since 1.0.2](https://github.com/zachbr/Dis4IRC/compare/v1.0.2...v1.1.0)
* The build date has been removed from the jar to support [reproducible builds](https://en.wikipedia.org/wiki/Reproducible_builds).
* The stats command will now show a percentage for each side of the bridge.
* The bridge will now exit in error if it cannot connect at startup.
* No-prefix messages can now optionally send an additional message with the triggering user's name.
* Better error messages for startup and connection failures.
* Fixes for mixed case IRC channel mappings.
* Fixes for startup IRC commands and additional logging.
* Fix for IRC nickname coloring issue.
* Add user quit and user kick relaying support.
* Updates to the underlying IRC and Discord libraries.

## 1.0.2 - 2019-01-02T23:30:28Z - `d4c6204`
[Commits since 1.0.1](https://github.com/zachbr/Dis4IRC/compare/v1.0.1...v1.0.2)
* Hotfix - Do not re-save config at startup as a workaround for [GH-19](https://github.com/zachbr/Dis4IRC/issues/19).

## 1.0.1 - 2018-12-31T05:16:47Z - `54f47af`
[Commits since 1.0.0](https://github.com/zachbr/Dis4IRC/compare/v1.0.0...v1.0.1)
* Better handling of whitespace-only messages for Discord.
* Statistics command now has a 60s rate limit on use.
* Respects guild-specific bot display name with webhooks.
* Markdown parser ignores messages shorter than 3 characters.

## 1.0.0 - 2018-11-22T01:43:07Z - `068f468`
* Initial Release.
