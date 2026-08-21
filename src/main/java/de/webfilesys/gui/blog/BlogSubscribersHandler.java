package de.webfilesys.gui.blog;

import de.webfilesys.InvitationManager;
import de.webfilesys.gui.user.UserRequestHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BlogSubscribersHandler extends UserRequestHandler {

	public BlogSubscribersHandler(
    		HttpServletRequest req, 
    		HttpServletResponse resp,
            HttpSession session,
            PrintWriter output, 
            String uid) {
        super(req, resp, session, output, uid);
	}	
	
	protected void process() {
        resp.setContentType("application/json");
        output.println("{\"subscribers\": [");

        ArrayList<String> subscriberList = InvitationManager.getInstance().listSubscribers(uid, getCwd());
        if (subscriberList != null) {
            boolean first = true;
            for (String subscriber : subscriberList) {
                if (first) {
                    first = false;
                } else {
                    output.print(",");
                }
                output.println("\"" + subscriber + "\"");
            }
        }
        output.println("]}");
        output.flush();
	}
}
