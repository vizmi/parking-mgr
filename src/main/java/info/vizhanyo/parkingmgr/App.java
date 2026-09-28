package info.vizhanyo.parkingmgr;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@SpringBootApplication
public class App implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	private final JedisPool jedisPool;
	private final String emptyLots;
	private final String filledLots;
	private final int lots;

	public App(JedisPool jedisPool,
			@Value("${redis.empty.lots}") String emptyLots,
			@Value("${redis.filled.lots}") String filledLots,
			@Value("${lots}") int lots) {
		this.jedisPool = jedisPool;
		this.emptyLots = emptyLots;
		this.filledLots = filledLots;
		this.lots = lots;
	}

	@Override
	public void run(ApplicationArguments arg0) throws Exception {
		logger.info("Initalizing database...");
		fillLot();

		logger.info("...Initalization completed");
	}

	private void fillLot() {
		try (Jedis jedis = jedisPool.getResource()) {
			// Delete old data is any
			jedis.del(emptyLots);
			jedis.del(filledLots);

			for(int i = 0; i < lots; i++) {
				jedis.zadd(emptyLots, i, Integer.toString(i));
			}
			logger.info("created {} new lots", lots);
		}
	}
}
