package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.ClinicalProtocol;
import kpi.pavlenko.shvets.coursework.entity.Patient;
import kpi.pavlenko.shvets.coursework.entity.ProtocolDocument;
import kpi.pavlenko.shvets.coursework.repository.ClinicalProtocolRepository;
import kpi.pavlenko.shvets.coursework.repository.ProtocolDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private ProtocolDocumentRepository documentRepository;
    @Mock
    private ClinicalProtocolRepository protocolRepository;

    @InjectMocks
    private DocumentService documentService;

    @TempDir // JUnit 5 создаст и очистит временную папку для тестов
    Path tempUploadDir;

    private ClinicalProtocol testProtocol;
    private ProtocolDocument testDocument;

    @BeforeEach
    void setUp() {
        // Используем временную директорию, созданную JUnit
        ReflectionTestUtils.setField(documentService, "uploadDir", tempUploadDir.toString());
        documentService.init(); // Теперь init() будет работать с реальной временной папкой

        testProtocol = ClinicalProtocol.builder()
                .id(1L)
                .patient(Patient.builder().id(1L).build())
                .build();

        testDocument = ProtocolDocument.builder()
                .id(1L)
                .protocol(testProtocol)
                .originalName("test.pdf")
                .storedName(UUID.randomUUID() + ".pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .build();
    }

    // --- init ---
    @Test
    void init_shouldCreateDirectory() {
        // Этот тест теперь неявно покрывается @BeforeEach,
        // но можно оставить для явной проверки, если нужно.
        // В данном случае, если init() не сработает, упадут другие тесты.
        assertThat(tempUploadDir).exists();
    }

    // --- upload ---
    @Test
    void upload_shouldSaveDocumentAndFile_whenValid() throws IOException {
        // Given
        MultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "some data".getBytes());
        when(protocolRepository.findById(testProtocol.getId())).thenReturn(Optional.of(testProtocol));
        when(documentRepository.save(any(ProtocolDocument.class))).thenAnswer(inv -> {
            ProtocolDocument doc = inv.getArgument(0);
            doc.setId(1L); // Имитируем, что БД присвоила ID. Имя файла установит сам сервис.
            return inv.getArgument(0);
        });

        // When
        ProtocolDocument result = documentService.upload(testProtocol.getId(), mockFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalName()).isEqualTo("test.pdf");

        // FIX: Проверяем, что файл действительно был создан во временной папке,
        // используя имя, которое вернул сервис.
        Path savedFile = tempUploadDir.resolve(result.getStoredName());
        assertThat(savedFile).exists();
        assertThat(Files.readAllBytes(savedFile)).isEqualTo("some data".getBytes());

        verify(documentRepository, times(1)).save(any(ProtocolDocument.class));
    }

    @Test
    void upload_shouldThrowException_whenFileIsEmpty() {
        // Given
        MultipartFile mockFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> documentService.upload(testProtocol.getId(), mockFile));
        assertThat(exception.getMessage()).isEqualTo("Файл порожній");
    }

    @Test
    void upload_shouldThrowException_whenFileTypeNotAllowed() {
        // Given
        MultipartFile mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "some data".getBytes());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> documentService.upload(testProtocol.getId(), mockFile));
        assertThat(exception.getMessage()).isEqualTo("Дозволені лише зображення та PDF");
    }

    @Test
    void upload_shouldThrowException_whenProtocolNotFound() {
        // Given
        MultipartFile mockFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "some data".getBytes());
        when(protocolRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> documentService.upload(99L, mockFile));
        assertThat(exception.getMessage()).isEqualTo("Протокол не знайдено");
    }

    // --- loadAsResource ---
    @Test
    void loadAsResource_shouldReturnResource_whenFileExists() throws IOException {
        // Given
        // Создаем реальный файл во временной директории
        Path dummyFile = tempUploadDir.resolve(testDocument.getStoredName());
        Files.write(dummyFile, "dummy content".getBytes());

        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When
        Resource result = documentService.loadAsResource(testDocument.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.exists()).isTrue();
        assertThat(result.isReadable()).isTrue();
        assertThat(result.getFilename()).isEqualTo(testDocument.getStoredName());
    }

    @Test
    void loadAsResource_shouldThrowException_whenDocumentNotFound() {
        // Given
        when(documentRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> documentService.loadAsResource(99L));
        assertThat(exception.getMessage()).isEqualTo("Документ не знайдено");
    }

    @Test
    void loadAsResource_shouldThrowException_whenFileNotOnDisk() {
        // Given
        // Файл НЕ создается на диске
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> documentService.loadAsResource(testDocument.getId()));
        assertThat(exception.getMessage()).contains("Файл не знайдено на диску");
    }

    // --- getById ---
    @Test
    void getById_shouldReturnDocument_whenExists() {
        // Given
        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));

        // When
        ProtocolDocument result = documentService.getById(testDocument.getId());

        // Then
        assertThat(result).isEqualTo(testDocument);
    }

    // --- getForProtocol ---
    @Test
    void getForProtocol_shouldReturnDocuments() {
        // Given
        List<ProtocolDocument> documents = Collections.singletonList(testDocument);
        when(documentRepository.findByProtocolId(testProtocol.getId())).thenReturn(documents);

        // When
        List<ProtocolDocument> result = documentService.getForProtocol(testProtocol.getId());

        // Then
        assertThat(result).isEqualTo(documents);
    }

    // --- delete ---
    @Test
    void delete_shouldDeleteDocumentAndFile() throws IOException {
        // Given
        Path dummyFile = tempUploadDir.resolve(testDocument.getStoredName());
        Files.createFile(dummyFile);
        assertThat(dummyFile).exists(); // Убедимся, что файл есть перед удалением

        when(documentRepository.findById(testDocument.getId())).thenReturn(Optional.of(testDocument));
        doNothing().when(documentRepository).delete(testDocument);

        // When
        documentService.delete(testDocument.getId());

        // Then
        verify(documentRepository, times(1)).delete(testDocument);
        assertThat(dummyFile).doesNotExist(); // Проверяем, что файл был удален с диска
    }
}
