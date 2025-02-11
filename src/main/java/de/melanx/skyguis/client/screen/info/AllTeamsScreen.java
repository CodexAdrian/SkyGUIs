package de.melanx.skyguis.client.screen.info;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import de.melanx.skyblockbuilder.data.SkyblockSavedData;
import de.melanx.skyblockbuilder.data.Team;
import de.melanx.skyguis.client.screen.BaseScreen;
import de.melanx.skyguis.client.screen.CreateTeamScreen;
import de.melanx.skyguis.client.widget.ClickableText;
import de.melanx.skyguis.client.widget.ScrollbarWidget;
import de.melanx.skyguis.config.ClientConfig;
import de.melanx.skyguis.tooltip.SmallTextTooltip;
import de.melanx.skyguis.util.ComponentBuilder;
import de.melanx.skyguis.util.Math2;
import de.melanx.skyguis.util.TextHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.*;
import java.util.stream.Collectors;

public class AllTeamsScreen extends BaseScreen {

    public static final int ENTRIES = 13;
    private static final Component TEAMS_COMPONENT = ComponentBuilder.text("teams").setStyle(Style.EMPTY.withBold(true));
    private static final Component MEMBERS_COMPONENT = ComponentBuilder.text("members").setStyle(Style.EMPTY.withBold(true));
    private static final Component YOUR_TEAM = ComponentBuilder.text("your_team");
    private final List<Team> teams;
    private final Team playerTeam;
    private ScrollbarWidget scrollbar;
    private Button yourTeamButton;

    public AllTeamsScreen() {
        super(TEAMS_COMPONENT, 200, 230);
        //noinspection ConstantConditions
        this.teams = SkyblockSavedData.get(Minecraft.getInstance().level).getTeams().stream()
                .filter(team -> !team.getName().isEmpty())
                .filter(team -> !team.isSpawn())
                .sorted(Comparator.comparing((team -> team.getName().toLowerCase(Locale.ROOT))))
                .collect(Collectors.toList());
        this.playerTeam = SkyblockSavedData.get(Minecraft.getInstance().level).getTeamFromPlayer(Minecraft.getInstance().player);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new AllTeamsScreen());
    }

    @Override
    protected void init() {
        if (this.playerTeam == null) {
            this.addRenderableWidget(new Button(this.x(10), this.y(199), 160, 20, ComponentBuilder.title("create_team"), button -> {
                Minecraft.getInstance().setScreen(new CreateTeamScreen());
            }));
            this.yourTeamButton = null;
        } else {
            MutableComponent component = Component.literal(this.playerTeam.getName());
            this.yourTeamButton = this.addRenderableWidget(new ClickableText(this.x(15) + TextHelper.stringLength(YOUR_TEAM), this.y(207), TextHelper.DARK_GREEN.getRGB(), component, button -> {
                Minecraft.getInstance().setScreen(new TeamEditScreen(this.playerTeam, this));
            }));
        }
        this.scrollbar = new ScrollbarWidget(this, this.xSize - 20, 33, 12, this.ySize - 45);
        this.updateScrollbar();
    }

    @Override
    public void render_(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render_(poseStack, mouseX, mouseY, partialTick);
        this.scrollbar.render(poseStack);
        this.font.draw(poseStack, TEAMS_COMPONENT, this.x(10), this.y(13), Color.DARK_GRAY.getRGB());
        int memberLength = this.font.width(MEMBERS_COMPONENT.getVisualOrderText());
        this.font.draw(poseStack, MEMBERS_COMPONENT, this.x(179) - memberLength, this.y(13), Color.DARK_GRAY.getRGB());

        if (this.playerTeam != null) {
            this.hLine(poseStack, this.x(8), this.x(this.xSize - 26), this.y(ENTRIES * 12 + 41), Color.GRAY.getRGB());
            this.font.draw(poseStack, YOUR_TEAM, (this.x(10)), this.y(207), Color.DARK_GRAY.getRGB());

            // need to render tooltip here to be on top of scrollbar
            if (this.yourTeamButton.isHovered) {
                this.renderTeamTooltip(poseStack, mouseX, mouseY, this.playerTeam);
            }
        }

        int j = 0;
        for (int i = this.scrollbar.getOffset(); i < this.teams.size(); i++) {
            if (j >= ENTRIES) break;
            Team team = this.teams.get(i);
            String name = team.getName();
            String s = TextHelper.shorten(this.font, name, 175 - memberLength);

            Component playerSizeComponent = Component.literal(String.valueOf(team.getPlayers().size()));
            Component teamNameComponent = Component.literal(s);
            float x = this.x(179 - (float) memberLength / 2 - (float) this.font.width(playerSizeComponent.getVisualOrderText()) / 2);
            int y = this.y(37 + j * 12);
            this.font.draw(poseStack, teamNameComponent, this.x(10), y, team.isEmpty() ? TextHelper.LIGHT_RED.getRGB() : TextHelper.DARK_GREEN.getRGB());
            this.font.draw(poseStack, playerSizeComponent, x, y, Color.DARK_GRAY.getRGB());
            boolean inBounds = Math2.isInBounds(this.x(10), y, this.font.width(teamNameComponent.getVisualOrderText()), 11, mouseX, mouseY);
            if (inBounds) {
                this.renderTeamTooltip(poseStack, mouseX, mouseY, team);
            }
            j++;
        }
    }

    private void renderTeamTooltip(@Nonnull PoseStack poseStack, int mouseX, int mouseY, @Nonnull Team team) {
        List<Component> textLines = Lists.newArrayList(Component.literal(team.getName()));
        List<Component> smallTextLines = Lists.newArrayList();
        if (this.minecraft != null && this.minecraft.options.advancedItemTooltips) {
            smallTextLines.add(ComponentBuilder.text("team_id").append(": " + team.getId().toString()));
        }
        smallTextLines.add(ComponentBuilder.text("members").append(": " + team.getPlayers().size()));
        smallTextLines.add(ComponentBuilder.text("created_at").append(": " + ClientConfig.date.format(new Date(team.getCreatedAt()))));
        smallTextLines.add(ComponentBuilder.text("last_changed").append(": " + ClientConfig.date.format(new Date(team.getLastChanged()))));
        this.renderTooltip(poseStack, textLines, Optional.of(new SmallTextTooltip(smallTextLines, Color.GRAY)), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        mouseX -= this.relX;
        mouseY -= this.relY;

        int entries = Math.min(ENTRIES, this.teams.size());
        if (Math2.isInBounds(10, 37, 175, entries * 12, mouseX, mouseY)) {
            int index = (int) ((mouseY - 37) / 12) + this.scrollbar.getOffset();
            Team team = this.teams.get(index);
            if (Math2.isInBounds(10, 37, this.font.width(team.getName()), entries * 12, mouseX, mouseY)) {
                if (team.hasPlayer(Minecraft.getInstance().player)) {
                    Minecraft.getInstance().setScreen(new TeamEditScreen(team, this));
                } else {
                    Minecraft.getInstance().setScreen(new TeamInfoScreen(team, this));
                }
            }

            return true;
        }

        return super.mouseClicked(mouseX + this.relX, mouseY + this.relY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.scrollbar.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.scrollbar.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return this.scrollbar.mouseScrolled(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }

    public void updateScrollbar() {
        this.scrollbar.setEnabled(this.teams.size() > ENTRIES);
        this.scrollbar.setMaxOffset(this.teams.size() - ENTRIES);
    }
}
