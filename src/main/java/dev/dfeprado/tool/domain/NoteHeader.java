package dev.dfeprado.tool.domain;

import java.time.LocalDate;

public record NoteHeader(String brokerName, String number, LocalDate date) {}
