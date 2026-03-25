package br.com.docquery.document.document.infrastructure.parsing;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    public String extractText(byte[] content) {
        try {
            return tika.parseToString(new ByteArrayInputStream(content));
        } catch (IOException | TikaException e) {
            throw new RuntimeException("Failed to extract text from document", e);
        }
    }

}