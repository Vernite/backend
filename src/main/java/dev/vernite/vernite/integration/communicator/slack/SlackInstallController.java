package dev.vernite.vernite.integration.communicator.slack;

import javax.servlet.annotation.WebServlet;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackOAuthAppServlet;

@WebServlet("/integration/slack/install")
public class SlackInstallController extends SlackOAuthAppServlet {
    public SlackInstallController(App app) {
        super(app);
    }
}
