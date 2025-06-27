package br.com.sankhya.medic.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.medic.metodospadrao.Updates;
import br.com.sankhya.medic.metodospadrao.Utilitarios;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Services {
	Utilitarios util = new Utilitarios();
	Updates upd = new Updates();
	
	public String bodyDevolvePed;      
    public String responseDevolvePed;   

    public String bodyAddPed;         
    public String responseAddPed;       

	// criando pedido
	public int Addped(String token, BigDecimal codparc, BigDecimal nunota) throws Exception {
		int idpdd = 0;
		String bodyRq = "";
		try {
			// Realiza update para marcar envio no banco mysql
			Response response = null;
			String url = util.consultaParam("KPT_URLAPIVEEVO");

			OkHttpClient client = new OkHttpClient();
			DynamicVO parc = JapeFactory.dao(DynamicEntityNames.PARCEIRO).findByPK(codparc);

			DynamicVO cab = JapeFactory.dao(DynamicEntityNames.CABECALHO_NOTA).findByPK(nunota);

			DynamicVO emp = JapeFactory.dao(DynamicEntityNames.EMPRESA).findByPK(cab.asBigDecimalOrZero("CODEMP"));

			String tipvenda = util.consultaTipVenda(cab.asBigDecimalOrZero("CODTIPVENDA"));
			if ("0".equals(tipvenda)) {
				// mudar para tsiavi
				//throw new Exception("Método de pagamento não cadastrado");
				upd.log(nunota, "Nota não pode ser enviada, pois o método de pagamento não foi cadastrado", bodyAddPed, responseAddPed);
			}
			String metPag = util.consultaMetPag(cab.asBigDecimalOrZero("CODTIPVENDA"));
			int idAdr = buscaCliente(token, codparc);

			if (idAdr == 0) {
				// mudar para tsiavi
				//throw new Exception("Cliente não possui cadastro completo na API VEEVO");
				upd.log(nunota, "Nota não pode ser enviada, pois o cliente não está cadastrado",bodyAddPed, responseAddPed);
			}

			DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
			LocalDateTime localDateTime = LocalDateTime.parse(cab.asTimestamp("DTFATUR").toString(), inputFormatter);
			ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("America/Sao_Paulo"));
			ZonedDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));
			String invProcessDt = utcDateTime.format(DateTimeFormatter.ISO_INSTANT);

			Collection<DynamicVO> ProdutosIte = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA).find("NUNOTA =" + nunota);

			StringBuilder itemsJson = new StringBuilder();
			itemsJson.append("  \"items\": [\r\n");

			int count = 0;
			for (DynamicVO itens : ProdutosIte) {
				BigDecimal codprod = itens.asBigDecimal("CODPROD");
				DynamicVO produtosPRO = JapeFactory.dao(DynamicEntityNames.PRODUTO).findByPK(codprod);

				if (!produtosPRO.asString("AD_IDPROD").trim().isEmpty()
						&& !produtosPRO.asString("AD_IDPROD").trim().equals("0")) {

					if (count > 0) {
						itemsJson.append(",\r\n"); // separador de itens
					}

					itemsJson.append("    {\r\n").append("      \"sku\": \"").append(produtosPRO.asString("AD_IDPROD"))
							.append("\",\r\n").append("      \"qty\": ").append(itens.asBigDecimal("QTDNEG"))
							.append("\r\n").append("    }");

					count++;
				}
			}
			itemsJson.append("\r\n  ],\r\n");

			if (count > 0) {

				bodyRq = "{\"customerTaxvat\":\"" + parc.asString("CGC_CPF") + "\",\"commercialTable\":445," + itemsJson
						+ "\"address\":{\"customer_address_id\":" + idAdr + "},\"paymentMethod\":{\"method\":\""
						+ metPag + "\",\"additional_data\":[\"{\\\"code\\\": \\\"" + tipvenda
						+ "\\\"}\"]},\"acceptanceData\":{\"accepted_distributor_code\":\"" + emp.asString("CGC")
						+ "\"},\"invoiceData\":{\"distributor_invoice_number\":\""
						+ cab.asBigDecimalOrZero("NUMNOTA").toString() + "\",\"distributor_cnpj\":\""
						+ emp.asString("CGC") + "\",\"distributor_invoice_access_key\":\"" + cab.asString("CHAVENFE")
						+ "\",\"distributor_invoice_series_code\":\"" + "1"
						+ "\",\"invoice_processed_at\":\"" + invProcessDt + "\"},\"coupons\":[]}";
				RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyRq);
				
				
				this.bodyAddPed = bodyRq; // Captura o body usado

				Request request = new Request.Builder().url(url + "/V1/orders/create-complete").put(body)
						.addHeader("Authorization", token)
						.addHeader("Content-Type", "application/json").build();
				Call call = client.newCall(request);
				response = call.execute();
				
				
				if (response.isSuccessful() && response.body() != null) {
					String respostaJson = response.body().string().trim();
					this.responseAddPed = respostaJson;
					idpdd = Integer.parseInt(respostaJson);
					if (idpdd == 0) {
						System.out.println("Erro: " + response.code());
						// LANÇAR NA TELA DE LOG
						upd.log(nunota, "Nota não gerou id de criação",bodyAddPed, responseAddPed);
						//throw new Exception("" + response.code() + response);
					}

				} else {
					System.out.println("Request invalida com status: " + response.code() + response.body());
//					JSONObject root = new JSONObject(response.body().string());
					// LANÇAR NA TELA DE LOG
					upd.log(nunota, "Nota apresentou status inválido na criação", bodyAddPed, responseAddPed);
					//throw new Error(root.getString("message"));
				}
				response.close();

			} else {
				// LANÇAR A TELA DE LOG
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return idpdd;
	}

	// requisição de devolução passando o id da nota
	public int Devolveped(String token, int order_id, String comment, BigDecimal nunotadev, BigDecimal nunotaorig) throws Exception {
		Integer iddev = 0;
		try {
			Response response = null;
			String url = util.consultaParam("KPT_URLAPIVEEVO") + "/V1/order/" + String.valueOf(order_id) + "/refund\r\n"
					+ "";

			OkHttpClient client = new OkHttpClient();
			StringBuilder itemsJson = new StringBuilder();
			  //Consulta os itens da nota de devolução
	        Collection<DynamicVO> itensDevolucao = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA)
	            .find("NUNOTA = ?", nunotadev);

	     //Armazena os CODPROD da devolução em um Set
	        Set<BigDecimal> codprodsDevolucao = new HashSet<>();
	        for (DynamicVO item : itensDevolucao) {
	            codprodsDevolucao.add(item.asBigDecimal("CODPROD"));
	        }
	        
	        // Consulta os itens da nota original
	        Collection<DynamicVO> itensOriginais = JapeFactory.dao(DynamicEntityNames.ITEM_NOTA)
	            .find("NUNOTA = ?", nunotaorig);
	        
	        
			int count = 0;
			for (DynamicVO item : itensOriginais) {
	            BigDecimal itemID = item.asBigDecimalOrZero("AD_IDITEM");
				if (itemID.intValue() > 0) {

					if (count > 0) {
						itemsJson.append(",\r\n"); // separador de itens

					}
					 itemsJson.append("    {\r\n")
	                 .append("      \"order_item_id\": ").append(itemID.intValue()).append(",\r\n")
	                 .append("      \"qty\": ").append(item.asBigDecimalOrZero("QTDNEG").intValue()).append("\r\n")
	                 .append("    }");
	        count++;
				}
			}

			String bodyRq = "{\n" + "  \"items\": [" + itemsJson + "],\n" + "  \"comment\": {\n" + "    \"comment\": \""
					+ comment + "\"\n" + "  }\n" + "}";

			RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyRq);
			Request request = new Request.Builder().url(url).post(body).addHeader("Authorization", token)
					.addHeader("Content-Type", "application/json").build();
			
			this.bodyDevolvePed = bodyRq;
			

			Call call = client.newCall(request);
			response = call.execute();
			
			if (response.isSuccessful() && response.body() != null) {
				String respostaJson = response.body().string().trim();
			    respostaJson = respostaJson.replace("\"", "");
			    this.responseDevolvePed = respostaJson;
				iddev = Integer.parseInt(respostaJson);

			} else {
				System.out.println("Request invalida com status: " + response.code());
//				JSONObject root = new JSONObject(response.body().string());
				// LANÇAR NA TELA DE LOG
				upd.log(nunotadev, "Nota apresentou status inválido na devolução", bodyAddPed, responseAddPed);
				//throw new Error(root.getString("message"));

			}

			response.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
		return iddev;
	}

	// Com o cpf ou cnpj do cliente faz uma consulta na api verificando se ele tem id
	public int buscaCliente(String token, BigDecimal codparc) throws Exception {
		int id = 0;
		JSONObject root = null;
		try {

			DynamicVO parc = JapeFactory.dao(DynamicEntityNames.PARCEIRO).findByPK(codparc);
			String url = util.consultaParam("KPT_URLAPIVEEVO");

			OkHttpClient client = new OkHttpClient();

			Request request = new Request.Builder().url(url + "/V1/customers/search/taxvat/" + parc.asString("CGC_CPF"))
					.get().addHeader("Authorization", token).addHeader("Content-Type", "application/json").build();

			Call call = client.newCall(request);
			// Execução da request
			Response response = call.execute();

			if (response.isSuccessful() && null != response.body()) {
				root = new JSONObject(response.body().string());

				JSONArray items = root.getJSONArray("items");

				if (items.length() > 0) {
					JSONObject firstItem = items.getJSONObject(0);

					JSONArray addresses = firstItem.getJSONArray("addresses");

					if (addresses.length() > 0) {
						JSONObject firstAddress = addresses.getJSONObject(0);

						id = firstAddress.getInt("id");
					} else {
						return 0;
					}
				} else {
					return 0;
				}

			} else {
				System.out.println("Request busca cliente CNPJ/CPF invalida com status: " + response.code());
				// LANÇAR NA TELA DE LOG
				throw new Exception();

			}

			response.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e + "Root=" + root.toString());
		}
		return id;
	}

	public Map<Integer, String> ConsultaItens(String token, Integer orderId) throws Exception {
		Map<Integer, String> itens = new HashMap<>();
		JSONObject root = null;
		try {
			String url = util.consultaParam("KPT_URLITENS");

			OkHttpClient client = new OkHttpClient();

			Request request = new Request.Builder().url(url + orderId).get().addHeader("Authorization", token)
					.addHeader("Content-Type", "application/json").build();

			Call call = client.newCall(request);
			// Execução da request
			Response response = call.execute();

			if (response.isSuccessful() && null != response.body()) {
				root = new JSONObject(response.body().string());

				JSONArray items = root.getJSONArray("items");

				for (int i = 0; i < items.length(); i++) {
					JSONObject item = items.getJSONObject(i);
					int itemId = item.getInt("item_id");
					String sku = item.getString("sku");

					itens.put(itemId, sku);
				}

			} else {
				System.out.println("Request busca cliente CNPJ/CPF invalida com status: " + response.code());
				// LANÇAR NA TELA DE LOG
				throw new Exception();
				//upd.log(nunota, "Nota não pode ser enviada, pois o cliente não está cadastrado");
			}

			response.close();

		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e + "Root=" + root.toString());
		}
		return itens;
	}

}
