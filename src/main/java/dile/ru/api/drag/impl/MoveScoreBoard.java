package dile.ru.api.drag.impl;

public final class MoveScoreBoard extends HudPanel {

    public MoveScoreBoard() {
        super("movescoreboard", "Move ScoreBoard", 760.0F, 5.0F, 12.0F, 12.0F);
    }

    @Override
    public void setHudVisible(boolean visible) {
        super.setHudVisible(visible);
        drag.visible(false);
        ScoreBoardHud scoreBoardHud = ScoreBoardHud.getInstance();
        if (scoreBoardHud != null) {
            scoreBoardHud.getDrag().locked(!visible);
            if (!visible) {
                scoreBoardHud.resetToVanilla();
            }
        }
    }

    @Override
    public void render() {
    }
}
