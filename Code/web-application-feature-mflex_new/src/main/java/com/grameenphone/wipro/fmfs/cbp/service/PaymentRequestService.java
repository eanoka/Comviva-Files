package com.grameenphone.wipro.fmfs.cbp.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.util.ByteArrayDataSource;
import jakarta.servlet.ServletOutputStream;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.consts.SystemRoles;
import com.grameenphone.wipro.fmfs.cbp.enums.BillStatus;
import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;
import com.grameenphone.wipro.fmfs.cbp.model.data.cpp.PinVerificationRequest;
import com.grameenphone.wipro.fmfs.cbp.model.data.cpp.PinVerificationResponse;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillRevertibleCache;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequest;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequestHop;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentTask;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.RoleAction;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.UserAction;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.WorkflowHop;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BillExcelReport;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.BillPaymentApprovalRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedBill;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedPRRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedPaymentRequestDetail;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.RequestDetail;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.RequestSummary;
import com.grameenphone.wipro.fmfs.cbp.model.view.report.RequestModal;
import com.grameenphone.wipro.fmfs.cbp.model.view.report.CompanyInfo;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.ActionRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillRevertibleCacheRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.PaymentRequestHopRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.PaymentRequestRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.PaymentTaskRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.RoleRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.WorkflowRepository;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.MacroReplacer;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.excel.ExcelWriter;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.orm.HibernateUtil;
import com.grameenphone.wipro.utility.orm.WhereBuilder;
import com.grameenphone.wipro.utility.security.CryptoUtil;

@Service
public class PaymentRequestService {
    protected final static Logger logger = LoggerFactory.getLogger(PaymentRequestService.class);

    @Autowired
    BillDataService billDataService;

    @Autowired
    AuthService authService;

    @Autowired
    BillRepository billRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ActionRepository actionRepository;

    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    PaymentRequestRepository paymentRequestRepository;

    @Autowired
    PaymentRequestHopRepository paymentRequestHopRepository;

    @Autowired
    PaymentTaskRepository paymentTaskRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    BillRevertibleCacheRepository billCacheRepository;

    @Value("${email.template.payment.request.create.notification}")
    String paymentRequestCreateNotificationTemplate;

    @Value("${subject.payment.request.create.notification}")
    String paymentRequestCreateNotificationSubject;

    @Value("${email.template.payment.request.approved.notification}")
    String paymentApprovedNotificationTemplate;

    @Value("${subject.payment.request.approved.notification}")
    String paymentApprovedNotificationSubject;

    @Value("${email.template.payment.request.levelwize.approved.notification}")
    String paymentApprovedLevelwizeNotificationTemplate;

    @Value("${subject.payment.request.levelwize.approved.notification}")
    String paymentApprovedLevelwizeNotificationSubject;

    @Value("${email.template.payment.request.reject.notification}")
    String paymentRejectNotificationTemplate;

    @Value("${subject.payment.request.reject.notification}")
    String paymentRejectNotificationSubject;
    
    @Value("${email.template.payment.request.partial.reject.notification}")
    String paymentPartialRejectNotificationTemplate;

    @Value("${subject.payment.request.partial.reject.notification}")
    String paymentPartialRejectNotificationSubject;

    @Value("${email.template.payment.request.initiate.notification}")
    String paymentInitiatedNotificationTemplate;

    @Value("${subject.payment.request.initiate.notification}")
    String paymentInitiatedNotificationSubject;
    
    @Value("${flex.mfs.communicator.module.url}")
    String communicatorModuleUrl;

    @Value("${flex.mfs.communicator.module.timeout}")
    int communicatorModuleTimeout;

    @Value("${payment.request.attachment.dump.dir}")
    String paymentRequestAttachmentLocation;
    
    @Autowired
    BillRepository billreRepository;

    public PaginatedBill getSelectableUnpaidBills(long[] subAccountIds, long category, long company, String consumerId, long offset, int totalPerPage) {
        User currentUser = SessionAttributes.current().getUser();
        Collection<ClientDivision> allowedDivisions = billDataService.getAllowedDivisions(currentUser);
        PaginatedBill bills = new PaginatedBill();
        List<BillStatus> payableStatus = new ArrayList<>();
        payableStatus.add(BillStatus.Unpaid);
        payableStatus.add(BillStatus.Fail);
        WhereBuilder<Bill, ?> query = CrudDao.get(Bill.class).query()
                .inif(() -> subAccountIds != null && subAccountIds.length > 0, "clientDivision.id", () -> Arrays.stream(subAccountIds).mapToObj(x -> x).collect(Collectors.toList()))
                .inif(() -> subAccountIds == null || subAccountIds.length == 0, "clientDivision", allowedDivisions)
                .eqif(() -> company == 0 && category != 0, "company.category.id", category)
                .eqif(() -> company != 0, "company.id", company)
                .eqif(() -> StringUtil.hasText(consumerId), "accountNo", consumerId)
                .in("status", payableStatus)
                .or()
                    .nl("request")
                    .insub("request.lastHop.workflowHop.code", Arrays.asList(new WorkflowHops[] {WorkflowHops.CPL, WorkflowHops.REJ}))
                .close();
        bills.count = query.count();
        if(bills.count <= offset) {
            offset = (long)(Math.ceil(bills.count / (double)totalPerPage) - 1) * totalPerPage;
        }
        List<Bill> pendingBills;
        if(bills.count == 0) {
            offset = 0;
            pendingBills = new ArrayList<>();
        } else {
            pendingBills = query.findAll(offset, totalPerPage);
        }
        bills.offset = offset;
        bills.perPage = totalPerPage;
        bills.records = pendingBills;
        return bills;
    }

