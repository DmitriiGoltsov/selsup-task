package org.goltsov.selsup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final static String CREATE_DOCUMENT_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private TimeUnit timeUnit;

    private int requestLimit;

    private ScheduledExecutorService scheduler;

    private Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.semaphore = new Semaphore(requestLimit);

        scheduler.scheduleAtFixedRate(this::resetSemaphore, 0, 1, timeUnit);
    }

    public String createGoodCirculationDocument(Document document, String signature) throws IllegalAccessException {
        String docType = document.getDocumentType();

        ObjectMapper mapper = mapperFactory(docType);
        String body;

        try {
            body = mapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }

        if (body == null) {
            throw new IllegalArgumentException("Could not map an Document object to a String");
        }

        return sendPostRequest(signature, body);
    }

    private String sendPostRequest(String token, String body) {

        try (var client = HttpClients.createDefault()) {

            ClassicHttpRequest postRequest = ClassicRequestBuilder.post(CREATE_DOCUMENT_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + token)
                    .setEntity(body, ContentType.APPLICATION_JSON)
                    .build();

            HttpClientResponseHandler<String> handler = new BasicHttpClientResponseHandler();

            return client.execute(postRequest, handler);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonDocument createDocument() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("string", "string", "string",
                "string", "string", "string", "string", "string",
                "string"));
        Description description = new Description("0000000000");
        return new JsonDocument(description, "25", "doc_status", true, "1111111111",
                "2222222222", "3333333333", "2023-01-01",
                "OWN_PRODUCTION", products, "2023-01-01", "3553535");
    }

    private ObjectMapper mapperFactory(String documentType) {

        ObjectMapper mapper;

        switch (documentType) {
            case "LP_INTRODUCE_GOODS" -> {
                mapper = new ObjectMapper();
            }

            /* Yaml mapper указан для примера. Так как по условиям задания нам требуется формировать лишь JSON, то ни он
            * ни Xml маппер здесь не требуются. Они показаны лишь для того, чтобы продемонстрировать, что
            * при необходимости приложение будет легко расширяемым: если нужно формировать иные типы документов, помимо
            * существующих, то достаточно будет немного скорректировать данный класс и, возможно,
            * метод createGoodCirculationDocument(), а не писать новый функционал с нуля. */

            case "LP_INTRODUCE_GOODS_CSV" -> {
                mapper = new YAMLMapper();
            }

            case "LP_INTRODUCE_GOODS_XML" -> {
                mapper = new XmlMapper();
            }
            default -> throw new IllegalStateException("Unexpected value: " + documentType);
        }

        return mapper;
    }


    private void resetSemaphore() {
        semaphore.release(requestLimit - semaphore.availablePermits());
    }

    interface Document {

        String getDocumentType();
    }

    abstract class UnspecifiedDocument implements Document {

        private Description description;

        @JsonProperty("doc_id")
        private String docId;

        @JsonProperty("doc_status")
        private String docStatus;

        @JsonProperty("doc_type")
        private DocumentType docType;

        @JsonProperty("import_request")
        private boolean importRequest;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("participant_inn")
        private String participantInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("production_type")
        private String productionType;

        private List<Product> products;

        @JsonProperty("reg_date")
        private String registrationDate;

        @JsonProperty("reg_number")
        private String registrationNumber;

        public UnspecifiedDocument(Description description, String docId, String docStatus, DocumentType docType,
                                   boolean importRequest, String ownerInn, String participantInn, String producerInn,
                                   String productionDate, String productionType, List<Product> products,
                                   String registrationDate, String registrationNumber) {
            this.description = description;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.registrationDate = registrationDate;
            this.registrationNumber = registrationNumber;
        }

        public UnspecifiedDocument() {
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public DocumentType getDocType() {
            return docType;
        }

        public void setDocType(DocumentType docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public String getRegistrationDate() {
            return registrationDate;
        }

        public void setRegistrationDate(String registrationDate) {
            this.registrationDate = registrationDate;
        }

        public String getRegistrationNumber() {
            return registrationNumber;
        }

        public void setRegistrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
        }
    }

    class XmlDocument extends UnspecifiedDocument implements Document {

        public XmlDocument(Description description, String docId, String docStatus,
                           boolean importRequest, String ownerInn, String participantInn, String producerInn,
                           String productionDate, String productionType, List<Product> products,
                           String registrationDate, String registrationNumber) {

            super(description, docId, docStatus, DocumentType.LP_INTRODUCE_GOODS_XML, importRequest, ownerInn,
                    participantInn, producerInn, productionDate, productionType, products,
                    registrationDate, registrationNumber);
        }

        public XmlDocument() {
        }

        @Override
        public String getDocumentType() {
            return DocumentType.LP_INTRODUCE_GOODS_XML.toString();
        }
    }

    class CvsDocument extends UnspecifiedDocument implements Document {

        public CvsDocument(Description description, String docId, String docStatus, boolean importRequest,
                           String ownerInn, String participantInn, String producerInn, String productionDate,
                           String productionType, List<Product> products,
                           String registrationDate, String registrationNumber) {

            super(description, docId, docStatus, DocumentType.LP_INTRODUCE_GOODS_CSV, importRequest, ownerInn,
                    participantInn, producerInn, productionDate, productionType, products, registrationDate,
                    registrationNumber);
        }

        public CvsDocument() {
        }

        @Override
        public String getDocumentType() {
            return DocumentType.LP_INTRODUCE_GOODS_CSV.toString();
        }
    }

    class JsonDocument extends UnspecifiedDocument implements Document {

        public JsonDocument(Description description, String docId, String docStatus, boolean importRequest,
                            String ownerInn, String participantInn, String producerInn, String productionDate,
                            String productionType, List<Product> products, String registrationDate,
                            String registrationNumber) {

            super(description, docId, docStatus, DocumentType.LP_INTRODUCE_GOODS, importRequest, ownerInn,
                    participantInn, producerInn, productionDate, productionType, products, registrationDate,
                    registrationNumber);
        }

        public JsonDocument() {
        }

        @Override
        public String getDocumentType() {
            return DocumentType.LP_INTRODUCE_GOODS.toString();
        }
    }

    enum DocumentType {
        LP_INTRODUCE_GOODS,
        LP_INTRODUCE_GOODS_CSV,
        LP_INTRODUCE_GOODS_XML
    }

    class Description {

        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }
    }

    class Product {

        @JsonProperty("certificate_document")
        private String certificateDocument;

        @JsonProperty("certificate_document_date")
        private String certificateDocumentDate;

        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;

        @JsonProperty("owner_inn")
        private String ownerInn;

        @JsonProperty("producer_inn")
        private String producerInn;

        @JsonProperty("production_date")
        private String productionDate;

        @JsonProperty("tnved_code")
        private String tnvedCode;

        @JsonProperty("uit_code")
        private String uitCode;

        @JsonProperty("uitu_code")
        private String uituCode;

        public Product(String certificateDocument, String certificateDocumentDate, String certificateDocumentNumber,
                       String ownerInn, String producerInn, String productionDate,
                       String tnvedCode, String uitCode, String uituCode) {
            this.certificateDocument = certificateDocument;
            this.certificateDocumentDate = certificateDocumentDate;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
            this.uitCode = uitCode;
            this.uituCode = uituCode;
        }

        public Product() {
        }

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public String getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(String certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(String productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }

    static class Main {
        public static void main(String[] args) throws IllegalAccessException {

            CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 2);
            String signature = "token";
            Document document = crptApi.createDocument();

            System.out.println(crptApi.createGoodCirculationDocument(document, signature));
        }
    }
}
