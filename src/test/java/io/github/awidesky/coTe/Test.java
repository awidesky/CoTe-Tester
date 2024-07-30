package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.awidesky.coTe.exception.CompileErrorException;

class Test {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		//ProcessExecutor.runNow(new SimpleLogger(System.out, true), null, "echo", "$SHELL");
		try (CoTe ct = new CoTe(1, 1)) {}
		Files.list(Paths.get("probs/out")).parallel().forEach(t -> {
			try {
				Files.deleteIfExists(t);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	@AfterAll
	static void tearDownAfterClass() {
		System.out.flush();
		System.err.flush();
	}

	@org.junit.jupiter.api.Test
	void test() throws IOException {
		Arrays.stream(new File("probs/test_codes").listFiles())
				.filter(f -> f.getName().endsWith(".cpp"))
				.forEach(f -> {
					Scanner sc = new Scanner(f.getName().replace(".cpp", ""));
					sc.useDelimiter(Pattern.quote("_"));
					int i = sc.nextInt();
					int j = sc.nextInt();
					sc.close();
					System.out.println("[INFO] Prob : " + (i + "_" + j));
					System.out.flush();
					try (CoTe ct = new CoTe(i, j)) {
						ct.test(f);
					} catch (CompileErrorException e) {
						System.err.println("[ERROR] Compile Error!");
						e.printStackTrace();
					}
					System.out.println();
				});
	}

}
