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
@CrossOrigin(origins = "http://localhost:5173")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("파일이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String savedFilename = UUID.randomUUID().toString() + extension;

            File destination = new File(uploadDir + savedFilename);
            file.transferTo(destination);

            String fileUrl = "http://localhost:8080/uploads/" + savedFilename;
            return new ResponseEntity<>(fileUrl, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("서버 오류로 파일 업로드 실패", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}