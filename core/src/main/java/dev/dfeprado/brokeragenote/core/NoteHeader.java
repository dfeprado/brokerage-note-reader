package dev.dfeprado.brokeragenote.core;

import java.time.LocalDate;

public record NoteHeader(String brokerName, String number, LocalDate date) {
}
