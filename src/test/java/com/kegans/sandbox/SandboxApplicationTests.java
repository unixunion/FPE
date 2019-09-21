package com.kegans.sandbox;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.http.HttpClient;
import java.security.KeyStore;


@RunWith(SpringRunner.class)
@SpringBootTest
public class SandboxApplicationTests {

	private static Logger logger = LoggerFactory.getLogger(SandboxApplicationTests.class);

	HttpClient httpClient;
	SSLContext sslContext;
	TrustManagerFactory tmf;
	KeyStore keyStore;

	@Test
	public void contextLoads() {

	}

	@Test
	public void testAddress() {
		String address = "23 walaby way, sydney, Australia";
		byte[] result = new GenericHasher().hash(address, address.length());
		logger.info(new String(result));
		Assert.assertEquals("98 gwvuZA Oba, PZuUem, lctaKKLQv", new String(result));
	}

	@Test
	public void testCCNumber() {
		String address = "2123123454316123";
		byte[] result = new GenericHasher().hash(address, address.length());
		logger.info(new String(result));
		Assert.assertEquals("3554737756478847", new String(result));
	}

	@Test
	public void testCCNumber2() {
		String address = "2123 1234 5431 6123";
		byte[] result = new GenericHasher().hash(address, address.length());
		logger.info(new String(result));
		Assert.assertEquals("8766 2375 8255 2419", new String(result));
	}



}
