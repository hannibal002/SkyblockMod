package me.Danker.features;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.hud.Hud;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import me.Danker.config.CfgConfig;
import me.Danker.config.ModConfig;
import me.Danker.handlers.TextRenderer;
import me.Danker.utils.RenderUtils;
import me.Danker.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class CakeTimer {

    public static double cakeTime;
    public static final ResourceLocation CAKE_ICON = new ResourceLocation("dsm", "icons/cake.png");

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

        if (!Utils.inSkyblock) return;
        if (message.contains(":")) return;

        if (message.contains("Yum! You gain +") && message.contains(" for 48 hours!")) {
            cakeTime = System.currentTimeMillis() / 1000 + 172800; // Add 48 hours
            CfgConfig.writeDoubleConfig("misc", "cakeTime", cakeTime);
        }
    }

    public static class CakeTimerHud extends Hud {

        @Exclude
        String exampleText = ModConfig.getColour(cakeColour) + "11h16m";

        @Dropdown(
                name = "Cake Text Color",
                options = {"Black", "Dark Blue", "Dark Green", "Dark Aqua", "Dark Red", "Dark Purple", "Gold", "Gray", "Dark Gray", "Blue", "Green", "Aqua", "Red", "Light Purple", "Yellow", "White"}
        )
        public static int cakeColour = 6;

        @Override
        protected void draw(UMatrixStack matrices, float x, float y, float scale, boolean example) {
            if (example) {
                Minecraft mc = Minecraft.getMinecraft();
                double scaleReset = Math.pow(scale, -1);
                GL11.glScaled(scale, scale, scale);

                mc.getTextureManager().bindTexture(CAKE_ICON);
                RenderUtils.drawModalRectWithCustomSizedTexture(x / scale, y / scale, 0, 0, 16, 16, 16, 16);

                GL11.glScaled(scaleReset, scaleReset, scaleReset);
                TextRenderer.drawHUDText(exampleText, x + 20 * scale, y + 5 * scale, scale);
                return;
            }

            if (enabled && Utils.inSkyblock) {
                Minecraft mc = Minecraft.getMinecraft();
                double scaleReset = Math.pow(scale, -1);
                GL11.glScaled(scale, scale, scale);

                mc.getTextureManager().bindTexture(CAKE_ICON);
                RenderUtils.drawModalRectWithCustomSizedTexture(x / scale, y / scale, 0, 0, 16, 16, 16, 16);

                GL11.glScaled(scaleReset, scaleReset, scaleReset);
                TextRenderer.drawHUDText(getText(), x + 20 * scale, y + 5 * scale, scale);
            }
        }

        @Override
        protected float getWidth(float scale, boolean example) {
            return (RenderUtils.getWidthFromText(example ? exampleText : getText()) + 20 * scale) * scale;
        }

        @Override
        protected float getHeight(float scale, boolean example) {
            return (RenderUtils.getHeightFromText(example ? exampleText : getText()) + 5 * scale) * scale;
        }

        String getText() {
            double timeNow = System.currentTimeMillis() / 1000;
            if (cakeTime - timeNow < 0) {
                return EnumChatFormatting.RED + "NONE";
            } else {
                return ModConfig.getColour(cakeColour) + Utils.getTimeBetween(timeNow, cakeTime);
            }
        }

    }

}
