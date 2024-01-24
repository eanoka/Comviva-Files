package com.grameenphone.wipro.fmfs.cbp.service;

import static com.grameenphone.wipro.fmfs.cbp.service.AccountService.BILLDATA_VALIDATION_CONFIG_KEY;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.enums.BillStatus;
import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillData;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillDetailTask;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillRevertibleCache;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BilldataAdditionalField;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Category;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientConfig;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Company;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.CompanyAdditionalFields;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.WorkflowHop;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BulkBillDataExcelProcessReport;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BulkBillDataExcelRow;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.FilterBillRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedBill;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillDataRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillDetailTaskRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillRevertibleCacheRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BilldataAdditionalFieldRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.ClientConfigRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.PaymentRequestRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserRepository;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.common.BillDataStatus;
import com.grameenphone.wipro.utility.common.Event;
import com.grameenphone.wipro.utility.common.MacroReplacer;
import com.grameenphone.wipro.utility.common.MapUtil;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.excel.ExcelWriter;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.orm.HibernateUtil;
import com.grameenphone.wipro.utility.orm.WhereBuilder;

import io.github.millij.poi.SpreadsheetReadException;
import io.github.millij.poi.ss.reader.XlsxReader;

@Service
public class BillDataService {
    private final static Logger logger = LoggerFactory.getLogger(BillDataService.class);

    @Autowired
    BillDetailTaskRepository billDetailTaskRepository;

    @Autowired
    BillDataRepository billDataRepository;

    @Autowired
    BillRepository billRepository;
    
    @Autowired
    BillRevertibleCacheRepository billCacheRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    PaymentRequestRepository paymentRequestRepository;

    @Autowired
    ClientConfigRepository clientConfigRepository;

    @Autowired
    BilldataAdditionalFieldRepository billdataAdditionalFieldRepository;

    @Value("${temp.upload.dump.dir}")
    String dumpFileLocation;

    @Value("${email.template.billdata.modification.notification}")
    String billdataModificationNotificationTemplate;

    @Value("${email.template.billdata.modification.subject}")
    String billdataModificationNotificationSubject;

    @Value("${email.template.billdata.approve.notification}")
    String billdataApproveNotificationTemplate;

    @Value("${email.template.billdata.approve.subject}")
    String billdataApproveNotificationSubject;

    public List<Category> getAllCategories() {
        return CrudDao.get(Category.class).findAll();
    }

    @Transactional
    public void createBillDetailTask(long[] subAccountIds, long category, long company, String consumerId) {
        BillDetailTask billDetailTask = new BillDetailTask();
        billDetailTask.setAccountNo(consumerId);
        User currentUser = SessionAttributes.current().getUser();
        if (subAccountIds != null && subAccountIds.length != 0) {
            for (long subAccountId : subAccountIds) {
                billDetailTask.addClientDivisions(CrudDao.get(ClientDivision.class).proxy(subAccountId));
            }
        } else if (!currentUser.isAllowAllDivision()) {
            for (ClientDivision clientDivision : currentUser.getClientDivisions()) {
                billDetailTask.addClientDivisions(clientDivision);
            }
        }
        billDetailTask.setAddedBy(currentUser);
        if (company != 0) {
            billDetailTask.setCompany(CrudDao.get(Company.class).proxy(company));
        }
        if (category != 0) {
            billDetailTask.setCategory(CrudDao.get(Category.class).proxy(category));
        }
        Client underlyingClient = currentUser.getClient();
        billDetailTask.setClient(underlyingClient);
        billDetailTaskRepository.save(billDetailTask);
    }

