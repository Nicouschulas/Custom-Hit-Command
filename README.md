# **Custom Hit Command: Unleash Dynamic Actions on Impact!**
![Curseforge Downloads](https://img.shields.io/curseforge/dt/1308743?style=for-the-badge&logo=curseforge&color=D02F2F&link=https://www.curseforge.com/minecraft/bukkit-plugins/chc&link=https://www.curseforge.com/minecraft/bukkit-plugins/chc) ![Modrinth Downloads](https://img.shields.io/modrinth/dt/eXM4AQg2?style=for-the-badge&logo=modrinth&color=D02F2F&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fchc&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fchc)  ![Spiget Downloads](https://img.shields.io/spiget/downloads/127038?style=for-the-badge&logo=spigotmc&color=D02F2F&link=https%3A%2F%2Fwww.spigotmc.org%2Fresources%2Fchc-custom-hit-command.127038%2F&link=https%3A%2F%2Fwww.spigotmc.org%2Fresources%2Fchc-custom-hit-command.127038%2F) ![Discord](https://img.shields.io/discord/866268252245590016?style=for-the-badge&logo=discord&color=D02F2F&link=https%3A%2F%2Fdiscord.com%2Finvite%2FZ3rrqYSpUE&link=https%3A%2F%2Fdiscord.com%2Finvite%2FZ3rrqYSpUE)


## **❓ What is CHC?**
**Custom Hit Command** let's you trigger specific commands every time you **hit another player** with a **chosen item group** and/or **single item(s)** you marked before with `/chc sethititem`! This **lightweight** and **highly customizable** plugin is built for Paper 1.21, ensuring **fast performance** and **seamless integration**.

## **🌟 Key Features:**

🔧 **Configurable Hit Item**: Define which item group and/or single item(s) triggers the action (e.g., all Iron Swords, a Blaze Rod)

🎯 **Dynamic Command Execution**: Set any server command to run upon a successful hit. You can choose whether the command is executed by the attacking player or the console. Use the **`%hitted_player%`** placeholder to automatically insert the name of the player you hit (e.g., `/duel %hitted_player%`)

✨ **Visual Hit Particles**: Configure particle effects to appear at the location of the hitted player, adding a visually dynamic element to your commands

💬 **Customizable Messages**: Tailor nearly all plugin messages to match your server's style

🔎 **Enhanced Logging**: An optional, enhanced logging system can be enabled to provide detailed debug information for troubleshooting

🔔 **Update Notifications**: An automatic update checker can be configured to notify you in the console or directly in-game (or both) when a new version is available. It can be easily toggled on/off in the configuration

**So what are you waiting for? Get Custom Hit Command now and bring a new layer of excitement to your server!**

## **📥 Downloads:**
Modrinth: https://modrinth.com/plugin/chc

Curseforge: https://www.curseforge.com/minecraft/bukkit-plugins/chc

Spigot: https://www.spigotmc.org/resources/chc-custom-hit-command.127038/

## ️**🛡️ Commands & Permissions:**
`/chc reload` | `customhitcommand.reload`

`/chc sethititem` | `customhitcommand.sethititem`

`customhitcommand.update` (for in-game update notifications)

`customhitcommand.use` (allows the use of the hit item (default true))

`customhitcommand.admin` (parent permission for all permissions)


## **❓ Q&A:**
**Why is the compiled plugin (JAR file) larger than the source code on GitHub?**

Our plugin includes dependencies like **bStats** to gather anonymous usage statistics. When the plugin is compiled into a single JAR file, the bStats library (along with all its own dependencies) is packaged directly into the final plugin file. This process, called 'shading', makes the plugin self-contained which naturally increases the overall file size compared to just the raw source code.

## **🆘 Need Help?**
If you encounter any problems, bugs, or have questions about the plugin, please don't hesitate to contact me directly. I'll be happy to take a look and provide support! Your positive experience is our priority.
