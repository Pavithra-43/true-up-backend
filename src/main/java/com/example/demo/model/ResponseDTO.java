package com.example.demo.model;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Component
@Data
@JsonInclude(Include.NON_NULL)
public class ResponseDTO {

	private String appName;

	private String branchName;

	private String prd03cBranch;

	private String prd04cBranch;

	private String healthCheck;

	private boolean infoMsg;

	private String url;

	private String ilab02Branch;

	private String qlab01Branch;

	private String qlab02Branch;

	private String qlab03Branch;

	private String qlab06Branch;

	private String qlab07Branch;
	private String plab01Branch;

	private boolean infoMsgIlab02;
	private boolean infoMsgQlab01;

	private boolean infoMsgQlab02;

	private boolean infoMsgQlab03;

	private boolean infoMsgQlab06;

	private boolean infoMsgQlab07;

	private boolean infoMsgPlab01;
	
	private InfoMsg infoMsgs;

}
