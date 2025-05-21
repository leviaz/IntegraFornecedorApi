package br.com.sankhya.medic.jobs;

import java.math.BigDecimal;
import java.util.Collection;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.medic.metodospadrao.Updates;
import br.com.sankhya.medic.metodospadrao.Utilitarios;
import br.com.sankhya.medic.services.Services;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class Cancelarnotas implements ScheduledAction {
	// CHAMAR CLASSES AQUI
	Updates update = new Updates();
	Utilitarios util = new Utilitarios();
	Services services = new Services();

	@Override
	public void onTime(ScheduledActionContext ctx) {
		try {
			consultaPdd();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void consultaPdd() throws Exception {
		String top = util.consultaParam("KPT_TOPDEVAPI");
		String url = util.consultaParam("KPT_URLAPIVEEVO");
		String token = util.consultaParam("KPT_TOKENAPI");

		if (top == null || top.trim().isEmpty()) {
			throw new Exception("Ação não pode ser executada pois o parâmetro de TOP não existe ou está vazio.");
		}
		if (url == null || url.trim().isEmpty()) {
			throw new Exception("Ação não pode ser executada pois o parâmetro de URL não existe ou está vazio.");
		}
		if (token == null || token.trim().isEmpty()) {
			throw new Exception("Ação não pode ser executada pois o parâmetro de TOKEN não existe ou está vazio.");
		}

		Collection<DynamicVO> nunotas = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA).find(" CODTIPOPER IN (" + top
				+ ")" + "AND AD_IDNOTA IS NOT NULL AND DTNEG = TRUNC(SYSDATE - 1) AND AD_IDNOTADEV IS NULL");

		for (DynamicVO nunota : nunotas) {
			BigDecimal nuNota = nunota.asBigDecimalOrZero("NUNOTA");
			Integer idnota = nunota.asBigDecimalOrZero("AD_IDNOTA").intValue();
			int idDevNota = services.Devolveped(token, idnota, "Comment");
			if (idDevNota > 0) {
				update.attPK(DynamicEntityNames.CABECALHO_NOTA, "NUNOTA", nuNota, "AD_IDNOTADEV", idDevNota);
			}
			else {
				update.attPK(DynamicEntityNames.CABECALHO_NOTA, "NUNOTA", nunota.asBigDecimal("NUNOTA"), "OBSERVACAO", "erro ao enviar cancelamento de nota");

			}
		}

	}

}
