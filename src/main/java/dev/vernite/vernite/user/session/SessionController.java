/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.user.session;

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

import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import reactor.netty.http.client.HttpClient;

@RestController
@RequestMapping("/session")
public class SessionController {
    private static final Logger L = Logger.getLogger("GeoIP");
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final WebClient CLIENT = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(1))))
            .build();
    private static final Map<String, GeoIP> geoip = new ConcurrentHashMap<>();

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Value("${maxmindPassword}")
    private String maxmindPassword;

    @Operation(summary = "List all active sessions", description = "This method returns array of all sessions. Result can be empty array.")
    @ApiResponse(responseCode = "200", description = "List of all active sessions. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserSession.class)))
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
                    .header("Authorization", "Basic " + maxmindPassword)
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

    @Operation(summary = "Revoke session", description = "This method is used to revoke session. On success does not return anything.")
    @ApiResponse(responseCode = "200", description = "Session revoked")
    @ApiResponse(responseCode = "403", description = "Cannot revoke current (active) session or another user")
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User loggedUser,
            @Parameter(hidden = true) @CookieValue(AuthController.COOKIE_NAME) String session, @PathVariable long id) {
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
