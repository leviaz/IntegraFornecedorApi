package br.com.sankhya.medic.metodospadrao;

import java.math.BigDecimal;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.wrapper.JapeFactory;

public class Updates {

	public void attPK(String instancia, String campoPk, BigDecimal valorpk, String campoatt, Object valoratt)
			throws Exception {
		JapeSession.SessionHandle hnd = null;
		try {
			hnd = JapeSession.open();

			JapeFactory.dao(instancia).prepareToUpdateByPK(campoPk, valorpk).set(campoatt, valoratt).update();

		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			JapeSession.close(hnd);
		}
	}

}
