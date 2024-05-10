/*
 * Copyright (C) 2024 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.miscfeatures.profileviewer

import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.core.util.StringUtils
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer
import io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewerPage
import io.github.moulberry.notenoughupdates.profileviewer.SkyblockProfiles
import io.github.moulberry.notenoughupdates.util.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

class HoppityPage(pvInstance: GuiProfileViewer) : GuiProfileViewerPage(pvInstance) {
    private val manager get() = NotEnoughUpdates.INSTANCE.manager
    private val pv_hoppity = ResourceLocation("notenoughupdates:pv_hoppity.png")
    private var hoppityJson = Constants.HOPPITY

    private var guiLeft = GuiProfileViewer.getGuiLeft()
    private var guiTop = GuiProfileViewer.getGuiTop()

    private var currentProfile: SkyblockProfiles.SkyblockProfile? = null

    private val rabbitFamilyInfo = mutableListOf<UpgradeInfo>()
    private val factoryModifiersInfo = mutableListOf<UpgradeInfo>()
    private val otherModifiersInfo = mutableListOf<UpgradeInfo>()

    private var currentChocolate = 0L
    private var prestigeChocolate = 0L
    private var allTimeChocolate = 0L
    private var prestigeLevel = 0
    private var barnCapacity = 20

    // assuming cookie buff as Hypixel won't provide data for this
    private val baseMultiplier = 1.25
    private var rawChocolatePerSecond = 0
    private var multiplier = 0.0
    private var chocolatePerSecond = 0.0
    private var talisman: String? = null
    private var talismanChocolate = 0

    private val rabbitToRarity = mutableMapOf<String, String>()

    private var tooltipToDisplay = listOf<String>()

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        guiLeft = GuiProfileViewer.getGuiLeft()
        guiTop = GuiProfileViewer.getGuiTop()

        val newProfile = selectedProfile
        if (newProfile == null) {
            Utils.drawStringCentered("§cMissing Profile Data", guiLeft + 250, guiTop + 101, true, 0)
            return
        }

        if (hoppityJson == null) {
            Utils.drawStringCentered("§cMissing Repo Data", guiLeft + 250, guiTop + 101, true, 0)
            return
        }

        if (newProfile != currentProfile) {
            getData()
            currentProfile = selectedProfile
        }

        if (rabbitToRarity.isEmpty()) {
            Utils.drawStringCentered("§cMissing Repo Data", guiLeft + 250, guiTop + 101, true, 0)
            return
        }

        MC.textureManager.bindTexture(pv_hoppity)
        Utils.drawTexturedRect(
            guiLeft.toFloat(),
            guiTop.toFloat(),
            instance.sizeX.toFloat(),
            instance.sizeY.toFloat(),
            GL11.GL_NEAREST
        )

        Utils.renderShadowedString("§eRabbit Family", (guiLeft + 74).toFloat(), (guiTop + 14).toFloat(), 105)
        Utils.renderShadowedString("§eFactory Modifiers", (guiLeft + 74).toFloat(), (guiTop + 76).toFloat(), 105)
        Utils.renderShadowedString("§eOther", (guiLeft + 74).toFloat(), (guiTop + 138).toFloat(), 105)

        Utils.renderShadowedString("§eChocolate Factory", (guiLeft + 214).toFloat(), (guiTop + 14).toFloat(), 105)
        Utils.renderShadowedString("§eStats", (guiLeft + 214).toFloat(), (guiTop + 30).toFloat(), 105)

        Utils.renderShadowedString("§eRabbit Collection", (guiLeft + 356).toFloat(), (guiTop + 14).toFloat(), 105)

        GlStateManager.enableDepth()

        drawAlignedStringWithHover(
            "§eChocolate:",
            "§f${StringUtils.shortNumberFormat(currentChocolate.toDouble())}",
            guiLeft + 160,
            guiTop + 53,
            110,
            mouseX,
            mouseY,
            listOf("§eCurrent Chocolate: §f${StringUtils.formatNumber(currentChocolate)}")
        )
        drawAlignedStringWithHover(
            "§eSince Prestige:",
            "§f${StringUtils.shortNumberFormat(prestigeChocolate.toDouble())}",
            guiLeft + 160,
            guiTop + 68,
            110,
            mouseX,
            mouseY,
            listOf("§eChocolate Since Prestige: §f${StringUtils.formatNumber(prestigeChocolate)}")
        )
        drawAlignedStringWithHover(
            "§eAll Time:",
            "§f${StringUtils.shortNumberFormat(allTimeChocolate.toDouble())}",
            guiLeft + 160,
            guiTop + 83,
            110,
            mouseX,
            mouseY,
            listOf("§eAll Time Chocolate: §f${StringUtils.formatNumber(allTimeChocolate)}")
        )
        fun chocolateForNextPrestige(): Long {
            return when (prestigeLevel) {
                1 -> 150_000_000
                2 -> 1_000_000_000
                3 -> 4_000_000_000
                4 -> 10_000_000_000
                else -> 1
            }
        }

        Utils.renderAlignedString(
            "§eUntil Prestige:",
            "§f${StringUtils.shortNumberFormat(chocolateForNextPrestige().toDouble() - prestigeChocolate.toDouble())}",
            (guiLeft + 160).toFloat(),
            (guiTop + 98).toFloat(),
            110
        )
        val chocolateTilPrestigePercentage = (prestigeChocolate.toFloat() / chocolateForNextPrestige()).coerceIn(0f, 1f)
        if (chocolateTilPrestigePercentage != 1f) {
            instance.renderGoldBar(guiLeft + 160.toFloat(), guiTop + 109.toFloat(), 110f)
        } else {
            instance.renderBar(guiLeft + 160.toFloat(), guiTop + 109.toFloat(), 110f, chocolateTilPrestigePercentage)
        }

        val xBar = guiLeft + 160
        val yBar = guiTop + 109
        if (mouseX in xBar..(xBar + 110) && mouseY in yBar..(yBar + 5)) {
                tooltipToDisplay = buildList {
                    add("§6${StringUtils.formatNumber(prestigeChocolate)}§7/§6${StringUtils.formatNumber(chocolateForNextPrestige())}")
                }
        }

        Utils.renderAlignedString(
            "§eLast Updated:",
            "§f${prestigeChocolate} umm uhh  umm uhh  umm uhh ",
            (guiLeft + 160).toFloat(),
            (guiTop + 117).toFloat(),
            110
        )

        Utils.renderAlignedString(
            "§eMultiplier:",
            "§f${multiplier.roundToDecimals(3)}x",
            (guiLeft + 160).toFloat(),
            (guiTop + 133).toFloat(),
            110
        )
        Utils.renderAlignedString(
            "§eRaw Chocolate/Second:",
            "§f${StringUtils.formatNumber(rawChocolatePerSecond)}",
            (guiLeft + 160).toFloat(),
            (guiTop + 148).toFloat(),
            110
        )
        Utils.renderAlignedString(
            "§eChocolate/Second:",
            "§f${StringUtils.formatNumber(chocolatePerSecond.roundToDecimals(2))}",
            (guiLeft + 160).toFloat(),
            (guiTop + 163).toFloat(),
            110
        )
        Utils.renderAlignedString(
            "§eChocolate/Day:",
            "§f${StringUtils.formatNumber(chocolatePerSecond.roundToDecimals(2) * 86400)}",
            (guiLeft + 160).toFloat(),
            (guiTop + 178).toFloat(),
            110
        )

        rabbitFamilyInfo.displayInfo(20, 34, mouseX, mouseY)
        factoryModifiersInfo.displayInfo(31, 96, mouseX, mouseY)
        otherModifiersInfo.displayInfo(42, 158, mouseX, mouseY)

        drawRabbitStats(mouseX, mouseY)

        if (tooltipToDisplay.isNotEmpty()) {
            tooltipToDisplay = tooltipToDisplay.map { "§7$it" }
            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, instance.width, instance.height, -1)
            tooltipToDisplay = listOf()
        }
    }

    private fun drawAlignedStringWithHover(
        first: String,
        second: String,
        x: Int,
        y: Int,
        length: Int,
        mouseX: Int,
        mouseY: Int,
        hover: List<String>,
    ) {
        Utils.renderAlignedString(first, second, x.toFloat(), y.toFloat(), length)
        if (mouseX in x..(x + length) && mouseY in y..(y + 13)) {
            tooltipToDisplay = hover
        }
    }

    private fun List<UpgradeInfo>.displayInfo(xPos: Int, yPos: Int, mouseX: Int, mouseY: Int) {
        var x = guiLeft + xPos
        val y = guiTop + yPos

        this.forEach { upgradeInfo ->
            Utils.drawStringCentered(
                "${upgradeInfo.colourCode}${upgradeInfo.level}§f",
                x + 10,
                y + 26,
                true,
                0
            )

            MC.textureManager.bindTexture(GuiProfileViewer.pv_elements)
            Utils.drawTexturedRect(
                (x).toFloat(),
                (y).toFloat(),
                20f,
                20f,
                0f,
                20 / 256f,
                0f,
                20 / 256f,
                GL11.GL_NEAREST
            )

            Utils.drawItemStack(upgradeInfo.stack, x + 2, y + 2)

            if (mouseX in x..(x + 20) && mouseY in y..(y + 20)) {
                val tooltip = when (upgradeInfo.upgradeType) {

                    UpgradeType.RABBIT_EMPLOYEES -> {
                        if (upgradeInfo.level == 0) {
                            fallbackList
                        } else {
                            buildList {
                                add("${upgradeInfo.colourCode}${upgradeInfo.displayName} §8- §7[${upgradeInfo.level}] ${upgradeInfo.colourCode}${upgradeInfo.suffixName}")
                                add("")
                                add("§7Produces §6+${upgradeInfo.level * upgradeInfo.extraCps} Chocolate §7per second.")
                            }
                        }
                    }

                    UpgradeType.HAND_BAKED_CHOCOLATE -> {
                        if (upgradeInfo.level == 0) {
                            fallbackList
                        } else {
                            buildList {
                                add("§d${upgradeInfo.displayName} ${upgradeInfo.level.toRoman()}")
                                add("")
                                add("§7Chocolate Per Click: §6+${upgradeInfo.level} Chocolate")
                            }
                        }
                    }

                    UpgradeType.TIME_TOWER -> {
                        if (upgradeInfo.level == 0) {
                            fallbackList
                        } else {
                            buildList {
                                add("§d${upgradeInfo.displayName} ${upgradeInfo.level.toRoman()}")
                                add("")
                                add("§6+${upgradeInfo.level * 0.1}x Chocolate §7per second for §a1h§7.")
                                add("Time Tower charges ${TODO()}")
                            }
                        }
                    }

                    UpgradeType.RABBIT_SHRINE -> {
                        if (upgradeInfo.level == 0) {
                            fallbackList
                        } else {
                            buildList {
                                add("§d${upgradeInfo.displayName} ${upgradeInfo.level.toRoman()}")
                                add("")
                                add("§7Increases §dodds §7of finding")
                                add("§aChocolate Rabbits §7of higher rarity")
                                add("§7by ${upgradeInfo.level}% §7during §dHoppity's Hunt§7.")

                            }
                        }
                    }

                    UpgradeType.COACH_JACKRABBIT -> {
                        if (upgradeInfo.level == 0) {
                            fallbackList
                        } else {
                            buildList {
                                add("§d${upgradeInfo.displayName} ${upgradeInfo.level.toRoman()}")
                                add("")
                                add("§7Chocolate Multiplier increased by")
                                add("§6+${upgradeInfo.level * 0.01}x Chocolate §7per second.")
                            }
                        }
                    }

                    UpgradeType.CHOCOLATE_FACTORY -> {
                        buildList {
                            add("§6${upgradeInfo.displayName} ${upgradeInfo.level.toRoman()}")
                            upgradeInfo.chocolateFactoryTooltip.lines().forEach {
                                add(it)
                            }
                        }
                    }

                    UpgradeType.RABBIT_BARN -> {
                        if (upgradeInfo.level == 0) {
                            buildList {
                                add("§a${upgradeInfo.displayName} I")
                                add("§7Barn: §a${RabbitCollectionRarity.TOTAL.uniques}§7/§a20")
                            }
                        } else {
                            buildList {
                                add("§a${upgradeInfo.displayName} ${upgradeInfo.level.toRoman()}")
                                add("§7Rabbit Barn: §a${RabbitCollectionRarity.TOTAL.uniques}§7/§a${(upgradeInfo.level * 2) + 18}")
                            }
                        }
                    }

                    UpgradeType.TALISMAN -> {
                        buildList {
                            add("")
                        }
                    }

                    UpgradeType.OTHER -> buildList {
                        add("§d§l:3")
                    }
                }
                tooltipToDisplay = tooltip
            }
            x += 22
        }
    }

    private val fallbackList
        get() = listOf(
            "§7Not Obtained Yet!",
            "§8${GuiProfileViewer.getDisplayName()} §8hasn't obtained this yet"
        )

    private fun drawRabbitStats(mouseX: Int, mouseY: Int) {
        val x = guiLeft + 296
        var y = guiTop + 34

        RabbitCollectionRarity.values().forEach { rabbitInfo ->
            Utils.renderAlignedString(
                rabbitInfo.displayName,
                "§f${rabbitInfo.uniques}/${rabbitInfo.maximum}",
                (x + 30).toFloat(),
                (y + 7).toFloat(),
                90
            )

            MC.textureManager.bindTexture(GuiProfileViewer.pv_elements)
            Utils.drawTexturedRect(
                (x).toFloat(),
                (y).toFloat(),
                20f,
                20f,
                0f,
                20 / 256f,
                0f,
                20 / 256f,
                GL11.GL_NEAREST
            )

            if (mouseX in x..(x + 120) && mouseY in y..(y + 20)) {
                val tooltip = if (rabbitInfo.uniques < 1) {
                    buildList {
                        add("§l${rabbitInfo.displayName} Rabbits")
                        if (rabbitInfo == RabbitCollectionRarity.TOTAL) {
                            add("§8${GuiProfileViewer.getDisplayName()} hasn't found any rabbits.")
                        } else {
                            add("§8${GuiProfileViewer.getDisplayName()} hasn't found any ${rabbitInfo.apiName} rabbits.")
                        }
                    }
                } else {
                    buildList {
                        add("§l${rabbitInfo.displayName} Rabbits")
                        add("${rabbitInfo.apiName} rabbits found: §a${rabbitInfo.uniques}§7/§a${rabbitInfo.maximum}")
                        add("Duplicate Rabbits: §a${rabbitInfo.duplicates}")
                        add("Total Rabbits Found: §a${rabbitInfo.uniques + rabbitInfo.duplicates}")
                        add("")
                        add("§6+${rabbitInfo.chocolatePerSecond} Chocolate §7per second.")
                        add("§6+${rabbitInfo.multiplier.roundToDecimals(3)}x Chocolate §7per second.")
                    }
                }
                tooltipToDisplay = tooltip
            }

            Utils.drawItemStack(rabbitInfo.stack, x + 2, y + 2)

            y += 22
        }
    }

    private fun getData() {
        val selectedProfile = selectedProfile?.profileJson ?: return

        rabbitToRarity.clear()
        RabbitCollectionRarity.resetData()

        val hoppityData = hoppityJson.getAsJsonObject("hoppity") ?: return
        val rabbitRarities = hoppityData.getAsJsonObject("rarities") ?: return
        val specialRabbits = hoppityData.getAsJsonObject("special") ?: return
        val prestigeMultipliers = hoppityData.getAsJsonObject("prestigeMultipliers") ?: return
        val talismanChocolateData = hoppityData.getAsJsonObject("talisman") ?: return

        val foundMythicRabbits = mutableSetOf<String>()

        val hoppityInfo = Utils.getElementOrDefault(selectedProfile, "events.easter", JsonObject()).asJsonObject

        getTalismanTier(talismanChocolateData)

        for (rarity in rabbitRarities.entrySet()) {
            val rarityName = rarity.key
            val rarityInfo = rarity.value.asJsonObject
            val rabbits = rarityInfo.getAsJsonArray("rabbits")
            for (rabbit in rabbits) {
                rabbitToRarity[rabbit.asString] = rarityName
            }
            val rabbitRarity = RabbitCollectionRarity.fromApiName(rarityName) ?: continue
            rabbitRarity.maximum = rabbits.size()
        }

        val rabbits = Utils.getElementOrDefault(hoppityInfo, "rabbits", JsonObject()).asJsonObject
        for ((rabbitName, rabbitInfo) in rabbits.entrySet()) {
            if (rabbitInfo.isJsonObject) continue
            val rabbitRarity = rabbitToRarity[rabbitName]?.let { RabbitCollectionRarity.fromApiName(it) } ?: continue
            rabbitRarity.addAmount(rabbitInfo.asInt)
            if (rabbitRarity == RabbitCollectionRarity.MYTHIC) {
                foundMythicRabbits.add(rabbitName)
            }
        }

        for (rarity in rabbitRarities.entrySet()) {
            val rabbitRarity = RabbitCollectionRarity.fromApiName(rarity.key) ?: continue
            val rarityInfo = rarity.value.asJsonObject
            val cps = rarityInfo.get("chocolate").asInt
            val multiplier = rarityInfo.get("multiplier").asDouble
            rabbitRarity.setChocolateData(cps, multiplier)
        }

        for (mythic in foundMythicRabbits) {
            val specialRabbit = specialRabbits.getAsJsonObject(mythic)
            val cps = specialRabbit.get("chocolate").asInt
            val multiplier = specialRabbit.get("multiplier").asDouble
            RabbitCollectionRarity.MYTHIC.chocolatePerSecond += cps
            RabbitCollectionRarity.MYTHIC.multiplier += multiplier
        }

        val totalRabbit = RabbitCollectionRarity.TOTAL
        totalRabbit.uniques = RabbitCollectionRarity.values().sumOf { it.uniques }
        totalRabbit.duplicates = RabbitCollectionRarity.values().sumOf { it.duplicates }
        totalRabbit.chocolatePerSecond = RabbitCollectionRarity.values().sumOf { it.chocolatePerSecond }
        totalRabbit.multiplier = RabbitCollectionRarity.values().sumOf { it.multiplier }
        totalRabbit.maximum = RabbitCollectionRarity.values().sumOf { it.maximum }

        rabbitFamilyInfo.clear()
        factoryModifiersInfo.clear()
        otherModifiersInfo.clear()

        val employeesInfo = Utils.getElementOrDefault(hoppityInfo, "employees", JsonObject()).asJsonObject
        val timeTowerInfo = Utils.getElementOrDefault(hoppityInfo, "time_tower", JsonObject()).asJsonObject

        val coachLevel = hoppityInfo.getIntOrValue("chocolate_multiplier_upgrades", 0)
        val barnLevel = hoppityInfo.getIntOrValue("rabbit_barn_capacity_level", 0)
        barnCapacity = barnLevel * 2 + 20

        rabbitFamilyInfo.add(
            UpgradeInfo(
                rabbitBro,
                employeesInfo.getIntOrValue("rabbit_bro", 0),
                UpgradeType.RABBIT_EMPLOYEES,
                "Rabbit Bro",
                1
            )
        )
        rabbitFamilyInfo.add(
            UpgradeInfo(
                rabbitCousin,
                employeesInfo.getIntOrValue("rabbit_cousin", 0),
                UpgradeType.RABBIT_EMPLOYEES,
                "Rabbit Cousin",
                2
            )
        )
        rabbitFamilyInfo.add(
            UpgradeInfo(
                rabbitSis,
                employeesInfo.getIntOrValue("rabbit_sis", 0),
                UpgradeType.RABBIT_EMPLOYEES,
                "Rabbit Sis",
                3
            )
        )
        rabbitFamilyInfo.add(
            UpgradeInfo(
                rabbitDaddy,
                employeesInfo.getIntOrValue("rabbit_father", 0),
                UpgradeType.RABBIT_EMPLOYEES,
                "Rabbit Daddy",
                4
            )
        )
        rabbitFamilyInfo.add(
            UpgradeInfo(
                rabbitGranny,
                employeesInfo.getIntOrValue("rabbit_grandma", 0),
                UpgradeType.RABBIT_EMPLOYEES,
                "Rabbit Granny",
                5,
            )
        )

        factoryModifiersInfo.add(
            UpgradeInfo(
                handBaked,
                hoppityInfo.getIntOrValue("click_upgrades", 0) + 1,
                UpgradeType.HAND_BAKED_CHOCOLATE,
                "Hand-Baked Chocolate"
            )
        )
        factoryModifiersInfo.add(
            UpgradeInfo(
                timeTower,
                timeTowerInfo.getIntOrValue("level", 0),
                UpgradeType.TIME_TOWER,
                "Time Tower"
            )
        )
        factoryModifiersInfo.add(
            UpgradeInfo(
                rabbitShrine,
                hoppityInfo.getIntOrValue("rabbit_rarity_upgrades", 0),
                UpgradeType.RABBIT_SHRINE,
                "Rabbit Shrine"
            )
        )
        factoryModifiersInfo.add(
            UpgradeInfo(
                coachJackrabbit,
                coachLevel,
                UpgradeType.COACH_JACKRABBIT,
                "Coach Jackrabbit"
            )
        )
        otherModifiersInfo.add(
            UpgradeInfo(
                prestigeItem,
                hoppityInfo.getIntOrValue("chocolate_level", 1),
                UpgradeType.CHOCOLATE_FACTORY,
                "Chocolate Factory"
            )
        )
        otherModifiersInfo.add(
            UpgradeInfo(
                rabbitBarn,
                barnLevel,
                UpgradeType.RABBIT_BARN,
                "Rabbit Barn"
            )
        )
        val shownTalismanItem = talisman?.let { manager.createItem(it) } ?: talismanItem

        otherModifiersInfo.add(
            UpgradeInfo(
                shownTalismanItem,
                0,
                UpgradeType.TALISMAN,
                "tempname"
            )
        )

        currentChocolate = hoppityInfo.getLongOrValue("chocolate", 0)
        prestigeChocolate = hoppityInfo.getLongOrValue("chocolate_since_prestige", 0)
        allTimeChocolate = hoppityInfo.getLongOrValue("total_chocolate", 0)
        prestigeLevel = hoppityInfo.getIntOrValue("chocolate_level", 1)

        val prestigeMultiplier = prestigeMultipliers.get(prestigeLevel.toString()).asDouble
        val coachMultiplier = 0.01 * coachLevel
        val rabbitMultiplier = RabbitCollectionRarity.TOTAL.multiplier
        multiplier = baseMultiplier + prestigeMultiplier + coachMultiplier + rabbitMultiplier

        val rabbitChocolate = RabbitCollectionRarity.TOTAL.chocolatePerSecond
        val employeeChocolate = rabbitFamilyInfo.sumOf { it.extraCps * it.level }
        rawChocolatePerSecond = rabbitChocolate + employeeChocolate + talismanChocolate

        chocolatePerSecond = rawChocolatePerSecond * multiplier
    }

    private fun getTalismanTier(talismanChocolateData: JsonObject) {
        talisman = null
        var bestTalisman: String? = null
        var bestTalismanCps = 0

        val playerItems = GuiProfileViewer.getSelectedProfile()?.inventoryInfo ?: return
        val talismanInventory = playerItems["talisman_bag"] ?: return
        val playerInventory = playerItems["inv_contents"] ?: return

        for (item in talismanInventory) {
            if (item.isJsonNull) continue
            val internalName = item.asJsonObject.get("internalname").asString
            if (talismanChocolateData.has(internalName)) {
                val cps = talismanChocolateData.get(internalName).asInt
                if (cps > bestTalismanCps) {
                    bestTalisman = internalName
                    bestTalismanCps = cps
                }
            }
        }

        for (item in playerInventory) {
            if (item.isJsonNull) continue
            val internalName = item.asJsonObject.get("internalname").asString
            if (talismanChocolateData.has(internalName)) {
                val cps = talismanChocolateData.get(internalName).asInt
                if (cps > bestTalismanCps) {
                    bestTalisman = internalName
                    bestTalismanCps = cps
                }
            }
        }
        println("best talisman: $bestTalisman, cps: $bestTalismanCps")
        talisman = bestTalisman
    }

    private val rabbitBro: ItemStack = Utils.createSkull(
        "ThatGravyBoat",
        "6adb1fab-c55c-31b1-a575-052b11f3a9c9",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMjU5NDI0NjM2MywKICAicHJvZmlsZUlkIiA6ICJjZjc4YzFkZjE3ZTI0Y2Q5YTIxYmU4NWQ0NDk5ZWE4ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNYXR0c0FybW9yU3RhbmRzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzI4NzkzNGJkZDlkZjI3MDViMjUxYmI5OTdlMDI5YjE4YzFlOTRkZjEyOTkyYjgxMDdlNzQ0OTdiMjA1Y2E3ZTgiCiAgICB9CiAgfQp9"
    )
    private val rabbitCousin: ItemStack = Utils.createSkull(
        "ThatGravyBoat",
        "3c2e46e4-0bd8-3e75-ac59-c6fe48ce2155",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMjU5NDI2ODkxNCwKICAicHJvZmlsZUlkIiA6ICJlMjc5NjliODYyNWY0NDg1YjkyNmM5NTBhMDljMWMwMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLRVZJTktFTE9LRSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hOTgyODI1YzAxYjY1OGYzNDhhMDk5YjQ1NzkwMjlhMTgwZDJlNDE1MTgzOTUxYjJlNmU1ZTI3MjU3ZGY0MjU0IgogICAgfQogIH0KfQ=="
    )
    private val rabbitSis: ItemStack = Utils.createSkull(
        "ThatGravyBoat",
        "8b08b9b4-28da-33fe-971b-2c15c6526d80",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMjg0NzA5MzAxMSwKICAicHJvZmlsZUlkIiA6ICIyMWNjMzkxZmNkMjc0NzY5OTg5Y2M3M2VjYWRiNTE3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJHT1NUTFk5NyIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9mZDA3NmUwZTNkNDA3MmQwZmZmZWUwYTg3YTVkNzI2ZmMzNGIyYmNlYzM4YzI2NGZiOWI2Nzg3MWE4ZWFkNjMzIgogICAgfQogIH0KfQ=="
    )
    private val rabbitDaddy: ItemStack = Utils.createSkull(
        "ThatGravyBoat",
        "794aa517-c2a6-3762-8791-187665c4eda0",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMjg0NzA0NzAwNSwKICAicHJvZmlsZUlkIiA6ICIzOThiZGM3NWVhYzQ0ZjMzYWEyMDBiMTYyNTRmMDhlOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJJa2h3YW4wNTEwIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzU3Y2FiMGMzNGQ3ZGRjZjcyZGI1NmZmMzZmMjg4M2Y1NTRjZmY3NmViNWQzYjNlMDU2MjMzODAzNmM5NzYwNDMiCiAgICB9CiAgfQp9"
    )
    private val rabbitGranny: ItemStack = Utils.createSkull(
        "ThatGravyBoat",
        "d6c31145-355d-3807-868c-a7e26e11fc59",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMjU5NDIyNDA2NCwKICAicHJvZmlsZUlkIiA6ICI2OGVmMmM5NTc5NjM0MjE4YjYwNTM5YWVlOTU3NWJiNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVNdWx0aUFjb3VudCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kNmViMmQ4NWVlOGUzYWYxYzJlYzkzNGJlYjcwYTM5YzVlNzY2YjIzYmRhYjYzMjEwYmQyYWFjZDczY2JiZmM4IgogICAgfQogIH0KfQ=="
    )

    private val handBaked = ItemStack(Items.cookie)
    private val timeTower = ItemStack(Items.clock)
    private val rabbitShrine = ItemStack(Items.rabbit_foot)
    private val coachJackrabbit: ItemStack = Utils.createSkull(
        "ThatGravyBoat",
        "4793d1f6-8b13-3e09-857b-cfb47dea24d3",
        "ewogICJ0aW1lc3RhbXAiIDogMTcxMzAyMjkyOTYwNCwKICAicHJvZmlsZUlkIiA6ICI2NGY0MGFiNzFmM2E0NGZiYjg0N2I5ZWFhOWZjNDRlNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJvZGF2aWRjZXNhciIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iYzBjYzY3ZTc5YzIyOGU1NDFlNjhhZWIxZDgxZWQ3YWY1MTE2NjYyMmFkNGRiOTQxN2Q3YTI5ZDFiODlhZjk1IgogICAgfQogIH0KfQ=="
    )

    private val prestigeItem = ItemStack(Blocks.dropper)
    private val rabbitBarn = ItemStack(Blocks.oak_fence)
    private val talismanItem = ItemStack(Items.dye, 1, 8)

    data class UpgradeInfo(
        val stack: ItemStack,
        val level: Int,
        val upgradeType: UpgradeType,
        var displayName: String,
        val extraCps: Int = 0
    ) {
        val colourCode: String
            get() {
                if (upgradeType == UpgradeType.RABBIT_EMPLOYEES) {
                    return when (level) {
                        in (0..9) -> "§f"
                        in (10..74) -> "§a"
                        in (75..124) -> "§9"
                        in (125..174) -> "§5"
                        in (175..199) -> "§6"
                        200 -> "§d"
                        else -> "§7"
                    }
                }
                return "§7"
            }
        val suffixName: String
            get() {
                if (upgradeType == UpgradeType.RABBIT_EMPLOYEES) {
                    return when (level) {
                        in (0..9) -> "Intern"
                        in (10..74) -> "Employee"
                        in (75..124) -> "Assistant"
                        in (125..174) -> "Manager"
                        in (175..199) -> "Director"
                        200 -> "Executive"
                        else -> ""
                    }
                }
                return ""
            }
        val chocolateFactoryTooltip: String
            get() {
                if (upgradeType == UpgradeType.CHOCOLATE_FACTORY) {
                    return when (level) {
                        1 -> "§7Chocolate Production Multiplier: §61x\n" +
                                "§7Max Rabbit Rarity: §a§lUNCOMMON\n" +
                                "§7Max Chocolate: §6500M\n" +
                                "§7Max Employee: [120] §9Assistant"

                        2 -> "Chocolate Production Multiplier: §61.1x\n" +
                                "Max Rabbit Rarity: §9§lRARE\n" +
                                "Max Chocolate: §61.2B\n" +
                                "Max Employee: [140] §5Manager"

                        3 -> "Chocolate Production Multiplier: §61.25x\n" +
                                "Max Rabbit Rarity: §5§lEPIC\n" +
                                "Max Chocolate: §64B\n" +
                                "Max Employee: [160] §5Manager"

                        4 -> "Chocolate Production Multiplier: §61.5x\n" +
                                "Max Rabbit Rarity: §6§lLEGENDARY\n" +
                                "Max Chocolate: §610B\n" +
                                "Max Employee: [180] §6Director"

                        5 -> "Chocolate Production Multiplier: §62x\n" +
                                "Max Rabbit Rarity: §d§lMYTHIC\n" +
                                "Max Chocolate: §625B\n" +
                                "Max Employee: [200] §dExecutive"
                        else -> ""
                    }
                }
                return ""
            }
    }

    enum class UpgradeType {
        RABBIT_EMPLOYEES,
        HAND_BAKED_CHOCOLATE,
        TIME_TOWER,
        RABBIT_SHRINE,
        COACH_JACKRABBIT,
        CHOCOLATE_FACTORY,
        RABBIT_BARN,
        TALISMAN,
        OTHER
    }

    companion object {
        private val totalRabbit: ItemStack = Utils.createSkull(
            "CalMWolfs",
            "c67dc557-0d47-38a4-a2d4-4e776001ed82",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxMTYzNDM5MTg3OCwKICAicHJvZmlsZUlkIiA6ICIxNmQ4NjI4NzYzMWY0NDY2OGQ0NDM2ZTJlY2IwNTllNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZXphVG91cm5leSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9iNzllN2YzMzQxYjY3MmQ5ZGU2NTY0Y2JhY2EwNTJhNmE3MjNlYTQ2NmEyZTY2YWYzNWJhMWJhODU1ZjBkNjkyIgogICAgfQogIH0KfQ=="
        )
        private val commonRabbit: ItemStack = Utils.createSkull(
            "CalMWolfs",
            "12cfcf5a-aaf8-3a88-ab5a-bacb8557f002",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxMTYzNTExNDE5OCwKICAicHJvZmlsZUlkIiA6ICI5ZDE1OGM1YjNiN2U0ZGNlOWU0OTA5MTdjNmJlYmM5MSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTbm9uX1NTIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzE1ZDBmMGMzNjhlNTRkMjBlM2M1ZTU1MGEwNGE0NjlkMDE2MWIxZmVjZjI2YzhlNTE4MzE4YzA5ZTExMzRmNmIiCiAgICB9CiAgfQp9"
        )
        private val uncommonRabbit: ItemStack = Utils.createSkull(
            "CalMWolfs",
            "60b1f785-0d68-342c-8047-9f13a235e68a",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxMTYzNTM2MzM0NCwKICAicHJvZmlsZUlkIiA6ICI0MzFhMmRlYTQ4YTE0NTMxYjEyZDU5MzY0NDUxNmIyNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJpQ2FwdGFpbk5lbW8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjgxMDdjNTIwMWVhMjFiYWE4OTU1MTc1MTBiMDA3ZjVmNjE1ZTNjNjYxNWRmNjk2YjkwNmFiOThlNmY5ZjA2IgogICAgfQogIH0KfQ"
        )
        private val rareRabbit: ItemStack = Utils.createSkull(
            "CalMWolfs",
            "68beea4d-1f24-341f-910c-bca2155f1070",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxMTYzNTI0MDkwNCwKICAicHJvZmlsZUlkIiA6ICJmY2ZhYTg0MzA0YjE0NDUxOThkNWYxNzQ3ZjI0Y2Q5MCIsCiAgInByb2ZpbGVOYW1lIiA6ICJMYXJzVGhlV29sZiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80NTZhMjA5ODAzZTFlOGViOTQxMTc3ZTJjYzhhMDFiY2VhODg0ZDk0ZGM3N2MzOGUyMmY1Y2QxYTg2MmY4OWNhIgogICAgfQogIH0KfQ"
        )
        private val epicRabbit: ItemStack = Utils.createSkull(
            "CalMWolfs",
            "217a75ba-42a7-3cf9-b366-35c07316942b",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxMTYzNDg4OTY5NywKICAicHJvZmlsZUlkIiA6ICIzYmFlMTVhMWU0Zjg0ZTc5OWE3N2QwZDBhZTNlZDc5NiIsCiAgInByb2ZpbGVOYW1lIiA6ICJiYXlyb25fZ2FtZXJfMjU0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzMzNmYwYTUxY2ZiMTBiODE5ZGUxZmNkZjM0NzBmM2QzMzZkYjI2MWQxZmZiYTk0M2E3ODU2NTQwODA5ZGI0ZWUiCiAgICB9CiAgfQp9"
        )
        private val legendaryRabbit: ItemStack = Utils.createSkull(
            "CalMWolfs",
            "447a59da-2eff-3b06-8487-6af08e798c81",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxMTYzNDc1NTM1NCwKICAicHJvZmlsZUlkIiA6ICI5YzM5OTdhMjVjNWY0NmY0OWZlMWFhY2RlZjRiMmMwNSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWxsZXJmcmVkZHk4OTQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGUxYzE3MGI0ZjZjMzc2MTRlZTk2MDk2MDE2NDg1NWFiNzQyNmNlZmI0NDA5N2Y3OTU3ZmEzMGE2N2I5MzVlZiIKICAgIH0KICB9Cn0"
        )
    }

    // todo once someone has a mythic rabbit add it here
    private enum class RabbitCollectionRarity(
        val apiName: String,
        colourCode: String,
        val stack: ItemStack,
        var uniques: Int,
        var duplicates: Int,
        var chocolatePerSecond: Int = 0,
        var multiplier: Double = 0.0,
        var maximum: Int = 0
    ) {
        TOTAL("Total", "§c", totalRabbit, 0, 0),
        COMMON("Common", "§f", commonRabbit, 0, 0),
        UNCOMMON("Uncommon", "§a", uncommonRabbit, 0, 0),
        RARE("Rare", "§9", rareRabbit, 0, 0),
        EPIC("Epic", "§5", epicRabbit, 0, 0),
        LEGENDARY("Legendary", "§6", legendaryRabbit, 0, 0),
        MYTHIC("Mythic", "§d", totalRabbit, 0, 0),
        ;

        val displayName = "$colourCode$apiName"

        fun addAmount(amount: Int) {
            if (amount == 0) return
            this.uniques += 1
            this.duplicates += amount - 1
        }

        fun setChocolateData(cps: Int, multiplier: Double) {
            this.chocolatePerSecond = cps * uniques
            this.multiplier = multiplier * uniques
        }

        companion object {
            fun fromApiName(apiName: String): RabbitCollectionRarity? {
                return values().firstOrNull { it.apiName.lowercase() == apiName }
            }

            fun resetData() {
                values().forEach {
                    it.uniques = 0
                    it.duplicates = 0
                    it.chocolatePerSecond = 0
                    it.multiplier = 0.0
                    it.maximum = 0
                }
            }
        }
    }
}