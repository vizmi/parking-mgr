package info.vizhanyo.parkingmgr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Proves the README's concurrency claim against a real Redis instance (not a mock):
 * with more cars arriving simultaneously than there are lots, every lot is handed out
 * exactly once and the rest are correctly turned away.
 */
@Testcontainers
class PMServiceConcurrencyTest {

    private static final int LOTS = 5;
    private static final int CARS = 25;
    private static final String EMPTY_LOTS = "test|empty.lots";
    private static final String FILLED_LOTS = "test|filled.lots";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private static JedisPool jedisPool;
    private PMService service;

    @BeforeAll
    static void startPool() {
        jedisPool = new JedisPool(redis.getHost(), redis.getMappedPort(6379));
    }

    @AfterAll
    static void stopPool() {
        jedisPool.close();
    }

    @BeforeEach
    void seedLots() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(EMPTY_LOTS);
            jedis.del(FILLED_LOTS);
            for (int i = 0; i < LOTS; i++) {
                jedis.zadd(EMPTY_LOTS, i, Integer.toString(i));
            }
        }
        service = new PMService(jedisPool, EMPTY_LOTS, FILLED_LOTS);
    }

    @Test
    void concurrentArrivals_neverDoubleAssignALot() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CARS);
        CountDownLatch startLine = new CountDownLatch(1);

        List<Callable<String>> arrivals = IntStream.range(0, CARS)
                .<Callable<String>>mapToObj(i -> () -> {
                    startLine.await();
                    try {
                        return service.enter("CAR-" + i);
                    } catch (IllegalStateException noLotsAvailable) {
                        return null;
                    }
                })
                .collect(Collectors.toList());

        List<Future<String>> futures = new ArrayList<>();
        for (Callable<String> arrival : arrivals) {
            futures.add(pool.submit(arrival));
        }
        startLine.countDown();

        List<String> assignedLots = new ArrayList<>();
        for (Future<String> future : futures) {
            String lot = future.get(10, TimeUnit.SECONDS);
            if (lot != null) {
                assignedLots.add(lot);
            }
        }
        pool.shutdown();

        assertEquals(LOTS, assignedLots.size(), "exactly as many cars as there are lots should get in");
        assertEquals(LOTS, new HashSet<>(assignedLots).size(), "no lot should ever be handed out twice");
    }
}
