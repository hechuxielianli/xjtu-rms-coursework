package com.example.rms.user.application.contract;
import com.example.rms.shared.application.Actor;
/** Restricted to server authentication; NEVER serialize this internal credential projection. */
public record AuthenticationIdentity(Actor actor,String passwordHash) {
    @Override public String toString() { return "AuthenticationIdentity[credential=REDACTED]"; }
}
