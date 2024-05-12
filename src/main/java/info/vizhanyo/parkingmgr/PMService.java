package info.vizhanyo.parkingmgr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

@Service
public class PMService {

	private static final Logger logger = LoggerFactory.getLogger(PMService.class);

    @Value("${redis.url}")
	private String url;

	@Value("${redis.empty.lots}")
	private String emptyLots;

	@Value("${redis.filled.lots}")
	private String filledLots;

    public String enter(String licPlate) {
        logger.info("{} entering", licPlate);

        try (Jedis jedis = new Jedis(url)) {
            if (jedis.hexists(filledLots, licPlate))
                throw new IllegalArgumentException("License plate %s already in the lot".formatted(licPlate));
            
            // atomic get/remove from the empty lots list
            Tuple lotTuple = jedis.zpopmin(emptyLots);
            if (lotTuple == null)
                throw new IllegalStateException("No lots available");

            String lot = lotTuple.getElement();
            jedis.hset(filledLots, licPlate, lot);
            return lot;
        }

    }

    public void exit(String licPlate) {
        logger.info("{} exiting", licPlate);
        try (Jedis jedis = new Jedis(url)) {
            String lot = jedis.hget(filledLots, licPlate);
            if (lot == null)
                throw new IllegalStateException("Car with license plate %s not found in the parking lot".formatted(licPlate));

            jedis.hdel(filledLots, licPlate);
            jedis.zadd(emptyLots, Integer.parseInt(lot), lot);
        }

    }
}