    public PaginatedBill getPaginatedBills(long requestId, long offset, int totalPerPage) {
        PaginatedBill bills = new PaginatedBill();
        WhereBuilder<Bill, ?> query = CrudDao.get(Bill.class).query().eq("request.id", requestId);
        bills.count = query.count();
        if(bills.count <= offset) {
            offset = (long)(Math.ceil(bills.count / (double)totalPerPage) - 1) * totalPerPage;
        }
        List<Bill> pendingBills;
        if(bills.count == 0) {
            offset = 0;
            pendingBills = new ArrayList<>();
        } else {
            pendingBills = query.findAll(offset, totalPerPage);
        }
        bills.offset = offset;
        bills.perPage = totalPerPage;
        bills.records = pendingBills;

        String requestQuery = "select p.id, p.addedBy.name, p.createTime, sum(b.billAmount), sum(0 + b.vat), sum(0 + b.serviceCharge), p.client ,p.attachment, p.lastHop, p.addedBy.email from Bill b right outer join b.request p where p.id = :id and (p.addedBy = :initiator or 0 < (select count(k) from p.lastHop.possibleExecutors k where k = :user)) group by p.id order by p.createTime desc";
        List<Object[]> result = (List<Object[]>)(List)CrudDao.get(Bill.class).executeQuery(requestQuery, 0, -1, new KV<>("id", requestId), new KV<>("user", SessionAttributes.current().getUser()), new KV<>("initiator", SessionAttributes.current().getUser()));
        if(result.size() > 0) {
            Object[] oa = result.get(0);
            bills.request = new RequestSummary((Long)oa[0], (String)oa[1], (Date)oa[2], (Double)oa[3], (Double)oa[4], (Double)oa[5], (Client) oa[6] , (String)oa[7], (PaymentRequestHop)oa[8], (String)oa[9]);
        }
        
		if (bills.request != null) {
			PaymentRequestHop requestHop = bills.request.lastHop;
			List<PaymentRequestHop> requestHopList = new ArrayList<>();
			while (requestHop != null) {
				requestHopList.add(requestHop);
				requestHop = requestHop.getPreviousHop();
			}
			Collections.reverse(requestHopList);
			bills.hops = requestHopList;
        }

        return bills;
    }

    @Transactional
    public PaymentRequest createPaymentRequest(Map<Long, Map<String,Object>> billIdAmount, MultipartFile bulkBillData) throws IOException 
    {
        PaymentRequest request = new PaymentRequest();
        User user = SessionAttributes.current().getUser();
        request.setAddedBy(user);
        request.setClient(user.getClient());
        request = paymentRequestRepository.save(request);
        CrudDao<Bill> billDao = CrudDao.get(Bill.class);
        Collection<Bill> bills = new ArrayList<>();
        Collection<ClientDivision> allowedDivisions = billDataService.getAllowedDivisions(user);
        Set<ClientDivision> divisions = new LinkedHashSet<>();
        PaymentRequest _finalRequest = request;
        billIdAmount.forEach((k, v) -> {
            Bill bill = billDao.findOne(k);
            if(!bill.getCompany().isHasBill()) 
            {
            	v.forEach((k1, v1) -> {
            		if(k1.equals("amount"))
            		{
            			bill.setBillAmount(Double.valueOf(v1.toString()).doubleValue());
            }
            	});
            }
            v.forEach((k1,v1) -> 
            {
            	if(k1.contains("additionalFields")) {
            		Map savable = (Map) v1;
            		if(!savable.isEmpty()) {
		        		try  {
							saveAdditionalFieldsToRevertibleCache(bill, savable);
						} catch (Exception e) {
							logger.error("Exception while saving bill revertible cache: ", e);
    					}
					}
            	}
            });
            bill.setRequest(_finalRequest);
            ClientDivision division = bill.getClientDivision();
            if(!allowedDivisions.contains(division)) {
                throw new AppRuntimeException("You have no permission to request bill payment for one of these Sub Account");
            }
            divisions.add(division);
            bills.add(bill);
        });
        billRepository.saveAll(bills);
        request.setClientDivisions(divisions);
        request.setLastHop(createWFAPaymentRequestHop(request));

        if (bulkBillData != null) {
            try {
                String dumpFileName = System.currentTimeMillis() + "_" + bulkBillData.getOriginalFilename();
                byte[] bytes = bulkBillData.getBytes();
                Path path = Paths.get(paymentRequestAttachmentLocation + dumpFileName);
                Files.write(path, bytes);
                request.setAttachment(dumpFileName);
            } catch (Exception ex) {
                logger.debug("Couldn't process bulk bill upload file : " + ex);
                AppRuntimeException apiResponse = new AppRuntimeException("Couldn't upload bill data");
                throw apiResponse;
            }
        }

        paymentRequestRepository.save(request);
        return request;
    }

