package com.org.prime.helper.cache;

import java.util.List;

public interface PrimeBucketCache {
    List<Long> getPrimesUpTo(Long requestedLimit);
}
