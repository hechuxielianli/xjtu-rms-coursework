package com.example.rms.audit.application.port;
import com.example.rms.audit.domain.AuditEntry;
public interface AuditAppendPort { void append(AuditEntry entry); }
