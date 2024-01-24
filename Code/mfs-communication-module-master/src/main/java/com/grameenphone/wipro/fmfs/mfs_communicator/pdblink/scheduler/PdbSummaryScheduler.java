package com.grameenphone.wipro.fmfs.mfs_communicator.pdblink.scheduler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.grameenphone.wipro.annot.SchedulerContainingRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.ManualSchedulerResolver;
import com.grameenphone.wipro.utility.common.SequentialCsvParser;
import com.grameenphone.wipro.utility.common.SmsUtil;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.jdbc.NamedParameterStatement;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.opencsv.CSVWriter;

@Component
public class PdbSummaryScheduler implements ManualSchedulerResolver{
	protected final static Logger log = LoggerFactory.getLogger(PdbSummaryScheduler.class);

	@Value("${pdb.sftp.user}")
	String sftpUser;

	@Value("${pdb.sftp.host}")
	String sftpHost;

	@Value("${pdb.sftp.port}")
	int sftpPort;

	@Value("${pdb.sftp.password}")
	String sftpPassword;

	@Value("${pdb.sftp.pull.location}")
	String fileReadLocation;

	@Value("${pdb.sftp.push.location}")
	String fileDropLocation;

	@Value("${pdb.summarydb.connection.url}")
	String connUrl;

	@Value("${pdb.summarydb.username}")
	String summaryDBUsername;

	@Value("${pdb.summarydb.password}")
	String summaryDBPassword;
	
	private static volatile boolean isBillDataLoadSchedulerRunning = false;
	private static volatile boolean isCustomerDataLoadSchedulerRunning = false;

	@Value("${pdb.summarydb.error.file.dump.location}")
	String errorFileLocation;

	@Value("${pdb.summarydb.smsTo}")
	String smsToSend;
	
	private static final int BILL_DATA_BATCH_SIZE = 20000;
	private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm";

	public void pushPaidDataTopPDBSft() {
		new Thread(() -> {
			log.debug("Starting pdb push paid billdata/customer data report scheduler");

			List<PDBPaidMasterData> masterDataList = null;
			List<PDBPaidBillData> billDataList = null;

			String suffix = formattedCurrentDateTime();
			String masterDatafileName = "PDB_Master_Data_" + suffix + ".csv";
			String billDetailfileName = "PDB_Detail_Data_" + suffix + ".csv";

			try {
				masterDataList = getPaidMasterDataList();
				if (masterDataList.size() != 0) {
					pushToSftpLocation(masterDatafileName, createMasterDataCsv(masterDataList), fileDropLocation);
				}  else {
					log.debug("No master detail records exists.");
				}
			} catch (Exception e) {
				log.error("Exception in paid customermaster data report scheduler:", e);
			}

			try {
				billDataList = getPaidBillDataList();
				if (billDataList.size() != 0) {
					pushToSftpLocation(billDetailfileName, createBillDataCsv(billDataList), fileDropLocation);
				} else {
					log.debug("No bill detail records exists.");
				}
			} catch (Exception e) {
				log.error("Exception in paid bill data report scheduler", e);
			}
			log.debug("Completed pdb push paid billdata/customer data report scheduler");
		}, "PUSH-PAYMENTDATA-SCHEDULER").start();
	}

