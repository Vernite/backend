package com.workflow.workflow.user;

import java.util.Date;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
        if (req != null && req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (c.getName().equals(AuthController.COOKIE_NAME)) {
                    Optional<UserSession> session = userSessionRepository.findBySession(c.getValue());
                    if (!session.isPresent()) {
                        break;
                    }
                    UserSession us = session.get();
                    us.setIp(req.getHeader("X-Forwarded-For"));
                    if (us.getIp() == null) {
                        us.setIp(req.getRemoteAddr());
                    }
                    us.setLastUsed(new Date());
                    userSessionRepository.save(us);
                    return us.getUser();
                }
            }
        }
        if (parameter.hasParameterAnnotation(NotNull.class)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not logged");
        } else {
            return null;
        }
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == User.class;
    }
}
