package br.com.sankhya.medic.jobs;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class BOTAO implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {	
		Lancanotas lc = new Lancanotas();
		lc.consultaPdd();

	}}