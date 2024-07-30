package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
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
				.sorted(Comparator.comparing(f -> {
					return Integer.parseInt(f.getName().replaceAll("[_(\\.cpp)]", ""));
				}))
				.map(f -> {
					Scanner sc = new Scanner(f.getName().replace(".cpp", ""));
					sc.useDelimiter(Pattern.quote("_"));
					int i = sc.nextInt();
					int j = sc.nextInt();
					boolean r = false;
					sc.close();
					System.out.println("[INFO] Prob : " + (i + "_" + j));
					System.out.flush();
					try (CoTe ct = new CoTe(i, j)) {
						r = ct.test(f);
					} catch (CompileErrorException e) {
						System.err.println("[ERROR] Compile Error!");
						e.printStackTrace();
					}
					System.out.println();
					return new Result(i, j, r);
				})
				.map(Result::toString)
				.toList()
				.forEach(System.out::println);
	}

	private class Result {
		public final int week;
		public final int prob;
		public final boolean correct;

		public Result(int week, int prob, boolean correct) {
			super();
			this.week = week;
			this.prob = prob;
			this.correct = correct;
		}

		@Override
		public String toString() {
			return "Week " + week + ", prob " + prob + " : " + (correct ? "Correct" : "Wrong_answer");
		}
		
	}
}
