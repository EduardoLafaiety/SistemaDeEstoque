package com.estoque.service.form;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.estoque.domain.Picking;
import com.estoque.domain.PickingType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PickingForm {

    private Long id;

    @NotNull
    private PickingType type;

    private Long partnerId;

    @NotNull(message = "Informe o local de origem")
    private Long sourceLocationId;

    @NotNull(message = "Informe o local de destino")
    private Long destLocationId;

    @NotNull(message = "Informe a data programada")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledDate;

    @Size(max = 120)
    private String origin;

    private String notes;

    private List<LineForm> lines = new ArrayList<>();

    public static PickingForm from(Picking p) {
        var f = new PickingForm();
        f.id = p.getId();
        f.type = p.getType();
        f.partnerId = p.getPartner() == null ? null : p.getPartner().getId();
        f.sourceLocationId = p.getSourceLocation().getId();
        f.destLocationId = p.getDestLocation().getId();
        f.scheduledDate = p.getScheduledDate();
        f.origin = p.getOrigin();
        f.notes = p.getNotes();
        p.getMoves().forEach(m -> f.lines.add(new LineForm(m.getProduct().getId(), m.getQuantity(), null)));
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PickingType getType() { return type; }
    public void setType(PickingType type) { this.type = type; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public Long getSourceLocationId() { return sourceLocationId; }
    public void setSourceLocationId(Long sourceLocationId) { this.sourceLocationId = sourceLocationId; }
    public Long getDestLocationId() { return destLocationId; }
    public void setDestLocationId(Long destLocationId) { this.destLocationId = destLocationId; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<LineForm> getLines() { return lines; }
    public void setLines(List<LineForm> lines) { this.lines = lines; }
}
