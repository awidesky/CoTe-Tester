package io.github.awidesky.coTe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.github.awidesky.guiUtil.Logger;

public class StringFeeder implements Consumer<OutputStream> {

	private final List<String> list;
	private final AtomicInteger idx = new AtomicInteger(-1);
	private Logger l = Logger.nullLogger;
	private boolean slowed = false;
	
	public StringFeeder(Path file, boolean slowed) throws IOException {
		this.slowed = slowed;
		List<String> l = Files.lines(file, StandardCharsets.UTF_8).toList();
		list = new ArrayList<String>(l.size() + 1);
		list.addAll(l);
		list.add("\n");
	}
	
	public void setLogger(Logger l) {
		this.l = l;
	}

	public String getElementOf(int index) {
		return list.get(index);
	}
	
	public int getIndex() {
		return idx.get();
	}
	
	@Override
	public void accept(OutputStream o) {
		System.out.println("ready writeline");
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(o))) {
			for(String s : list) {
				bw.write(s);
				bw.newLine();
				bw.flush();
				idx.incrementAndGet();
				System.out.println("writeline : " + s);
				l.debug(s);
				Thread.yield();
				if(slowed ) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e1) {
			l.error(e1);
		}
	}

}