    @Transactional
    public void createBillData(long id, long subAccountId, long companyId, String consumerId, long mobileNo, Map<String, Object> additionalParam, String alias) {
        boolean isNew;

        BillData billData = CrudDao.get(BillData.class).query().eq("accountNo", consumerId).eq("company.id", companyId).ne("id", id).findOne();
        if (billData != null) {
            throw new AppRuntimeException("Bill data already exists");
        }
        if (id != 0) {
            billData = CrudDao.get(BillData.class).findOne(id);
            if (billData == null) {
                throw new AppRuntimeException("Bill data not found");
            }
            isNew = false;
        } else {
            billData = new BillData();
            isNew = true;
        }

        User user = SessionAttributes.current().getUser();
        Client client = user.getClient();
        ClientConfig clientConfig = clientConfigRepository.findByClientIdAndKey(client.getId(), BILLDATA_VALIDATION_CONFIG_KEY);

        boolean isValidationEnabled = clientConfig != null && clientConfig.getValue().equals("true");
        if (isValidationEnabled) {
            if (isNew) {
                billData.setStatus(BillDataStatus.PENDING_FOR_CREATION);
            } else {
                BillData tempBillData = billData;
                billData = new BillData();
                billData.setStatus(BillDataStatus.PENDING_FOR_MODIFICATION);
                billData.setModifiedDataFor(tempBillData);
            }
        } else {
            billData.setStatus(BillDataStatus.VALIDATED);
            billData.setValidatedById(user.getId());
        }

        ClientDivision division = CrudDao.get(ClientDivision.class).query().eq("id", subAccountId).eq("client", client).findOne();
        if (division == null) {
            throw new AppRuntimeException("Invalid sub account");
        }
        Collection<ClientDivision> divisions = getAllowedDivisions(user);
        if (!divisions.contains(division)) {
            throw new AppRuntimeException("You have no permission to add bill for this Sub Account");
        }
        String msisdn = StringUtil.sanitizeMsisdn("" + mobileNo);
        if (msisdn == null) {
            throw new AppRuntimeException("Invalid MSISDN Given");
        }
        Company company = CrudDao.get(Company.class).proxy(companyId);
        boolean isOtherThanMsisdnUpdate = false;
        if(!isNew && !isValidationEnabled) {
        	isOtherThanMsisdnUpdate = isUpdateInOtherThanMsisdn(billData, division, company, consumerId);
        }
        billData.setAccountNo(consumerId);
        billData.setClientDivision(CrudDao.get(ClientDivision.class).proxy(subAccountId));
        billData.setAddedBy(user);
        billData.setUpdatedBy(user);
        billData.setCompany(company);
        billData.setMsisdn(Integer.parseInt(msisdn));
        BillData savedBillData = billDataRepository.save(billData);
        if(alias == null) {
        	alias = "BD" + String.format("%08x", billData.getId()).toUpperCase();        	
        }
        billData.setAlias(alias);
        if (isValidationEnabled) {
            sendModificationNotification(user, isNew ? "create" : "update", billData.getClientDivision());
        } else {
            if (id != 0 && isOtherThanMsisdnUpdate) {
                deleteAssociatedBillsConsideringConstraint(id);
            }
        }
        if(additionalParam != null && !additionalParam.isEmpty() && savedBillData != null)
        {
        	saveBillDataAdditionalField(additionalParam, savedBillData, companyId);
        }
    }
    
    public void saveBillDataAdditionalField(Map<String, Object> additionalParam, BillData savedBillData, long companyId)
    {
    	for(Map.Entry<String, Object> entry : additionalParam.entrySet())
    	{
    		BilldataAdditionalField field = new BilldataAdditionalField();
    		field.setBillData(savedBillData);
    		CompanyAdditionalFields additionalField = CrudDao.get(CompanyAdditionalFields.class).query().eq("paramCode", entry.getKey()).eq("company.id", companyId).findOne();
    		field.setFields(additionalField);
    		field.setValue(String.valueOf(entry.getValue()));
    		billdataAdditionalFieldRepository.save(field);
    	}
    }

    public Collection<ClientDivision> getAllowedDivisions(User user) {
        user = CrudDao.get(User.class).proxy(user.getId());
        if (user.isAllowAllDivision()) {
            return user.getClient().getClientDivisions();
        }
        return user.getClientDivisions();
    }

