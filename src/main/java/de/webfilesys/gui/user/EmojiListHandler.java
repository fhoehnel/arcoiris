package de.webfilesys.gui.user;

import de.webfilesys.EmojiManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.ArrayList;

public class EmojiListHandler extends UserRequestHandler {

	public EmojiListHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}	
	
	protected void process() {
        resp.setContentType("application/json");
        output.println("{\"emojis\": [");
        ArrayList<String> emojiList = EmojiManager.getInstance().getEmoticons();
        if (emojiList != null) {
            boolean first = true;
            for (String emoji : emojiList) {
                String emojiName = emoji.substring(0, emoji.lastIndexOf('.'));
                if (first) {
                    first = false;
                } else {
                    output.print(",");
                }
                output.println("\"" + emojiName + "\"");
            }
        }
        output.println("]}");
        output.flush();
	}
}
