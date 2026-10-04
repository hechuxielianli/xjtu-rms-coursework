package com.example.rms.shared.application;
import java.util.Optional;
/** Consumer port; auth supplies SecurityContext + current database state adapter. */
public interface CurrentActor {
    Actor require();
    Optional<Actor> optional();
}
