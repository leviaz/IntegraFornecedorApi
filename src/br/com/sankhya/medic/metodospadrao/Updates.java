package br.com.sankhya.medic.metodospadrao;

import java.math.BigDecimal;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class Updates {

	public void attPK(String instancia, BigDecimal valorpk, String campoatt, Object valoratt)
			throws Exception {
		JapeSession.SessionHandle hnd = null;
		try {
			hnd = JapeSession.open();

			JapeFactory.dao(instancia).prepareToUpdateByPK(valorpk).set(campoatt, valoratt).update();

		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			JapeSession.close(hnd);
		}
	}
	
	public void attPKITE( BigDecimal nunota, BigDecimal sequencia, BigDecimal idItem)
			throws Exception {
		JapeSession.SessionHandle hnd = null;
		try {
			hnd = JapeSession.open();

			JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).prepareToUpdateByPK(nunota, sequencia)
			.set("AD_IDITEM", idItem)
			.update();

		} catch (Exception e) {
			throw new Exception(e);
		} finally {
			JapeSession.close(hnd);
		}
	}

}
