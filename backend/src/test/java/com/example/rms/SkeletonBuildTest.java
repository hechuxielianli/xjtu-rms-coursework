package com.example.rms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import static org.junit.jupiter.api.Assertions.*;

/** Confirms compiled readiness shell only; not a business acceptance test. */
class SkeletonBuildTest {
    @Test
    void usesFrozenJavaAndBootVersion() {
        assertEquals(21, Runtime.version().feature());
        assertEquals("3.5.16", SpringBootVersion.getVersion());
        assertNotNull(RmsApplication.class);
    }
}
