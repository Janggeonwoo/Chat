package com.example.chat.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173") // 🔥 CORS 에러를 한 방에 해결하는 무적의 애노테이션
public class FileUploadController {

    // application.yml에 적어둔 로컬 저장 경로를 자동으로 땡겨옵니다.
    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("파일이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            // 1. 저장할 폴더가 없으면 자동으로 생성
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // 2. 파일명 중복을 방지하기 위해 UUID를 붙여서 고유한 이름 생성
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String savedFilename = UUID.randomUUID().toString() + extension;

            // 3. 지정된 로컬 폴더 경로에 파일 저장 실행
            File destination = new File(uploadDir + savedFilename);
            file.transferTo(destination);

            // 4. 저장 완료 후 브라우저가 이 사진에 접근할 수 있는 웹 URL 주소를 리액트에게 리턴!
            String fileUrl = "http://localhost:8080/uploads/" + savedFilename;
            return new ResponseEntity<>(fileUrl, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("서버 오류로 파일 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}