package com.vyson.urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * [Q5] Blacklist Service that manages blacklisted API keys loaded dynamically from an external config file.
 *
 * The config file is read dynamically at RUNTIME:
 * By checking file.lastModified(), any addition, modification, or removal of keys in the blacklist
 * configuration file is detected immediately without needing to restart the Spring Boot application.
 */
@Service
public class BlacklistService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);

    private final Path configFilePath;
    private final AtomicLong lastModifiedTimestamp = new AtomicLong(0L);
    private final AtomicReference<Set<String>> blacklistedKeysCache = new AtomicReference<>(Collections.emptySet());

    public BlacklistService(@Value("${blacklist.file.path:config/blacklist.txt}") String filePath) {
        this.configFilePath = Path.of(filePath);
        loadBlacklistIfModified();
    }

    /**
     * Checks if the given API key is blacklisted.
     * Evaluates file modification timestamp at runtime before checking.
     */
    public boolean isBlacklisted(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        loadBlacklistIfModified();
        return blacklistedKeysCache.get().contains(apiKey.trim());
    }

    /**
     * Dynamically reads the blacklist config file if it has been updated on disk.
     */
    public synchronized void loadBlacklistIfModified() {
        File file = configFilePath.toFile();
        if (!file.exists()) {
            if (lastModifiedTimestamp.get() != 0L) {
                log.warn("Blacklist file {} was deleted or does not exist. Clearing blacklist cache.", configFilePath);
                blacklistedKeysCache.set(Collections.emptySet());
                lastModifiedTimestamp.set(0L);
            }
            return;
        }

        long currentModified = file.lastModified();
        if (currentModified > lastModifiedTimestamp.get()) {
            try {
                List<String> lines = Files.readAllLines(configFilePath);
                Set<String> keys = new HashSet<>();
                for (String line : lines) {
                    String trimmed = line.trim();
                    // Skip empty lines and comment lines starting with #
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        keys.add(trimmed);
                    }
                }
                blacklistedKeysCache.set(Collections.unmodifiableSet(keys));
                lastModifiedTimestamp.set(currentModified);
                log.info("Blacklist config reloaded dynamically at runtime from {}. Loaded {} blocked keys.",
                        configFilePath, keys.size());
            } catch (IOException e) {
                log.error("Failed to read blacklist config file at runtime: {}", e.getMessage());
            }
        }
    }

    /**
     * Helper to retrieve currently blacklisted keys (e.g. for inspection or testing).
     */
    public Set<String> getBlacklistedKeys() {
        loadBlacklistIfModified();
        return blacklistedKeysCache.get();
    }

    /**
     * Programmatic helper to add a key to the config file at runtime (useful for testing runtime updates).
     */
    public synchronized void addKeyToConfigFile(String apiKey) throws IOException {
        File file = configFilePath.toFile();
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        List<String> existing = file.exists() ? Files.readAllLines(configFilePath) : new java.util.ArrayList<>();
        if (!existing.contains(apiKey)) {
            existing.add(apiKey);
            Files.write(configFilePath, existing);
            loadBlacklistIfModified();
        }
    }

    /**
     * Programmatic helper to remove a key from the config file at runtime.
     */
    public synchronized void removeKeyFromConfigFile(String apiKey) throws IOException {
        File file = configFilePath.toFile();
        if (file.exists()) {
            List<String> existing = Files.readAllLines(configFilePath);
            existing.removeIf(line -> line.trim().equals(apiKey));
            Files.write(configFilePath, existing);
            loadBlacklistIfModified();
        }
    }
}
