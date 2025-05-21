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

public class Lancanotas implements ScheduledAction {
	//CHAMAR CLASSES AQUI
	Updates update = new Updates ();
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
		
		Collection<DynamicVO> nunotas = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA).find(" CODTIPOPER IN ("+top+")"
				+ "AND AD_IDNOTA IS NULL AND DTNEG = TRUNC(SYSDATE - 1)");
		
		for (DynamicVO nunota  : nunotas ) {
			BigDecimal idParc = JapeFactory.dao(DynamicEntityNames.PARCEIRO).findOne("CODPARC = "+ nunota.asBigDecimal("CODPARC"))
					.asBigDecimalOrZero("IDPARC"); //ID DO PARCEIRO NO VEEVA
			if(idParc.intValue() == 0) {
				//METODO PARA CADASTRAR PARCEIRO
				//RETORNAR O ID E FAZER UPDATE DO ID NO CAMPO IDPARC NA TGFPAR
				update.attPK(DynamicEntityNames.PARCEIRO, "CODPARC", nunota.asBigDecimal("CODPARC"), "IDPARC", idParc.toString()); // USA ESSE UPDATE PARA ATUALIZAR
				if(idParc.intValue() > 0) {
					//FALTA VALIDAR OS PRODUTOS PARA CHAMAR O METODO
					Collection<DynamicVO> ProdutosIte = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).find("NUNOTA ="+nunota.asBigDecimal("NUNOTA"));
					int countValidador = 0 ;
					for (DynamicVO itens  : ProdutosIte) {
						BigDecimal codprod = itens.asBigDecimal("CODPROD");
						DynamicVO produtosPRO = JapeFactory.dao(DynamicEntityNames.PRODUTO).findByPK(codprod);	
						if(produtosPRO.asBigDecimalOrZero("AD_IDPROD").intValue() == 0) {							
//							update.attPK(DynamicEntityNames.CABECALHO_NOTA, "NUNOTA", nunota.asBigDecimal("NUNOTA"), "OBSERVACAO", "Produto: "+ codprod+" não tem id cadastrado");
							countValidador ++;
						}
						
					}
					if(countValidador == 0) {
						BigDecimal codparc = nunota.asBigDecimal("CODPARC");
						BigDecimal nuNota = nunota.asBigDecimal("NUNOTA");
						Integer idNota = services.Addped(token,codparc ,nuNota );
						if(idNota > 0) {
							update.attPK(DynamicEntityNames.CABECALHO_NOTA, "NUNOTA", nuNota, "AD_IDNOTA", idNota.toString()); //MUDAR PARA OBJECT
						}
					}
					
				}
				
			}
			
		}

	}


}
