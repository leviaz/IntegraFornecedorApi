package br.com.sankhya.medic.jobs;

import java.math.BigDecimal;
import java.sql.ResultSet;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.medic.metodospadrao.Updates;
import br.com.sankhya.medic.metodospadrao.Utilitarios;
import br.com.sankhya.medic.services.Services;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

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

	public void consultaPdd() throws Exception {
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
		
		    EntityFacade entity = EntityFacadeFactory.getDWFFacade();
		    JdbcWrapper jdbc = entity.getJdbcWrapper();
		    ResultSet rset = null;
		    jdbc.openSession();
		    try {
		        NativeSql sql = new NativeSql(jdbc);
		        String sqlStr = String.format("SELECT CAB2.NUNOTA AS NOTA_ORIGEM, CAB2.AD_IDNOTA, CAB1.NUNOTA AS NOTA_DEVOLVIDA\r\n"
		        		+ "FROM TGFCAB CAB1\r\n"
		        		+ "JOIN TGFVAR VAR ON VAR.NUNOTA = CAB1.NUNOTA\r\n"
		        		+ "JOIN TGFCAB CAB2 ON CAB2.NUNOTA = VAR.NUNOTAORIG\r\n"
		        		+ "WHERE CAB1.CODTIPOPER IN (" + top + ")\r\n"
		        		+ "AND CAB1.AD_IDNOTADEV IS NULL\r\n"
		        		+ "AND CAB2.AD_IDNOTA IS NOT NULL\r\n"
		        		+ "AND CAB1.NUNOTA <> CAB2.NUNOTA AND CAB2.DTNEG=TRUNC(SYSDATE)  AND CAB1.NUNOTA !=208446");
		        sql.appendSql(sqlStr);
		        rset = sql.executeQuery();
		    	while (rset.next()) {
		    		BigDecimal nunotadev = rset.getBigDecimal("NOTA_DEVOLVIDA");
		    		BigDecimal nunotaorig = rset.getBigDecimal("NOTA_ORIGEM");
					Integer idnota = rset.getBigDecimal("AD_IDNOTA").intValue();
					Integer idDevNota = services.Devolveped(token, idnota, "Comment",nunotadev,nunotaorig);
					if (idDevNota > 0) {
						update.attPK("TGFCAB","NUNOTA", nunotadev, "AD_IDNOTADEV", idDevNota);
						update.log(nunotadev, "Nota de devolução feito com sucesso:"+ idDevNota.toString(),services.bodyAddPed, services.responseAddPed);
					}
					else {
						//LANÇAR NA TELA DE LOG
//						update.attPK(DynamicEntityNames.CABECALHO_NOTA, nunota.asBigDecimal("NUNOTA"), "OBSERVACAO", "erro ao enviar cancelamento de nota");

					}
		    	}

		    } catch (Exception e) {
		        e.printStackTrace();
		        MGEModelException.throwMe(e);
		    } finally {
		        JdbcWrapper.closeSession(jdbc);
		    }
		}

}
