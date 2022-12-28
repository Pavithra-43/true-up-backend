package com.example.demo.service;

import java.io.File;
import java.util.List;

import org.springframework.http.ResponseEntity;

import com.example.demo.model.ResponseDTO;

public interface ActService {

	ResponseEntity<?> extractTrueUpDetails(String org, String space);

	ResponseEntity<?> extractAllBranchDetails(String org, String capability);


	File jsonFileToExcelFile(List<String> appList, List<ResponseDTO> responseDTOList);

}
