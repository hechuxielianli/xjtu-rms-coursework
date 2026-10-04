package com.example.rms.version.application;
import com.example.rms.version.application.contract.VersionData;
import com.example.rms.version.application.port.VersionStore;
import com.example.rms.shared.application.*;
import com.example.rms.shared.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class VersionQueryService {
    private final VersionStore store;private final CurrentActor current;
    public VersionQueryService(VersionStore store,CurrentActor current) { this.store=store;this.current=current; }
    @Transactional(readOnly=true) public PageData<VersionData> list(String id,int page,int size) { current.require().requireAny(RoleCode.values());long requirement=InputPolicy.decimalId(id);var paging=new Paging(page,size);exists(requirement);return store.list(requirement,paging); }
    @Transactional(readOnly=true) public VersionData read(String id,String versionId) { current.require().requireAny(RoleCode.values());long requirement=InputPolicy.decimalId(id),version=InputPolicy.decimalId(versionId);exists(requirement);return store.read(requirement,version); }
    private void exists(long id) { if(!store.requirementExists(id))throw new RmsException(ErrorCode.NOT_FOUND); }
}
