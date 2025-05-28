package br.com.sankhya.medic.jobs;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.modelcore.MGEModelException;

public class BOTAO implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {	
		Cancelarnotas lc = new Cancelarnotas ();
		lc.consultaPdd();

	}}