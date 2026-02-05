package com.badminton.util;

import com.badminton.response.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Slf4j
public class ResponseConvertor {

    public static final <T> ResponseEntity<Result<T>> convert(Result<T> response) {
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(response.getErrorCode()).body(response);
    }

    /**
     * convert to Media resource
     *
     * @param response
     * @param <R>
     * @return
     */
    public static <R extends Resource> ResponseEntity<? extends Resource> convertToResource(Result<R> response) {
        HttpHeaders headers = new HttpHeaders();
        if (response.isSuccess()) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData(
                    "attachment",
                    "manager-report.xlsx"
            );
            String sessionFromStr = "";
            R data = response.getData();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=report_" + sessionFromStr + ".xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        }
        String errorContent = "error while exporting";
        ByteArrayResource errorByteArray = new ByteArrayResource(errorContent.getBytes());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE);
        headers.setContentDispositionFormData(
                "attachment",
                "error.txt"
        );
        return ResponseEntity.ok().headers(headers).body(errorByteArray);
    }
}
