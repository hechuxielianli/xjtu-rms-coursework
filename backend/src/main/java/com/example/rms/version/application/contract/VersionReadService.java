package com.example.rms.version.application.contract;
import com.example.rms.version.application.port.VersionStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class VersionReadService {
    private final VersionStore store;
    public VersionReadService(VersionStore store) { this.store=store; }
    @Transactional(propagation=Propagation.MANDATORY,readOnly=true) public VersionData requireWithin(long requirementId,long versionId) { return store.read(requirementId,versionId); }
}
