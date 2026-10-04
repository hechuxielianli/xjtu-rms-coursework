package com.example.rms.shared.application;
/** Technical failure port: only safe method/path metadata, never a body or credentials. */
public interface HttpWriteFailureObserver { void inputFailed(String method,String path); }
