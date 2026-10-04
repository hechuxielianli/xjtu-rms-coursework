package com.example.rms.shared.application;
/** Executes a graph mutation only after a database-wide lock, in its owned local transaction. */
public interface GraphWriteExecutor {
    @FunctionalInterface interface Work<T> { T run() throws Exception; }
    <T> T execute(Work<T> work);
    void requireActive();
}
