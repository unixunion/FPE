package com.kegans.sandbox;

import com.kegans.sandbox.config.SpringAsyncConfig;
import com.kegans.sandbox.ff1.impl.EmailAddress;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@EnableAsync
@Configuration
@ContextConfiguration(classes = {SpringAsyncConfig.class})
public class SandboxApplicationTests {

	private static Logger logger = LoggerFactory.getLogger(SandboxApplicationTests.class);


	@Autowired
	GenericHasher genericHasher;


	@Test
	public void testAddress() throws ExecutionException, InterruptedException {
		String address = "23 walaby way, sydney, Australia";
		Future<byte[]> result = genericHasher.encrypt(address);
		logger.info(new String(result.get()));
		Assert.assertEquals("17 MKLMpR nFL, pVPEfY, IrKFxEtyK", new String(result.get()));
	}


	@Test
	public void testCCNumber() throws ExecutionException, InterruptedException {
		String address = "2123123454316123";
		Future<byte[]> result = genericHasher.encrypt(address);
		logger.info(new String(result.get()));
		Assert.assertEquals("0969811618440442", new String(result.get()));
	}


	@Test
	public void testCCNumber2() throws ExecutionException, InterruptedException {
		String address = "2123 1234 5431 6123";
		Future<byte[]> result = genericHasher.encrypt(address);
		logger.info(new String(result.get()));
		Assert.assertEquals("2315 8970 5849 0665", new String(result.get()));
	}


