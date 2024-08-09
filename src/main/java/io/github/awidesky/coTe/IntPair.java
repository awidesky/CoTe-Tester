package io.github.awidesky.coTe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntPair implements Comparable<IntPair> {
	public int week;
	public int prob;
	
	public IntPair(String str) {
		Matcher m = Pattern.compile("\\D*(\\d+)\\D+(\\d+)\\D*").matcher(str);
		if(!m.find()) System.out.println(str);;
		week = Integer.parseInt(m.group(1));
		prob = Integer.parseInt(m.group(2));
	}

	public int getWeek() {
		return week;
	}
	public int getProb() {
		return prob;
	}
	
	@Override
	public int hashCode() {
		return week * 10 + prob;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof IntPair other)
			return prob == other.prob && week == other.week;
		if (obj instanceof String other)
			return other.equals(week + "\\D+" + prob);
		if (obj instanceof Integer other)
			return other == hashCode();
		
		return false;
	}

	@Override
	public String toString() {
		return week + "_" + prob;
	}

	@Override
	public int compareTo(IntPair o) {
		int ret = week - o.week;
		if(ret == 0) ret = prob - o.prob;
		return ret;
	}
	
}