    private void saveAdditionalFieldsToRevertibleCache(Bill bill, Map additionalField) throws IOException {
    	BillRevertibleCache cache = CrudDao.get(BillRevertibleCache.class).query().eq("bill.id", bill.getId()).findOne();
    	if(cache == null)
    	{
    		cache = new BillRevertibleCache();
    		cache.setBill(bill);
    		cache.setValuesAsJson(Json.toJson(additionalField));
    	}
    	else
    	{
    		cache.setValuesAsJson(Json.toJson(additionalField));
    	}
    	billCacheRepository.save(cache);
	}

    /**
     * Waiting For Approval
     * @param request
     * @return
     */
    private PaymentRequestHop createWFAPaymentRequestHop(PaymentRequest request) {
        PaymentRequestHop paymentRequestHop = new PaymentRequestHop();
        paymentRequestHop.setPaymentRequest(request);
        paymentRequestHop.setWorkflowHop(CrudDao.get(WorkflowHop.class).findOneByAllMatches(new KV<>("code", WorkflowHops.WFA), new KV<>("clientId", null)));
        List<User> users = userRepository.getFirstLevelApprovers(request);
        paymentRequestHop.setPossibleExecutors(users);
        return paymentRequestHop;
    }

    private PaymentRequestHop createWFAPaymentRequestHop(PaymentRequest request, WorkflowHop hop) {
        PaymentRequestHop paymentRequestHop = new PaymentRequestHop();
        paymentRequestHop.setPaymentRequest(request);
        paymentRequestHop.setWorkflowHop(hop);
        List<User> users = userRepository.getCustomLevelApprovers(request, hop.getRequiredAction().getName());
        paymentRequestHop.setPossibleExecutors(users);
        return paymentRequestHop;
    }

    /**
     * Ready For Payment
     * @param request
     * @return
     */
    private PaymentRequestHop createWPIPaymentRequestHop(PaymentRequest request) {
        PaymentRequestHop paymentRequestHop = new PaymentRequestHop();
        paymentRequestHop.setPaymentRequest(request);
        paymentRequestHop.setWorkflowHop(CrudDao.get(WorkflowHop.class).findOneByAllMatches(new KV<>("code", WorkflowHops.WPI)));
        List<User> users = userRepository.getPaymentFinalizers(request);
        paymentRequestHop.setPossibleExecutors(users);
        return paymentRequestHop;
    }

    /**
     * Scheduled For Execution
     * @param request
     * @return
     */
    private PaymentRequestHop createSFEPaymentRequestHop(PaymentRequest request) {
        PaymentRequestHop paymentRequestHop = new PaymentRequestHop();
        paymentRequestHop.setPaymentRequest(request);
        paymentRequestHop.setWorkflowHop(CrudDao.get(WorkflowHop.class).findOneByAllMatches(new KV<>("code", WorkflowHops.SFE)));
        return paymentRequestHop;
    }

    /**
     * Complete
     * @param request
     * @return
     */
    private PaymentRequestHop createREJPaymentRequestHop(PaymentRequest request) {
        PaymentRequestHop paymentRequestHop = new PaymentRequestHop();
        paymentRequestHop.setPaymentRequest(request);
        paymentRequestHop.setWorkflowHop(CrudDao.get(WorkflowHop.class).findOneByAllMatches(new KV<>("code", WorkflowHops.REJ)));
        return paymentRequestHop;
    }

    @Transactional(readOnly = true)
    public void sendCreationNotification(PaymentRequest request, MultipartFile attachment) {
        CrudDao.get(PaymentRequest.class).refresh(request); //new created requests misses some auto property set at db
        Collection<User> users = request.getLastHop().getPossibleExecutors();
        try {
            String modifiedTemplate = MacroReplacer.replaceMacros(paymentRequestCreateNotificationTemplate, Map.of("initiator", request.getAddedBy(), "request", request, "request_id", "PR" + String.format("%08x", request.getId()).toUpperCase()));
            MimeBodyPart messageAttachment = null;
            if(attachment != null) {
                messageAttachment = new MimeBodyPart();
                ByteArrayDataSource bds = new ByteArrayDataSource(attachment.getBytes(), "application/octet-stream");
                messageAttachment.setDataHandler(new DataHandler(bds));
                messageAttachment.setFileName(attachment.getOriginalFilename());
            }
            notificationService.sendEmail(paymentRequestCreateNotificationSubject, modifiedTemplate, users.stream().map(u -> u.getEmail()).collect(Collectors.joining(",")), request.getAddedBy().getEmail(), null, messageAttachment, true);
        } catch (MessagingException | IOException e) {
            logger.error("Couldn't send email to approvers", e);
        }
    }

    @Transactional(readOnly = true)
    public void sendApprovedNotification(PaymentRequest request, String attachment) {
        CrudDao.get(PaymentRequest.class).refresh(request); //new created requests misses some auto property set at db
        Collection<User> users = request.getLastHop().getPossibleExecutors().stream().filter(u-> !u.isDeleted() && u.isActive()).collect(Collectors.toList());
        try {
            String modifiedTemplate = MacroReplacer.replaceMacros(paymentApprovedNotificationTemplate, Map.of("initiator", request.getAddedBy(), "request", request, "request_id", "PR" + String.format("%08x", request.getId()).toUpperCase()));
            MimeBodyPart messageAttachment = getAttachement(attachment);
            notificationService.sendEmail(paymentApprovedNotificationSubject, modifiedTemplate, users.stream().map(u -> u.getEmail()).collect(Collectors.joining(",")), request.getAddedBy().getEmail(), null, messageAttachment, true);
        } catch (Exception e) {
            logger.error("Couldn't send email to payers", e);
        }
    }

