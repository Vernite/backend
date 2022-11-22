package dev.vernite.vernite.integration.communicator.slack;

import javax.servlet.annotation.WebServlet;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackAppServlet;

@WebServlet("/integration/slack/events")
public class SlackEventsController extends SlackAppServlet {
    public SlackEventsController(App app) {
        super(app);
    }
}
