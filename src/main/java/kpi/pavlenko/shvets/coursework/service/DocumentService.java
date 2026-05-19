package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.entity.ProtocolDocument;
import kpi.pavlenko.shvets.coursework.repository.ClinicalProtocolRepository;
import kpi.pavlenko.shvets.coursework.repository.ProtocolDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DocumentService {
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final ProtocolDocumentRepository documentRepository;
    private final ClinicalProtocolRepository protocolRepository;

    public DocumentService(ProtocolDocumentRepository documentRepository,
                           ClinicalProtocolRepository protocolRepository) {
        this.documentRepository = documentRepository;
        this.protocolRepository = protocolRepository;
    }

    public ProtocolDocument upload(Long protocolId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("Файл порожній");
        }

        // Перевірка типу файлу
        String contentType = file.getContentType();
        if (!isAllowed(contentType)) {
            throw new RuntimeException("Дозволені лише зображення та PDF");
        }

        // Створити директорію якщо не існує
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Унікальне ім'я файлу на диску
        String extension = getExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;

        // Зберегти файл на диск
        Path filePath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Знайти протокол
        ClinicalProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Протокол не знайдено"));

        ProtocolDocument doc = ProtocolDocument.builder()
                .protocol(protocol)
                .originalName(file.getOriginalFilename())
                .storedName(storedName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .build();

        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public Resource loadAsResource(Long docId) throws MalformedURLException {
        ProtocolDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Документ не знайдено"));

        Path filePath = Paths.get(uploadDir).resolve(doc.getStoredName());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("Файл не знайдено на диску: " + doc.getStoredName());
        }
        return resource;
    }

    @Transactional(readOnly = true)
    public ProtocolDocument getById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Документ не знайдено"));
    }

    @Transactional(readOnly = true)
    public List<ProtocolDocument> getForProtocol(Long protocolId) {
        return documentRepository.findByProtocolId(protocolId);
    }

    public void delete(Long docId) throws IOException {
        ProtocolDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Документ не знайдено"));

        Path filePath = Paths.get(uploadDir).resolve(doc.getStoredName());
        Files.deleteIfExists(filePath);

        documentRepository.delete(doc);
    }

    private boolean isAllowed(String contentType) {
        return contentType != null && (
                contentType.startsWith("image/") ||
                        contentType.equals("application/pdf")
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
