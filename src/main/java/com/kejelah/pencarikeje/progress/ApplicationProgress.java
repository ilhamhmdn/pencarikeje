package com.kejelah.pencarikeje.progress;

import com.kejelah.pencarikeje.application.Application;
import com.kejelah.pencarikeje.common.BaseAuditEntity;
import com.kejelah.pencarikeje.status.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * One dated event in an application's history.
 *
 * <p>An application always has at least one progress event (MVP.md 3.1, PRG-04).
 * Rejection context lives in {@link #notes} like every other status — there is no
 * dedicated rejection_reason column (PRG-06).
 */
@Entity
@Table(name = "application_progress")
public class ApplicationProgress extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(columnDefinition = "text")
    private String notes;

    protected ApplicationProgress() {
        // for JPA
    }

    public ApplicationProgress(Application application, Status status, LocalDate eventDate, String notes) {
        this.application = application;
        this.status = status;
        this.eventDate = eventDate;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
