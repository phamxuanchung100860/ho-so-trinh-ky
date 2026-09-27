package com.example.hosotrinhky.controller;

import com.example.hosotrinhky.model.Document;
import com.example.hosotrinhky.repository.UserRepository;
import com.example.hosotrinhky.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final DocumentService service; private final UserRepository users;
    public ApiController(DocumentService service, UserRepository users){this.service=service;this.users=users;}
    @GetMapping("/documents") public Object documents(){return service.all();}
    @GetMapping("/documents/{id}") public Object document(@PathVariable Long id){return service.get(id);}
    @PostMapping("/documents") public Document create(@RequestParam String documentCode,@RequestParam String title,@RequestParam(required=false) String description){return service.create(documentCode,title,description);}
    @PostMapping("/documents/{id}/files") public Object upload(@PathVariable Long id,@RequestParam MultipartFile file) throws IOException{return service.upload(id,file);}
    @PostMapping("/documents/{id}/submit") public ResponseEntity<?> submit(@PathVariable Long id){service.submit(id);return ResponseEntity.ok().build();}
    @GetMapping("/documents/{id}/workflow") public Object workflow(@PathVariable Long id){return service.workflow(id);}
    @GetMapping("/documents/{id}/logs") public Object logs(@PathVariable Long id){return service.logs(id);}
    @GetMapping("/documents/{id}/print") public Object print(@PathVariable Long id){return java.util.Map.of("document",service.get(id),"workflow",service.workflow(id),"actions",service.actions(id));}
    @PostMapping("/flows/{id}/approve") public ResponseEntity<?> approve(@PathVariable Long id,@RequestParam(required=false) String comment){service.approve(id,comment);return ResponseEntity.ok().build();}
    @PostMapping("/flows/{id}/reject") public ResponseEntity<?> reject(@PathVariable Long id,@RequestParam(required=false) String comment){service.reject(id,comment);return ResponseEntity.ok().build();}
    @PostMapping("/flows/{id}/return") public ResponseEntity<?> ret(@PathVariable Long id,@RequestParam(required=false) String comment){service.returnDocument(id,comment);return ResponseEntity.ok().build();}
    @PostMapping("/flows/{id}/note") public ResponseEntity<?> note(@PathVariable Long id,@RequestParam String comment){service.note(id,comment);return ResponseEntity.ok().build();}
    @GetMapping("/users") public Object users(){return users.findAll();}
}
