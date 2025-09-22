# **✨ CustomHitCommand: Unleash Dynamic Actions on Impact! ✨**
![Download count](https://img.shields.io/curseforge/dt/1308743?style=for-the-badge&logo=curseforge&color=D02F2F&link=https://www.curseforge.com/minecraft/bukkit-plugins/chc&link=https://www.curseforge.com/minecraft/bukkit-plugins/chc) ![Modrinth Downloads](https://img.shields.io/modrinth/dt/eXM4AQg2?style=for-the-badge&logo=modrinth&color=D02F2F&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fchc&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fchc)  ![Spiget Downloads](https://img.shields.io/spiget/downloads/127038?style=for-the-badge&logo=spigotmc&color=D02F2F&link=https%3A%2F%2Fwww.spigotmc.org%2Fresources%2Fchc-custom-hit-command.127038%2F&link=https%3A%2F%2Fwww.spigotmc.org%2Fresources%2Fchc-custom-hit-command.127038%2F)

**Custom Hit Command** lets you trigger specific commands every time you hit another player with a chosen item! This **lightweight** and **highly customizable** plugin is built for Paper 1.21, ensuring ⚡**fast performance** and ✨**seamless integration**.

# ⚠️ **DELAY:** ⚠️
I am currently working on adding multi-version support to include all versions from 1.8 to the latest. This will take some time, so don't be surprised if there are no updates in the next few days.

## ️**🛡️ Commands & Permissions:**
- `/chc reload` | `customhitcommand.reload`
- `customhitcommand.update` (for in-game update notifications)

## **🌟 Key Features:**

🔧 **Configurable Hit Item**: Define which item triggers the action (e.g., an Iron Sword, a Blaze Rod – your choice!).

🎯 **Dynamic Command Execution**: Set any server command to run upon a successful hit. You can choose whether the command is executed by the attacking player or the console. Use the **`%hitted_player%`** placeholder to automatically insert the name of the player you hit (e.g., `/duel %hitted_player%`).

✨ **Visual Hit Particles**: Configure eye-catching particle effects to appear at the location of the hit player, adding a visually dynamic element to your commands.

💬 **Customizable Messages**: Tailor nearly all plugin messages to match your server's style.

🔎 **Enhanced Logging**: An optional, enhanced logging system can be enabled to provide detailed debug information for troubleshooting.

⚙️ **Robust Error Handling**: The plugin features a more robust fallback system and improved error handling, making it more resilient to configuration mistakes.

🔔 **Update Notifications**: An automatic update checker can be configured to notify you in the console or directly in-game when a new version is available. It can be easily toggled on/off in the configuration.

⚙️ **Effortless Setup:** Simple config.yml for quick adjustments.

Get CustomHitCommand now and bring a new layer of excitement to your server!

## **📥 Download:**
Download it at Modrinth: https://modrinth.com/plugin/chc

## **❓ Q&A:**
**Why is the compiled plugin (JAR file) larger than the source code on GitHub?**

Our plugin includes **bStats** as a dependency to gather anonymous usage statistics. When the plugin is compiled into a single JAR file, the bStats library (along with all its own dependencies) is packaged directly into the final plugin file. This process, called 'shading', makes the plugin self-contained and ensures it runs smoothly without requiring users to download extra libraries, but it naturally increases the overall file size compared to just the raw source code.

## **🆘 Need Help?**
If you encounter any problems, bugs, or have questions about the plugin, please don't hesitate to contact me directly. I'll be happy to take a look and provide support! Your positive experience is our priority.
