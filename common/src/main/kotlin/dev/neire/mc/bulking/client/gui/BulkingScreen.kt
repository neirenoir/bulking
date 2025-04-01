package dev.neire.mc.bulking.client.gui

import com.google.common.collect.Lists
import com.illusivesoulworks.diet.DietConstants
import com.illusivesoulworks.diet.api.type.IDietGroup
import com.illusivesoulworks.diet.api.util.DietColor
import com.illusivesoulworks.diet.client.DietKeys
import com.illusivesoulworks.diet.common.config.DietConfig
import com.illusivesoulworks.diet.common.data.suite.DietSuites
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import dev.neire.mc.bulking.common.BulkingFoodData
import dev.neire.mc.bulking.common.ComputedEffects
import dev.neire.mc.bulking.common.registries.BulkingAttributes
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

class BulkingScreen(
    private val fromInventory: Boolean,
) : Screen(
        Component.translatable("gui.${DietConstants.MOD_ID}.title"),
    ) {
    companion object {
        private val BACKGROUND = ResourceLocation("minecraft", "textures/gui/demo_background.png")
        private val ICONS = ResourceLocation(DietConstants.MOD_ID, "textures/gui/icons.png")
        private val WIDGETS = ResourceLocation("minecraft", "textures/gui/container/generic_54.png")
        var computedEffects: ComputedEffects? = null

        private fun coloredBlit(
            matrixStack: PoseStack,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            uOffset: Float,
            vOffset: Float,
            uWidth: Int,
            vHeight: Int,
            textureWidth: Int,
            textureHeight: Int,
            red: Int,
            green: Int,
            blue: Int,
            alpha: Int,
        ) {
            val x2 = x + width
            val y2 = y + height
            val minU = (uOffset + 0.0f) / textureWidth
            val maxU = (uOffset + uWidth) / textureWidth
            val minV = (vOffset + 0.0f) / textureHeight
            val maxV = (vOffset + vHeight) / textureHeight
            val matrix = matrixStack.last().pose()
            val bufferbuilder = Tesselator.getInstance().builder

            bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX)
            bufferbuilder
                .vertex(matrix, x.toFloat(), y2.toFloat(), 0f)
                .color(red, green, blue, alpha)
                .uv(minU, maxV)
                .endVertex()
            bufferbuilder
                .vertex(matrix, x2.toFloat(), y2.toFloat(), 0f)
                .color(red, green, blue, alpha)
                .uv(maxU, maxV)
                .endVertex()
            bufferbuilder
                .vertex(matrix, x2.toFloat(), y.toFloat(), 0f)
                .color(red, green, blue, alpha)
                .uv(maxU, minV)
                .endVertex()
            bufferbuilder
                .vertex(matrix, x.toFloat(), y.toFloat(), 0f)
                .color(red, green, blue, alpha)
                .uv(minU, minV)
                .endVertex()

            BufferUploader.drawWithShader(bufferbuilder.end())
        }
    }

    private val groups = HashSet<IDietGroup>()
    private val xSize = 248
    private var ySize = 0

    override fun init() {
        super.init()

        minecraft?.let { mc ->
            mc.player?.let { player ->
                mc.level?.let { level ->
                    if (player.foodData is BulkingFoodData) {
                        val tracker =
                            (player.foodData as BulkingFoodData).dietTracker
                        DietSuites
                            .getSuite(level, tracker.suite)
                            .map { it.groups }
                            .orElse(emptySet())
                            .let { groups.addAll(it) }
                    }
                }
            }
        }

        ySize = groups.size * 20 + 60

        addRenderableWidget(
            Button
                .builder(Component.translatable("gui.diet.close")) { button ->
                    minecraft?.let { mc ->
                        mc.player?.let { player ->
                            if (fromInventory) {
                                mc.setScreen(InventoryScreen(player))
                            } else {
                                onClose()
                            }
                        }
                    }
                }.size(55, 20)
                .pos(width / 2 + xSize / 2 - 55 - 20, (height + ySize) / 2 - 30)
                .build(),
        )
    }

    override fun render(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float,
    ) {
        renderBackground(guiGraphics)
        renderForeground(guiGraphics, mouseX, mouseY)
        renderTitle(guiGraphics, mouseX, mouseY)
        renderStomachSlots(guiGraphics)
        renderDisplayItems(guiGraphics, mouseX, mouseY)
        super.render(guiGraphics, mouseX, mouseY, partialTicks)
    }

    private fun renderStomachSlots(guiGraphics: GuiGraphics) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, WIDGETS)

        val slotSize = 19
        val slotSpacing = 24
        val slotU = 6
        val slotV = 16

        val startX = width / 2 - xSize / 2 + 10
        val startY = (height + ySize) / 2 - 30 - slotSize / 3 + 1

        val player = Minecraft.getInstance().player ?: return
        val stomachSize =
            player.getAttribute(BulkingAttributes.STOMACH_SIZE_ATTRIBUTE)?.value
                ?: return
        for (i in 0 until stomachSize.toInt()) {
            val x = startX + (i * slotSpacing)

            // Use GuiGraphics to draw the slot (newer Minecraft versions)
            guiGraphics.blit(
                WIDGETS,
                x,
                startY,
                25,
                25,
                slotU.toFloat(),
                slotV.toFloat(),
                slotSize,
                slotSize,
                256,
                256, // Texture dimensions
            )
        }
    }

    private fun renderDisplayItems(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) {
        val xPos = width / 2 - xSize / 2 + 10 + 3
        val yPos = (height + ySize) / 2 - 30

        val player = Minecraft.getInstance().player ?: return
        if (player.foodData !is BulkingFoodData) {
            return
        }
        val stomach = (player.foodData as BulkingFoodData).dietTracker.stomach
        // Render each item with spacing
        stomach.forEachIndexed { index, entry ->
            val stack = entry.first
            val offset = index * 24 + 2
            guiGraphics.renderItem(stack, xPos + offset, yPos)

            // If mouse is hovering over this item, show tooltip
            if (mouseX >= xPos + offset &&
                mouseX < xPos + offset + 16 &&
                mouseY >= yPos &&
                mouseY < yPos + 16
            ) {
                guiGraphics.renderTooltip(font, stack, mouseX, mouseY)
            }
        }
    }

    fun renderTitle(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) {
        val titleWidth = font.width(title.string)
        guiGraphics.drawString(
            font,
            title,
            width / 2 - titleWidth / 2,
            height / 2 - ySize / 2 + 10,
            getTextColor(),
            false,
        )

        minecraft?.let { mc ->
            val computed = computedEffects ?: return
            if (computed.first.isNotEmpty() || computed.second.isNotEmpty()) {
                val lowerX = width / 2 + titleWidth / 2 + 5
                val lowerY = height / 2 - ySize / 2 + 7
                val upperX = lowerX + 16
                val upperY = lowerY + 16
                guiGraphics.blit(ICONS, lowerX, lowerY, 16, 16, 0f, 37f, 16, 16, 256, 256)

                if (mouseX in lowerX..upperX && mouseY in lowerY..upperY) {
                    val tooltips = BulkingTooltip.getEffects()
                    guiGraphics.renderComponentTooltip(font, tooltips, mouseX, mouseY)
                }
            }
        }
    }

    private fun getTextColor(): Int {
        val config = DietConfig.CLIENT.textColor.get()

        return if (config.startsWith("#")) {
            Integer.parseInt(config.substring(1), 16)
        } else {
            Integer.parseInt(config)
        }
    }

    fun renderForeground(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) {
        minecraft?.level?.let { level ->
            minecraft?.player?.let { player ->
                if (player.foodData is BulkingFoodData) {
                    val diet = (player.foodData as BulkingFoodData).dietTracker
                    DietSuites.getSuite(level, diet.getSuite()).ifPresent { suite ->
                        var y = height / 2 - ySize / 2 + 25
                        val x = width / 2 - xSize / 2 + 10
                        var tooltipComponent: Component? = null

                        for (group in suite.groups) {
                            guiGraphics.renderItem(ItemStack(group.icon), x, y - 5)
                            val text =
                                Component.translatable(
                                    "groups.${DietConstants.MOD_ID}.${group.name}.name",
                                )
                            guiGraphics.drawString(font, text, x + 20, y, getTextColor(), false)

                            RenderSystem.setShader(GameRenderer::getPositionColorTexShader)
                            RenderSystem.setShaderTexture(0, ICONS)

                            val color = if (diet.isActive()) group.color else DietColor.GRAY
                            val red = color.red()
                            val green = color.green()
                            val blue = color.blue()

                            var percent = (diet.getValue(group.name) * 100.0f).toInt()

                            val percentText = "$percent%"

                            coloredBlit(
                                guiGraphics.pose(),
                                x + 90,
                                y + 2,
                                102,
                                5,
                                20f,
                                0f,
                                102,
                                5,
                                256,
                                256,
                                red,
                                green,
                                blue,
                                255,
                            )

                            if (percent > 0) {
                                val texWidth = percent + 1
                                coloredBlit(
                                    guiGraphics.pose(),
                                    x + 90,
                                    y + 2,
                                    texWidth,
                                    5,
                                    20f,
                                    5f,
                                    texWidth,
                                    5,
                                    256,
                                    256,
                                    red,
                                    green,
                                    blue,
                                    255,
                                )
                            }

                            val xPos = x + 200
                            val yPos = y + 1

                            guiGraphics.drawString(font, percentText, xPos + 1, yPos, 0, false)
                            guiGraphics.drawString(font, percentText, xPos - 1, yPos, 0, false)
                            guiGraphics.drawString(font, percentText, xPos, yPos + 1, 0, false)
                            guiGraphics.drawString(font, percentText, xPos, yPos - 1, 0, false)
                            guiGraphics.drawString(font, percentText, xPos, yPos, color.rgb, false)

                            val lowerY = y - 5
                            val upperX = x + 16
                            val upperY = lowerY + 16

                            if (mouseX in x..upperX && mouseY in lowerY..upperY) {
                                val key = "groups.${DietConstants.MOD_ID}.${group.name}.tooltip"

                                if (Language.getInstance().has(key)) {
                                    tooltipComponent = Component.translatable(key)
                                }
                            }

                            y += 20
                        }

                        tooltipComponent?.let {
                            val tooltips = Lists.newArrayList(it)
                            guiGraphics.renderComponentTooltip(font, tooltips, mouseX, mouseY)
                        }
                    }
                }
            }
        }
    }

    override fun renderBackground(guiGraphics: GuiGraphics) {
        super.renderBackground(guiGraphics)

        minecraft?.let {
            val i = (width - xSize) / 2
            val j = (height - ySize) / 2
            guiGraphics.blit(BACKGROUND, i, j, xSize, 4, 0f, 0f, 248, 4, 256, 256)
            guiGraphics.blit(BACKGROUND, i, j + 4, xSize, ySize - 8, 0f, 4f, 248, 24, 256, 256)
            guiGraphics.blit(BACKGROUND, i, j + ySize - 4, xSize, 4, 0f, 162f, 248, 4, 256, 256)
        }
    }

    override fun keyPressed(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
    ): Boolean {
        minecraft?.let { mc ->
            mc.player?.let { player ->
                if (mc.options.keyInventory.matches(keyCode, scanCode)) {
                    mc.setScreen(InventoryScreen(player))
                    return true
                } else if (DietKeys.OPEN_GUI.matches(keyCode, scanCode)) {
                    if (fromInventory) {
                        mc.setScreen(InventoryScreen(player))
                    } else {
                        onClose()
                    }
                    return true
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun isPauseScreen(): Boolean = false
}
