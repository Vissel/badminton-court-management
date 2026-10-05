package com.badminton.backup.controller;

import com.badminton.backup.entity.BackupFile;
import com.badminton.backup.entity.BackupTrigger;
import com.badminton.backup.entity.BackupWatermark;
import com.badminton.backup.model.*;
import com.badminton.backup.repository.BackupFileRepository;
import com.badminton.backup.repository.BackupTriggerRepository;
import com.badminton.backup.repository.BackupWatermarkRepository;
import com.badminton.backup.service.BackupOrchestrator;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backup")
public class BackupController {
    private final BackupOrchestrator orchestrator;
    private final BackupTriggerRepository triggers;
    private final BackupFileRepository files;
    private final BackupWatermarkRepository watermarks;
    public BackupController(BackupOrchestrator orchestrator, BackupTriggerRepository triggers,
            BackupFileRepository files, BackupWatermarkRepository watermarks) {
        this.orchestrator = orchestrator; this.triggers = triggers; this.files = files; this.watermarks = watermarks;
    }

    @PostMapping("/triggers")
    public ResponseEntity<TriggerResponse> trigger(@Valid @RequestBody BackupRequest request) {
        return ResponseEntity.accepted().body(orchestrator.requestBackup(TriggerSource.REST, request.scope(), null, null));
    }

    @GetMapping("/triggers")
    public Page<BackupTrigger> history(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return triggers.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
    }

    @GetMapping("/triggers/{id}")
    public ResponseEntity<BackupTrigger> detail(@PathVariable long id) {
        return triggers.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/triggers/{id}/files")
    public List<BackupFile> files(@PathVariable long id) { return files.findByTriggerTriggerIdOrderByFileId(id); }

    @GetMapping("/watermarks")
    public List<BackupWatermark> watermarks() { return watermarks.findAll(); }
}
