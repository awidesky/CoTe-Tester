package io.github.awidesky.coTe;

import io.github.awidesky.coTe.exception.CoTeException;

public record Result(ResultType result, CoTeException coteException) {
	@Override
	public String toString() {
		return result + ((coteException == null) ? "" : " (%s)".formatted(coteException.getMessage()));
	}
}