    @Transactional
    public int delete(long[] billDataIds) {
        int deleteCount = 0;
        User user = SessionAttributes.current().getUser();
        Client client = user.getClient();
        ClientConfig clientConfig = clientConfigRepository.findByClientIdAndKey(client.getId(), BILLDATA_VALIDATION_CONFIG_KEY);

        if (clientConfig != null && clientConfig.getValue().equals("true")) {
            Set<ClientDivision> clientDivisions = new HashSet<>();
            for (long id : billDataIds) {
                try {
                    BillData billData = billDataRepository.findById(id).get();
                    billData.setStatus(BillDataStatus.PENDING_FOR_REMOVAL);
                    clientDivisions.add(billData.getClientDivision());
                    billDataRepository.save(billData);
                    deleteCount++;
                } catch (Throwable h) {
                }
            }
            sendModificationNotification(user, "delete", clientDivisions.toArray(ClientDivision[]::new));
        } else {
            for (long id : billDataIds) {
                try {
                    try {
                    	List<BilldataAdditionalField> fieldList = CrudDao.get(BilldataAdditionalField.class).query().eq("billData", id).findAll();
                    	for(BilldataAdditionalField field : fieldList)
                    	{
                    		billdataAdditionalFieldRepository.deleteById(field.getId());
                    	}
                        deleteAssociatedBillsConsideringConstraint(id);
                    } catch(AppRuntimeException d) {
                        continue;
                    }
                    billDataRepository.deleteById(id);
                    deleteCount++;
                } catch (Throwable h) {
                }
            }
        }
        return deleteCount;
    }

    @Transactional
    public int rejectBills(long[] billDataIds, String comment) {
        int deleteCount = 0;
        Set<User> modifiers = new HashSet<>();
        for (long id : billDataIds) {
            try {
                BillData billData = billDataRepository.findById(id).get();
                User updater = billData.getUpdatedBy();
                if (billData.getStatus().equals(BillDataStatus.PENDING_FOR_CREATION)) {
                    billDataRepository.deleteById(id);
                    deleteCount++;
                } else if (billData.getStatus().equals(BillDataStatus.PENDING_FOR_MODIFICATION)) {
                    billDataRepository.deleteById(id);
                    deleteCount++;
                } else if (billData.getStatus().equals(BillDataStatus.PENDING_FOR_REMOVAL)) {
                    billData.setStatus(BillDataStatus.VALIDATED);
                    billDataRepository.save(billData);
                    deleteCount++;
                }
                modifiers.add(updater);
            } catch (Throwable h) {
                logger.error("Couldn't update bill data on reject", h);
            }
        }
        sendApprovedNotification(SessionAttributes.current().getUser(), modifiers, "reject", comment);
        return deleteCount;
    }

    @Transactional
    public int approveBills(long[] billDataIds, String comment) {
    	User user = SessionAttributes.current().getUser();
        int updateCount = 0;
        Set<User> modifiers = new HashSet<>();
        for (long id : billDataIds) {
            try {
                BillData billData = billDataRepository.findById(id).get();
                User updater = billData.getUpdatedBy();
                if (billData.getStatus().equals(BillDataStatus.PENDING_FOR_CREATION)) {
                    billData.setStatus(BillDataStatus.VALIDATED);
                    billData.setValidatedById(user.getId());
                    billDataRepository.save(billData);
                    updateCount++;
                } else if (billData.getStatus().equals(BillDataStatus.PENDING_FOR_MODIFICATION)) {
                    BillData billDataRecord = billData.getModifiedDataFor();
					try {
						if (isUpdateInOtherThanMsisdn(billDataRecord, billData.getClientDivision(), billData.getCompany(), billData.getAccountNo())) {
							deleteAssociatedBillsConsideringConstraint(billDataRecord.getId());
						}
					} catch (Exception t) {
						continue;
					}
                    billDataRecord.setAccountNo(billData.getAccountNo());
                    billDataRecord.setCompany(billData.getCompany());
                    billDataRecord.setClientDivision(billData.getClientDivision());
                    billDataRecord.setMsisdn(billData.getMsisdn());
                    billDataRecord.setUpdatedBy(billData.getUpdatedBy());
                    billDataRecord.setValidatedById(user.getId());
                    billDataRepository.deleteById(id);
                    billDataRepository.save(billDataRecord);
                    updateCount++;
                } else if (billData.getStatus().equals(BillDataStatus.PENDING_FOR_REMOVAL)) {
                    try {
                    	List<BilldataAdditionalField> fieldList = CrudDao.get(BilldataAdditionalField.class).query().eq("billData", id).findAll();
                    	for(BilldataAdditionalField field : fieldList)
                    	{
                    		billdataAdditionalFieldRepository.deleteById(field.getId());
                    	}
                        deleteAssociatedBillsConsideringConstraint(id);
                    } catch (Exception t) {
                        continue;
                    }
                    billDataRepository.deleteById(id);
                    updateCount++;
                }
                modifiers.add(updater);
            } catch (Throwable h) {
                logger.error("Couldn't update bill data on approve", h);
            }
        }
        sendApprovedNotification(SessionAttributes.current().getUser(), modifiers, "approve", comment);
        return updateCount;
    }