    @Transactional(readOnly = true)
    public void sendLevelWiseApprovedNotification(PaymentRequest request, String attachment) {
    	CrudDao.get(PaymentRequest.class).refresh(request); //new created requests misses some auto property set at db
        Collection<User> users = request.getLastHop().getPossibleExecutors().stream().filter(u-> !u.isDeleted() && u.isActive()).collect(Collectors.toList());
        try {
            String modifiedTemplate = MacroReplacer.replaceMacros(paymentApprovedLevelwizeNotificationTemplate, Map.of("initiator", request.getAddedBy(), "request", request, "request_id", "PR" + String.format("%08x", request.getId()).toUpperCase()));
            MimeBodyPart messageAttachment = getAttachement(attachment);
            notificationService.sendEmail(paymentApprovedLevelwizeNotificationSubject, modifiedTemplate, users.stream().map(u -> u.getEmail()).collect(Collectors.joining(",")), request.getAddedBy().getEmail(), null, messageAttachment, true);
        } catch (Exception e) {
            logger.error("Couldn't send email to payers", e);
        }
    }

    @Transactional(readOnly = true)
    public void sendInitiatedNotification(PaymentRequest request, String attachment) {
        CrudDao.get(PaymentRequest.class).refresh(request); //new created requests misses some auto property set at db
        try {
            String modifiedTemplate = MacroReplacer.replaceMacros(paymentInitiatedNotificationTemplate, Map.of("initiator", request.getAddedBy(), "request", request, "request_id", "PR" + String.format("%08x", request.getId()).toUpperCase()));
            MimeBodyPart messageAttachment = getAttachement(attachment);
            notificationService.sendEmail(paymentInitiatedNotificationSubject, modifiedTemplate, request.getAddedBy().getEmail(), null, null, messageAttachment, true);
        } catch (Exception e) {
            logger.error("Couldn't send email to payers", e);
        }
    }
    
    private MimeBodyPart getAttachement(String attachment) throws MessagingException, IOException {
        MimeBodyPart messageAttachment = null;
        if(attachment != null) {
        	Path path = Paths.get(paymentRequestAttachmentLocation + attachment);
            messageAttachment = new MimeBodyPart();
            ByteArrayDataSource bds = new ByteArrayDataSource(Files.readAllBytes(path), "application/octet-stream");
            messageAttachment.setDataHandler(new DataHandler(bds));
            messageAttachment.setFileName(attachment);
        }
        return messageAttachment;
    }
    
