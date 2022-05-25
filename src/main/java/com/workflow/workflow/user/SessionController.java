package com.workflow.workflow.user;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import reactor.netty.http.client.HttpClient;

@RestController
@RequestMapping("/session")
public class SessionController {
    private static final Logger L = Logger.getLogger("GeoIP");
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final String USER_AND_PASSWD = "NzIyOTM1OlFyQjlROGZ4UDd4NHl6aUg=";
    private static final WebClient CLIENT = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(1))))
            .build();
    private static final Map<String, GeoIP> geoip = new ConcurrentHashMap<>();

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Operation(summary = "List all active sessions", description = "This method returns array of all sessions. Result can be empty array.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all active sessions. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserSession.class)))
            })
    })
    @GetMapping
    public Future<List<UserSession>> all(@NotNull @Parameter(hidden = true) User loggedUser,
            @Parameter(hidden = true) @CookieValue(AuthController.COOKIE_NAME) String session) {
        List<UserSession> sessions = userSessionRepository.findByUser(loggedUser);
        HashSet<String> ips = new HashSet<>();
        long t = System.currentTimeMillis();
        for (UserSession s : sessions) {
            s.setCurrent(s.getSession().equals(session));
            GeoIP g = geoip.get(s.getIp());
            if (g != null && t - g.getCache() > TimeUnit.DAYS.toMillis(1)) {
                geoip.remove(s.getIp());
                g = null;
            }
            if (g == null) {
                ips.add(s.getIp());
            } else {
                s.setGeoip(g);
            }
        }
        CountDownLatch countDownLatch = new CountDownLatch(ips.size());
        for (String ip : ips) {
            CLIENT.get()
                    .uri("https://geolite.info/geoip/v2.1/city/" + ip)
                    .header("Authorization", "Basic " + USER_AND_PASSWD)
                    .retrieve().bodyToMono(MaxmindResponse.class)
                    .subscribe(n -> {
                        GeoIP geoIP = new GeoIP();
                        geoIP.setCache(t);
                        if (n.getCity() != null && n.getCity().getNames().containsKey("en")) {
                            geoIP.setCity(n.getCity().getNames().get("en"));
                        }
                        if (n.getCountry() != null && n.getCountry().getNames().containsKey("en")) {
                            geoIP.setCountry(n.getCountry().getNames().get("en"));
                        }
                        geoip.put(ip, geoIP);
                        for (UserSession s : sessions) {
                            if (s.getIp().equals(ip)) {
                                s.setGeoip(geoIP);
                            }
                        }
                        countDownLatch.countDown();
                    }, (e) -> {
                        L.warning("Failed: " + e);
                        countDownLatch.countDown();
                    });
        }
        CompletableFuture<List<UserSession>> f = new CompletableFuture<>();
        EXECUTOR_SERVICE.execute(() -> {
            try {
                countDownLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            f.complete(sessions);
        });
        return f;
    }

    @Operation(summary = "Revoke session.", description = "This method is used to revoke session. On success does not return anything.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session revoked"),
            @ApiResponse(responseCode = "403", description = "Cannot revoke current (active) session or another user")
    })
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User loggedUser,
            @Parameter(hidden = true) @CookieValue(AuthController.COOKIE_NAME) String session, @PathVariable int id) {
        UserSession sess = this.userSessionRepository.findById(id).orElse(null);
        if (sess == null) {
            return;
        }
        if (sess.getSession().equals(session)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "cannot revoke current session, click logout");
        }
        if (sess.getUser().getId() != loggedUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "cannot revoke session with given ID");
        }
        this.userSessionRepository.delete(sess);
    }
}