    /**
     * xlsx file must have headers in first line.
     *
     * @param commentFile must be xlsx file
     * @return service report
     */
    public String uploadBulkData(File commentFile, HttpSession session) {
        User user = SessionAttributes.current().getUser();
        Client client = user.getClient();
        Collection<ClientDivision> divisions = getAllowedDivisions(user);
        try {
            List<BulkBillDataExcelRow> rows = new XlsxReader().read(BulkBillDataExcelRow.class, commentFile, 0);
            //{Total Count}
            int[] index = new int[]{1};
            //{Add Count, Error Count, Update Count}
            int[] counts = new int[]{0, 0, 0};
            List<BulkBillDataExcelProcessReport> errors = new ArrayList<>();
            ClientConfig clientConfig = clientConfigRepository.findByClientIdAndKey(client.getId(), BILLDATA_VALIDATION_CONFIG_KEY);
            boolean isValidationEnabled = clientConfig != null && "true".equals(clientConfig.getValue());
            Set<ClientDivision> billDataDivisions = new HashSet<>();
            rows.forEach(row -> {
                BulkBillDataExcelProcessReport errorReport = new BulkBillDataExcelProcessReport();
                errorReport.reference = index[0];
                if (StringUtil.isNullOrEmpty(row.getSubAccountName())) {
                    errorReport.error = "No Sub Account Given";
                } else {
                    ClientDivision division = CrudDao.get(ClientDivision.class).query().eq("name", row.getSubAccountName()).eq("client", client).findOne();
                    if (division == null) {
                        errorReport.error = "Invalid Sub Account Given";
                    } else {
                        if (!divisions.contains(division)) {
                            errorReport.error = "You have no permission to add bill for this Sub Account";
                        } else {
                            if (StringUtil.isNullOrEmpty(row.getCategoryName())) {
                                errorReport.error = "No Category Given";
                            } else {
                                Category category = CrudDao.get(Category.class).query().eq("name", row.getCategoryName()).findOne();
                                if (category == null) {
                                    errorReport.error = "Invalid Category Given";
                                } else {
                                    if (StringUtil.isNullOrEmpty(row.getCompanyName())) {
                                        errorReport.error = "No Company Given";
                                    } else {
                                        Optional<Company> optionalCompany = category.getCompanies().stream().filter(c -> c.getName().equals(row.getCompanyName())).findFirst();
                                        if (!optionalCompany.isPresent()) {
                                            errorReport.error = "Invalid Company Given";
                                        } else {
                                            Company company = optionalCompany.get();
                                            if (StringUtil.isNullOrEmpty(row.getAccountNo())) {
                                                errorReport.error = "No Meter/Account No Given";
                                            } else {
                                                boolean isNew;
                                                boolean isOtherThanMsisdnUpdate = false;
                                                BillData billData = CrudDao.get(BillData.class).query().eq("accountNo", row.getAccountNo()).eq("company", company).findOne();
                                                if (billData == null) {
                                                    isNew = true;
                                                    billData = new BillData();
                                                    billData.setAccountNo(row.getAccountNo());
                                                    billData.setCompany(company);
                                                    billData.setAddedBy(user);
                                                } else {
                                                    isNew = false;
                                                    if(!isValidationEnabled) {
                                                        isOtherThanMsisdnUpdate = isUpdateInOtherThanMsisdn(billData, division, company, row.getAccountNo());
                                                    }
                                                }

                                                if (isValidationEnabled) {
                                                    if (isNew) {
                                                        billData.setStatus(BillDataStatus.PENDING_FOR_CREATION);
                                                    } else {
                                                        BillData tempBillData = billData;
                                                        billData = new BillData();
                                                        billData.setStatus(BillDataStatus.PENDING_FOR_MODIFICATION);
                                                        billData.setUpdatedBy(user);
                                                        billData.setModifiedDataFor(tempBillData);
                                                    }
                                                } else {
                                                    billData.setStatus(BillDataStatus.VALIDATED);
                                                }

                                                if (StringUtil.isNullOrEmpty(row.getMsisdn())) {
                                                    errorReport.error = "No MSISDN Given";
                                                } else {
                                                    String msisdn = StringUtil.sanitizeMsisdn(row.getMsisdn());
                                                    if (msisdn == null) {
                                                        errorReport.error = "Invalid MSISDN Given";
                                                    } else {
                                                        billData.setClientDivision(division);
                                                        billData.setUpdatedBy(user);
                                                        billData.setMsisdn(Integer.parseInt(msisdn));
                                                        if(row.getName() != null)
                                                        {
                                                        	billData.setAlias(row.getName());
                                                        }
                                                        try {
                                                            if (isNew) {
                                                            	BillData savedBillData = billDataRepository.save(billData);
                                                            	billData.setAlias("BD" + String.format("%08x", savedBillData.getId()).toUpperCase());
                                                            	billDataRepository.save(savedBillData);
                                                            	counts[0]++;
															} else {
																try {
                                                                    if (!isNew && !isValidationEnabled && isOtherThanMsisdnUpdate) {
                                                                        deleteAssociatedBillsConsideringConstraint(billData.getId());
                                                                    }
                                                                    BillData savedBillData = billDataRepository.save(billData);
                                                                    billData.setAlias("BD" + String.format("%08x", savedBillData.getId()).toUpperCase());
                                                                	billDataRepository.save(savedBillData);
                                                                    counts[2]++;
																} catch (AppRuntimeException e) {
																	errorReport.error = "There is at least one bill which is in Approval state";
																}
															}
                                                            billDataDivisions.add(division);
                                                        } catch (Throwable t) {
                                                            logger.error("Couldn't persist bill data record", t);
                                                            errorReport.error = "Couldn't persist bill data record on " + System.currentTimeMillis();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (errorReport.error != null) {
                    counts[1]++;
                    errorReport.subAccountName = row.getSubAccountName();
                    errorReport.categoryName = row.getCategoryName();
                    errorReport.companyName = row.getCompanyName();
                    errorReport.accountNo = row.getAccountNo();
                    errorReport.msisdn = row.getMsisdn();
                    errors.add(errorReport);
                }
                index[0]++;
            });
            if (errors.size() > 0) {
                File dumpFile = new File(dumpFileLocation, "ERROR_BILL_DATA_" + System.currentTimeMillis() + ".xlsx");
                try {
                    ExcelWriter<BulkBillDataExcelProcessReport> excel = new ExcelWriter<>() {};
                    excel.addHeader();
                    excel.write(errors);
                    dumpFile.createNewFile();
                    try (FileOutputStream errorFile = new FileOutputStream(dumpFile)) {
                        excel.flush(errorFile);
                    }
                } catch (Throwable t) {
                    logger.error("Couldn't write error file", t);
                }
                session.setAttribute("SESSION_TEMP_DOWNLOAD: bill_data_upload_error", dumpFile);
                Event.after(5 * 60 * 1000, "ticket_comment_upload_error", () -> {
                    session.removeAttribute("SESSION_TEMP_DOWNLOAD: bill_data_upload_error");
                });
            }
            if(isValidationEnabled) {
                sendModificationNotification(user, "upload", billDataDivisions.toArray(ClientDivision[]::new));
            }
            return "Added: " + counts[0] + "; Updated: " + counts[2] + "; Failed: " + counts[1];
        } catch (SpreadsheetReadException e) {
            logger.error("Couldn't process bulk bill data file", e);
            throw new AppRuntimeException("File couldn't be processed", 5001);
        }
    }
    
    private void deleteAssociatedBillsConsideringConstraint(long billDataId) {
        List<BillStatus> payableStatus = new ArrayList<>();
        payableStatus.add(BillStatus.Unpaid);
        payableStatus.add(BillStatus.Fail);
        List<Bill> attachedBills = CrudDao.get(Bill.class).query().eq("billData.id", billDataId).findAll();
        attachedBills.forEach(u -> {
            if (u.getStatus().equals(BillStatus.Success)) {
                try {
                    saveAdditionalFieldsInBillRevertibleCache(billDataId, u);
                } catch (IOException e) {
                    logger.debug("Exception while adding details in bill_revertible_cache: " + e);
                }
                try {
                    saveAdditionalFieldsInBillRevertibleCache(billDataId, u);
                } catch (IOException e) {
                    logger.debug("Exception while adding details in bill_revertible_cache: " + e);
                }
                u.setBillData(null);
                billRepository.save(u);
            } else if (u.getRequest() == null) {
                billCacheRepository.deleteByBill(u);
                billRepository.delete(u);
            } else {
                WorkflowHop hop = u.getRequest().getLastHop().getWorkflowHop();
                if (hop.getCode().equals(WorkflowHops.CPL) || hop.getCode().equals(WorkflowHops.REJ)) {
                    billCacheRepository.deleteByBill(u);
                    billRepository.delete(u);
                } else {
                    logger.debug("Prevent billdata modification due there is at least one bill which is in Active PaymentRequest for billdata id :" + billDataId);
                    throw new AppRuntimeException("There is at least one bill which is in Approval state for payment for billdata id :" + billDataId);
                }
            }
        });
    }
    
    public void saveAdditionalFieldsInBillRevertibleCache(long billDataId, Bill bill) throws IOException
	{
		List<BilldataAdditionalField> findBillDataAdditionalFields = CrudDao.get(BilldataAdditionalField.class).query().eq("billData.id", billDataId).findAll();
		BillRevertibleCache cache = CrudDao.get(BillRevertibleCache.class).query().eq("bill.id", bill.getId()).findOne();
		Map savable = new HashMap();
    	if(findBillDataAdditionalFields != null)
    	{
    		if(cache != null)
    		{
    			if(cache.getValuesAsJson() != null)
    			{
    				savable = Json.fromJson(cache.getValuesAsJson(), Map.class);
    				for(BilldataAdditionalField field : findBillDataAdditionalFields)
            		{
            			CompanyAdditionalFields cmpyFieldDetail = CrudDao.get(CompanyAdditionalFields.class).query().eq("bill.id", field.getId()).findOne();
            			savable.put(cmpyFieldDetail.getParamCode(), field.getValue());
            		}
    			}
    		}
    		else
    		{
    			cache = new BillRevertibleCache();
        		for(BilldataAdditionalField field : findBillDataAdditionalFields)
        		{
        			CompanyAdditionalFields cmpyFieldDetail = CrudDao.get(CompanyAdditionalFields.class).query().eq("bill.id", field.getId()).findOne();
        			savable.put(cmpyFieldDetail.getParamCode(), field.getValue());
        		}
    		}
    		cache.setBill(bill);
    		try 
    		{
				cache.setValuesAsJson(Json.toJson(savable));
			} 
    		catch (JsonProcessingException e) {
				logger.debug("Exception occurred:", e);
			}
    		billCacheRepository.save(cache);
    	}
    }

    public long getSelectableBillCount(long[] subAccountIds, long category, long company, String consumerId) {
        User currentUser = SessionAttributes.current().getUser();
        Collection<ClientDivision> allowedDivisions = getAllowedDivisions(currentUser);
        return CrudDao.get(BillData.class).query().inif(() -> subAccountIds != null && subAccountIds.length > 0, "clientDivision.id", () -> Arrays.stream(subAccountIds).mapToObj(x -> x).collect(Collectors.toList())).inif(() -> subAccountIds == null || subAccountIds.length == 0, "clientDivision", allowedDivisions).eqif(() -> company != 0, "company.id", company).eqif(() -> company == 0 && category != 0, "company.category.id", category).eqif(() -> consumerId != null, "accountNo", consumerId).count();
    }

    public PaginatedBill getFilteredBills(FilterBillRequest request) {
        PaginatedBill bills = new PaginatedBill();
        WhereBuilder<Bill, ?> query = CrudDao.get(Bill.class).query()
                .eqif(() -> request.accno != null, "accountNo", request.accno)
                .eqif(() -> request.company != null, "company.id", request.company)
                .eqif(() -> request.company == null && request.category != null, "company.category.id", request.category);
        if (SessionAttributes.current().IS_GP) {
            if (request.account == null) {
                throw new AppRuntimeException("No account chosen to filter");
            }
            query.eq("clientDivision.client.id", request.account);
        } else if (request.account != null) {
            throw new AppRuntimeException("You are not allowed to filter using account");
        } else {
            User user = SessionAttributes.current().getUser();
            if (user.isAllowAllDivision() && (request.subAccount == null || request.subAccount.size() == 0)) {
                query.eq("clientDivision.client.id", user.getClient().getId());
            } else {
                Collection<ClientDivision> divisions = user.isAllowAllDivision() ? HibernateUtil.initializeProxy(user.getClient().getClientDivisions()) : user.getClientDivisions();
                List<Long> allDivisionIds = divisions.stream().map(d -> d.getId()).collect(Collectors.toList());
                if (request.subAccount != null && request.subAccount.size() > 0 && !request.subAccount.stream().allMatch(s -> allDivisionIds.contains(s))) {
                    throw new AppRuntimeException("You are not allowed to filter for this sub account");
                }
                query.in("clientDivision.id", request.subAccount != null && request.subAccount.size() > 0 ? request.subAccount : allDivisionIds);
            }
        }
        bills.count = query.count();
        if (bills.count <= request.offset) {
            request.offset = (long) (Math.ceil(bills.count / (double) request.totalPerPage) - 1) * request.totalPerPage;
        }
        List<Bill> pendingBills;
        if (bills.count == 0) {
            request.offset = 0;
            pendingBills = new ArrayList<>();
        } else {
            pendingBills = query.findAll(request.offset, request.totalPerPage);
        }
        bills.offset = request.offset;
        bills.perPage = request.totalPerPage;
        bills.records = pendingBills;
        return bills;
    }

    public void sendModificationNotification(User modifier, String action, ClientDivision... divisions) {
        try {
            Collection<User> users = userRepository.getBilldataValidators(Arrays.asList(divisions));
            String modifiedTemplate = MacroReplacer.replaceMacros(billdataModificationNotificationTemplate, Map.of("modifier", modifier, "action", action));
            notificationService.sendEmail(billdataModificationNotificationSubject, modifiedTemplate, users.stream().map(u -> u.getEmail()).collect(Collectors.joining(",")), modifier.getEmail(), true);
        } catch (Throwable e) {
            logger.error("Couldn't send email to validators", e);
        }
    }

    public void sendApprovedNotification(User validator, Set<User> notifyee, String action, String comment) {
        try {
            Map bindingMap = MapUtil.of(new KV<>("validator", validator), new KV<>("action", action), new KV<>("comment", comment)) ;
            String modifiedTemplate = MacroReplacer.replaceMacros(billdataApproveNotificationTemplate, bindingMap);
            String modifiedSubject = MacroReplacer.replaceMacros(billdataApproveNotificationSubject, bindingMap);
            notificationService.sendEmail(modifiedSubject, modifiedTemplate, notifyee.stream().map(u -> u.getEmail()).collect(Collectors.joining(",")), null, true);
        } catch (Throwable e) {
            logger.error("Couldn't send email to initiantor", e);
        }
    }
    
    private boolean isUpdateInOtherThanMsisdn(BillData oldBillData, ClientDivision clientDivision, Company company, String consumerId) {
        boolean isOtherThanMsisdnUpdate = false;
        if (!oldBillData.getAccountNo().equals(consumerId) || !oldBillData.getClientDivision().equals(clientDivision) || !oldBillData.getCompany().equals(company)) {
            isOtherThanMsisdnUpdate = true;
        }
        return isOtherThanMsisdnUpdate;
	}
}
