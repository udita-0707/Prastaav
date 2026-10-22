package com.prastav.audit;

/**
 * Small write-only port for audit entries (Interface Segregation): the listener needs to append,
 * nothing else, so it depends on this one-method interface rather than on a repository.
 */
public interface AuditRecorder {

    void record(AuditLog entry);
}
