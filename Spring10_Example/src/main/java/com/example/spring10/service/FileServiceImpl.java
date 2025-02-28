package com.example.spring10.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.spring10.dto.FileDto;
import com.example.spring10.repository.FileDao;

@Service
public class FileServiceImpl implements FileService{
	
	//파일을 저장할 위치 
	@Value("${file.location}")
	private String fileLocation;
	
	@Autowired private FileDao dao;

	@Override
	public void saveFile(FileDto dto) {
		// FileDto 객체에서 MultipartFile 객체를 얻어낸다.
		MultipartFile myFile=dto.getMyFile();
		
		//만일 파일이 업로드 되지 않았다면
		if(myFile.isEmpty()) {
			throw new RuntimeException("파일이 업로드 되지 않았습니다.");
		}
		
		//원본 파일명 
		String orgFileName = myFile.getOriginalFilename();
		//파일의 크기
		long fileSize = myFile.getSize();
		//저장할 파일의 이름을 Universal Unique 한 문자열로 얻어내기
		String saveFileName=UUID.randomUUID().toString();
		//저장할 파일의 전체 경로 구성하기
		String filePath=fileLocation + File.separator + saveFileName;
		try {
			//업로드된 파일을 저장할 파일 객체 생성
			File saveFile=new File(filePath);
			myFile.transferTo(saveFile);
		}catch(Exception e) {
			e.printStackTrace();
		}
		//업로더
		String userName=SecurityContextHolder.getContext().getAuthentication().getName();
		//FileDto 에 추가 정보를 담는다. 
		dto.setUploader(userName);
		dto.setOrgFileName(orgFileName);
		dto.setSaveFileName(saveFileName);
		dto.setFileSize(fileSize);
		//dao 를 이용해서 DB 에 저장하기
		dao.insert(dto);
	}

	@Override
	public void updateFile(FileDto dto) {
		dao.update(dto);
	}

	@Override
	public void deleteFile(long num) {
		dao.delete(num);
	}

	@Override
	public List<FileDto> getFiles() {
		
		return dao.getList();
	}

	@Override
	public ResponseEntity<InputStreamResource> getResponse(long num) {
		//이제 드디어 DB 에서 읽어온다.
		FileDto dto=dao.getData(num);
		String orgFileName=dto.getOrgFileName();
		String saveFileName=dto.getSaveFileName();
		long fileSize=dto.getFileSize();
		
		try {
			//다운로드 시켜줄 원본 파일명
			String encodedName=URLEncoder.encode(orgFileName, "utf-8");
			//파일명에 공백이 있는경우 파일명이 이상해지는걸 방지
			encodedName=encodedName.replaceAll("\\+"," ");
			//응답 헤더정보(스프링 프레임워크에서 제공해주는 클래스) 구성하기 (웹브라우저에 알릴정보)
			HttpHeaders headers=new HttpHeaders();
			//파일을 다운로드 시켜 주겠다는 정보
			headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream"); 
			//파일의 이름 정보(웹브라우저가 해당정보를 이용해서 파일을 만들어 준다)
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename="+encodedName);
			//파일의 크기 정보도 담아준다.
			headers.setContentLength(fileSize);			
			//읽어들일 파일의 경로 구성
			String filePath=fileLocation + File.separator + saveFileName;
			
			//파일에서 읽어들일 스트림 객체
			InputStream is=new FileInputStream(filePath);
			//InputStreamResource 객체의 참조값 얻어내기
			InputStreamResource isr=new InputStreamResource(is);
			//ResponseEntity 객체를 구성해서 
			ResponseEntity<InputStreamResource> resEntity=ResponseEntity.ok()
					.headers(headers)
					.body(isr);
			//리턴해주면 파일이 다운로드 된다.
			return resEntity;
		}catch(Exception e) {
			//예외 정보를 콘솔에 출력
			e.printStackTrace();
			//예외 발생시키기 
			throw new RuntimeException("파일을 다운로드 하는중에 에러 발생!");
		}
	}
	
}
