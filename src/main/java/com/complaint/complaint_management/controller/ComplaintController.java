
package com.complaint.complaint_management.controller;

import com.complaint.complaint_management.entity.Complaint;
import com.complaint.complaint_management.entity.Student;
import com.complaint.complaint_management.repository.ComplaintRepository;
import com.complaint.complaint_management.repository.StudentRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:5173")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;

    @Value("${complaint.upload-dir:uploads/complaints}")
    private String uploadDir;

    public ComplaintController(
            ComplaintRepository complaintRepository,
            StudentRepository studentRepository) {
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
    }

    // Submit a new complaint
    @PostMapping
    public ResponseEntity<?> submitComplaint(
            @RequestBody Complaint complaint) {

        if (complaint.getStudentId() == null
                || complaint.getTitle() == null
                || complaint.getTitle().isBlank()
                || complaint.getDescription() == null
                || complaint.getDescription().isBlank()
                || complaint.getCategory() == null
                || complaint.getCategory().isBlank()) {

            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Please fill in all required fields."));
        }

        // Confirm that the student exists.
        if (!studentRepository.existsById(complaint.getStudentId())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Student account not found."));
        }

        complaint.setStatus("SUBMITTED");
        complaint.setPhotoPath(null);
        complaint.setFeedback(null);

        Complaint saved = complaintRepository.save(complaint);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", saved.getStatus());
        response.put("complaintId", saved.getId());
        response.put("message", "Complaint submitted successfully.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get complaints belonging to one student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentComplaints(
            @PathVariable Long studentId) {

        if (!studentRepository.existsById(studentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Student account not found."));
        }

        return ResponseEntity.ok(
                complaintRepository.findByStudentId(studentId));
    }

    // Get all complaints, including associated student details, for admins
    @GetMapping
    public List<Map<String, Object>> getAllComplaints() {

        List<Complaint> complaints =
                complaintRepository.findAllByOrderByCreatedAtDesc();

        List<Map<String, Object>> results = new ArrayList<>();

        for (Complaint complaint : complaints) {
            Map<String, Object> item = new LinkedHashMap<>();

            item.put("id", complaint.getId());
            item.put("studentId", complaint.getStudentId());
            item.put("title", complaint.getTitle());
            item.put("description", complaint.getDescription());
            item.put("category", complaint.getCategory());
            item.put("status", complaint.getStatus());
            item.put("photoPath", complaint.getPhotoPath());
            item.put("feedback", complaint.getFeedback());
            item.put("createdAt", complaint.getCreatedAt());

            Student student = studentRepository
                    .findById(complaint.getStudentId())
                    .orElse(null);

            item.put("studentName",
                    student != null ? student.getName() : "Unknown student");

            item.put("studentEmail",
                    student != null ? student.getEmail() : "Unavailable");

            results.add(item);
        }

        return results;
    }

    // Update complaint status
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Complaint complaint =
                complaintRepository.findById(id).orElse(null);

        if (complaint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Complaint not found."));
        }

        String status = request.get("status");

        Set<String> allowedStatuses = Set.of(
                "SUBMITTED",
                "IN_PROGRESS",
                "RESOLVED",
                "REJECTED"
        );

        if (status == null || !allowedStatuses.contains(status)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid complaint status."));
        }

        complaint.setStatus(status);
        complaintRepository.save(complaint);

        return ResponseEntity.ok(
                Map.of("message", "Complaint status updated successfully."));
    }

    // Accept feedback only after the complaint is resolved
    @PutMapping("/{id}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Complaint complaint =
                complaintRepository.findById(id).orElse(null);

        if (complaint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Complaint not found."));
        }

        if (!"RESOLVED".equals(complaint.getStatus())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Feedback can be submitted only for resolved complaints."));
        }

        String feedback = request.get("feedback");

        if (feedback == null || feedback.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Feedback cannot be empty."));
        }

        complaint.setFeedback(feedback.trim());
        complaintRepository.save(complaint);

        return ResponseEntity.ok(
                Map.of("message", "Feedback submitted successfully."));
    }

    // Upload a complaint photo
    @PostMapping(
            value = "/{id}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo) {

        Complaint complaint =
                complaintRepository.findById(id).orElse(null);

        if (complaint == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("message", "Complaint not found."));
        }

        if (photo == null || photo.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Please select a photo."));
        }

        if (photo.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Photo must be 5 MB or smaller."));
        }

        String contentType = photo.getContentType();

        if (contentType == null
                || !Set.of("image/jpeg", "image/png", "image/webp")
                        .contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Only JPG, PNG, and WebP photos are accepted."));
        }

        try {
            Path directory = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(directory);

            String originalName = photo.getOriginalFilename();
            String extension = "";

            if (originalName != null
                    && originalName.lastIndexOf('.') >= 0) {
                extension = originalName
                        .substring(originalName.lastIndexOf('.'))
                        .toLowerCase();
            }

            if (!Set.of(".jpg", ".jpeg", ".png", ".webp")
                    .contains(extension)) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Unsupported photo extension."));
            }

            String filename = UUID.randomUUID() + extension;
            Path destination = directory.resolve(filename).normalize();

            if (!destination.startsWith(directory)) {
                return ResponseEntity.badRequest().body(
                        Map.of("message", "Invalid file path."));
            }

            Files.copy(
                    photo.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);

            complaint.setPhotoPath(filename);
            complaintRepository.save(complaint);

            return ResponseEntity.ok(
                    Map.of(
                            "message", "Photo uploaded successfully.",
                            "filename", filename));

        } catch (IOException exception) {
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Could not save the photo."));
        }
    }

    // Display an uploaded complaint photo
    @GetMapping("/photos/{filename:.+}")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable String filename) {

        // Accept only a filename, not a directory path.
        if (filename.contains("/") || filename.contains("\\")
                || filename.equals(".") || filename.equals("..")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path directory = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Path file = directory.resolve(filename).normalize();

            if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType;

            String lowerName = filename.toLowerCase();

            if (lowerName.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (lowerName.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            } else if (lowerName.endsWith(".jpg")
                    || lowerName.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(Files.readAllBytes(file));

        } catch (IOException exception) {
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
