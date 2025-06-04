package org.cytoscape.cytocontainer.rest.engine.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.cytoscape.cytocontainer.rest.model.BinaryData;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.CytoContainerResult;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

public class KubernetesCytoContainerRunner implements Callable<CytoContainerResult> {

    static Logger _logger = LoggerFactory.getLogger(KubernetesCytoContainerRunner.class);

    private String _id;
    private CytoContainerRequest _cdr;
    private String _dockerImage;
    private Map<String, String> _customParameters;
    private String _taskDir;
    private String _workDir;
    private long _startTime;
    private String _inputFilePath;
    private boolean _outputIsBinary;
    private String _rawResultContentType;
    private int _maxAttempts;
    private long _sleepIntervalMs;

    public KubernetesCytoContainerRunner(String id,
                                         CytoContainerRequest cdr,
                                         long startTime,
                                         String taskDir,
                                         String dockerImage,
                                         Map<String, String> customParameters,
                                         boolean outputIsBinary,
                                         String rawResultContentType,
                                         int maxAttempts,
                                         long sleepIntervalMs) throws Exception {
        _id = id;
        _cdr = cdr;
        _dockerImage = dockerImage;
        _customParameters = customParameters;
        _startTime = startTime;
        _taskDir = taskDir;
        _workDir = _taskDir + File.separator + _id;
        _outputIsBinary = outputIsBinary;
        _rawResultContentType = rawResultContentType;
        _maxAttempts = maxAttempts;
        _sleepIntervalMs = sleepIntervalMs;

        _inputFilePath = writeInputFile();
    }

    private String writeInputFile() throws CytoContainerException, IOException {
        File workDir = new File(_workDir);
        if (!workDir.isDirectory()) {
            if (!workDir.mkdirs()) {
                throw new CytoContainerException("Unable to create directory: " + _workDir);
            }
        }
        File destFile = new File(_workDir + File.separator + "input.txt");
        if (_cdr.getData() instanceof TextNode) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(destFile))) {
                bw.write(_cdr.getData().asText());
            }
        } else {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(destFile, _cdr.getData());
        }
        return destFile.getAbsolutePath();
    }

    private String buildCommand() {
        StringBuilder command = new StringBuilder(_inputFilePath);
        if (_customParameters != null) {
            for (Map.Entry<String, String> entry : _customParameters.entrySet()) {
                command.append(" ").append(entry.getKey());
                if (entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    command.append(" ").append(entry.getValue());
                }
            }
        }
        return command.toString();
    }

    @Override
    public CytoContainerResult call() throws Exception {
        ApiClient client = Config.fromCluster();
        Configuration.setDefaultApiClient(client);
        BatchV1Api batchApi = new BatchV1Api();
        CoreV1Api coreApi = new CoreV1Api();

        String jobName = "cyto-job-" + _id;

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .template(new V1PodTemplateSpec()
                                .metadata(new V1ObjectMeta().name(jobName))
                                .spec(new V1PodSpec()
                                        .restartPolicy("Never")
                                        .containers(Collections.singletonList(
                                                new V1Container()
                                                        .name("cyto-task")
                                                        .image(_dockerImage)
                                                        .command(Arrays.asList("sh", "-c", buildCommand()))
                                                        .volumeMounts(Collections.singletonList(
                                                                new V1VolumeMount().mountPath("/data").name("work-volume")
                                                        ))
                                        ))
                                        .volumes(Collections.singletonList(
                                                new V1Volume()
                                                        .name("work-volume")
                                                        .hostPath(new V1HostPathVolumeSource().path(_workDir).type("Directory"))
                                        ))
                                )
                        ).ttlSecondsAfterFinished(300));

        batchApi.createNamespacedJob("default", job, null, null, null);

        String podName = waitForPodAndParseStderr(coreApi, jobName);
        String logs = coreApi.readNamespacedPodLog(podName, "default", null, null, null, null, null, null, null, null);

        return buildResult(logs);
    }

    private String waitForPodAndParseStderr(CoreV1Api coreApi, String jobName) throws Exception {
        File stderrFile = new File(_workDir + File.separator + "stderr.txt");
        for (int i = 0; i < _maxAttempts; i++) {
            V1PodList pods = coreApi.listNamespacedPod("default", null, null, null, null,
                    "job-name=" + jobName, null, null, null, 5, null);
            if (!pods.getItems().isEmpty()) {
                V1Pod pod = pods.getItems().get(0);
                String podName = pod.getMetadata().getName();
                String phase = pod.getStatus().getPhase();

                try {
                    String stderr = coreApi.readNamespacedPodLog(podName, "default", null, null, null, null, null, true, null, null);
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(stderrFile, false))) {
                        writer.write(stderr);
                    }
                    parseProgressIndicators(stderr);
                } catch (Exception e) {
                    _logger.warn("Error reading stderr log: {}", e.getMessage());
                }

                if ("Succeeded".equals(phase) || "Failed".equals(phase)) {
                    return podName;
                }
            }
            Thread.sleep(_sleepIntervalMs);
        }
        throw new RuntimeException("Timed out waiting for pod to complete");
    }

    private void parseProgressIndicators(String log) {
        if (log == null) return;
        String[] lines = log.split("\n");
        for (String line : lines) {
            if (line.contains("<<<PROGRESS:") && line.contains(">>>")) {
                String value = line.substring(line.indexOf(":") + 1, line.indexOf(">>>")).trim();
                _logger.info("Progress: {}", value);
            } else if (line.contains("<<<MESSAGE:") && line.contains(">>>")) {
                String value = line.substring(line.indexOf(":") + 1, line.indexOf(">>>")).trim();
                _logger.info("Message: {}", value);
            }
        }
    }

    private CytoContainerResult buildResult(String logs) {
        CytoContainerResult result = new CytoContainerResult();
        result.setId(_id);
        result.setStartTime(_startTime);
        result.setProgress(100);
        result.setWallTime(System.currentTimeMillis() - _startTime);
        result.setStatus(CytoContainerResult.COMPLETE_STATUS);
        result.setResult(new TextNode(logs));
        return result;
    }
}
