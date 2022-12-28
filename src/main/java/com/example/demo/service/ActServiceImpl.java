package com.example.demo.service;

import java.beans.Introspector;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.demo.model.ResponseDTO;
import com.example.demo.model.Views;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ActServiceImpl implements ActService {

	@Autowired
	ActServiceUtil actServiceUtil;

	@Autowired
	ObjectMapper mapper;

	/**
	 * @param env
	 * @param organization
	 * @param sendmailResponse
	 * @return
	 */
	@Override
	public ResponseEntity<?> extractTrueUpDetails(String organization, String space) {
		ResponseEntity<?> response = null;
		try {
			String env = Introspector.decapitalize(space.split("_")[1]);// SpringBoot_Qlab02
			String capability = null;
			if (space.startsWith("SpringBoot")) {
				capability = "rsp";
			}
			if (space.startsWith("TibcoCE")) {
				capability = "tibco";
			}
			List<ResponseDTO> responseDTOList = new ArrayList<>();

//			space = "SpringBoot_Prod";
			List<String> appList = Arrays.asList(new String[]{ "addresslookup-rsp-"+env, "addressupdate-rsp-"+env, "addressverification-rsp-"+env,  "billdetails-rsp-"+env});//actServiceUtil.extractAllAppListNPE(organization, space);
//			log.debug("appList in NPE trueup utility:" + appList);
			for (String app : appList) {
				ResponseDTO responseDTO = new ResponseDTO();
				responseDTO.setAppName(app);
				String prodApp = StringUtils.truncate(app, app.lastIndexOf("-rsp") + 4);
				if (app.contains("addbandollaradjustment")) {
					continue;
				}

				if (app.contains("billdetails")) {
					break;
				}
				// local test

//				if (!env.equals("prd03c") && !env.equals("prd04c")) {
//					app = StringUtils.truncate(app, app.lastIndexOf("-rsp") + 4).concat("-" + env);
//				}

				Map<String, String> urlMap = actServiceUtil.extractURLInfo(app, env, true, "npe");
				String branchResponse = null;
				String healthCheckResponse = null;
				if (StringUtils.isNotBlank(urlMap.get("actuatorInfo"))) {
					branchResponse = actServiceUtil.executeHttpUrl(env, urlMap.get("actuatorInfo"), null, app)
							.get("branchResponse");
				}
				if (StringUtils.isNotBlank(urlMap.get("actuatorHealth"))) {
					healthCheckResponse = actServiceUtil.executeHttpUrl(env, null, urlMap.get("actuatorHealth"), app)
							.get("healthCheckResponse");

				}
				if (StringUtils.isNotBlank(healthCheckResponse)) {
					responseDTO.setHealthCheck(healthCheckResponse);
				}
				if (StringUtils.isNotBlank(branchResponse)) {
					responseDTO.setBranchName(branchResponse);

					boolean upToDateReq = false;

					if (ObjectUtils.isNotEmpty(responseDTO) && StringUtils.isNotBlank(responseDTO.getBranchName())) {
						upToDateReq = actServiceUtil.verifyTrueUpRequired(prodApp, responseDTO.getBranchName(),
								responseDTO);

						if (upToDateReq) {
							responseDTO.setInfoMsg(Boolean.FALSE);// + " => True-Up Needed for " + app + " in " + env);
//						String sendMailResp = "http://localhost:8087/send/mail";//TBD
//						sendmailResponse = restTemplate.getForEntity(sendMailResp, String.class);//TBD
						}

						else {
							responseDTO.setInfoMsg(Boolean.TRUE);// + " => PROD SYNCED");
						}

					}
//					log.debug("True Up Utility jsonObject:" + responseDTO);
					responseDTOList.add(responseDTO);
				}
			}

			if (ObjectUtils.isNotEmpty(responseDTOList) && !responseDTOList.isEmpty())

			{
				response = new ResponseEntity<>(responseDTOList, HttpStatus.OK);

			}

		} catch (

		Exception e) {
			log.error("Exception During True-Up Utility Call:" + e.getMessage());
		}
		return response;
	}

	@Cacheable(cacheNames = "getCfData")
	@Override
	public ResponseEntity<?> extractAllBranchDetails(String org, String capability) {
		List<String> appList = Arrays.asList(new String[]{ "addresslookup-rsp", "addressupdate-rsp", "addressverification-rsp",  "billdetails-rsp"});//actServiceUtil.extractAllAppListProd(org, space);
//		log.debug("AppList in PROD  in Trueup Status: " +appList);
		ResponseEntity<?> response = null;
		try {
			List<ResponseDTO> responseDTOList = new ArrayList<>();
			List<String> envList = Arrays.asList("prd03c", "prd04c", "ilab02", "qlab01", "qlab02", "qlab03", "qlab06",
					"qlab07", "plab01");

			responseDTOList = actServiceUtil.extractResponseDtoList(appList, responseDTOList, envList);

			if (ObjectUtils.isNotEmpty(responseDTOList) && !responseDTOList.isEmpty()) {

				// View
//				ObjectWriter viewWriter;
//
//				viewWriter = mapper.writerWithView(Views.Public.class);
//
//				viewWriter.writeValueAsString(responseDTOList);

				response = new ResponseEntity<>(responseDTOList, HttpStatus.OK);

			}
			File jsonFileToExcelFile = jsonFileToExcelFile(appList, responseDTOList);// generate excel

			// mail api ->excel to pass
			actServiceUtil.invokeMailAPI(jsonFileToExcelFile);

		} catch (

		Exception e) {
			log.error("Exception During All branch Utility Call:" + e.getMessage());
		}
		return response;
	}



	@Override
	public File jsonFileToExcelFile(List<String> appList, List<ResponseDTO> responseDTOList) {
		File outFile = null;
		try {
			List<String> envList = Arrays.asList("prd03c", "prd04c", "ilab02", "qlab01", "qlab02", "qlab03", "qlab06",
					"qlab07", "plab01");
			if (responseDTOList == null) {
				responseDTOList = new ArrayList<>();
				responseDTOList = actServiceUtil.extractResponseDtoList(appList, responseDTOList, envList);
			}
			try (XSSFWorkbook workbook = new XSSFWorkbook()) {
				XSSFSheet sheet = workbook.createSheet("sheet1");// creating a blank sheet
				int rownum = 0;
				Row row = sheet.createRow(rownum++);
				// HEADER
				Cell cell = row.createCell(0);
				cell.setCellValue("APP Name");

				cell = row.createCell(1);
				cell.setCellValue("Prd03c");

				cell = row.createCell(2);
				cell.setCellValue("Prd04c");

				cell = row.createCell(3);
				cell.setCellValue("Ilab02");

				cell = row.createCell(4);
				cell.setCellValue("Qlab01");

				cell = row.createCell(5);
				cell.setCellValue("Qlab02");

				cell = row.createCell(6);
				cell.setCellValue("Qlab03");

				cell = row.createCell(7);
				cell.setCellValue("Qlab06");

				cell = row.createCell(8);
				cell.setCellValue("Qlab07");

				// HEADER END


				for (ResponseDTO respDto : responseDTOList) {
					row = sheet.createRow(rownum++);
					createList(respDto, row);

				}
				outFile = new File("AllAppDetails.xlsx");
				FileOutputStream out = new FileOutputStream(outFile.toString()); // file name with path
				workbook.write(out);
				out.close();
			}

		} catch (Exception e) {
			log.error("Exception in JSON to Excel" + e);
		}
		return outFile;
	}

	private static void createList(ResponseDTO dto, Row row) // creating cells for each row
	{
		Cell cell = row.createCell(0);
		cell.setCellValue(dto.getAppName());

		cell = row.createCell(1);
		cell.setCellValue(dto.getPrd03cBranch());

		cell = row.createCell(2);
		cell.setCellValue(dto.getPrd04cBranch());

		cell = row.createCell(3);
		cell.setCellValue(dto.getIlab02Branch());

		cell = row.createCell(4);
		cell.setCellValue(dto.getQlab01Branch());

		cell = row.createCell(5);
		cell.setCellValue(dto.getQlab02Branch());

		cell = row.createCell(6);
		cell.setCellValue(dto.getQlab03Branch());

		cell = row.createCell(7);
		cell.setCellValue(dto.getQlab06Branch());

		cell = row.createCell(8);
		cell.setCellValue(dto.getQlab07Branch());

	}
}
