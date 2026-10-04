package com.example.rms.user.application.port;
import com.example.rms.user.application.UserData;
import com.example.rms.shared.application.PageData;
import com.example.rms.shared.domain.*;
import java.time.Instant;
import java.util.Set;
public interface UserStore {
    PageData<UserData> list(Paging paging);
    UserData lock(long id);
    UserData create(String username,String email,String displayName,String hash,Set<RoleCode> roles,long actorId,Instant now);
    UserData edit(long id,String username,String email,String displayName,Instant now);
    UserData roles(long id,Set<RoleCode> roles,long actorId,Instant now);
    UserData enabled(long id,boolean enabled,Instant now);
}
