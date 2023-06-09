package com.example.geode.third;

import com.example.geode.common.dto.Customer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ThreadUtils;
import org.apache.geode.cache.GemFireCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class CustomerService {

    private static final long SLEEP_IN_SECONDS = 2;

    private final AtomicBoolean cacheMiss = new AtomicBoolean(false);

    private final AtomicLong customerId = new AtomicLong(0L);

    private volatile Long sleepInSeconds;

    @Cacheable("CustomersByName")
    public Customer findBy(String name) throws InterruptedException {
        setCacheMiss();
        ThreadUtils.sleep(Duration.ofSeconds(getSleepInSeconds()));
        // ThreadUtils.safeSleep(name, Duration.ofSeconds(getSleepInSeconds()));
        return Customer.newCustomer(this.customerId.incrementAndGet(), name);
    }

    @Autowired
    GemFireCache cache;
    public void removeCache(String region) {
        log.info("before clearing cache:{}", cache.getRegion(region).values());
        cache.getRegion(region)
                .removeAll(cache.getRegion(region).keySet());
        log.info("after clearing cache:{}", cache.getRegion(region).values());
    }

    public Collection<Object> listCacheByRegion(String region) {
        return cache.getRegion(region).values();
    }

    public boolean isCacheMiss() {
        return this.cacheMiss.compareAndSet(true, false);
    }

    protected void setCacheMiss() {
        this.cacheMiss.set(true);
    }

    public Long getSleepInSeconds() {

        Long sleepInSeconds = this.sleepInSeconds;

        return sleepInSeconds != null ? sleepInSeconds : SLEEP_IN_SECONDS;
    }

    public void setSleepInSeconds(Long seconds) {
        this.sleepInSeconds = seconds;
    }
}