package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.ActService;
import com.example.demo.service.ActServiceUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
//@CrossOrigin(origins = "https://trueup-app-ui.apps.px-npe02c.cf.t-mobile.com")
@CrossOrigin(origins = "*", allowedHeaders = "*")
//@CrossOrigin(origins = "http://localhost:50619")
public class ActController {
	@Autowired
	private ActService actService;
	@Autowired
	ActServiceUtil actServiceUtil;

	@RequestMapping("/{org}/{space}") // rsp->capability type
	private ResponseEntity<?> trueUpUtility(@PathVariable("org") String org, @PathVariable("space") String space)
			throws Exception {
//		log.debug("trueUpUtility started");
		ResponseEntity<?> responseEntity = null;
		responseEntity = actService.extractTrueUpDetails(org, space);
		log.info("responseEntity-> " + responseEntity);
		return responseEntity;

	}

	@RequestMapping("branch/{org}/{capability}") // rsp->capability type
	private ResponseEntity<?> trueUpStatus(@PathVariable("org") String org,
			@PathVariable("capability") String capability) throws Exception {
		ResponseEntity<?> responseEntity = null;
		long strTime = System.currentTimeMillis();
		responseEntity = actService.extractAllBranchDetails(org, capability);
		long endTime = System.currentTimeMillis() - strTime;
		log.info("responseEntity-> " + responseEntity + "\n Elapsed Time"+endTime);
		return responseEntity;

	}

	@RequestMapping("sch") // rsp->capability type
	private ResponseEntity<?> trueUpStatusSch() throws Exception {
		ResponseEntity<?> responseEntity = null;

		responseEntity = getTrueUpStatusResponse();
		return responseEntity;

	}

	/**
	 * @param org
	 * @param capability
	 * @return
	 */
//	@Scheduled(cron = "10 * * * * *")
	private ResponseEntity<?> getTrueUpStatusResponse() {
		ResponseEntity<?> responseEntity;
//		log.debug("Scheduled Job: Notification Alert");
		String org = "DSG-CHARGE";
		String capability = "rsp";
		responseEntity = actService.extractAllBranchDetails(org, capability);
//		log.debug("responseEntity-> " + responseEntity);
		return responseEntity;
	}

	

}
