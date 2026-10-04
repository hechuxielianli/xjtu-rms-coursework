package com.example.rms.auth.application;
import com.example.rms.shared.application.*;
import org.springframework.stereotype.Service;
@Service
public class SessionService {
    private final CurrentActor actor;
    public SessionService(CurrentActor actor) { this.actor=actor; }
    public Actor current() { return actor.require(); }
}
