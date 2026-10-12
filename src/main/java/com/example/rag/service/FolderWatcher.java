package com.example.rag.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

/**
 * Watches the docs folder and re-indexes shortly after files are added, changed or removed.
 * Events are debounced so copying a large file doesn't trigger many rebuilds.
 */
@Component
public class FolderWatcher {

    private static final Logger log = LoggerFactory.getLogger(FolderWatcher.class);
    private static final long QUIET_MS = 1500;

    private final IndexService index;
    private WatchService watchService;

    public FolderWatcher(IndexService index) {
        this.index = index;
    }

    @PostConstruct
    void start() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        index.docsDir().register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

        Thread t = new Thread(this::loop, "folder-watcher");
        t.setDaemon(true);
        t.start();
        log.info("Watching {} for changes.", index.docsDir().toAbsolutePath());
    }

    private void loop() {
        try {
            while (true) {
                WatchKey key = watchService.take();
                key.pollEvents();
                key.reset();

                // Debounce: keep draining until the folder is quiet.
                while ((key = watchService.poll(QUIET_MS, TimeUnit.MILLISECONDS)) != null) {
                    key.pollEvents();
                    key.reset();
                }
                try {
                    index.refreshIfChanged();
                } catch (Exception e) {
                    log.warn("Re-index failed: {}", e.getMessage());
                }
            }
        } catch (InterruptedException | ClosedWatchServiceException e) {
            // shutting down
        }
    }

    @PreDestroy
    void stop() throws IOException {
        if (watchService != null) watchService.close();
    }
}
