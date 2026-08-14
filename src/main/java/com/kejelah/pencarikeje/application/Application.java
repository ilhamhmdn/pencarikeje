package com.kejelah.pencarikeje.application;

import com.kejelah.pencarikeje.auth.User;
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
 * One job applied to. Belongs to exactly one user.
 *
 * <p>Holds the <em>current state</em> plus static facts. Everything that
 * happened lives in {@code application_progress} instead (MVP.md 1.3).
 */
@Entity
@Table(name = "applications")
public class Application extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "role_name", nullable = false, length = 255)
    private String roleName;

    @Column(name = "job_description", columnDefinition = "text")
    private String jobDescription;

    @Column(name = "portal_url", columnDefinition = "text")
    private String portalUrl;

    @Column(name = "date_applied", nullable = false)
    private LocalDate dateApplied;

    /**
     * Denormalised cache of the current status, not an independent field.
     *
     * <p>The source of truth is the latest progress event. No code path may write
     * this directly; only {@code ApplicationProgressService} recomputes it, inside
     * the same transaction as the progress change (MVP.md 3.3).
     */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;

    @Column(name = "resume_filename", length = 255)
    private String resumeFilename;

    @Column(name = "resume_path", columnDefinition = "text")
    private String resumePath;

    @Column(columnDefinition = "text")
    private String notes;

    protected Application() {
        // for JPA
    }

    public Application(User user, String companyName, String roleName, LocalDate dateApplied, Status status) {
        this.user = user;
        this.companyName = companyName;
        this.roleName = roleName;
        this.dateApplied = dateApplied;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public LocalDate getDateApplied() {
        return dateApplied;
    }

    public void setDateApplied(LocalDate dateApplied) {
        this.dateApplied = dateApplied;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Writes the denormalised current-status cache.
     *
     * <p><strong>Only {@code ApplicationProgressService} may call this</strong>,
     * and only while recomputing from the latest progress event. The deliberately
     * awkward name exists so that any other call site is obvious in review — Java
     * visibility cannot express "one class in another package" (MVP.md 3.3).
     */
    public void applyRecomputedStatus(Status status) {
        this.status = status;
    }

    public String getResumeFilename() {
        return resumeFilename;
    }

    public void setResumeFilename(String resumeFilename) {
        this.resumeFilename = resumeFilename;
    }

    public String getResumePath() {
        return resumePath;
    }

    public void setResumePath(String resumePath) {
        this.resumePath = resumePath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
