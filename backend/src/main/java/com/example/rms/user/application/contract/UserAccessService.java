package com.example.rms.user.application.contract;
import com.example.rms.shared.application.Actor;
import java.util.List;
/** Public owner projection boundary; no repositories or JPA entities cross this contract. */
public interface UserAccessService {
    UserSummaryData readSummary(long userId);
    /** Restricted credential projection: immutable, distinct by userId, at most two alias candidates. */
    List<AuthenticationIdentity> readCandidatesForAuthentication(String login);
    Actor requireEnabledActor(long userId);
    Actor requireEnabledAssignee(long userId);
}
