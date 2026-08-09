package com.payhub.reconciliation.infrastructure.statement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.payhub.reconciliation.application.port.out.StatementSourcePort;

@Component
public class ClasspathCsvStatementSource implements StatementSourcePort {

    @Override
    public List<StatementLine> load(String statementKey) {
        String path = "statements/" + statementKey + ".csv";
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Statement not found: " + statementKey);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            List<StatementLine> lines = new ArrayList<>();
            String header = reader.readLine();
            if (header == null) {
                return List.of();
            }
            String row;
            while ((row = reader.readLine()) != null) {
                if (row.isBlank()) {
                    continue;
                }
                String[] cols = row.split(",", -1);
                if (cols.length < 5) {
                    throw new IllegalArgumentException("Invalid statement row: " + row);
                }
                lines.add(new StatementLine(
                        cols[0].trim(),
                        UUID.fromString(cols[1].trim()),
                        cols[2].trim(),
                        cols[3].trim(),
                        cols[4].trim()
                ));
            }
            return List.copyOf(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read statement " + statementKey, e);
        }
    }
}