	private List<PDBPaidBillData> getPaidBillDataList() throws ClassNotFoundException, SQLException {
		Connection con = null;
		Statement stmt = null;
		String query = "select BATCH_NO,BILL_NUMBER,BATCH_CODE,SCROLL_NO,TRANSACTION_ID,PDB_AMOUNT,GOVT_DUTY,STATUS,CREATED_ON,CREATED_BY,MODIFIED_NO,MODIFIED_BY from payment_dtl where batch_no in (select batch_no from payment_mst where pay_date=trunc(sysdate -1))";
		
		List<PDBPaidBillData> list = new ArrayList<>();
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			con = DriverManager.getConnection(connUrl, summaryDBUsername, summaryDBPassword);
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				PDBPaidBillData data = new PDBPaidBillData();
				data.BATCH_NO = rs.getString(1);
				data.BILL_NUMBER = rs.getString(2);
				data.BATCH_CODE = rs.getString(3);
				data.SCROLL_NO = rs.getString(4);
				data.TRANSACTION_ID = rs.getString(5);
				data.PDB_AMOUNT = rs.getString(6);
				data.GOVT_DUTY = rs.getString(7);
				data.STATUS = rs.getString(8);
				data.CREATED_ON = rs.getString(9);
				data.CREATED_BY = rs.getString(10);
				data.MODIFIED_NO = rs.getString(11);
				data.MODIFIED_BY = rs.getString(12);
				list.add(data);
			}
		} catch (SQLException ex) {
			log.error("Exception occured while extracting the paid billdata", ex);
		} catch (Exception ex) {
			log.error("Exception preparing the paid billdata content list", ex);
		} finally {
			stmt.close();
			con.close();
		}
		return list;
	}

	private List<PDBPaidMasterData> getPaidMasterDataList() throws ClassNotFoundException, SQLException {
		Connection con = null;
		Statement stmt = null;
		String query = "select BATCH_NO,LOCATION_CODE,PAY_DATE,TOTAL_PDB_AMOUNT,REVENUE_STAMP_AMOUNT,NET_PDB_AMOUNT,TOTAL_GOVT_DUTY,STATUS,CREATED_ON,CREATED_BY,MODIFIED_ON,MODIFIED_BY from payment_mst where pay_date=trunc(sysdate -1)";
		
		List<PDBPaidMasterData> list = new ArrayList<>();
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			con = DriverManager.getConnection(connUrl, summaryDBUsername, summaryDBPassword);
			stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while (rs.next()) {
				PDBPaidMasterData data = new PDBPaidMasterData();
				data.BATCH_NO = rs.getString(1);
				data.LOCATION_CODE = rs.getString(2);
				data.PAY_DATE = rs.getString(3);
				data.TOTAL_PDB_AMOUNT = rs.getString(4);
				data.REVENUE_STAMP_AMOUNT = rs.getString(5);
				data.NET_PDB_AMOUNT = rs.getString(6);
				data.TOTAL_GOVT_DUTY = rs.getString(7);
				data.STATUS = rs.getString(8);
				data.CREATED_ON = rs.getString(9);
				data.CREATED_BY = rs.getString(10);
				data.MODIFIED_ON = rs.getString(11);
				data.MODIFIED_BY = rs.getString(12);
				list.add(data);
			}
		} catch (SQLException ex) {
			log.error("Exception occured while extracting the paid customer master data", ex);
		} catch (Exception ex) {
			log.error("Exception preparing the paid customer master data content list", ex);
		} finally {
			stmt.close();
			con.close();
		}
		return list;
	}

	@SchedulerContainingRequest(name="PDB Pull BillData Schedular", value="pdbSummaryScheduler_pullBillDataFromCSVandLoad")
	public void pullBillDataFromCSVandLoad() {
		if (isBillDataLoadSchedulerRunning) {
			log.debug("There is already an instance is running for PDB summarydb BillData processing.");
			return;
		}
		isBillDataLoadSchedulerRunning = true;
		new Thread(() -> {
			log.debug("PDB summarydb BillData table loading scheduler start");
			try {
				String billDatafileNamePrefix = "PDB_Bill_Data_";

				List<String> billDataFileNames = pullFilesForProcessing(fileReadLocation, billDatafileNamePrefix);
				if (billDataFileNames.size() == 0) {
					log.debug("No file to process for bill data");
					return;
				}
				for (String billDataFileName : billDataFileNames) {
					int retryCount = 0;
					String billContent = null;
					while (retryCount < 5) {
						try {
							log.debug("Reading bill file: " + billDataFileName);
							billContent = new String(readFileContentFromSftpLocation(billDataFileName));
							break;
						} catch (Exception e) {
							if (++retryCount == 5) {
								log.error("Bill file reading error (" + billDataFileName + "): ", e);
								sendSmsNotification("PDB Summary BillData Scheduler: Failed to read the file: " + billDataFileName + " from PDB SFT server.");
							}
						}
					}
					if (billContent != null) {
						log.debug("Processing bill file: " + billDataFileName);
						insertBillDataList(billContent);
						moveProcessedFileToArchive(billDataFileName, fileReadLocation);
					} else {
						log.debug("Unable to read file content: " + billDataFileName);
					}
				}
			} catch (Exception ex) {
				log.error("Exception in pullBillDataFromCSVandLoad: ", ex);
			} finally {
				log.debug("PDB summarydb BillData table loading scheduler completed");
				isBillDataLoadSchedulerRunning = false;
			}
		}, "PULL-BILLDATA-SCHEDULER").start();
	}
	
	private void moveProcessedFileToArchive(String fileName, String fileReadLocation) {
		String processedfileLocation = fileReadLocation + "/" + "processed";
		Session session = null;
		ChannelSftp sftpChannel = null;
		try {
			session = getSSHSession();
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();

			sftpChannel.cd(fileReadLocation);
			if (sftpChannel.get(fileName) != null){
				sftpChannel.rename(fileReadLocation + "/" + fileName , processedfileLocation + "/" + fileName);
				sftpChannel.cd(fileReadLocation);
			}
		} catch(Exception e) {
			log.error("Error", e);
		} finally {
			if (sftpChannel != null) {
				sftpChannel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	private String formattedCurrentDateTime() {
		return DateTimeFormatter.ofPattern(DATE_FORMAT).format(LocalDateTime.now());
	}
	
	@SchedulerContainingRequest(name="PDB Pull CustomerData Schedular", value="pdbSummaryScheduler_pullCustomerDataFromCSVandLoad")
	public void pullCustomerDataFromCSVandLoad() {
		if (isCustomerDataLoadSchedulerRunning) {
			log.debug("There is already an instance is running for PDB summarydb CustomerData processing.");
			return;
		}
		isCustomerDataLoadSchedulerRunning = true;
		new Thread(() -> {
			log.debug("PDB summarydb CustomerMasterData table loading scheduler start");

			try {
				String masterDatafileNamePrefix = "PDB_Customer_Data_";
				String masterContent = null;

				List<String> masterDataFileNames = pullFilesForProcessing(fileReadLocation, masterDatafileNamePrefix);
				if (masterDataFileNames.size() == 0) {
					log.debug("No file to process for bill data");
					return;
				}
				for (String masterDataFileName : masterDataFileNames) {
					int retryCount = 0;
					while (retryCount < 5) {
						try {
							log.debug("Processing customer file: " + masterDataFileName);
							masterContent = new String(readFileContentFromSftpLocation(masterDataFileName));
							break;
						} catch (Exception e) {
							if (++retryCount == 5) {
								log.error("", e);
							}
						}
					}

					if (masterContent != null) {
						List<PDBCustomerMasterData> masterDataList = prepareMasterDataList(masterContent);
						int records = insertMasterDataList(masterDataList);
						moveProcessedFileToArchive(masterDataFileName, fileReadLocation);
						log.debug("No of records proceed " + records + " for file " + masterDataFileName);
					}
				}
			} catch (Exception ex) {
				log.error("Exception in pullCustomerDataFromCSVandLoad: ", ex);
			} finally {
				log.debug("PDB summarydb CustomerMasterData table loading scheduler completed");
				isCustomerDataLoadSchedulerRunning = false;
			}
		}, "PULL-CUSTOMERDATA-SCHEDULER").start();
	}

	private void addBillDataInBatch(PDBBillData billData, NamedParameterStatement mergeStatement) throws SQLException {
		mergeStatement.setString("account", billData.ACCOUNT_NUMBER);
		mergeStatement.setString("bill", billData.BILL_NUMBER);
		mergeStatement.setDate("p1", new java.sql.Date(billData.GENERATED_DATE.getTime()));
		mergeStatement.setString("p2", billData.BILL_STATUS == null ? "N" : billData.BILL_STATUS);
		mergeStatement.setDate("p3", new java.sql.Date(billData.BILL_DUE_DATE.getTime()));
		if (billData.BILLAMT_AFTERDUEDATE == null || billData.BILLAMT_AFTERDUEDATE.equals("")) {
			mergeStatement.setDouble("p4", 0.0);
		} else {
			mergeStatement.setDouble("p4", billData.BILLAMT_AFTERDUEDATE);
		}
		if (billData.BILLENDDATE_FORPAYMENT == null || billData.BILLENDDATE_FORPAYMENT.equals("")) {
			mergeStatement.setDate("p5", null);
		} else {
			mergeStatement.setDate("p5", new java.sql.Date(billData.BILLENDDATE_FORPAYMENT.getTime()));
		}
		mergeStatement.setDate("p6", new java.sql.Date(System.currentTimeMillis()));
		mergeStatement.setString("p7", billData.CREATED_BY);
		if (billData.MODIFIED_ON == null || billData.MODIFIED_ON.equals("")) {
			mergeStatement.setDate("p8", null);
		} else {
			mergeStatement.setDate("p8", new java.sql.Date(billData.MODIFIED_ON.getTime()));
		}
		mergeStatement.setString("p9", billData.MODIFIED_BY);
		mergeStatement.setString("p10", billData.NOTIFICATION_SENT_STATUS);
		mergeStatement.setDouble("p11", billData.CURRENT_PRINCIPLE);
		if (billData.CURRENT_GOVT_DUTY == null || billData.CURRENT_GOVT_DUTY.equals("")) {
			mergeStatement.setDouble("p12", 0.0);
		} else {
			mergeStatement.setDouble("p12", billData.CURRENT_GOVT_DUTY);
		}
		mergeStatement.setDouble("p13", billData.ARREAR_PRINCIPLE);
		mergeStatement.setDouble("p14", billData.ARREAR_GOVT_DUTY);
		if (billData.LATE_PAYMENT_SURCHARGE == null || billData.LATE_PAYMENT_SURCHARGE.equals("")) {
			mergeStatement.setDouble("p15", 0.0);
		} else {
			mergeStatement.setDouble("p15", billData.LATE_PAYMENT_SURCHARGE);
		}
		mergeStatement.setDouble("p16", billData.TOTAL_BILL_AMOUNT);
		mergeStatement.setString("p17", billData.LOCATION_CODE);
		mergeStatement.setString("billmonth", billData.BILL_MONTH);
		mergeStatement.setString("p18", billData.TARIFF);
		mergeStatement.setString("p19", billData.CUSTOMER_TYPE);
		if (billData.CURRENT_SURCHARGE == null || billData.CURRENT_SURCHARGE.equals("")) {
			mergeStatement.setDouble("p20", 0.0);
		} else {
			mergeStatement.setDouble("p20", billData.CURRENT_SURCHARGE);
		}
		if (billData.ARREAR_SURCHARGE == null || billData.ARREAR_SURCHARGE.equals("")) {
			mergeStatement.setDouble("p21", 0.0);
		} else {
			mergeStatement.setDouble("p21", billData.ARREAR_SURCHARGE);
		}
		if (billData.ADJUSTED_PRINCIPLE == null || billData.ADJUSTED_PRINCIPLE.equals("")) {
			mergeStatement.setDouble("p22", 0.0);
		} else {
			mergeStatement.setDouble("p22", billData.ADJUSTED_PRINCIPLE);
		}
		if (billData.ADJUSTED_GOVT_DUTY == null || billData.ADJUSTED_GOVT_DUTY.equals("")) {
			mergeStatement.setDouble("p23", 0.0);
		} else {
			mergeStatement.setDouble("p23", billData.ADJUSTED_GOVT_DUTY);
		}
		if (billData.ADVANCE_AMOUNT == null || billData.ADVANCE_AMOUNT.equals("")) {
			mergeStatement.setDouble("p24", 0.0);
		} else {
			mergeStatement.setDouble("p24", billData.ADVANCE_AMOUNT);
		}
		if (billData.ADJ_ADV_GOVT_DUTY == null || billData.ADJ_ADV_GOVT_DUTY.equals("")) {
			mergeStatement.setDouble("p25", 0.0);
		} else {
			mergeStatement.setDouble("p25", billData.ADJ_ADV_GOVT_DUTY);
		}
		mergeStatement.addBatch();
	}

	private void insertBillDataList(String fileContent) throws ClassNotFoundException, SQLException {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		File file = new File(errorFileLocation, "SKIPPED_FILE_" + new SimpleDateFormat("yy_MM_dd_HH_mm_ss").format(new Date()) + ".csv");
		FileWriter[] errorWriter = new FileWriter[] {null};
		Consumer<String> errorFileWriter = line -> {
			if(!file.exists()) {
				try {
					file.createNewFile();
					errorWriter[0] = new FileWriter(file);
				} catch (IOException e) {
					log.error("Issues in error file creation: " + line, e);
				}
			}
			if(errorWriter[0] != null) {
				try {
					errorWriter[0].write(line + "\n");
				} catch (IOException e) {
					log.error("Issues in error file writing: " + line, e);
				}
			}
		};
		try (Connection con = DriverManager.getConnection(connUrl, summaryDBUsername, summaryDBPassword)) {
			String mergeQuery = "MERGE INTO UTILITY_BILL_PDB pdb USING (select 'BPDB' COMPANY_CODE, :account ACCOUNT_NUMBER, :bill BILL_NUMBER, :billmonth BILL_MONTH from dual) ref ON (pdb.COMPANY_CODE = ref.COMPANY_CODE and pdb.ACCOUNT_NUMBER = ref.ACCOUNT_NUMBER and pdb.BILL_NUMBER = ref.BILL_NUMBER and pdb.BILL_MONTH = ref.BILL_MONTH) WHEN MATCHED THEN UPDATE SET GENERATED_DATE = :p1, BILL_STATUS = :p2, BILL_DUE_DATE = :p3, BILLAMT_AFTERDUEDATE = :p4, BILLENDDATE_FORPAYMENT = :p5, CREATED_ON = :p6, CREATED_BY = :p7, MODIFIED_ON = :p8, MODIFIED_BY = :p9, NOTIFICATION_SENT_STATUS = :p10, CURRENT_PRINCIPLE = :p11, CURRENT_GOVT_DUTY = :p12, ARREAR_PRINCIPLE = :p13, ARREAR_GOVT_DUTY = :p14, LATE_PAYMENT_SURCHARGE = :p15, TOTAL_BILL_AMOUNT = :p16, LOCATION_CODE = :p17, TARIFF = :p18, CUSTOMER_TYPE = :p19, CURRENT_SURCHARGE = :p20, ARREAR_SURCHARGE = :p21, ADJUSTED_PRINCIPLE = :p22, ADJUSTED_GOVT_DUTY = :p23, ADVANCE_AMOUNT = :p24, ADJ_ADV_GOVT_DUTY = :p25 WHEN NOT MATCHED THEN INSERT (COMPANY_CODE, ACCOUNT_NUMBER, BILL_NUMBER, GENERATED_DATE, BILL_STATUS, BILL_DUE_DATE, BILLAMT_AFTERDUEDATE, BILLENDDATE_FORPAYMENT, CREATED_ON, CREATED_BY, MODIFIED_ON, MODIFIED_BY, NOTIFICATION_SENT_STATUS, CURRENT_PRINCIPLE, CURRENT_GOVT_DUTY, ARREAR_PRINCIPLE, ARREAR_GOVT_DUTY, LATE_PAYMENT_SURCHARGE, TOTAL_BILL_AMOUNT, LOCATION_CODE, BILL_MONTH, TARIFF, CUSTOMER_TYPE, CURRENT_SURCHARGE, ARREAR_SURCHARGE, ADJUSTED_PRINCIPLE, ADJUSTED_GOVT_DUTY, ADVANCE_AMOUNT, ADJ_ADV_GOVT_DUTY) VALUES ('BPDB', :account, :bill, :p1, :p2, :p3, :p4, :p5, :p6, :p7, :p8, :p9, :p10, :p11, :p12, :p13, :p14, :p15, :p16, :p17, :billmonth, :p18, :p19, :p20, :p21, :p22, :p23, :p24, :p25)";
			int index = 0;
			int skipCount = 0;
			try (NamedParameterStatement mergeStatement = new NamedParameterStatement(con, mergeQuery)) {
				SequentialCsvParser billArrays = new SequentialCsvParser(fileContent);
				String[] cols = null;
				int noOfRecordsInCurrentBatch = 0;
				String[][] rowsInBatch = new String[BILL_DATA_BATCH_SIZE][];
				int[] repeatLoopIndex = new int[]{-1}; //to track whether now repeating loop after getting failure
				String[][] batchToRepeat = null;
				billArrays.next(); // to skip first header row
				while(true) {
					if (noOfRecordsInCurrentBatch == BILL_DATA_BATCH_SIZE || (cols = getNextLine(batchToRepeat, repeatLoopIndex, billArrays)) == null) {
						try {
							mergeStatement.executeBatch();
						} catch(BatchUpdateException ex) {
							int processed = ex.getUpdateCounts().length;
							mergeStatement.clearBatch();
							String[] failedCols = rowsInBatch[processed];
							String failedLine = StringUtil.joinNullSafe(",", failedCols);
							log.error("Exception occurred in billdata processing for: " + failedLine, ex);
							errorFileWriter.accept("--," + failedLine + ",EXECUTION_FAILURE");
							skipCount++;
							repeatLoopIndex[0] = processed + 1;
							cols = getNextLine(batchToRepeat = rowsInBatch, repeatLoopIndex, billArrays);
							rowsInBatch = new String[BILL_DATA_BATCH_SIZE][];
							int entriesToReduce = noOfRecordsInCurrentBatch - processed;
							index -= entriesToReduce - 1;
						} finally {
							noOfRecordsInCurrentBatch = 0;
						}
					}
					if(cols == null) {
						return;
					}

					index++;
					if (cols.length != 29) {
						skipCount++;
						errorFileWriter.accept(index + "," + StringUtil.joinNullSafe(",", cols) + ",INSUFFICIENT_COLUMN");
						continue;
					}
					PDBBillData billData = null;
					try {
						billData = prepareBillData(cols);
					} catch(Throwable e) {
						log.error("Bill Data error for record:: " + StringUtil.joinNullSafe(",", cols), e);
					}
					if(billData == null) {
						skipCount++;
						errorFileWriter.accept(index + "," + StringUtil.joinNullSafe(",", cols) + ",PROCESSING_EXCEPTION");
						continue;
					}
					try {
						addBillDataInBatch(billData, mergeStatement);
						rowsInBatch[noOfRecordsInCurrentBatch++ % BILL_DATA_BATCH_SIZE] = cols;
					} catch (Throwable t) {
						skipCount++;
						errorFileWriter.accept(index + "," + StringUtil.joinNullSafe(",", cols) + ",PROCESSING_EXCEPTION");
						log.error("Bill Data error for record:: " + StringUtil.joinNullSafe(",", cols), t);
					}
				}
			} catch (Throwable ex) {
				log.error("Exception occurred in insertBillData()", ex);
				if (skipCount == 0) {
					sendSmsNotification("PDB Summary BillData Scheduler: There is an issue in the Data or Data execution, Please check the logs for further details.");
				}
			} finally {
				if(errorWriter[0] != null) {
					try {
						errorWriter[0].close();
					} catch (IOException e) {
					}
				}
				log.debug("Total records traversed: " + index + ", with failed records: " + skipCount);
				if (skipCount > 0) {
					sendSmsNotification("PDB Summary BillData Scheduler: There is an issue in the Data or Data execution, Please check the error file: "+file.getName() +" for the records causing issue and problem.");
				}
			}
		}
	}

	private String[] getNextLine(String[][] rowsInBatch, int[] repeatLoopIndex, SequentialCsvParser parser) {
		return repeatLoopIndex[0] == -1 || rowsInBatch.length <= repeatLoopIndex[0] ? parser.next() : rowsInBatch[repeatLoopIndex[0]++];
	}

	private int insertMasterDataList(List<PDBCustomerMasterData> masterDataList) throws ClassNotFoundException, SQLException {
		int noOfRecords = 0;
		Class.forName("oracle.jdbc.driver.OracleDriver");
		Connection con = DriverManager.getConnection(connUrl, summaryDBUsername, summaryDBPassword);
		String insertQuery = "INSERT INTO CUSTOMER_MASTER_DATA_PDB(COMPANY_CODE, ACCOUNT_NO, NAME, LOCATION_CODE, AREAR_CODE, ACTIVATION_DATE, STATUS, CREATED_ON, CREATED_BY, MODIFIED_ON, MODIFIED_BY, DUMPING_FOR) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement insertpstmt = con.prepareStatement(insertQuery);
		try {
			for (PDBCustomerMasterData data : masterDataList) {
				insertpstmt.setString(1, data.COMPANY_CODE);
				insertpstmt.setString(2, data.ACCOUNT_NO);
				insertpstmt.setString(3, data.NAME);
				insertpstmt.setString(4, data.LOCATION_CODE);
				insertpstmt.setString(5, data.AREAR_CODE);
				insertpstmt.setDate(6, data.ACTIVATION_DATE == null ? null : new java.sql.Date(data.ACTIVATION_DATE.getTime()));
				insertpstmt.setString(7, data.STATUS);
				insertpstmt.setDate(8, data.CREATED_ON == null ? null : new java.sql.Date(data.CREATED_ON.getTime()));
				insertpstmt.setString(9, data.CREATED_BY);
				insertpstmt.setDate(10, data.MODIFIED_ON == null ? null : new java.sql.Date(data.MODIFIED_ON.getTime()));
				insertpstmt.setString(11, data.MODIFIED_BY);
				insertpstmt.setString(12, data.DUMPING_FOR);
				insertpstmt.addBatch();
				noOfRecords++;
			}
			insertpstmt.executeBatch();
			return noOfRecords;
		} catch (Throwable ex) {
			log.debug("No of bill records inserted: 0");
			log.error("Exception occured in insertMasterDataList():", ex);
		} finally {
			insertpstmt.close();
			con.close();
		}
		return 0;
	}

	private List<PDBCustomerMasterData> prepareMasterDataList(String masterContent) {
		List<PDBCustomerMasterData> masterDataList = new ArrayList<>();
		try {
			String[] masterArrays = masterContent.split("\r?\n|\r");
			for (String record : masterArrays) {
				if (masterArrays[0] == record) {
					continue;
				}
				String[] cols = record.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				if (cols.length != 12) {
					continue;
				}
				String accNo = "";
				try {
					accNo = cols[1];
					PDBCustomerMasterData data = new PDBCustomerMasterData();
					data.COMPANY_CODE = cols[0] == null || cols[0].equals("") ? null : cols[0];
					data.ACCOUNT_NO = cols[1] == null || cols[1].equals("") ? null : cols[1];
					data.NAME = cols[2] == null || cols[2].equals("") ? null : cols[2];
					data.LOCATION_CODE = cols[3] == null || cols[3].equals("") ? null : cols[3];
					data.AREAR_CODE = cols[4] == null || cols[4].equals("") ? null : cols[4];
					data.ACTIVATION_DATE = cols[5] == null|| cols[5].equals("") ? null : new SimpleDateFormat("dd-MMM-yy").parse(cols[5]);
					data.STATUS = cols[6] == null || cols[6].equals("") ? null : cols[6];
					data.CREATED_ON = cols[7] == null|| cols[7].equals("") ? null : new SimpleDateFormat("dd-MMM-yy").parse(cols[7]);
					data.CREATED_BY = cols[8] == null || cols[8].equals("") ? null : cols[8];
					data.MODIFIED_ON = cols[9] == null || cols[9].equals("") ? null : new SimpleDateFormat("dd-MMM-yy").parse(cols[9]);
					data.MODIFIED_BY = cols[10] == null || cols[10].equals("") ? null : cols[10];
					data.DUMPING_FOR = cols[11].equals("") ? null : cols[11];
					masterDataList.add(data);
				} catch (Exception e) {
					log.error("Data error for record with acccount number: " + accNo);
				}
			}
			return masterDataList;
		} catch (Exception ex) {
			log.error("Exception occured in prepareMasterDataList()", ex);
		}
		return null;
	}

	private PDBBillData prepareBillData(String[] cols) throws ParseException {
		SimpleDateFormat inputDateFormatter = new SimpleDateFormat("dd-MMM-yy");
		PDBBillData data = new PDBBillData();
		data.COMPANY_CODE = StringUtil.getNonEmpty(cols[0], "BPDB");
		data.ACCOUNT_NUMBER = StringUtil.getNonEmpty(cols[1]);
		data.BILL_NUMBER = StringUtil.getNonEmpty(cols[2]);
		data.GENERATED_DATE = cols[3] == null || cols[3].equals("") ? null : inputDateFormatter.parse(cols[3]);
		data.BILL_STATUS = StringUtil.getNonEmpty(cols[4]);
		data.BILL_DUE_DATE = cols[5] == null || cols[5].equals("") ? null : inputDateFormatter.parse(cols[5]);
		data.BILLAMT_AFTERDUEDATE = cols[6] == null || cols[6].equals("") ? null : Double.parseDouble(cols[6]);
		data.BILLENDDATE_FORPAYMENT = cols[7] == null || cols[7].equals("") ? null : inputDateFormatter.parse(cols[7]);
		data.CREATED_ON = cols[8] == null || cols[8].equals("") ? null : inputDateFormatter.parse(cols[8]);
		data.CREATED_BY = StringUtil.getNonEmpty(cols[9]);
		data.MODIFIED_ON = cols[10] == null || cols[10].equals("") ? null : inputDateFormatter.parse(cols[10]);
		data.MODIFIED_BY = StringUtil.getNonEmpty(cols[11]);
		data.NOTIFICATION_SENT_STATUS = StringUtil.getNonEmpty(cols[12]);
		data.CURRENT_PRINCIPLE = cols[13] == null || cols[13].equals("") ? null : Double.parseDouble(cols[13]);
		data.CURRENT_GOVT_DUTY = cols[14] == null || cols[14].equals("") ? null : Double.parseDouble(cols[14]);
		data.ARREAR_PRINCIPLE = cols[15] == null || cols[15].equals("") ? null : Double.parseDouble(cols[15]);
		data.ARREAR_GOVT_DUTY = cols[16] == null || cols[16].equals("") ? null : Double.parseDouble(cols[16]);
		data.LATE_PAYMENT_SURCHARGE = cols[17] == null || cols[17].equals("") ? null : Double.parseDouble(cols[17]);
		data.TOTAL_BILL_AMOUNT = cols[18] == null || cols[18].equals("") ? null : Double.parseDouble(cols[18]);
		data.LOCATION_CODE = StringUtil.getNonEmpty(cols[19]);
		data.BILL_MONTH = StringUtil.getNonEmpty(cols[20]);
		data.TARIFF = StringUtil.getNonEmpty(cols[21]);
		data.CUSTOMER_TYPE = StringUtil.getNonEmpty(cols[22]);
		data.CURRENT_SURCHARGE = cols[23] == null || cols[23].equals("") ? null : Double.parseDouble(cols[23]);
		data.ARREAR_SURCHARGE = cols[24] == null || cols[24].equals("") ? null : Double.parseDouble(cols[24]);
		data.ADJUSTED_PRINCIPLE = cols[25] == null || cols[25].equals("") ? null : Double.parseDouble(cols[25]);
		data.ADJUSTED_GOVT_DUTY = cols[26] == null || cols[26].equals("") ? null : Double.parseDouble(cols[26]);
		data.ADVANCE_AMOUNT = cols[27] == null || cols[27].equals("") ? null : Double.parseDouble(cols[27]);
		data.ADJ_ADV_GOVT_DUTY = cols[28] == null || cols[28].equals("") ? null : Double.parseDouble(cols[28]);
		//Note: Skipping the records which has required fields as null or empty
		if (data.ACCOUNT_NUMBER == null || data.BILL_NUMBER == null || data.GENERATED_DATE == null || data.BILL_DUE_DATE == null || data.CREATED_BY == null
				|| data.CURRENT_PRINCIPLE == null || data.ARREAR_PRINCIPLE == null || data.ARREAR_GOVT_DUTY == null || data.TOTAL_BILL_AMOUNT == null) {
			log.error("Bill Data required fields null for record:: " + StringUtil.joinNullSafe(",", cols));
			return null;
		}
		return data;
	}

	private String createMasterDataCsv(List<PDBPaidMasterData> dataList) {
		Writer writer = null;
		CSVWriter csvWriter = null;
		try {
			writer = new StringWriter();
			csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

			String[] header = { "BATCH_NO", "LOCATION_CODE", "PAY_DATE", "TOTAL_PDB_AMOUNT", "REVENUE_STAMP_AMOUNT",
					"NET_PDB_AMOUNT", "TOTAL_GOVT_DUTY", "STATUS", "CREATED_ON", "CREATED_BY", "MODIFIED_ON", "MODIFIED_BY" };

			csvWriter.writeNext(header);

			for (PDBPaidMasterData data : dataList) {
				csvWriter.writeNext(new String[] { data.BATCH_NO, data.LOCATION_CODE, data.PAY_DATE,
						data.TOTAL_PDB_AMOUNT, data.REVENUE_STAMP_AMOUNT, data.NET_PDB_AMOUNT, data.TOTAL_GOVT_DUTY,
						data.STATUS, data.CREATED_ON, data.CREATED_BY, data.MODIFIED_ON, data.MODIFIED_BY });
			}
		} catch (Exception e) {
			log.error("Unable to generate paid customermaster data csv file:: ", e);
			return null;
		} finally {
			try {
				csvWriter.close();
				writer.close();
			} catch (Exception e) {
			}
		}
		return writer.toString();
	}

	private String createBillDataCsv(List<PDBPaidBillData> dataList) {
		Writer writer = null;
		CSVWriter csvWriter = null;
		try {
			writer = new StringWriter();
			csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER,
					CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			String[] header = { "BATCH_NO", "BILL_NUMBER", "BATCH_CODE", "SCROLL_NO", "TRANSACTION_ID", "PDB_AMOUNT",
					"GOVT_DUTY", "STATUS", "CREATED_ON", "CREATED_BY", "MODIFIED_NO", "MODIFIED_BY" };
			csvWriter.writeNext(header);

			for (PDBPaidBillData data : dataList) {
				csvWriter.writeNext(new String[] { data.BATCH_NO, data.BILL_NUMBER, data.BATCH_CODE, data.SCROLL_NO,
						data.TRANSACTION_ID, data.PDB_AMOUNT, data.GOVT_DUTY, data.STATUS, data.CREATED_ON,
						data.CREATED_BY, data.MODIFIED_NO, data.MODIFIED_BY });
			}
		} catch (Exception e) {
			log.error("Unable to generate paid billdata csv file:: ", e);
			return null;
		} finally {
			try {
				csvWriter.close();
				writer.close();
			} catch (Exception e) {
			}
		}
		return writer.toString();
	}

	private void pushToSftpLocation(String fileName, String fileContent, String fileLocation) {
		Session session = null;
		ChannelSftp sftpChannel = null;
		try {
			session = getSSHSession();
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			sftpChannel.cd(fileLocation);
			if (fileName != null && fileContent != null) {
				sftpChannel.put(new ByteArrayInputStream(fileContent.getBytes()), fileName);
			}
		} catch (Exception e) {
			log.error("Unable to save csv to sftp location:: ", e);
		} finally {
			sftpChannel.disconnect();
			session.disconnect();
			log.debug("Disconnect from sftp location.");
		}
	}

	private Session getSSHSession() throws JSchException {
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		JSch jsch = new JSch();
		Session session;
		int tryCount = 0;
		while(true) {
			try {
				session = jsch.getSession(sftpUser, sftpHost, sftpPort);
				session.setPassword(sftpPassword);
				session.setConfig(config);
				session.connect();
				return session;
			} catch (JSchException e) {
				if(++tryCount == 10) {
					throw e;
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException ex) {
				}
			}
		}
	}

	private byte[] readFileContentFromSftpLocation(String fileName) throws IOException, SftpException, JSchException {
		Session session = null;
		ChannelSftp sftpChannel = null;
		try {
			session = getSSHSession();
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			sftpChannel.cd(fileReadLocation);
			java.io.InputStream inputStream = sftpChannel.get(fileName);
			return inputStream.readAllBytes();
		} finally {
			if (sftpChannel != null) {
				sftpChannel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}

	private List<String> pullFilesForProcessing(String fileLocation, String fileNamePrefix) throws JSchException, SftpException, IOException, ParseException {
		Session session = null;
		ChannelSftp sftpChannel = null;
		try {
			session = getSSHSession();
			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();
			sftpChannel.cd(fileLocation);

			List<LsEntry> listOfFile = sftpChannel.ls(fileNamePrefix + "*");
			log.info("Files found for processing:: \n" + listOfFile.stream().map(LsEntry::getFilename).collect(Collectors.joining("\n")) + "\n");
			return listOfFile.stream().map(LsEntry::getFilename).collect(Collectors.toList());
		} finally {
			if (sftpChannel != null) {
				sftpChannel.disconnect();
			}
			if (session != null) {
				session.disconnect();
			}
		}
	}
	
	private void sendSmsNotification(String smsBody) {
        SmsUtil.sendSms(smsToSend, smsBody, smsBody, true); 
    }
}