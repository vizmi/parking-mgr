package info.vizhanyo.parkingmgr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;

@ExtendWith(MockitoExtension.class)
class PMServiceTest {

    private static final String EMPTY_LOTS = "test|empty.lots";
    private static final String FILLED_LOTS = "test|filled.lots";

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private PMService service;

    @BeforeEach
    void setUp() {
        when(jedisPool.getResource()).thenReturn(jedis);
        service = new PMService(jedisPool, EMPTY_LOTS, FILLED_LOTS);
    }

    @Test
    void enter_throwsWhenLicensePlateAlreadyParked() {
        when(jedis.hexists(FILLED_LOTS, "ABC-123")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.enter("ABC-123"));
    }

    @Test
    void enter_throwsWhenNoLotsAvailable() {
        when(jedis.hexists(FILLED_LOTS, "ABC-123")).thenReturn(false);
        when(jedis.zpopmin(EMPTY_LOTS)).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.enter("ABC-123"));
    }

    @Test
    void enter_assignsSmallestAvailableLotAndRecordsIt() {
        when(jedis.hexists(FILLED_LOTS, "ABC-123")).thenReturn(false);
        when(jedis.zpopmin(EMPTY_LOTS)).thenReturn(new Tuple("3", 3.0));

        String lot = service.enter("ABC-123");

        assertEquals("3", lot);
        org.mockito.Mockito.verify(jedis).hset(FILLED_LOTS, "ABC-123", "3");
    }

    @Test
    void exit_throwsWhenLicensePlateNotParked() {
        when(jedis.hget(FILLED_LOTS, "ABC-123")).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> service.exit("ABC-123"));
    }

    @Test
    void exit_freesTheLotAndClearsTheMapping() {
        when(jedis.hget(FILLED_LOTS, "ABC-123")).thenReturn("3");

        service.exit("ABC-123");

        org.mockito.Mockito.verify(jedis).hdel(FILLED_LOTS, "ABC-123");
        org.mockito.Mockito.verify(jedis).zadd(EMPTY_LOTS, 3, "3");
    }
}
