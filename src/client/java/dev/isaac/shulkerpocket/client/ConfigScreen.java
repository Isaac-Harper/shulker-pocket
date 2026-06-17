package dev.isaac.shulkerpocket.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Vanilla-widget config screen reachable from Mod Menu ({@link ModMenuIntegration}).
 *
 * <p>Edits a working copy of the {@link ClientConfig} options and only writes them back (and to
 * disk) when the player clicks <em>Done</em>; <em>Cancel</em> discards them, <em>Reset</em> restores
 * defaults. No custom rendering (everything is a renderable widget), so this avoids the 26.1
 * render-extraction API. All labels and tooltips go through {@code Component.translatable} (see
 * {@code lang/en_us.json}).
 */
public final class ConfigScreen extends Screen {
    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_SPACING = 24;
    private static final int ROWS = 6;

    private final Screen parent;

    // Working copies, seeded from the live config and committed on Done.
    private boolean invertScroll;
    private int cooldownMs;
    private boolean allowEmptyPosition;
    private boolean playSounds;
    private boolean useActivationKey;
    private boolean showTooltipHint;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("shulker_pocket.config.title"));
        this.parent = parent;
        seedFrom(ShulkerPocketClient.config);
    }

    private void seedFrom(ClientConfig cfg) {
        this.invertScroll = cfg.invertScroll;
        this.cooldownMs = cfg.cooldownMs;
        this.allowEmptyPosition = cfg.allowEmptyPosition;
        this.playSounds = cfg.playSounds;
        this.useActivationKey = cfg.useActivationKey;
        this.showTooltipHint = cfg.showTooltipHint;
    }

    private static Tooltip tip(String key) {
        return Tooltip.create(Component.translatable(key));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int x = cx - WIDGET_WIDTH / 2;
        int y = this.height / 2 - (ROWS * ROW_SPACING) / 2;

        StringWidget titleWidget = new StringWidget(this.title, this.font);
        titleWidget.setX(cx - titleWidget.getWidth() / 2);
        titleWidget.setY(y - 28);
        addRenderableWidget(titleWidget);

        addRenderableWidget(CycleButton.onOffBuilder(this.invertScroll)
            .withTooltip(v -> tip("shulker_pocket.config.invert_scroll.tooltip"))
            .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                Component.translatable("shulker_pocket.config.invert_scroll"),
                (button, value) -> this.invertScroll = value));
        y += ROW_SPACING;

        addRenderableWidget(new CooldownSlider(x, y, WIDGET_WIDTH, WIDGET_HEIGHT));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.onOffBuilder(this.allowEmptyPosition)
            .withTooltip(v -> tip("shulker_pocket.config.bare_hands_stop.tooltip"))
            .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                Component.translatable("shulker_pocket.config.bare_hands_stop"),
                (button, value) -> this.allowEmptyPosition = value));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.onOffBuilder(this.playSounds)
            .withTooltip(v -> tip("shulker_pocket.config.play_sounds.tooltip"))
            .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                Component.translatable("shulker_pocket.config.play_sounds"),
                (button, value) -> this.playSounds = value));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.booleanBuilder(
                Component.translatable("shulker_pocket.config.activation.key"),
                Component.translatable("shulker_pocket.config.activation.sneak")
                //? if >=1.21.11 {
                , this.useActivationKey
                //?}
                )
            //? if <1.21.11 {
            /*.withInitialValue(this.useActivationKey)*/
            //?}
            .withTooltip(v -> tip("shulker_pocket.config.activation.tooltip"))
            .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                Component.translatable("shulker_pocket.config.activation"),
                (button, value) -> this.useActivationKey = value));
        y += ROW_SPACING;

        addRenderableWidget(CycleButton.onOffBuilder(this.showTooltipHint)
            .withTooltip(v -> tip("shulker_pocket.config.tooltip_hint.tooltip"))
            .create(x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
                Component.translatable("shulker_pocket.config.tooltip_hint"),
                (button, value) -> this.showTooltipHint = value));

        int by = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("shulker_pocket.config.reset"),
                b -> resetDefaults())
            .bounds(cx - 102, by - 24, 204, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
            .bounds(cx - 104, by, 100, WIDGET_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> { commit(); onClose(); })
            .bounds(cx + 4, by, 100, WIDGET_HEIGHT).build());
    }

    private void resetDefaults() {
        seedFrom(new ClientConfig());
        rebuildWidgets(); // re-init so every widget reflects the restored defaults
    }

    private void commit() {
        ClientConfig cfg = ShulkerPocketClient.config;
        cfg.invertScroll = this.invertScroll;
        cfg.cooldownMs = this.cooldownMs;
        cfg.allowEmptyPosition = this.allowEmptyPosition;
        cfg.playSounds = this.playSounds;
        cfg.useActivationKey = this.useActivationKey;
        cfg.showTooltipHint = this.showTooltipHint;
        cfg.save();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    /** Slider over {@code [COOLDOWN_MIN_MS, COOLDOWN_MAX_MS]}, snapped to 10&nbsp;ms steps. */
    private final class CooldownSlider extends AbstractSliderButton {
        CooldownSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                (double) (cooldownMs - ClientConfig.COOLDOWN_MIN_MS)
                    / (ClientConfig.COOLDOWN_MAX_MS - ClientConfig.COOLDOWN_MIN_MS));
            setTooltip(tip("shulker_pocket.config.cooldown.tooltip"));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("shulker_pocket.config.cooldown", cooldownMs));
        }

        @Override
        protected void applyValue() {
            int range = ClientConfig.COOLDOWN_MAX_MS - ClientConfig.COOLDOWN_MIN_MS;
            int raw = ClientConfig.COOLDOWN_MIN_MS + (int) Math.round(this.value * range);
            cooldownMs = Mth.clamp((raw / 10) * 10,
                ClientConfig.COOLDOWN_MIN_MS, ClientConfig.COOLDOWN_MAX_MS);
        }
    }
}
