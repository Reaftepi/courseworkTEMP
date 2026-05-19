package kpi.pavlenko.shvets.coursework.service;

import kpi.pavlenko.shvets.coursework.entity.*;
import kpi.pavlenko.shvets.coursework.observer.AppointmentEventPublisher;
import kpi.pavlenko.shvets.coursework.repository.AppointmentRepository;
import kpi.pavlenko.shvets.coursework.repository.InvoiceRepository;
import kpi.pavlenko.shvets.coursework.repository.PatientRepository;
import kpi.pavlenko.shvets.coursework.repository.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PatientRepository patientRepository;
    private final StaffRepository staffRepository;
    private final AppointmentEventPublisher appointmentEventPublisher;

    public AppointmentService(AppointmentRepository appointmentRepository, InvoiceRepository invoiceRepository, PatientRepository patientRepository, StaffRepository staffRepository, AppointmentEventPublisher appointmentEventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.patientRepository = patientRepository;
        this.staffRepository = staffRepository;
        this.appointmentEventPublisher = appointmentEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getScheduleForDay(Long staffId, LocalDate date){
        return appointmentRepository.findByStaffAndDay(staffId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public boolean hasConflict(Long staffId, LocalDateTime startTime, int duration) {
        var endTime = startTime.plusMinutes(duration);
        List<Appointment> appointments = appointmentRepository.findByStaffAndDay(staffId, startTime.minusMinutes(90), endTime.plusMinutes(90));
        for (Appointment apt : appointments) {
            if(apt.getStatus() == AppStatus.CANCELLED) {
                continue;
            }
            LocalDateTime aptEnd = apt.getStartTime().plusMinutes(apt.getDuration());
            if(apt.getStartTime().isBefore(endTime) && aptEnd.isAfter(startTime)) {
                return true;
            }
        }
        return false;
    }

    public Appointment save(Appointment appointment) {
        // Перевірка на конфлікт часу для нових записів
        if (appointment.getId() == null && hasConflict(appointment.getStaff().getId(), appointment.getStartTime(), appointment.getDuration())) {
            throw new RuntimeException("Schedule conflict detected.");
        }
        Appointment savedAppointment = appointmentRepository.save(appointment);
        // Тут можна додати логіку сповіщень, якщо потрібно
        return savedAppointment;
    }

    public Appointment create(Long patientId, Long staffId, LocalDateTime startTime, int duration, BigDecimal price) {
        if(hasConflict(staffId, startTime, duration)) {
            throw new RuntimeException("Schedule conflict detected.");
        }
        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new RuntimeException("Patient not found."));
        Staff staff = staffRepository.findById(staffId).orElseThrow(() -> new RuntimeException("Staff not found."));

        Appointment apt = Appointment.builder()
                .patient(patient)
                .staff(staff)
                .startTime(startTime)
                .duration(duration)
                .status(AppStatus.SCHEDULED)
                .build();
        apt = appointmentRepository.save(apt);

        Invoices inv = Invoices.builder()
                .appointment(apt)
                .totalAmount(price)
                .isPaid(false)
                .build();
        inv = invoiceRepository.save(inv);

        appointmentEventPublisher.onAppointmentCreated(apt);
        appointmentEventPublisher.onInvoiceUnpaid(apt, inv);

        return apt;
    }

    public Appointment cancel(Long appointmentId) {
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));
        apt.setStatus(AppStatus.CANCELLED);
        appointmentEventPublisher.onAppointmentCancelled(apt);
        return appointmentRepository.save(apt);
    }

    public Appointment completed(Long appointmentId){
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));
        apt.setStatus(AppStatus.COMPLETED);
        return appointmentRepository.save(apt);
    }

    public void reschedule(Long appointmentId, LocalDateTime newStartTime) {
        Appointment apt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found."));
        if (apt.getStatus() == AppStatus.CANCELLED) {
            throw new RuntimeException("Cannot reschedule a cancelled appointment.");
        }
        if (hasConflict(apt.getStaff().getId(), newStartTime, apt.getDuration())) {
            throw new RuntimeException("Schedule conflict detected.");
        }
        apt.setStartTime(newStartTime);
        appointmentRepository.save(apt);
        appointmentEventPublisher.onScheduleChanged(apt);
    }

    public List<Appointment> getAllForDay(LocalDate date){
        return appointmentRepository.findAllByDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<Appointment> findAppointmentsByFilter(Long staffId, LocalDate date) {
        if (staffId != null && staffId > 0) {
            return getScheduleForDay(staffId, date);
        } else {
            return getAllForDay(date);
        }
    }

}