	@Test
	public void testCollisions() {

		SequencedNumbers sequencedNumbers = new SequencedNumbers(99999L);
		ConcurrentHashMap<String, String> resultX = new ConcurrentHashMap<>();
		long total = sequencedNumbers.max;

		List<String> input = new CopyOnWriteArrayList<>();

		logger.info("generating input data");
		while (sequencedNumbers.hasNext()) {
			input.add(sequencedNumbers.getNext());
		}

		Instant timeStart = Instant.now();

		final long[] collisions = {0};

		logger.info("hashing");
		input.stream()
				.forEach(i -> {
					Future<byte[]> result = genericHasher.encrypt(i);

					try {
						String collision = resultX.put(String.valueOf(result.get()), i);

						if (collision!=null) {
							logger.warn("collision: {} == {}", i, collision);
							collisions[0]++;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				});


		Instant timeEnd = Instant.now();

		logger.info("encrypt rate: {}", (resultX.size()/Duration.between(timeStart, timeEnd).toSeconds()));
		logger.info("Counting collisions");

//		Assert.assertTrue(total-resultX.size()<=2);

		Assert.assertEquals(0, collisions[0]);

		logger.info("collision rate: {} in {}", total-resultX.size(), total);
		logger.info("collisions: {}", collisions[0]);
	}


	@Test
	public void alphabetTest() throws IOException {

		ConcurrentHashMap<String, String> alphabet = new ConcurrentHashMap<>();

		InputStream inputStream =
				getClass()
						.getClassLoader()
						.getResourceAsStream("first_names.all.txt");

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		while (reader.ready()) {
			String name = reader.readLine();

			for (char b : name.toCharArray()) {
				alphabet.put(String.valueOf(b), "");
			}

		}

		final String[] result = {""};
		alphabet.forEach((c,v) -> {
			result[0] = result[0] + c;
		});

		logger.info(result[0]);


	}


	@Test
	public void nameCollisions() throws IOException, ExecutionException, InterruptedException {

		ConcurrentHashMap<String, String> resultX = new ConcurrentHashMap<>();

		long collisionCount=0;
		long totalCount=0;
		long skipped=0;

		InputStream inputStream =
				getClass()
						.getClassLoader()
						.getResourceAsStream("first_names.all.txt");

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		while (reader.ready()) {

			String data = reader.readLine();
//			logger.info("data: '{}'", data);
			try {
				Future<byte[]> h = genericHasher.encrypt(data);
				totalCount++;
				String collision = resultX.put(String.valueOf(h.get()), data);

				if (collision != null) {
					logger.warn("data collision: {} == {}", data, collision);
					collisionCount++;
				}
			} catch (Exception e) {
				skipped++;
			}
 		}

		logger.info("total: {}, collisions: {}, skipped: {}", totalCount, collisionCount, skipped);

		Assert.assertEquals(0, collisionCount);

	}


	@Test
	public void ccnumbersCollisions() throws IOException, ExecutionException, InterruptedException {

		ConcurrentHashMap<String, String> resultX = new ConcurrentHashMap<>();

		long collisionCount=0;
		long totalCount=0;
		long skipped=0;

		InputStream inputStream =
				getClass()
						.getClassLoader()
						.getResourceAsStream("ccnumbers.txt");

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		while (reader.ready()) {

			String data = reader.readLine();
			try {
				Future<byte[]> h = genericHasher.encrypt(data);
				totalCount++;
				String collision = resultX.put(String.valueOf(h.get()), data);

				if (collision != null) {
					logger.warn("data collision: {} == {}", data, collision);
					collisionCount++;
				}
			} catch (Exception e) {
				e.printStackTrace();
				skipped++;
			}
		}

		logger.info("total: {}, collisions: {}, skipped: {}", totalCount, collisionCount, skipped);

		Assert.assertEquals(0, collisionCount);

	}

	@Test
	public void first_last_collisions() throws IOException, ExecutionException, InterruptedException {

		ConcurrentHashMap<String, String> resultX = new ConcurrentHashMap<>();

		long collisionCount=0;
		long totalCount=0;
		long skipped=0;

		InputStream inputStream =
				getClass()
						.getClassLoader()
						.getResourceAsStream("first_last.txt");

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		while (reader.ready()) {

			String data = reader.readLine();

//			StringTokenizer st = new StringTokenizer(data);
//			logger.info(String.valueOf(st.countTokens()));


			try {
				Future<byte[]> h = genericHasher.encrypt(data);
				totalCount++;
				String collision = resultX.put(String.valueOf(h.get()), data);

				if (collision != null) {
					logger.warn("data collision: {} == {}", data, collision);
					collisionCount++;
				}
			} catch (Exception e) {
				logger.info(data);
				e.printStackTrace();
				skipped++;
			}
		}

		logger.info("total: {}, collisions: {}, skipped: {}", totalCount, collisionCount, skipped);

		Assert.assertEquals(0, collisionCount);

	}


	@Test
	public void ssn_collisions() throws IOException, ExecutionException, InterruptedException {

		ConcurrentHashMap<String, String> resultX = new ConcurrentHashMap<>();

		long collisionCount=0;
		long totalCount=0;
		long skipped=0;

		InputStream inputStream =
				getClass()
						.getClassLoader()
						.getResourceAsStream("ssn.txt");

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		while (reader.ready()) {

			String data = reader.readLine();
			try {
				Future<byte[]> h = genericHasher.encrypt(data);
				totalCount++;
				String collision = resultX.put(String.valueOf(h.get()), data);

				if (collision != null) {
					logger.warn("data collision: {} == {}", data, collision);
					collisionCount++;
				}
			} catch (Exception e) {
				skipped++;
			}
		}

		logger.info("total: {}, collisions: {}, skipped: {}", totalCount, collisionCount, skipped);

		Assert.assertEquals(0, collisionCount);
	}


	@Test
	public void testTokenizer() {
		StringTokenizer st = new StringTokenizer("the quick brown fox", genericHasher.getDelminators());
		Assert.assertEquals(4, st.countTokens());

		st.asIterator().forEachRemaining(token -> {
			logger.info(String.valueOf(token));
		});

		st = new StringTokenizer("Lynette Oyola", genericHasher.getDelminators());
		Assert.assertEquals(2, st.countTokens());

	}


	@Test
	public void testEmailAddress() throws ExecutionException, InterruptedException {
		EmailAddress emailAddress = new EmailAddress().setValue("abe@lincoln.com");
		Future<byte[]> enc = genericHasher.ff1(emailAddress);
		logger.info(new String(enc.get()));

	}



	public interface SequenceGenerator {
		String getNext();
		boolean hasNext();
		long getMax();
	}

	public class SequencedNumbers implements SequenceGenerator {

		private AtomicLong value = new AtomicLong(0);
		private long max;

		public SequencedNumbers(long max) {
			this.max = max;
		}

		@Override
		public synchronized String getNext() {
			String next = String.valueOf(value.incrementAndGet());
			while (next.length()<16) {
				next = "0" + next;
			}
			return next;
		}

		@Override
		public boolean hasNext() {
			if (value.getAcquire()< max){
				return true;
			} else {
				return false;
			}
		}

		public long getMax() {
			return max;
		}

	}





}
