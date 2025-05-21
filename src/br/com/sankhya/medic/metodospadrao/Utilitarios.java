package br.com.sankhya.medic.metodospadrao;

import java.math.BigDecimal;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.DynamicEntityNames;

public class Utilitarios {

	public String consultaTipVenda(BigDecimal codtipvenda) throws Exception {
		DynamicVO pagVO = JapeFactory.dao("AD_MTDPAGVEEVO").findOne("CODTIPVENDA=" +codtipvenda);
		return pagVO.asString("CODDAPI");
	}
	
	public String consultaMetPag(BigDecimal codtipvenda) throws Exception {
		DynamicVO pagVO = JapeFactory.dao("AD_MTDPAGVEEVO").findOne("CODTIPVENDA=" +codtipvenda);
		return pagVO.asString("METPAG");
	}
	
	public String consultaParam(String chave) throws Exception {
		DynamicVO Param = JapeFactory.dao(DynamicEntityNames.PARAMETRO_SISTEMA).findOne("CHAVE = '"+chave+"'");
		return Param.asString("TEXTO");
	}

}
