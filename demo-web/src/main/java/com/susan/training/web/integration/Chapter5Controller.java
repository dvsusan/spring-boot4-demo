package com.susan.training.web.integration;

import com.susan.training.common.ApiResponse;
import com.susan.training.integration.excel.ExcelBatchService;
import com.susan.training.integration.excel.ExcelJobInfo;
import com.susan.training.integration.file.FileMetadata;
import com.susan.training.integration.file.MinioFileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 第 29–30 篇使用的文件上传和 Excel 批处理接口。
 */
@RestController
@RequestMapping("/api/v1/chapter5")
public class Chapter5Controller {
    private final ObjectProvider<MinioFileStorageService> fileServiceProvider;
    private final ObjectProvider<ExcelBatchService> excelServiceProvider;

    public Chapter5Controller(
            ObjectProvider<MinioFileStorageService> fileServiceProvider,
            ObjectProvider<ExcelBatchService> excelServiceProvider
    ) {
        this.fileServiceProvider = fileServiceProvider;
        this.excelServiceProvider = excelServiceProvider;
    }

    /**
     * 上传文件并返回对象名和 15 分钟临时预览地址。
     */
    @PostMapping("/files")
    public ApiResponse<FileMetadata> uploadFile(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        MinioFileStorageService service = fileServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "MinIO 未开启");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return ApiResponse.success(service.upload(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    inputStream
            ));
        }
    }

    /**
     * 为已经上传的对象重新生成临时预览地址。
     */
    @GetMapping("/files/url")
    public ApiResponse<String> fileUrl(@RequestParam String objectName) throws Exception {
        MinioFileStorageService service = fileServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "MinIO 未开启");
        }
        return ApiResponse.success(service.presignedUrl(objectName));
    }

    /**
     * 流式导出指定行数，默认只生成 1000 行。
     */
    @GetMapping("/excel/export")
    public void export(
            @RequestParam(defaultValue = "1000") int rows,
            HttpServletResponse response
    ) throws Exception {
        ExcelBatchService service = excelServiceProvider.getIfAvailable();
        if (service == null) {
            response.setStatus(503);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":\"FEATURE_DISABLED\",\"message\":\"Excel 服务未开启\"}"
            );
            return;
        }
        if (rows < 0 || rows > 1_000_000) {
            throw new IllegalArgumentException("rows 必须在 0 到 1000000 之间");
        }
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=orders.xlsx"
        );
        service.exportOrders(response.getOutputStream(), rows);
    }

    /**
     * 接收 Excel 后立即返回任务 ID，实际读取在虚拟线程中完成。
     */
    @PostMapping("/excel/import")
    public ApiResponse<String> importExcel(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        ExcelBatchService service = excelServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "Excel 服务未开启");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return ApiResponse.success(service.importAsync(inputStream));
        }
    }

    /**
     * 查询异步导入状态和已处理行数。
     */
    @GetMapping("/excel/jobs/{jobId}")
    public ApiResponse<ExcelJobInfo> excelJob(@PathVariable String jobId) {
        ExcelBatchService service = excelServiceProvider.getIfAvailable();
        if (service == null) {
            return ApiResponse.failure("FEATURE_DISABLED", "Excel 服务未开启");
        }
        return ApiResponse.success(service.status(jobId));
    }
}
