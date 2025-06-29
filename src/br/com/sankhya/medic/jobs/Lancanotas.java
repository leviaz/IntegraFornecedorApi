package br.com.sankhya.medic.jobs;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.medic.metodospadrao.Updates;
import br.com.sankhya.medic.metodospadrao.Utilitarios;
import br.com.sankhya.medic.services.Services;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class Lancanotas implements ScheduledAction {
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

	public void consultaPdd() throws Exception {
			String top = util.consultaParam("KPT_TOPAPI");
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
			
			
			Collection<DynamicVO> nunotas = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA)
					.find(" CODTIPOPER IN (" + top + ")" + "AND AD_IDNOTA IS NULL AND TRUNC(DTNEG) = TRUNC(SYSDATE)");

		for (DynamicVO nunota : nunotas) {
//			DynamicVO parceiro = JapeFactory.dao(DynamicEntityNames.PARCEIRO).findByPK(nunota.asBigDecimalOrZero("CODPARC")); // ID DO PARCEIRO NO VEEVA
//			BigDecimal idParc = parceiro.asBigDecimalOrZero("AD_IDPARC");
//			if (idParc.intValue() == 0) {
//				// METODO PARA CADASTRAR PARCEIRO
//				// RETORNAR O ID E FAZER UPDATE DO ID NO CAMPO IDPARC NA TGFPAR
//				update.attPK("TGFPAR","CODPARC", nunota.asBigDecimalOrZero("CODPARC"), "AD_IDPARC", idParc); 
//				if (idParc.intValue() > 0) {
//					// FALTA VALIDAR OS PRODUTOS PARA CHAMAR O METODO
			Collection<DynamicVO> ProdutosIte = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA)
					.find("NUNOTA =" + nunota.asBigDecimalOrZero("NUNOTA"));
			int countValidador = 0;

			for (DynamicVO itens : ProdutosIte) {
				BigDecimal codprod = itens.asBigDecimalOrZero("CODPROD");
				DynamicVO produtosPRO = JapeFactory.dao(DynamicEntityNames.PRODUTO).findByPK(codprod);
				if (produtosPRO.asString("AD_IDPROD").trim().isEmpty()) {
				    countValidador++;
				}

			}
			if (countValidador == 0) {
				BigDecimal codparc = nunota.asBigDecimalOrZero("CODPARC");
				BigDecimal nuNota = nunota.asBigDecimalOrZero("NUNOTA");
				Integer idNota = services.Addped(token, codparc, nuNota); // APÓS LANÇAR A NOTA VOCÊ TEM QUE RETORNAR O ITEMID DO PRODUTO LANÇADO
				
				if (idNota > 0) {
					Map<Integer, String> itens = services.ConsultaItens(token, idNota); // CONSULTE O IDITEM DE CADA PRODUTO LANÇADO
					
					for (Map.Entry<Integer, String> entry : itens.entrySet()) {
						Integer itemId = entry.getKey(); // AD_IDITEM TGFITE
						String sku = entry.getValue(); // AD_IDPROD TGFPRO
					
						// FAÇA ESSE PROCESSO PARA CONSEGUIR CAPTURAR E FAZER UPDATE NA SEQUENCIA CORRETA
						DynamicVO prodVO = (DynamicVO) JapeFactory.dao(DynamicEntityNames.PRODUTO).findOne("AD_IDPROD = ?", sku);
						BigDecimal codprod = prodVO.asBigDecimalOrZero("CODPROD");

						DynamicVO SequenciaItens = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).findOne("NUNOTA = ? AND CODPROD = ? ", nunota.asBigDecimalOrZero("NUNOTA"), codprod);

						BigDecimal sequenciaitem = SequenciaItens.asBigDecimalOrZero("SEQUENCIA");
						
						if (itemId > 0) {
							update.attPKITE(nuNota, sequenciaitem, BigDecimal.valueOf(itemId));// FAÇA O UPDATE EM CADA SEQUENCIA
							
						}
					}
					update.attPK("TGFCAB", "NUNOTA", nuNota, "AD_IDNOTA", idNota); // APÓS FAZER O UPDATE DOS ITENS FAÇA DA NUNOTA
					update.log(nuNota, "Nota de venda lançada com sucesso IDNOTA:" + idNota.toString(),
                            services.bodyAddPed, services.responseAddPed);
				}
			}
			}

		
	}


		

	}