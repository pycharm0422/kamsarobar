package com.kamsarobar.media;

import org.springframework.core.io.Resource;

/**
 * Where photo bytes live. The default keeps them on local disk; for several backend instances,
 * add an S3 / Cloud Storage implementation of this interface - nothing else needs to change.
 */
public interface ImageStorage {

    void save(String key, byte[] content);

    /** Returns the stored file, or null if it does not exist. */
    Resource load(String key);

    void delete(String key);
}
