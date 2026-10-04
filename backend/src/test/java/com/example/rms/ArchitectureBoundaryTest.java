package com.example.rms;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArchitectureBoundaryTest {
    private static final Map<String,Set<String>> DAG=Map.ofEntries(Map.entry("auth",Set.of("user","audit","shared")),Map.entry("user",Set.of("audit","shared")),Map.entry("requirement",Set.of("user","audit","shared")),Map.entry("review",Set.of("requirement","version","audit","shared")),Map.entry("version",Set.of("shared")),Map.entry("change",Set.of("requirement","version","audit","shared")),Map.entry("relation",Set.of("requirement","audit","shared")),Map.entry("comment",Set.of("requirement","audit","shared")),Map.entry("audit",Set.of("shared")),Map.entry("shared",Set.of()));
    @Test void sourceDependenciesFollowOwnerContracts()throws Exception {
        Pattern imports=Pattern.compile("import com\\.example\\.rms\\.([a-z]+)\\.([a-z]+)([^;]*);");
        try(var files=Files.walk(Path.of("src/main/java/com/example/rms"))) {
            for(Path p:files.filter(f->f.toString().endsWith(".java")).toList()) {
                String text=Files.readString(p);String rel=Path.of("src/main/java/com/example/rms").relativize(p).toString().replace('\\','/');String[] parts=rel.split("/");if(parts.length<3)continue;
                String owner=parts[0],layer=parts[1];assertTrue(DAG.containsKey(owner),"Unknown module");
                Matcher m=imports.matcher(text);while(m.find()) {
                    String other=m.group(1),otherLayer=m.group(2),tail=m.group(3);
                    if(!owner.equals(other)){assertTrue(DAG.get(owner).contains(other),()->"Forbidden module import in "+rel);if(!other.equals("shared"))assertTrue(otherLayer.equals("application") && tail.startsWith(".contract"),()->"Entity/internal contract crossing in "+rel);}
                    if(layer.equals("domain") || layer.equals("application"))assertNotEquals("api",otherLayer,"Business layer imports API");
                    if(layer.equals("domain"))assertNotEquals("application",otherLayer,"Domain imports application");
                }
                if(rel.endsWith("Controller.java")){assertFalse(text.contains("Repository"),"Controller Repository access");assertFalse(text.contains("EntityManager"),"Controller persistence access");assertFalse(text.matches("(?s).*import com\\.example\\.rms\\.[^;]*\\.infrastructure\\.persistence\\.[^;]*;.*"),"Controller imports persistence types");}
            }
        }
    }
}
