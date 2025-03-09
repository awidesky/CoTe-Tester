package io.github.awidesky.coTe;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.awidesky.coTe.exception.CoTeException;
import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.RunErrorException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.StringLogger;

class Test {
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Compiler.getCompiler();
		System.out.println();
		
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
		List<Result> res = Arrays.stream(new File("probs/test_codes").listFiles())
				.parallel()
				.filter(f -> f.getName().matches("\\d{1,2}_\\d{1,2}\\.cpp"))
				.map(f -> {
					StringLogger l = new StringLogger();
					l.setPrintLogLevel(true);
					IntPair p = new IntPair(f.getName());
					boolean r = false;
					
					l.info("Prob : " + p.toString());
					try (CoTe ct = new CoTe(p)) {
						ct.setLogger(l);
						r = ct.test(f);
					} catch (CoTeException e) {
						l.error(e);
					} catch (IOException e1) {
						l.error(e1);
					}
					l.newLine();
					return new Result(p, r, l.getString());
				})
				.sorted().toList();
		
		System.out.println();
		res.forEach(Result::printLog);
		System.out.println();
		res.forEach(Result::printResult);
	}

	@org.junit.jupiter.api.Test
	void errorTest() {
		checkThrows("probs/test_codes/1_3_compileError.cpp", CompileErrorException.class);
		checkThrows("probs/test_codes/1_3_runError.cpp", RunErrorException.class);
	}
	private void checkThrows(String file, Class<? extends CoTeException> exceptionClass) {
		ConsoleLogger l = new ConsoleLogger();
		l.setPrintLogLevel(true);
		l.newLine();
		IntPair p = new IntPair("1_3");
		try (CoTe ct = new CoTe(p)) {
			ct.setLogger(l);
			l.info(assertThrows(exceptionClass, () -> ct.test(new File(file))).toString());
		} catch (IOException e1) {
			l.error(e1);
		}
		l.newLine();
	}
	private class Result implements Comparable<Result> {
		public final IntPair probPair;
		public final boolean correct;
		public final String log;

		public Result(IntPair prob, boolean correct, String log) {
			this.probPair = prob;
			this.correct = correct;
			this.log = log;
		}

		public void printLog() {
			System.out.println(log);
		}
		
		public void printResult() {
			System.out.printf("Week %2d, prob %d : %s\n", probPair.week, probPair.prob, correct ? "Correct" : "Wrong_answer");
		}

		@Override
		public int compareTo(Result o) {
			return probPair.compareTo(o.probPair);
		}
	}
}
