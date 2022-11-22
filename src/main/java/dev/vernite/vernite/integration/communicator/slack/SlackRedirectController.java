package dev.vernite.vernite.integration.communicator.slack;

import javax.servlet.annotation.WebServlet;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackOAuthAppServlet;

@WebServlet("/integration/slack/oauth_redirect")
public class SlackRedirectController extends SlackOAuthAppServlet {
    public SlackRedirectController(App app) {
        super(app);
    }
}
