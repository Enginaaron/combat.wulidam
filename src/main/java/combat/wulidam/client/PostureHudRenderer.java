package combat.wulidam.client;

import combat.wulidam.SoulsLikeCombatClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

// draws the yellow posture bar on the screen
public class PostureHudRenderer implements HudRenderCallback {
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;
    private static final int OFFSET_X = 10;
    private static final int OFFSET_Y = 10;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        int screenHeight = drawContext.getScaledWindowHeight();
        
        float posture = SoulsLikeCombatClient.getCurrentPosture();
        float maxPosture = SoulsLikeCombatClient.getMaxPosture();

        if (maxPosture > 0) {
            float postureFraction = posture / maxPosture;
            int currentPostureBarWidth = (int) (BAR_WIDTH * postureFraction);

            int x = OFFSET_X;
            // 2 pixels above stamina bar
            int y = screenHeight - OFFSET_Y - (BAR_HEIGHT * 2) - 2;

            // draw background (dark gray)
            drawContext.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xAA000000);

            // yellow posture bar
            drawContext.fill(x, y, x + currentPostureBarWidth, y + BAR_HEIGHT, 0xFFFFCC00);
        }
    }
}
