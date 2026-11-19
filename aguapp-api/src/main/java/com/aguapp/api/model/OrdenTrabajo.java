package com.aguapp.api.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record OrdenTrabajo(String id, String folio, String reporteId, String organismo,
                          String prioridad, String estado, String descripcion,
                          LocalDate fechaProgramada, OffsetDateTime creadoEn) { }
