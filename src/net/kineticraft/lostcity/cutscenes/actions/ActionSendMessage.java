package net.kineticraft.lostcity.cutscenes.actions;

import net.kineticraft.lostcity.cutscenes.CutsceneAction;
import net.kineticraft.lostcity.cutscenes.CutsceneEvent;

/**
 * Send a message to the players in the cutscene.
 * Created by Kneesnap on 7/22/2017.
 */
public class ActionSendMessage extends CutsceneAction {

    private String message;

    @Override
    public void execute(CutsceneEvent event) {
        event.getStatus().getPlayers().forEach(p -> p.sendMessage(String.format(message, p.getName())));
    }
}
