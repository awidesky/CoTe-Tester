package io.github.awidesky.coTe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import io.github.awidesky.coTe.compiler.CompilerTester;
import io.github.awidesky.coTe.exception.CoTeException;
import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.coTe.exception.RunErrorException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.StringLogger;

class Test {
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		CompilerTester.getCompiler();
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
		List<TestResult> res = Arrays.stream(new File("probs/test_codes").listFiles())
				.parallel()
				.filter(f -> f.getName().matches("\\d{1,2}_\\d{1,2}\\.cpp"))
				.map(f -> {
					StringLogger l = new StringLogger();
					l.setPrintLogLevel(true);
					IntPair p = new IntPair(f.getName());
					Result r = null;
					
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
					return new TestResult(p, r, l.getString());
				})
				.sorted().toList();
		
		System.out.println();
		res.forEach(TestResult::printLog);
		System.out.println();
		res.forEach(TestResult::printResult);
		
		List<TestResult> l = res.stream().filter(t -> t.result.result() != ResultType.CORRECT).toList();
		assertEquals(0, l.size(), "Failed problems : " + l.stream().map(t -> t.probPair).map(IntPair::toString).collect(Collectors.joining(", ")));
	}

	@org.junit.jupiter.api.Test
	void errorTest() throws CompileFailedException {
		checkThrows("probs/test_codes/1_3_compileError.cpp", CompileErrorException.class);
		checkThrows("probs/test_codes/1_3_runError.cpp", RunErrorException.class);
		
		ConsoleLogger l = new ConsoleLogger();
		l.setPrintLogLevel(true);
		l.newLine();
		try (CoTe ct = new CoTe(new IntPair("1_3"))) {
			ct.setLogger(l);
			assertEquals(ResultType.WRONG_ANSWER, ct.test(new File("probs/test_codes/1_3_wrongAnswer.cpp")).result());
		} catch (Exception e1) {
			l.error(e1);
		}
		l.newLine();
	}
	private void checkThrows(String file, Class<? extends CoTeException> exceptionClass) throws CompileFailedException {
		ConsoleLogger l = new ConsoleLogger();
		l.setPrintLogLevel(true);
		l.newLine();
		IntPair p = new IntPair("1_3");
		try (CoTe ct = new CoTe(p)) {
			ct.setLogger(l);
			assertInstanceOf(exceptionClass, ct.test(new File(file)).coteException());
		} catch (IOException e1) {
			l.error(e1);
		}
		l.newLine();
	}
	private class TestResult implements Comparable<TestResult> {
		public final IntPair probPair;
		public final Result result;
		public final String log;

		public TestResult(IntPair prob, Result r, String log) {
			this.probPair = prob;
			this.result = r;
			this.log = log;
		}

		public void printLog() {
			System.out.println(log);
		}
		
		public void printResult() {
			System.out.printf("Week %2d, prob %d : %s\n", probPair.week, probPair.prob, result.toString());
		}

		@Override
		public int compareTo(TestResult o) {
			return probPair.compareTo(o.probPair);
		}
	}
}
