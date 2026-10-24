package com.estoque.service.form;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.estoque.domain.PurchaseOrder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PurchaseForm {

    private Long id;

    @NotNull(message = "Informe o fornecedor")
    private Long partnerId;

    @NotNull(message = "Informe o local de entrega")
    private Long destLocationId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedDate;

    @Size(max = 120)
    private String origin;

    private String notes;

    private List<LineForm> lines = new ArrayList<>();

    public static PurchaseForm from(PurchaseOrder o) {
        var f = new PurchaseForm();
        f.id = o.getId();
        f.partnerId = o.getPartner().getId();
        f.destLocationId = o.getDestLocation().getId();
        f.expectedDate = o.getExpectedDate();
        f.origin = o.getOrigin();
        f.notes = o.getNotes();
        o.getLines().forEach(l -> f.lines.add(new LineForm(l.getProduct().getId(), l.getQuantity(), l.getUnitPrice())));
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public Long getDestLocationId() { return destLocationId; }
    public void setDestLocationId(Long destLocationId) { this.destLocationId = destLocationId; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<LineForm> getLines() { return lines; }
    public void setLines(List<LineForm> lines) { this.lines = lines; }
}
