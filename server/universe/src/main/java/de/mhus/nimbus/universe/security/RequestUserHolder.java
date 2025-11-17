package de.mhus.nimbus.universe.security;

import org.springframework.stereotype.Component;

@Component
public class RequestUserHolder {
    private static final ThreadLocal<CurrentUser> TL = new ThreadLocal<>();
    public void set(CurrentUser user) { TL.set(user); }
    public CurrentUser get() { return TL.get(); }
    public void clear() { TL.remove(); }
}

