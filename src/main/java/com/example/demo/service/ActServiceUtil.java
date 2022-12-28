package com.example.demo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActServiceUtil {
	@Autowired
	private HttpClient client;
	
	@Autowired
	private RestTemplate restTemplate;

	@Value("${spring.mail.host}")
	private String smtpHostName;

	@Value("${spring.mail.port1}")
	private String smtpPort1;
	@Value("${spring.mail.port2}")
	private String smtpPort2;
	@Value("${spring.mail.port3}")
	private String smtpPort3;

	@Value("${spring.mail.username}")
	private String username;
	@Value("${spring.mail.password}")
	private String password;

	@Value("${spring.mail.retry.count}")
	private int maxMailRetryCount;

	/**
	 * @param organization
	 * @param space
	 * @return
	 */
//	public List<String> extractAllAppListNPE(String organization, String space) {
//		return cloudFoundryOperations(cloudFoundryClientNPE, organization, space).applications().list()
//				.map(ApplicationSummary::getName).collect(Collectors.toList()).block();
//	}

	/**
	 * @param organization
	 * @param space
	 * @return
	 */
//	public List<String> extractAllAppListProd(String organization, String space) {
//		return cloudFoundryOperations(cloudFoundryClientProd, organization, space).applications().list()
//				.map(ApplicationSummary::getName).collect(Collectors.toList()).block();
//	}
//
//	DefaultCloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient, String organization,
//			String space) {
//		return DefaultCloudFoundryOperations.builder().cloudFoundryClient(cloudFoundryClient)
//
//				.organization(organization).space(space).build();
//	}

	/**
	 * @param lab
	 * @param url
	 * @param responseDTO
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public Map<String, String> executeHttpUrl(String lab, String actuatorInfoUrl, String actuatorHealthUrl, String app)
			throws Exception {
		Map<String, String> responseMap = new HashMap<>();
		try {
			List<Map<String, String>> extractHttpResponseMapList = extractHttpResponse(lab, actuatorInfoUrl,
					actuatorHealthUrl, app);

			extractHttpResponseMapList.forEach((extractHttpResponse) -> {
				String branchResponse = null;
				String healthCheckResponse = null;
				if (ObjectUtils.isNotEmpty(extractHttpResponse)
						&& ObjectUtils.isNotEmpty(extractHttpResponse.get("result"))
						&& StringUtils.isNotBlank(extractHttpResponse.get("result"))) {
					String result = extractHttpResponse.get("result");

					JSONObject jsonObject = new JSONObject(result.toString());
//					log.debug("Response received from Actuator within executeHttpUrl() " + jsonObject);
					if (extractHttpResponse.get("result").contains("branch")
							|| extractHttpResponse.get("result").contains("build")) {
						if (result.toString().contains("\"branch\":")) {
							branchResponse = new JSONObject(jsonObject.get("git").toString()).get("branch").toString();

						} else {
							branchResponse = new JSONObject(jsonObject.get("build").toString()).get("version")
									.toString();

						}
						if (StringUtils.isNotBlank(branchResponse)) {
							responseMap.put("branchResponse", branchResponse);
						}
					} else {
						if (StringUtils.isNotBlank(jsonObject.getString("status"))) {
							healthCheckResponse = jsonObject.getString("status");
							responseMap.put("healthCheckResponse", healthCheckResponse);
						}
					}

				}
			});
		} catch (Exception e) {
			log.error("Exception in Fetching Branch Info: " + e.getMessage());
		}
		return responseMap;
	}

	/**
	 * @param lab
	 * @param url
	 * @param app
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private List<Map<String, String>> extractHttpResponse(String lab, String actuatorInfoUrl, String actuatorHealthUrl,
			String app) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException,
			ClientProtocolException {
		List<Map<String, String>> respMapList = new ArrayList<>();
		if (StringUtils.isNotBlank(actuatorHealthUrl)) {
			HttpGet actuatorHealthRequest = new HttpGet(actuatorHealthUrl);
			Map<String, String> actuatorHealthRespMap = executeHttpRequest(actuatorHealthRequest);
			respMapList.add(actuatorHealthRespMap);
		}
		if (StringUtils.isNotBlank(actuatorInfoUrl)) {
			HttpGet actuatorInfoRequest = new HttpGet(actuatorInfoUrl);
//			log.debug("Request Generated within extractHttpResponse() " + actuatorInfoRequest);
			Map<String, String> actuatorInfoRespMap = executeHttpRequest(actuatorInfoRequest);
//			log.debug("Response Generated within extractHttpResponse() " + actuatorInfoRespMap);
			respMapList.add(actuatorInfoRespMap);
		}

		return respMapList;

	}

	/**
	 * @param lab
	 * @param app
	 * @param request
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 * @throws KeyManagementException
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private Map<String, String> executeHttpRequest(HttpGet request) throws NoSuchAlgorithmException, KeyStoreException,
			KeyManagementException, IOException, ClientProtocolException {
		// create the http client object
//		HttpClient client = HttpClientBuilder.create().build();
//		SSLContextBuilder builder = new SSLContextBuilder();
//		builder.loadTrustMaterial(null, (chain, authType) -> true);
//		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
//				NoopHostnameVerifier.INSTANCE);
//		client = HttpClients.custom().setSSLSocketFactory(sslsf).build();

		// execute the request and capture the response
		HttpResponse response = client.execute(request);
//		log.debug("Response Generated within executeHttpRequest() " + response);
		// get response code
//		log.debug("Response Code  :" + response.getStatusLine().getStatusCode());
		// get the response body
		Map<String, String> respMap = new HashMap<>();
		BufferedReader rd = null;
		if (ObjectUtils.isNotEmpty(response) && ObjectUtils.isNotEmpty(response.getStatusLine())
				&& ObjectUtils.isNotEmpty(response.getStatusLine().getStatusCode())
				&& response.getStatusLine().getStatusCode() != 404) {
			rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			// capture the response in string
			StringBuffer result = new StringBuffer();
//			StringBuilder result = new StringBuilder();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			if (ObjectUtils.isNotEmpty(response) && ObjectUtils.isNotEmpty(result)) {
				respMap.put("result", result.toString());
			}
		}
//		log.debug("Response Map within executeHttpRequest()  :" + respMap);
		return respMap;
	}

	/**
	 * @param lab
	 * @param app
	 * @param appEnv
	 * @return
	 */
	public Map<String, String> extractURLInfo(String app, String lab, boolean isHealthCheckReq, String appEnv) {
		Map<String, String> actuatorUrlMap = new HashMap<>();
		try {
			String env = null;
			String host = null;
			switch (lab.substring(0, 4)) {
			case "qlab":
				env = "test";
				host = "px-npe02c";
				break;
			case "plab":
				env = "test";
				host = "px-npe02a";
				break;
			case "ilab":
				env = "dev";
				host = "px-npe02c";
				break;
			case "prd0":
				env = "apps";
				if (lab.equalsIgnoreCase("prd03c")) {
					host = "px-prd03c";
				}
				if (lab.equalsIgnoreCase("prd04c")) {
					host = "px-prd04c";
				}
				break;
			default:
				break;
			}

			String uri = null;
			String actuatorInfo = null;
			String actuatorHealth = null;
			String npeStr = "";

			if (StringUtils.isNotBlank(app) && StringUtils.isNotBlank(env) && StringUtils.isNotBlank(host)
					&& StringUtils.isNotBlank(lab)) {
				if (lab.equalsIgnoreCase("prd03c") || lab.equalsIgnoreCase("prd04c")) {
					uri = app + "." + env + "." + host + ".cf.t-mobile.com";
				}

				else {
					if (appEnv.equalsIgnoreCase("prod")) {
						npeStr = "-" + lab;
					}

					uri = app + npeStr + "." + env + "." + host + ".cf.t-mobile.com";
				}
			}

			if (StringUtils.isNotBlank(uri)) {
				actuatorInfo = "http://" + uri + "/actuator/info";// rsp-qlab03.test.px-npe02c.cf.t-mobile.com
				if (StringUtils.isNotBlank(actuatorInfo)) {
					actuatorUrlMap.put("actuatorInfo", actuatorInfo);
				}
				if (isHealthCheckReq) {
					actuatorHealth = "http://" + uri + "/actuator/health";
					if (StringUtils.isNotBlank(actuatorInfo)) {
						actuatorUrlMap.put("actuatorHealth", actuatorHealth);
					}
				}
			}
		} catch (Exception e) {
			log.error("Exception while Constructing Url :" + e.getMessage());
		}
		return actuatorUrlMap;
	}

	public boolean verifyTrueUpRequired(String app, String branch_info, ResponseDTO responseDTO) throws Exception {
		boolean upToDateReqProd03c = false;
		boolean upToDateReqProd04c = false;
		String branch_prd03c = null;
		String branch_prd04c = null;
		boolean isTrueUpReqd = false;
		Map<String, String> prodUrl_prd03cMap = extractURLInfo(app, "prd03c", false, "npe");
		String prodUrl_prd03c = prodUrl_prd03cMap.get("actuatorInfo");
		if (StringUtils.isNotBlank(prodUrl_prd03c)) {
			branch_prd03c = executeHttpUrl("prd03c", prodUrl_prd03c, null, app).get("branchResponse");
		}

		Map<String, String> prodUrl_prd04cMap = extractURLInfo(app, "prd04c", false, "npe");
		String prodUrl_prd04c = prodUrl_prd04cMap.get("actuatorInfo");
		if (StringUtils.isNotBlank(prodUrl_prd04c)) {
			branch_prd04c = executeHttpUrl("prd04c", prodUrl_prd04c, null, app).get("branchResponse");
		}

		if (ObjectUtils.isNotEmpty(branch_prd03c)) {
			responseDTO.setPrd03cBranch(branch_prd03c);
			if (!branch_info.equalsIgnoreCase(branch_prd03c)) {
				upToDateReqProd03c = true;
			}

		}
		if (ObjectUtils.isNotEmpty(branch_prd04c)) {
			responseDTO.setPrd04cBranch(branch_prd04c);
			if (!branch_info.equalsIgnoreCase(branch_prd04c)) {
				upToDateReqProd04c = true;
			}
		}

		if ((upToDateReqProd03c) || (upToDateReqProd04c)) {
			isTrueUpReqd = true;
		}
		return isTrueUpReqd;
	}

	/**
	 * @param appList
	 * @param responseDTOList
	 * @param envList
	 * @return
	 * @throws Exception
	 */
	List<ResponseDTO> extractResponseDtoList(List<String> appList, List<ResponseDTO> responseDTOList,
			List<String> envList) throws Exception {
		String prd03cBranchName = null;
		String prd04cBranchName = null;
		for (String app : appList) {
			ResponseDTO responseDTO = new ResponseDTO();
			String appName = app;

			app = StringUtils.truncate(app, app.lastIndexOf("-rsp") + 4);
//			if (app.contains("activity")) {
//				continue;
//			}
//			if (app.contains("b2b")) {
//				continue;
//			}
//			if (app.equals("addresslookup-rsp-blue")) {
//				break;
//			}
//
//			if (app.contains("bill")) {
//				break;
//			} // local test
			for (String env : envList) {

				if (!env.equals("prd03c") && !env.equals("prd04c")) {
//					app = StringUtils.truncate(app, app.lastIndexOf("-rsp") + 4).concat("-" + env);
					app = appName.concat("-" + env);
				}

				Map<String, String> urlMap = extractURLInfo(appName, env, false, "prod");
				String branchResponse = null;
				if (StringUtils.isNotBlank(urlMap.get("actuatorInfo"))) {
					branchResponse = executeHttpUrl(env, urlMap.get("actuatorInfo"), null, appName)
							.get("branchResponse");
				}

				if (StringUtils.isNotBlank(branchResponse)) {
//					ResponseDTO responseDTO = new ResponseDTO();
					responseDTO.setAppName(appName);
					if (env.equals("prd03c")) {
						prd03cBranchName = branchResponse;
						responseDTO.setPrd03cBranch(prd03cBranchName);
					}
					if (env.equals("prd04c")) {
						prd04cBranchName = branchResponse;
						responseDTO.setPrd04cBranch(prd04cBranchName);
					} else {
						switch (env) {
						case "ilab02":
							responseDTO.setIlab02Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgIlab02(true);
//							}

							break;
						case "qlab01":
							responseDTO.setQlab01Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgQlab01(true);
//							}
							break;
						case "qlab02":
							responseDTO.setQlab02Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgQlab02(true);
//							}
							break;
						case "qlab03":
							responseDTO.setQlab03Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgQlab03(true);
//							}
							break;
						case "qlab06":
							responseDTO.setQlab06Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgQlab06(true);
//							}
							break;
						case "qlab07":
							responseDTO.setQlab07Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgQlab07(true);
//							}
							break;
						case "plab01":
							responseDTO.setPlab01Branch(branchResponse);

//							if ((StringUtils.isNotBlank(prd03cBranchName)
//									&& prd03cBranchName.equalsIgnoreCase(branchResponse))
//									|| (StringUtils.isNotBlank(prd04cBranchName)
//											&& prd04cBranchName.equalsIgnoreCase(branchResponse))) {
//								responseDTO.setInfoMsgPlab01(true);
//							}
							break;
						default:
							break;
						}

					}
//					log.debug("responseDTO->" + responseDTO);
//					responseDTOList.add(responseDTO);
				}

			}
			if (StringUtils.isNotBlank(responseDTO.getAppName())) {
				responseDTOList.add(responseDTO);
			}

		}
		return responseDTOList;
	}

	/**
	 * @param jsonFileToExcelFile
	 * @return
	 */
	public String invokeMailAPI(File jsonFileToExcelFile) {
		String sendMailResp = null;
		sendMailResp = sendSimpMail(jsonFileToExcelFile, 1);
		return sendMailResp;
	}

	public String sendSimpMail(File mFile, int count) {
		String res = null;
		String subject = "True up Details";
		String from = "dsg.capgemini@gmail.com";
		String recipients[] = { "anindita.mazumder1@t-mobile.com" };// , "pavithra.muthusamy@capgemini.com",
//				"pooja.narayan-palekar@capgemini.com" };

		String messageText = "Hi,\n" + "Please find the Details of All Apps in All Environments as attached\n"
				+ "\nRegards,\n" + "DSG-CHARGE";
		try {

			// Set the host smtp address
			Properties props = new Properties();
			if (count == 1) {
				props.put("mail.smtp.port", smtpPort1);
				props.put("mail.smtp.ssl.enable", "false");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.socketFactory.fallback", "true");

			}
			if (count == 2) {
				props.put("mail.smtp.port", smtpPort2);
			}
			if (count == 3) {
				props.put("mail.smtp.port", smtpPort3);
				props.put("mail.smtp.ssl.enable", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			}
			props.put("mail.smtp.host", smtpHostName);
			props.put("mail.smtp.auth", "true");

//			MailSSLSocketFactory socketFactory = new MailSSLSocketFactory();
//		    socketFactory.setTrustAllHosts(true);
//		    props.put("mail.imaps.ssl.socketFactory", socketFactory);
			props.setProperty("mail.smtp.user", username);
			props.setProperty("mail.smtp.password", password);

			Authenticator auth = new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			};

			Session session = Session.getInstance(props, auth);
//			session.setDebug(true);
//			 Store store = session.getStore("imaps");
//			 store.connect(smtpHostName,username, password);
			// create a message
			MimeMessage msg = new MimeMessage(session);

			// set the from and to address
			InternetAddress addressFrom = new InternetAddress(from);
			msg.setFrom(addressFrom);

			InternetAddress[] addressTo = new InternetAddress[recipients.length];
			for (int i = 0; i < recipients.length; i++) {
				addressTo[i] = new InternetAddress(recipients[i]);
			}
			msg.setRecipients(Message.RecipientType.TO, addressTo);

			// Setting the Subject and Content Type
			msg.setSubject(subject);
//			msg.setContent(messageText, "text/plain");
			msg.setText(messageText);
			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(msg, true);
			FileSystemResource fileSystem = new FileSystemResource(mFile);
			mimeMessageHelper.addAttachment(fileSystem.getFilename(), fileSystem);
			mimeMessageHelper.setText(messageText);
//			InputStreamSource iss = new InputStreamSource() {
//			    @Override
//			    public InputStream getInputStream() throws IOException {
//			        // provide fresh InputStream
//			        return new FileInputStream(fileSystem.getFilename());
//			    }
//			};
//			mimeMessageHelper.addAttachment(MimeUtility.encodeText("")), new ByteArrayResource(IOUtils.toByteArray((InputStream) iss))));
//			mimeMessageHelper.addAttachment("attachment", iss);
//			Transport transport = session.getTransport();
//			transport.connect();
			Transport.send(msg, username, password);
		}

//		try {
//
//			MimeMessage mimeMessage = mailSender.createMimeMessage();
//
//			MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
//
//			mimeMessageHelper.setFrom("dsg.capgemini@gmail.com");
//			mimeMessageHelper.setTo("anindita.mazumder1@t-mobile.com");
//			mimeMessageHelper.setTo("pavithra.muthusamy@capgemini.com");
//			mimeMessageHelper.setTo("pooja.narayan-palekar@capgemini.com");
//			mimeMessageHelper.setText("Hi,"
//					+ "Please find the Details of All Apps in All Environments as attached"
//					+ "Regards,"
//					+ "DSG-CHARGE");
//			
//			mimeMessageHelper.setSubject("True up Details");
//
//			FileSystemResource fileSystem = new FileSystemResource(mFile);
//
//			mimeMessageHelper.addAttachment("1:" + fileSystem.getFilename(), fileSystem);
//			mailSender.send(mimeMessage);
//
//			res = "mail sent successfully ";
//			log.debug(res + mFile);
//		} 
		catch (Exception e) {
			res = "Exception sending mail ";
			log.error(res + e.getMessage());
			if (e instanceof MailSendException || e instanceof com.sun.mail.util.MailConnectException) {
				log.error("Error in MailSend: " + e.getMessage());
				if (maxMailRetryCount > count) {
					count++;
					sendSimpMail(mFile, count);
				}
			}

		}
		return res;
	}
//
//	/**
//	 * @param organization
//	 * @param space
//	 * @return
//	 */
//	public List<String> extractAllAppListTKEProd(String organization, String space) {
//		List<String> appList = new ArrayList<>();
//		try {
//
//			ApiClient client = Config.fromConfig("C:\\Users\\aninmazu\\.kube\\config");
//			client.setVerifyingSsl(false);
//			Configuration.setDefaultApiClient(client);
//
//			CoreV1Api api = new CoreV1Api();
//
//			V1PodList list1 = api.listNamespacedPod("dsg-charge-qlab02", null, null, null, null, null, null, null, null,
//					null, null);
//
//			for (V1Pod item : list1.getItems()) {
//				if (item.getMetadata().getName().contains("qlab02")) {
//					appList.add(item.getMetadata().getName().substring(0,
//							item.getMetadata().getName().indexOf("qlab02") - 1));
//				}
//
//			}
//		} catch (Exception e) {
//			log.error("Error fetching AppList from TKE Space: " + e);
//		}
//
//		return appList;
//	}
}
