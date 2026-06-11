package combat.wulidam.client;

import combat.wulidam.SoulsLikeCombatClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;

public class StaminaHudRenderer implements HudRenderCallback {

    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;
    private static final int OFFSET_X = 10;
    private static final int OFFSET_Y = 10;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        int screenHeight = drawContext.getScaledWindowHeight();
        
        float stamina = SoulsLikeCombatClient.getCurrentStamina();
        float maxStamina = SoulsLikeCombatClient.getMaxStamina();
        
        if (maxStamina <= 0) return;
        
        float staminaFraction = stamina / maxStamina;
        int currentBarWidth = (int) (BAR_WIDTH * staminaFraction);
        
        int x = OFFSET_X;
        int y = screenHeight - OFFSET_Y - BAR_HEIGHT;
        
        // Draw background (dark gray)
        drawContext.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xAA000000);
        
        // Draw stamina bar (blue)
        // 0xFF3366FF is a nice blue
        drawContext.fill(x, y, x + currentBarWidth, y + BAR_HEIGHT, 0xFF3366FF);
    }
}