    @Transactional(readOnly = true)
    public void sendPartialPRRejectionNotification(PaymentRequest request, Long[] billIdsToReject, String attachment) {
    	//TODO Need to create a new template for partial rejection of a PaymentRequest, and set billsToreject in pr.
        CrudDao.get(PaymentRequest.class).refresh(request);
        List<Bill> billsToReject = (List<Bill>) billreRepository.findAllById(Arrays.asList(billIdsToReject));
        StringBuffer sb =  new StringBuffer();
		billsToReject.forEach(b -> {
			sb.append("<p>Account number: <Strong>#" + b.getAccountNo() + ",</Strong> Bill Number: <Strong>#" + b.getBillNo() + "</Strong></p>");
		});
        String bills =sb.toString();
        
        try {
            String modifiedTemplate = MacroReplacer.replaceMacros(paymentPartialRejectNotificationTemplate, Map.of("bills", bills, "initiator", request.getAddedBy(), "request", request, "request_id", "PR" + String.format("%08x", request.getId()).toUpperCase()));
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            generateExcelFile(billsToReject, outputStream);
            
            MimeBodyPart messageAttachmentBody = new MimeBodyPart();
            ByteArrayDataSource bds = new ByteArrayDataSource(outputStream.toByteArray(), "application/octet-stream");
            messageAttachmentBody.setDataHandler(new DataHandler(bds));
            messageAttachmentBody.setFileName("Rejected Bills.xlsx");
            notificationService.sendEmail(paymentPartialRejectNotificationSubject, modifiedTemplate, request.getAddedBy().getEmail(), null, null, messageAttachmentBody, true);
        } catch (Exception e) {
            logger.error("Couldn't send email to payers", e);
        }
    }
    
    
    public void generateExcelFile(List<Bill> data, OutputStream outputStream) throws Exception {
        String str[] = {"Account No", "Bill Amount", "Service Charge", "Vat", "Sync Date", "Due Date", "Bill Number", "MSISDN", "Status"};
    	List<String> headers = Arrays.asList(str);
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("GPAY Report");

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setColor(IndexedColors.BLACK.getIndex());

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerCellStyle);
        }

        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Bill bill = data.get(i);
            row.createCell(0).setCellValue(bill.getAccountNo());
            row.createCell(1).setCellValue(String.valueOf(bill.getBillAmount()));
            row.createCell(2).setCellValue(String.valueOf(bill.getServiceCharge()));
            row.createCell(3).setCellValue(String.valueOf(bill.getVat()));
            row.createCell(4).setCellValue(String.valueOf(bill.getSyncDate()));
            row.createCell(5).setCellValue(String.valueOf(bill.getDueDate()));
            row.createCell(6).setCellValue(bill.getBillNo());
            row.createCell(7).setCellValue(String.valueOf(bill.getMsisdn()));
            row.createCell(8).setCellValue(String.valueOf(bill.getStatus()));
        }

        for (int i = 0; i < headers.size(); i++) {
            sheet.setColumnWidth(i, 5000);
        }

        workbook.write(outputStream);
    }
    

    @Transactional(readOnly = true)
    public void sendRejectionNotification(PaymentRequest request, String attachment) {
        CrudDao.get(PaymentRequest.class).refresh(request); //new created requests misses some auto property set at db
        try {
            String modifiedTemplate = MacroReplacer.replaceMacros(paymentRejectNotificationTemplate, Map.of("initiator", request.getAddedBy(), "request", request, "request_id", "PR" + String.format("%08x", request.getId()).toUpperCase()));
            MimeBodyPart messageAttachment = getAttachement(attachment);
            notificationService.sendEmail(paymentRejectNotificationSubject, modifiedTemplate, request.getAddedBy().getEmail(), null, null, messageAttachment, true);
        } catch (Exception e) {
            logger.error("Couldn't send email to payers", e);
        }
    }

    public int getApprovalWaitingCount() {
        String query = "select count(distinct p.id) from Bill b inner join b.request p where b.billAmount > 0 and p.lastHop.workflowHop.code = :workflow and 0 < (select count(k) from p.lastHop.possibleExecutors k where k = :user)";
        List<Object> result = CrudDao.get(Bill.class).executeQuery(query, 0, -1, new KV<>("workflow", WorkflowHops.WFA), new KV<>("user", SessionAttributes.current().getUser()));
        return ((Long)result.get(0)).intValue();
    }
    
    public List<RequestModal> getAllCompletedRequests() {
        String query = "select p.id, p.addedBy.name, p.createTime from PaymentRequest p where  p.lastHop.workflowHop.code = :workflow group by p.id order by p.createTime desc";
        List<Object[]> result = (List<Object[]>)(List)CrudDao.get(PaymentRequest.class).executeQuery(query, 0, -1, new KV<>("workflow", WorkflowHops.CPL));
        
        List<RequestModal> requests = result.stream().map(oa -> new RequestModal((Long)oa[0], (String)oa[1], (Date)oa[2])).collect(Collectors.toList());
        
        requests = requests.stream().map(req -> {
        	List<CompanyInfo> companies = billRepository.findByRequestId(req.id).stream().map(b -> new CompanyInfo(b.getCompany().getCode(), b.getCompany().getName())).distinct().collect(Collectors.toList());
        	req.companies = companies;
        	return req;
        }).collect(Collectors.toList());
        return requests;
    }

    public List<RequestSummary> getApprovalWaitingRequests() {
        String query = "select p.id, p.addedBy.name, p.createTime, sum(b.billAmount), sum(0 + b.vat), sum(0 + b.serviceCharge), p.lastHop.workflowHop.requiredAction.name from Bill b right outer join b.request p where b.billAmount > 0 and p.lastHop.workflowHop.code = :workflow and 0 < (select count(k) from p.lastHop.possibleExecutors k where k = :user) group by p.id order by p.createTime desc";
        List<Object[]> result = (List<Object[]>)(List)CrudDao.get(Bill.class).executeQuery(query, 0, -1, new KV<>("workflow", WorkflowHops.WFA), new KV<>("user", SessionAttributes.current().getUser()));
        List<String> permissions = SessionAttributes.current().getPermissions();
        return result.stream().filter(oa -> permissions.contains(oa[6]) && oa[3] != null).map(oa -> new RequestSummary((Long) oa[0], (String) oa[1], (Date) oa[2], (Double) oa[3], (Double) oa[4], (Double) oa[5], null,null)).collect(Collectors.toList());
    }

	public List<RequestSummary> getApprovedRequests() {
		String query = "select p.id, p.addedBy.name, p.createTime, sum(b.billAmount), sum(0 + b.vat), sum(0 + b.serviceCharge),p.attachment from Bill b right outer join b.request p where b.billAmount > 0 and p.lastHop.workflowHop.code = :workflow and 0 < (select count(k) from p.lastHop.possibleExecutors k where k = :user) group by p.id order by p.createTime desc";
		List<Object[]> result = (List<Object[]>) (List) CrudDao.get(Bill.class).executeQuery(query, 0, -1, new KV<>("workflow", WorkflowHops.WPI), new KV<>("user", SessionAttributes.current().getUser()));
		return result.stream().filter(oa -> oa[3] != null).map(oa -> new RequestSummary((Long) oa[0], (String) oa[1], (Date) oa[2], (Double) oa[3], (Double) oa[4], (Double) oa[5], null, (String) oa[6])).collect(Collectors.toList());
	}

    public int getApprovedCount() {
        String query = "select count(distinct p.id) from Bill b inner join b.request p where b.billAmount > 0 and p.lastHop.workflowHop.code = :workflow and 0 < (select count(k) from p.lastHop.possibleExecutors k where k = :user)";
        List<Object> result = CrudDao.get(Bill.class).executeQuery(query, 0, -1, new KV<>("workflow", WorkflowHops.WPI), new KV<>("user", SessionAttributes.current().getUser()));
        return ((Long)result.get(0)).intValue();
    }
    
    public List<PaymentRequestHop> getAwaitingPaymentRequestHops(Long clientId, Set<Action> actions) {
    	String query = "select prh from PaymentRequestHop prh outer join prh.workflowHop hop where prh.executedBy is null and prh.executionTime is null and hop.autoExecution = false and hop.clientId = :clientId and hop.requiredAction in :actions";
    	return (List) CrudDao.get(PaymentRequestHop.class).executeQuery(query, 0, -1, new KV<>("clientId", clientId), new KV<>("actions", actions));
    }

    public List<PaymentRequest> approvePayments(String comment, long[] ids) throws HttpErrorResponseException {
        CrudDao<PaymentRequest> paymentRequestCrudDao = CrudDao.get(PaymentRequest.class);
        List<PaymentRequest> requests = new ArrayList<>();
        User user = SessionAttributes.current().getUser();
        List<String> permissions = SessionAttributes.current().getPermissions();
        for (long id : ids) {
            PaymentRequest request = paymentRequestCrudDao.findOne(id);
            PaymentRequestHop previousHop = request.getLastHop();
            if(!permissions.contains(previousHop.getWorkflowHop().getRequiredAction().getName()) || !previousHop.getPossibleExecutors().contains(user)) {
                throw new HttpErrorResponseException(403, null, "You are not authorized for this action");
            }

            PaymentRequestHop nextHop = null;
            List<WorkflowHop> hops = workflowRepository.findByCodeAndClientIdOrderByOrder(WorkflowHops.WFA, SessionAttributes.current().getUser().getClient().getId());
            WorkflowHop previousWorkflowHop = previousHop.getWorkflowHop();
            Long previousWorkflowHopId = previousWorkflowHop.getId();
            if(previousWorkflowHop.getClientId() == null) {
                if(hops.size() > 0) {
                    nextHop = createWFAPaymentRequestHop(request, hops.get(0));
                }
            } else {
                boolean hopMatched = false;
                WorkflowHop nextWorkflowHop = null;
                for (WorkflowHop hop : hops) {
                    if(hopMatched) {
                        nextWorkflowHop = hop;
                        break;
                    }
                    if (previousWorkflowHopId.equals(hop.getId())) {
                        hopMatched = true;
                    }
                }
                if(nextWorkflowHop != null) {
                    nextHop = createWFAPaymentRequestHop(request, nextWorkflowHop);
                }
            }
            if(nextHop == null) {
                nextHop = createWPIPaymentRequestHop(request);
            }

            nextHop.setPreviousHop(previousHop);
            request.setLastHop(nextHop);
            previousHop.setComment(comment);
            previousHop.setExecutedBy(user);
            previousHop.setExecutionTime(new Date());
            paymentRequestRepository.save(request);
            requests.add(request);
        }
        return requests;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<PaymentRequest> initiatePayments(String pin, String comment, long[] ids) throws IOException, NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        CrudDao<PaymentRequest> paymentRequestCrudDao = CrudDao.get(PaymentRequest.class);
        List<PaymentRequest> requests = new ArrayList<>();
        User user = SessionAttributes.current().getUser();
        HttpClient http = new HttpClient(communicatorModuleTimeout);
        String url = communicatorModuleUrl + "wallet/pin/verify";
        PinVerificationRequest pvr = new PinVerificationRequest();
        pvr.msisdn = String.valueOf(user.getClient().getMsisdn());
        pvr.pin = pin;
        http.setPayloadLoggerInterceptor((HttpClient.HttpRequestSnapshot r) -> r.body.replaceAll("(\"pin\")\\s*:\\s*\"[^\"]*\"", "$1: \"****\""));
        PinVerificationResponse verification = http.postForEntity(url, Json.toJson(pvr), Map.of("Content-Type", "application/json"), PinVerificationResponse.class);
        if (null == verification || !verification.response.valid) {
            throw new HttpErrorResponseException(403, null, "Provided pin is not correct");
        }
        for (long id : ids) {
            PaymentRequest request = paymentRequestCrudDao.findOne(id);
            PaymentRequestHop previousHop = request.getLastHop();
            if(!previousHop.getPossibleExecutors().contains(user)) {
                throw new HttpErrorResponseException(403, null, "You are not authorized for this action");
            }
            PaymentRequestHop nextHop = createSFEPaymentRequestHop(request);
            nextHop.setPreviousHop(previousHop);
            paymentRequestHopRepository.save(nextHop);
            request.setLastHop(nextHop);
            paymentRequestRepository.save(request);
            previousHop.setComment(comment);
            previousHop.setExecutedBy(user);
            previousHop.setExecutionTime(new Date());
            paymentRequestHopRepository.save(previousHop);

            PaymentTask task = new PaymentTask();
            Timestamp date = new Timestamp(System.currentTimeMillis() / 1000 * 1000); // to clear millisecond
            task.setUpdateTime(date);
            String key = new SimpleDateFormat("YYYYMMdd::HHmmss").format(date);
            task.setPin(CryptoUtil.encrypt("AES/CBC/PKCS5Padding", pin, "AES", key, key));
            task.setPaymentRequestHop(nextHop);
            paymentTaskRepository.save(task);
            requests.add(request);
        }
        return requests;
    }

    public List<PaymentRequest> rejectApprovals(String comment, long[] ids) throws HttpErrorResponseException {
        CrudDao<PaymentRequest> paymentRequestCrudDao = CrudDao.get(PaymentRequest.class);
        List<PaymentRequest> requests = new ArrayList<>();
        User user = SessionAttributes.current().getUser();
        List<String> permissions = SessionAttributes.current().getPermissions();
        for (long id : ids) {
            PaymentRequest request = paymentRequestCrudDao.findOne(id);
            PaymentRequestHop previousHop = request.getLastHop();
            if(!permissions.contains(previousHop.getWorkflowHop().getRequiredAction().getName()) || !previousHop.getPossibleExecutors().contains(user)) {
                throw new HttpErrorResponseException(403, null, "You are not authorized for this action");
            }
            PaymentRequestHop nextHop = createREJPaymentRequestHop(request);
            nextHop.setPreviousHop(previousHop);
            request.setLastHop(nextHop);
            previousHop.setComment(comment);
            previousHop.setExecutedBy(user);
            previousHop.setExecutionTime(new Date());
            paymentRequestRepository.save(request);
            requests.add(request);
        }
        return requests;
    }

    public PaginatedPaymentRequestDetail getFilteredRequests(PaginatedPRRequest request) {
        String dataQuery = "select new com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.RequestDetail(p.id, count(b), p.createTime, p.addedBy.name, sum((b.billAmount + (case when b.serviceCharge is null then 0 else b.serviceCharge end))), p.lastHop.workflowHop.displayStatus, p.attachment) from Bill b right outer join b.request p where " ;
        String countQuery = "select count(distinct p.id) from Bill b right outer join b.request p where ";
        String whereClause = "";
        Map<String, Object> params = new LinkedHashMap<>();
        SessionObject session = SessionAttributes.current();
        if(session.IS_GP) {
            if(request.account == null) {
                throw new AppRuntimeException("No account chosen to filter");
            }
            params.put("client", request.account);
            whereClause += "p.client.id = :client ";
        } else if(request.account != null) {
            throw new AppRuntimeException("You are not allowed to filter using account");
        } else {
            User user = session.getUser();
            if(user.isAllowAllDivision() && (request.subAccount == null || request.subAccount.size() == 0)) {
                params.put("client", user.getClient().getId());
                whereClause += "p.client.id = :client ";
            } else {
                Collection<ClientDivision> divisions = user.isAllowAllDivision() ? HibernateUtil.initializeProxy(user.getClient().getClientDivisions()) : user.getClientDivisions();
                List<Long> allDivisionIds = divisions.stream().map(d -> d.getId()).collect(Collectors.toList());
                if(request.subAccount != null && request.subAccount.size() > 0 && !request.subAccount.stream().allMatch(s -> allDivisionIds.contains(s))) {
                    throw new AppRuntimeException("You are not allowed to filter for this sub account");
                }
                params.put("divisions", request.subAccount != null && request.subAccount.size() > 0 ? request.subAccount : allDivisionIds);
                whereClause += "0 = (select count(*) from p.clientDivisions d where d.id not in :divisions) ";
            }
        }
        if(request.start != null) {
            Date start = DateUtils.truncate(request.start, Calendar.DAY_OF_MONTH);
            params.put("start", start);
            whereClause += "and p.createTime >= :start ";
        }
        if(request.end != null) {
            Date end = DateUtils.setMilliseconds(request.end, 999);
            params.put("end", end);
            whereClause += "and p.createTime <= :end ";
        }
        if (request.accno != null) {
            params.put("accno", request.accno);
            whereClause += "and b.accountNo = :accno ";
        }
        if (request.company != null) {
            params.put("cmpyid", request.company);
            whereClause += "and b.company.id = :cmpyid ";
        }
        if (request.category != null) {
            params.put("categoryid", request.category);
            whereClause += "and b.company.category.id = :categoryid ";
        }
        User user = session.getUser();
        if(request.my) {
            params.put("initiator", user);
            whereClause += "and p.addedBy = :initiator ";
        } else {
            if(!session.IS_GP && !user.getRole().getName().equals(SystemRoles.CLIENT_ADMIN)) {
                params.put("initiator", user);
                params.put("executor", user);
                whereClause += "and (p.addedBy = :initiator or 0 < (select count(h) from p.hops h where 0 < (select count(k) from h.possibleExecutors k where k = :executor))) ";
            }
        }
        PaginatedPaymentRequestDetail detail = new PaginatedPaymentRequestDetail();
        detail.count = (Long)((List)CrudDao.get(Bill.class).executeQuery(countQuery + whereClause, 0, -1, params)).get(0);
        detail.offset = request.offset;
        if(detail.count <= detail.offset) {
            detail.offset = (long)(Math.ceil(detail.count / (double)request.totalPerPage) - 1) * request.totalPerPage;
        }
        List<RequestDetail> records;
        if(detail.count == 0) {
            detail.offset = 0;
            records = new ArrayList<>();
        } else {
            records = (List<RequestDetail>)(List)CrudDao.get(Bill.class).executeQuery(dataQuery + whereClause + " group by p.id order by p.createTime desc", request.offset, request.totalPerPage, params);
        }
        detail.perPage = request.totalPerPage;
        detail.records = records;
        return detail;
    }

    public void downloadAsXls(Long id, OutputStream outputStream) throws IOException {
        List<Bill> bills = getPaginatedBills(id, 0, -1).records;
        ExcelWriter<BillExcelReport> excel = new ExcelWriter<>() {};
        excel.addHeader();
        excel.write(bills.stream().map(b -> new BillExcelReport(b.getClientDivision().getClient().getName(), b.getClientDivision().getName(), b.getCompany().getCategory().getName(), b.getCompany().getName(), b.getAccountNo(), b.getMfsTxnid(), b.getBillAmount() - b.getVat(), b.getVat(), b.getBillAmount(), b.getServiceCharge(), b.getBillAmount() + b.getVat(), b.getDueDate(), b.getStatus())).collect(Collectors.toList()));
        excel.flush(outputStream);
    }

    public void updatePaymentApprovers(BillPaymentApprovalRequest.BillPaymentApprovalHop firstLevelApprover, BillPaymentApprovalRequest.BillPaymentApprovalHop larstLevelApprover) {
        authService.allowUserNRolesForAction(firstLevelApprover.boundUsers, firstLevelApprover.boundRoles, Actions.APPROVE_PAYMENT);
        authService.allowUserNRolesForAction(larstLevelApprover.boundUsers, larstLevelApprover.boundRoles, Actions.INITIATE_PAYMENT);
    }

    @Transactional
    public void updatePaymentApproversHops(List<BillPaymentApprovalRequest.BillPaymentApprovalHop> hops) {
        List<WorkflowHop> currentWorkflowHops = workflowRepository.findByCodeAndClientIdOrderByOrder(WorkflowHops.WFA, SessionAttributes.current().getUser().getClient().getId());
        List<Long> newHopIds = hops.stream().map(h -> h.hop.getId()).collect(Collectors.toList());

        for(WorkflowHop currentHop : currentWorkflowHops) {
            if(!newHopIds.contains(currentHop.getId())) {
                currentHop.setCode(WorkflowHops.WFA_DIS);
                Action requiredAction = currentHop.getRequiredAction();
                currentHop.setRequiredAction(null);
                workflowRepository.save(currentHop);

                CrudDao<UserAction> userActionCrudDao = CrudDao.get(UserAction.class);
                List<UserAction> userActions = userActionCrudDao.query().eq("action.id", requiredAction.getId()).findAll();
                for(UserAction ua : userActions) {
                    userActionCrudDao.delete(ua);
                }

                CrudDao<RoleAction> roleActionCrudDao = CrudDao.get(RoleAction.class);
                List<RoleAction> roleActions = roleActionCrudDao.query().eq("action.id", requiredAction.getId()).findAll();
                for(RoleAction ra : roleActions) {
                    roleActionCrudDao.delete(ra);
                }

                actionRepository.delete(requiredAction);
            }
        }

        Long clientId = SessionAttributes.current().getUser().getClient().getId();
        for(int i = 0; i < hops.size(); i++) {
            BillPaymentApprovalRequest.BillPaymentApprovalHop hop = hops.get(i);
            WorkflowHop hopToUpdate = hop.hop;
            if(hopToUpdate.getId() == 0) {
                String actionName = hopToUpdate.getRequiredAction().getName();
                Action newAction = actionRepository.findByName(actionName);
                if(newAction != null) {
                    throw new AppRuntimeException(newAction.getName() + " already exists");
                }
                newAction = new Action();
                newAction.setName(hopToUpdate.getRequiredAction().getName());
                newAction.setClientId(clientId);
                actionRepository.save(newAction);

                WorkflowHop newCreateHop = new WorkflowHop();
                newCreateHop.setCode(WorkflowHops.WFA);
                newCreateHop.setDisplayStatus(hopToUpdate.getDisplayStatus());
                newCreateHop.setOrder(i + 1);
                newCreateHop.setClientId(clientId);
                newCreateHop.setRequiredAction(newAction);
                workflowRepository.save(newCreateHop);
            } else {
                WorkflowHop existingHop = workflowRepository.findById(hopToUpdate.getId()).get();
                existingHop.setDisplayStatus(hopToUpdate.getDisplayStatus());
                existingHop.getRequiredAction().setName(hopToUpdate.getRequiredAction().getName());
                actionRepository.save(existingHop.getRequiredAction());
                existingHop.setOrder(i + 1);
                workflowRepository.save(existingHop);
            }
            authService.allowUserNRolesForAction(hop.boundUsers, hop.boundRoles, hop.hop.getRequiredAction().getName());
        }
    }
	
	public void downloadPaymentRequestFile(Long id, ServletOutputStream outputStream) throws IOException {
		CrudDao<PaymentRequest> paymentRequestCrudDao = CrudDao.get(PaymentRequest.class);
		PaymentRequest pRequest = paymentRequestCrudDao.findOne(id);
		String fileName = pRequest.getAttachment();
		Path path = Paths.get(paymentRequestAttachmentLocation + fileName);
		outputStream.write(Files.readAllBytes(path));
		outputStream.flush();
	}

	public List<Bill> getPaybleBillsForRequest(long requestId) {
		List<BillStatus> payableStatus = new ArrayList<>();
        payableStatus.add(BillStatus.Unpaid);
        payableStatus.add(BillStatus.Fail);
		WhereBuilder<Bill, ?> query = CrudDao.get(Bill.class).query().eq("request.id", requestId).in("status", payableStatus);
		List<Bill> bills = query.findAll();
		return bills;
	}
}
