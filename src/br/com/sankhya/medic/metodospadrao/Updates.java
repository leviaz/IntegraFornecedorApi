package br.com.sankhya.medic.metodospadrao;

import java.math.BigDecimal;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class Updates {

	public void attPK(String instancia, String campopk, BigDecimal valorpk, String campoatt, Object valoratt)
	        throws Exception {
	    EntityFacade entity = EntityFacadeFactory.getDWFFacade();
	    JdbcWrapper jdbc = entity.getJdbcWrapper();
	    jdbc.openSession();
	    try {
	        NativeSql sql = new NativeSql(jdbc);
	        String sqlStr = String.format("UPDATE %s SET %s = :VALORATT WHERE %s = :VALORPK", instancia, campoatt, campopk);
	        sql.appendSql(sqlStr);
	        sql.setNamedParameter("VALORATT", valoratt);
	        sql.setNamedParameter("VALORPK", valorpk);
	        sql.executeUpdate();

	    } catch (Exception e) {
	        e.printStackTrace();
	        MGEModelException.throwMe(e);
	    } finally {
	        JdbcWrapper.closeSession(jdbc);
	    }
	}


	public void attPKITE(BigDecimal nunota, BigDecimal sequencia, BigDecimal idItem) throws Exception {
		EntityFacade entity = EntityFacadeFactory.getDWFFacade();
		JdbcWrapper jdbc = entity.getJdbcWrapper();
		jdbc.openSession();
		try {
			NativeSql sql = new NativeSql(jdbc);
			sql.appendSql("UPDATE TGFITE SET AD_IDITEM =:ATT WHERE NUNOTA = :NUNOTA AND SEQUENCIA = :SEQUENCIA");
			sql.setNamedParameter("ATT", idItem);
			sql.setNamedParameter("NUNOTA", nunota);
			sql.setNamedParameter("SEQUENCIA", sequencia);
			sql.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
			MGEModelException.throwMe(e);
		}

		finally {
			JdbcWrapper.closeSession(jdbc);
			

		}
	}
	
	public void log(BigDecimal nunota, String log) throws Exception {
			try {
				JapeFactory.dao("AD_KPTLOGAPI").create()
				.set("NUNOTA", nunota)
				.set("LOG", log.toCharArray())
				.set("DATA",TimeUtils.getNow()).save();
			} catch (Exception e) {
				MGEModelException.throwMe(e);
				e.printStackTrace();
			}}
		
}
